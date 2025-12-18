import * as admin from 'firebase-admin';
import * as functions from 'firebase-functions';
import {COLLECTIONS} from './utils/constants';

type NotificationSettings = {
  pushNotifications?: boolean;
  emailNotifications?: boolean;
  smsNotifications?: boolean;
};

type UserLocation = {
  latitude?: number;
  longitude?: number;
};

type FirestoreUser = {
  email?: string;
  phone?: string;
  preferredCategories?: string[];
  location?: UserLocation;
  notificationSettings?: NotificationSettings;
  services?: Array<{category?: string}>;
} & Record<string, unknown>;

type NotificationPayload = Record<string, string | number | boolean | undefined>;

/**
 * Algoritmo de notificações graduais baseado em perfil e preferências do usuário
 * Envia notificações sobre produtos, ordens e prestadores próximos
 */
export const sendGradualNotifications = functions.pubsub
  .schedule('every 6 hours')
  .timeZone('America/Sao_Paulo')
  .onRun(async (_context) => {
    const db = admin.firestore();
    
    try {
      // Buscar todos os usuários ativos
      const usersSnapshot = await db.collection(COLLECTIONS.USERS)
        .where('active', '==', true)
        .limit(100) // Processar em lotes
        .get();
      
      const notificationPromises: Promise<void>[] = [];
      
      for (const userDoc of usersSnapshot.docs) {
        const user = userDoc.data() as FirestoreUser;
        const userId = userDoc.id;
        
        // Verificar preferências de notificação
        const notificationSettings: NotificationSettings = user.notificationSettings || {};
        if (!notificationSettings.pushNotifications && 
            !notificationSettings.emailNotifications && 
            !notificationSettings.smsNotifications) {
          continue; // Usuário desabilitou todas as notificações
        }
        
        // Obter preferências do usuário
        const preferredCategories = user.preferredCategories || [];
        const userLocation = user.location || {};
        const userLat = userLocation.latitude;
        const userLng = userLocation.longitude;
        
        // 1. Notificações sobre produtos com desconto nas categorias preferidas
        if (preferredCategories.length > 0) {
          const productNotifications = await getProductNotifications(
            db, userId, preferredCategories, notificationSettings, user
          );
          notificationPromises.push(...productNotifications);
        }
        
        // 2. Notificações sobre novas ordens de serviço na região
        if (userLat && userLng) {
          const orderNotifications = await getNearbyOrderNotifications(
            db, userId, userLat, userLng, preferredCategories, notificationSettings, user
          );
          notificationPromises.push(...orderNotifications);
        }
        
        // 3. Notificações sobre prestadores próximos nas categorias preferidas
        if (userLat && userLng && preferredCategories.length > 0) {
          const providerNotifications = await getNearbyProviderNotifications(
            db, userId, userLat, userLng, preferredCategories, notificationSettings, user
          );
          notificationPromises.push(...providerNotifications);
        }
      }
      
      // Executar todas as notificações
      const results = await Promise.allSettled(notificationPromises);
      const successCount = results.filter(r => r.status === 'fulfilled').length;
      const failureCount = results.filter(r => r.status === 'rejected').length;
      
      functions.logger.info(
        `Gradual notifications sent: ${successCount} success, ${failureCount} failures`
      );
      
      return null;
    } catch (error) {
      functions.logger.error('Error in gradual notifications:', error);
      return null;
    }
  });

/**
 * Busca produtos com desconto nas categorias preferidas do usuário
 */
async function getProductNotifications(
  db: admin.firestore.Firestore,
  userId: string,
  preferredCategories: string[],
  notificationSettings: NotificationSettings,
  user: FirestoreUser
): Promise<Promise<void>[]> {
  const notifications: Promise<void>[] = [];
  
  try {
    // Verificar última notificação de produto para este usuário
    const lastProductNotification = await db.collection(COLLECTIONS.NOTIFICATIONS)
      .where('userId', '==', userId)
      .where('type', '==', 'product_discount')
      .orderBy('createdAt', 'desc')
      .limit(1)
      .get();
    
    const lastNotificationTime = lastProductNotification.docs[0]?.data()?.createdAt?.toMillis() || 0;
    const oneDayAgo = Date.now() - (24 * 60 * 60 * 1000);
    
    // Não enviar se já foi enviada nas últimas 24h
    if (lastNotificationTime > oneDayAgo) {
      return notifications;
    }
    
      // Buscar produtos com desconto nas categorias preferidas
      for (const category of preferredCategories.slice(0, 3)) { // Limitar a 3 categorias
        const productsSnapshot = await db.collection('products')
        .where('category', '==', category)
        .where('discount', '>', 0)
        .where('active', '==', true)
        .limit(5)
        .get();
      
      if (!productsSnapshot.empty) {
        const product = productsSnapshot.docs[0].data();
        const productId = productsSnapshot.docs[0].id;
        
        // Criar notificação
        const notificationData = {
          userId,
          type: 'product_discount',
          title: 'Produto com Desconto!',
          message: `${product.title} está com ${product.discount}% de desconto`,
          data: {
            productId,
            category,
            discount: product.discount
          },
          read: false,
          createdAt: admin.firestore.FieldValue.serverTimestamp()
        };
        
        // Salvar notificação
        const notificationPromise = db.collection(COLLECTIONS.NOTIFICATIONS)
          .add(notificationData)
          .then(async () => {
            // Enviar push notification
            if (notificationSettings.pushNotifications) {
              await sendPushNotification(
                db,
                userId,
                notificationData.title,
                notificationData.message,
                notificationData.data
              );
            }
            
            // Enviar email
            if (notificationSettings.emailNotifications && user.email) {
              await sendEmailNotification(user.email, notificationData.title, notificationData.message);
            }
            
            // Enviar SMS (se configurado)
            if (notificationSettings.smsNotifications && user.phone) {
              await sendSMSNotification(user.phone, notificationData.message);
            }
          });
        
        notifications.push(notificationPromise);
        break; // Enviar apenas uma notificação por execução
      }
    }
  } catch (error) {
    functions.logger.error(`Error getting product notifications for user ${userId}:`, error);
  }
  
  return notifications;
}

/**
 * Busca ordens de serviço próximas ao usuário
 */
async function getNearbyOrderNotifications(
  db: admin.firestore.Firestore,
  userId: string,
  userLat: number,
  userLng: number,
  preferredCategories: string[],
  notificationSettings: NotificationSettings,
  user: FirestoreUser
): Promise<Promise<void>[]> {
  const notifications: Promise<void>[] = [];
  
  try {
    // Verificar última notificação de ordem para este usuário
    const lastOrderNotification = await db.collection(COLLECTIONS.NOTIFICATIONS)
      .where('userId', '==', userId)
      .where('type', '==', 'nearby_order')
      .orderBy('createdAt', 'desc')
      .limit(1)
      .get();
    
    const lastNotificationTime = lastOrderNotification.docs[0]?.data()?.createdAt?.toMillis() || 0;
    const sixHoursAgo = Date.now() - (6 * 60 * 60 * 1000);
    
    // Não enviar se já foi enviada nas últimas 6h
    if (lastNotificationTime > sixHoursAgo) {
      return notifications;
    }
    
    // Buscar ordens recentes (últimas 24h) nas categorias preferidas
    const oneDayAgo = admin.firestore.Timestamp.fromMillis(Date.now() - (24 * 60 * 60 * 1000));
    
    for (const category of preferredCategories.slice(0, 2)) {
      const ordersSnapshot = await db.collection(COLLECTIONS.ORDERS)
        .where('category', '==', category)
        .where('status', '==', 'pending')
        .where('createdAt', '>=', oneDayAgo)
        .limit(10)
        .get();
      
      // Filtrar por proximidade (raio de 50km)
      for (const orderDoc of ordersSnapshot.docs) {
        const order = orderDoc.data();
        const orderLocation = order.location || {};
        const orderLat = orderLocation.latitude;
        const orderLng = orderLocation.longitude;
        
        if (orderLat && orderLng) {
          const distance = calculateDistance(userLat, userLng, orderLat, orderLng);
          
          if (distance <= 50) { // Dentro de 50km
            const notificationData = {
              userId,
              type: 'nearby_order',
              title: 'Nova Ordem Próxima!',
              message: `Há uma nova ordem de ${category} a ${Math.round(distance)}km de você`,
              data: {
                orderId: orderDoc.id,
                category,
                distance: Math.round(distance)
              },
              read: false,
              createdAt: admin.firestore.FieldValue.serverTimestamp()
            };
            
            const notificationPromise = db.collection(COLLECTIONS.NOTIFICATIONS)
              .add(notificationData)
              .then(async () => {
                if (notificationSettings.pushNotifications) {
                  await sendPushNotification(
                    db,
                    userId,
                    notificationData.title,
                    notificationData.message,
                    notificationData.data
                  );
                }
                if (notificationSettings.emailNotifications && user.email) {
                  await sendEmailNotification(
                    user.email,
                    notificationData.title,
                    notificationData.message
                  );
                }
                if (notificationSettings.smsNotifications && user.phone) {
                  await sendSMSNotification(user.phone, notificationData.message);
                }
              });
            
            notifications.push(notificationPromise);
            break; // Enviar apenas uma notificação por execução
          }
        }
      }
    }
  } catch (error) {
    functions.logger.error(`Error getting nearby order notifications for user ${userId}:`, error);
  }
  
  return notifications;
}

/**
 * Busca prestadores próximos nas categorias preferidas
 */
async function getNearbyProviderNotifications(
  db: admin.firestore.Firestore,
  userId: string,
  userLat: number,
  userLng: number,
  preferredCategories: string[],
  notificationSettings: NotificationSettings,
  user: FirestoreUser
): Promise<Promise<void>[]> {
  const notifications: Promise<void>[] = [];
  
  try {
    // Verificar última notificação de prestador para este usuário
    const lastProviderNotification = await db.collection(COLLECTIONS.NOTIFICATIONS)
      .where('userId', '==', userId)
      .where('type', '==', 'nearby_provider')
      .orderBy('createdAt', 'desc')
      .limit(1)
      .get();
    
    const lastNotificationTime = lastProviderNotification.docs[0]?.data()?.createdAt?.toMillis() || 0;
    const threeDaysAgo = Date.now() - (3 * 24 * 60 * 60 * 1000);
    
    // Não enviar se já foi enviada nos últimos 3 dias
    if (lastNotificationTime > threeDaysAgo) {
      return notifications;
    }
    
    // Buscar prestadores nas categorias preferidas
    for (const category of preferredCategories.slice(0, 2)) {
      const providersSnapshot = await db.collection(COLLECTIONS.USERS)
        .where('role', '==', 'provider')
        .where('active', '==', true)
        .limit(20)
        .get();
      
      // Filtrar por categoria e proximidade
      for (const providerDoc of providersSnapshot.docs) {
        const provider = providerDoc.data() as FirestoreUser;
        const providerServices = (provider.services || []) as Array<{category?: string}>;
        const hasCategory = providerServices.some((s) => s.category === category);
        
        if (!hasCategory) continue;
        
        const providerLocation = provider.location || {};
        const providerLat = providerLocation.latitude;
        const providerLng = providerLocation.longitude;
        
        if (providerLat && providerLng) {
          const distance = calculateDistance(userLat, userLng, providerLat, providerLng);
          
          if (distance <= 30) { // Dentro de 30km
            const notificationData = {
              userId,
              type: 'nearby_provider',
              title: 'Prestador Próximo!',
              message: `Há um prestador de ${category} a ${Math.round(distance)}km de você`,
              data: {
                providerId: providerDoc.id,
                category,
                distance: Math.round(distance)
              },
              read: false,
              createdAt: admin.firestore.FieldValue.serverTimestamp()
            };
            
            const notificationPromise = db.collection(COLLECTIONS.NOTIFICATIONS)
              .add(notificationData)
              .then(async () => {
                if (notificationSettings.pushNotifications) {
                  await sendPushNotification(
                    db,
                    userId,
                    notificationData.title,
                    notificationData.message,
                    notificationData.data
                  );
                }
                if (notificationSettings.emailNotifications && user.email) {
                  await sendEmailNotification(
                    user.email,
                    notificationData.title,
                    notificationData.message
                  );
                }
                if (notificationSettings.smsNotifications && user.phone) {
                  await sendSMSNotification(user.phone, notificationData.message);
                }
              });
            
            notifications.push(notificationPromise);
            break; // Enviar apenas uma notificação por execução
          }
        }
      }
    }
  } catch (error) {
    functions.logger.error(`Error getting nearby provider notifications for user ${userId}:`, error);
  }
  
  return notifications;
}

/**
 * Calcula distância entre duas coordenadas (Haversine)
 */
function calculateDistance(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const R = 6371; // Raio da Terra em km
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLon = (lon2 - lon1) * Math.PI / 180;
  const a = 
    Math.sin(dLat/2) * Math.sin(dLat/2) +
    Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
    Math.sin(dLon/2) * Math.sin(dLon/2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
  return R * c;
}

/**
 * Envia push notification
 */
async function sendPushNotification(
  db: admin.firestore.Firestore,
  userId: string,
  title: string,
  message: string,
  data: NotificationPayload
): Promise<void> {
  try {
    const userDoc = await db.collection(COLLECTIONS.USERS).doc(userId).get();
    const fcmTokens = userDoc.data()?.fcmTokens || [];
    
    if (fcmTokens.length > 0) {
      const messages = fcmTokens.map((token: string) => ({
        notification: { title, body: message },
        data: { ...data, type: data.type || 'general' },
        token
      }));
      
      await admin.messaging().sendAll(messages);
    }
  } catch (error) {
    functions.logger.error('Error sending push notification:', error);
  }
}

/**
 * Envia email notification (usando SendGrid, Mailgun, etc.)
 */
async function sendEmailNotification(email: string, title: string, message: string): Promise<void> {
  // TODO: Implementar integração com serviço de email (SendGrid, Mailgun, etc.)
  functions.logger.info(`Email notification would be sent to ${email}: ${title} - ${message}`);
}

/**
 * Envia SMS notification (usando Twilio, etc.)
 */
async function sendSMSNotification(phone: string, message: string): Promise<void> {
  // TODO: Implementar integração com serviço de SMS (Twilio, etc.)
  functions.logger.info(`SMS notification would be sent to ${phone}: ${message}`);
}

