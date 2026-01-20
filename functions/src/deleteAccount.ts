import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
import {validateAppCheck} from './security/appCheck';
import {getFirestore} from './utils/firestore';
import {getUserLocation, normalizeLocationId, getLocationCollection} from './utils/location';

const db = getFirestore();
const rtdb = admin.database();
const storage = admin.storage();

/**
 * Cloud Function para exclusão de conta do usuário
 * Atende aos requisitos do Google Play Store para exclusão de dados
 */
export const deleteUserAccount = functions.https.onCall(async (data, context) => {
  try {
    validateAppCheck(context);
    
    // Verificar autenticação
    if (!context.auth) {
      throw new functions.https.HttpsError(
        'unauthenticated',
        'Usuário não autenticado'
      );
    }

    const userId = context.auth.uid;
    functions.logger.info(`Iniciando exclusão de conta para usuário: ${userId}`);

    // 0. Obter localização do usuário ANTES de deletar o documento (necessário para deletar dados por localização)
    const userRef = db.collection('users').doc(userId);
    const userDoc = await userRef.get();
    let userLocation: {city: string; state: string} | null = null;
    
    if (userDoc.exists) {
      userLocation = await getUserLocation(db, userId);
      functions.logger.info(`Localização do usuário obtida: ${userLocation.city}, ${userLocation.state}`);
    }

    // 1. Deletar dados do Firestore
    
    // 1.1. Deletar TODAS as subcoleções do usuário (isolamento total de dados)
    const subcollections = [
      'services',
      'products',
      'orders',
      'purchase_orders',
      'reviews',
      'notifications',
      'conversations',
      'preferences',
      'settings',
      'stories' // Adicionar stories nas subcoleções
    ];
    
    functions.logger.info(`Deletando subcoleções do usuário ${userId}`);
    for (const subcollection of subcollections) {
      try {
        const subcollectionRef = userRef.collection(subcollection);
        const snapshot = await subcollectionRef.get();
        
        if (!snapshot.empty) {
          // Para conversas, também deletar mensagens (subcoleção)
          if (subcollection === 'conversations') {
            for (const doc of snapshot.docs) {
              const messagesRef = doc.ref.collection('messages');
              const messagesSnapshot = await messagesRef.get();
              if (!messagesSnapshot.empty) {
                const messagesBatch = db.batch();
                messagesSnapshot.forEach((msgDoc: admin.firestore.QueryDocumentSnapshot) => {
                  messagesBatch.delete(msgDoc.ref);
                });
                await messagesBatch.commit();
                functions.logger.info(`Mensagens da conversa ${doc.id} deletadas: ${messagesSnapshot.size} mensagens`);
              }
            }
          }
          
          const batch = db.batch();
          snapshot.forEach((doc: admin.firestore.QueryDocumentSnapshot) => {
            batch.delete(doc.ref);
          });
          await batch.commit();
          functions.logger.info(`Subcoleção ${subcollection} deletada: ${snapshot.size} documentos`);
        }
      } catch (error: unknown) {
        const errorMessage = error instanceof Error ? error.message : 'Erro desconhecido';
        functions.logger.warn(`Erro ao deletar subcoleção ${subcollection}: ${errorMessage}`);
        // Continuar mesmo se houver erro em uma subcoleção
      }
    }
    
    // 1.2. Deletar dados das coleções públicas (para limpeza completa)
    const batch = db.batch();

    // Deletar perfil do usuário
    batch.delete(userRef);

    // Deletar produtos do usuário (coleção pública)
    const productsSnapshot = await db.collection('products')
      .where('sellerId', '==', userId)
      .get();
    productsSnapshot.forEach((doc: admin.firestore.QueryDocumentSnapshot) => batch.delete(doc.ref));

    // Deletar serviços do usuário (coleção pública)
    const servicesSnapshot = await db.collection('services')
      .where('providerId', '==', userId)
      .get();
    servicesSnapshot.forEach((doc: admin.firestore.QueryDocumentSnapshot) => batch.delete(doc.ref));

    // Deletar pedidos relacionados (coleção pública)
    const ordersSnapshot = await db.collection('orders')
      .where('clientId', '==', userId)
      .get();
    ordersSnapshot.forEach((doc: admin.firestore.QueryDocumentSnapshot) => batch.delete(doc.ref));

    const providerOrdersSnapshot = await db.collection('orders')
      .where('providerId', '==', userId)
      .get();
    providerOrdersSnapshot.forEach((doc: admin.firestore.QueryDocumentSnapshot) => batch.delete(doc.ref));

    // Deletar pedidos de compra (coleção pública)
    const purchaseOrdersSnapshot = await db.collection('purchase_orders')
      .where('clientId', '==', userId)
      .get();
    purchaseOrdersSnapshot.forEach((doc: admin.firestore.QueryDocumentSnapshot) => batch.delete(doc.ref));

    const sellerOrdersSnapshot = await db.collection('purchase_orders')
      .where('sellerId', '==', userId)
      .get();
    sellerOrdersSnapshot.forEach((doc: admin.firestore.QueryDocumentSnapshot) => batch.delete(doc.ref));

    // Deletar avaliações (coleção pública)
    const reviewsSnapshot = await db.collection('reviews')
      .where('clientId', '==', userId)
      .get();
    reviewsSnapshot.forEach((doc: admin.firestore.QueryDocumentSnapshot) => batch.delete(doc.ref));

    // Deletar notificações (coleção pública)
    const notificationsSnapshot = await db.collection('notifications')
      .where('userId', '==', userId)
      .get();
    notificationsSnapshot.forEach((doc: admin.firestore.QueryDocumentSnapshot) => batch.delete(doc.ref));

    // Deletar conversas (coleção pública)
    const conversationsSnapshot = await db.collection('conversations')
      .where('userId', '==', userId)
      .get();
    conversationsSnapshot.forEach((doc: admin.firestore.QueryDocumentSnapshot) => batch.delete(doc.ref));

    // Deletar posts do usuário (coleção pública)
    const postsSnapshot = await db.collection('posts')
      .where('userId', '==', userId)
      .get();
    postsSnapshot.forEach((doc: admin.firestore.QueryDocumentSnapshot) => batch.delete(doc.ref));

    // Deletar stories do usuário (coleção pública)
    const storiesSnapshot = await db.collection('stories')
      .where('userId', '==', userId)
      .get();
    storiesSnapshot.forEach((doc: admin.firestore.QueryDocumentSnapshot) => batch.delete(doc.ref));

    // Executar batch delete
    await batch.commit();
    functions.logger.info(`Dados do Firestore deletados para usuário: ${userId}`);

    // 1.3. Deletar dados em coleções por localização
    try {
      // Usar localização obtida anteriormente (antes de deletar o documento)
      if (userLocation && userLocation.city && userLocation.state) {
        const {city, state} = userLocation;
        const locationId = normalizeLocationId(city, state);
        functions.logger.info(`Deletando dados do usuário em localização: ${locationId}`);
        
        // Deletar produtos em coleções por localização
        const locationProductsRef = getLocationCollection(db, 'products', city, state);
        const locationProductsSnapshot = await locationProductsRef
          .where('sellerId', '==', userId)
          .get();
        
        if (!locationProductsSnapshot.empty) {
          const locationProductsBatch = db.batch();
          locationProductsSnapshot.forEach((doc: admin.firestore.QueryDocumentSnapshot) => {
            locationProductsBatch.delete(doc.ref);
          });
          await locationProductsBatch.commit();
          functions.logger.info(
            `Produtos deletados de locations/${locationId}/products: ` +
            `${locationProductsSnapshot.size} produtos`
          );
        }
        
        // Deletar stories em coleções por localização
        const locationStoriesRef = getLocationCollection(db, 'stories', city, state);
        const locationStoriesSnapshot = await locationStoriesRef
          .where('userId', '==', userId)
          .get();
        
        if (!locationStoriesSnapshot.empty) {
          const locationStoriesBatch = db.batch();
          locationStoriesSnapshot.forEach((doc: admin.firestore.QueryDocumentSnapshot) => {
            locationStoriesBatch.delete(doc.ref);
          });
          await locationStoriesBatch.commit();
          functions.logger.info(
            `Stories deletadas de locations/${locationId}/stories: ` +
            `${locationStoriesSnapshot.size} stories`
          );
        }
        
        // Deletar ordens em coleções por localização
        const locationOrdersRef = getLocationCollection(db, 'orders', city, state);
        const locationOrdersClientSnapshot = await locationOrdersRef
          .where('clientId', '==', userId)
          .get();
        const locationOrdersProviderSnapshot = await locationOrdersRef
          .where('providerId', '==', userId)
          .get();
        
        if (!locationOrdersClientSnapshot.empty || !locationOrdersProviderSnapshot.empty) {
          const locationOrdersBatch = db.batch();
          locationOrdersClientSnapshot.forEach((doc: admin.firestore.QueryDocumentSnapshot) => {
            locationOrdersBatch.delete(doc.ref);
          });
          locationOrdersProviderSnapshot.forEach((doc: admin.firestore.QueryDocumentSnapshot) => {
            locationOrdersBatch.delete(doc.ref);
          });
          await locationOrdersBatch.commit();
          const totalOrders = locationOrdersClientSnapshot.size + locationOrdersProviderSnapshot.size;
          functions.logger.info(
            `Ordens deletadas de locations/${locationId}/orders: ${totalOrders} ordens`
          );
        }
      } else {
        functions.logger.warn(
          `Localização do usuário ${userId} não disponível, ` +
          'pulando exclusão de dados por localização'
        );
      }
    } catch (error: unknown) {
      const errorMessage = error instanceof Error ? error.message : 'Erro desconhecido';
      functions.logger.warn(`Erro ao deletar dados por localização: ${errorMessage}`);
      // Continuar mesmo se houver erro
    }

    // 1.4. Deletar visualizações e analytics de stories
    try {
      // Buscar todas as stories do usuário primeiro
      // (já deletadas acima, mas precisamos dos IDs)
      const allStoriesSnapshot = await db.collection('stories')
        .where('userId', '==', userId)
        .get();
      
      const storyIds: string[] = [];
      allStoriesSnapshot.forEach((doc: admin.firestore.QueryDocumentSnapshot) => {
        storyIds.push(doc.id);
      });
      
      // Também buscar stories em coleções por localização (usar localização obtida anteriormente)
      if (userLocation && userLocation.city && userLocation.state) {
        const locationStoriesRef = getLocationCollection(db, 'stories', userLocation.city, userLocation.state);
        const locationStoriesSnapshot = await locationStoriesRef
          .where('userId', '==', userId)
          .get();
        locationStoriesSnapshot.forEach((doc: admin.firestore.QueryDocumentSnapshot) => {
          if (!storyIds.includes(doc.id)) {
            storyIds.push(doc.id);
          }
        });
      }
      
      // Deletar visualizações e analytics de cada story
      for (const storyId of storyIds) {
        try {
          // Deletar visualizações (story_views/{storyId}/views)
          const storyViewsRef = db.collection('story_views').doc(storyId);
          const viewsRef = storyViewsRef.collection('views');
          const viewsSnapshot = await viewsRef.get();
          
          if (!viewsSnapshot.empty) {
            const viewsBatch = db.batch();
            viewsSnapshot.forEach((viewDoc: admin.firestore.QueryDocumentSnapshot) => {
              viewsBatch.delete(viewDoc.ref);
            });
            await viewsBatch.commit();
          }
          
          // Deletar ações (story_views/{storyId}/actions)
          const actionsRef = storyViewsRef.collection('actions');
          const actionsSnapshot = await actionsRef.get();
          if (!actionsSnapshot.empty) {
            const actionsBatch = db.batch();
            actionsSnapshot.forEach((actionDoc: admin.firestore.QueryDocumentSnapshot) => {
              actionsBatch.delete(actionDoc.ref);
            });
            await actionsBatch.commit();
          }
          
          // Deletar interações (story_views/{storyId}/interactions)
          const interactionsRef = storyViewsRef.collection('interactions');
          const interactionsSnapshot = await interactionsRef.get();
          if (!interactionsSnapshot.empty) {
            const interactionsBatch = db.batch();
            interactionsSnapshot.forEach((interactionDoc: admin.firestore.QueryDocumentSnapshot) => {
              interactionsBatch.delete(interactionDoc.ref);
            });
            await interactionsBatch.commit();
          }
          
          // Deletar documento principal de story_views
          await storyViewsRef.delete();
        } catch (error: unknown) {
          const errorMessage = error instanceof Error ? error.message : 'Erro desconhecido';
          functions.logger.warn(`Erro ao deletar analytics da story ${storyId}: ${errorMessage}`);
        }
      }
      
      functions.logger.info(`Analytics de stories deletados: ${storyIds.length} stories processadas`);
    } catch (error: unknown) {
      const errorMessage = error instanceof Error ? error.message : 'Erro desconhecido';
      functions.logger.warn(`Erro ao deletar analytics de stories: ${errorMessage}`);
      // Continuar mesmo se houver erro
    }

    // 1.5. Deletar mensagens de conversas (subcoleção messages)
    try {
      const conversationsSnapshot = await db.collection('conversations')
        .where('userId', '==', userId)
        .get();
      
      for (const conversationDoc of conversationsSnapshot.docs) {
        const messagesRef = conversationDoc.ref.collection('messages');
        const messagesSnapshot = await messagesRef.get();
        
        if (!messagesSnapshot.empty) {
          const messagesBatch = db.batch();
          messagesSnapshot.forEach((msgDoc: admin.firestore.QueryDocumentSnapshot) => {
            messagesBatch.delete(msgDoc.ref);
          });
          await messagesBatch.commit();
          functions.logger.info(
            `Mensagens da conversa ${conversationDoc.id} deletadas: ` +
            `${messagesSnapshot.size} mensagens`
          );
        }
      }
    } catch (error: unknown) {
      const errorMessage = error instanceof Error ? error.message : 'Erro desconhecido';
      functions.logger.warn(`Erro ao deletar mensagens de conversas: ${errorMessage}`);
      // Continuar mesmo se houver erro
    }

    // 2. Deletar dados do Realtime Database
    const rtdbRef = rtdb.ref(`users/${userId}`);
    await rtdbRef.remove();
    
    const presenceRef = rtdb.ref(`presence/${userId}`);
    await presenceRef.remove();
    
    const typingRef = rtdb.ref('typing').orderByChild(userId);
    const typingSnapshot = await typingRef.once('value');
    if (typingSnapshot.exists()) {
      const typingUpdates: { [key: string]: null } = {};
      typingSnapshot.forEach(child => {
        typingUpdates[`typing/${child.key}/${userId}`] = null;
      });
      await rtdb.ref().update(typingUpdates);
    }
    
    functions.logger.info(`Dados do Realtime Database deletados para usuário: ${userId}`);

    // 3. Deletar arquivos do Storage
    const bucket = storage.bucket();
    
    // Deletar imagens de perfil
    const profilePath = `${userId}/profile/`;
    const [profileFiles] = await bucket.getFiles({ prefix: profilePath });
    await Promise.all(profileFiles.map(file => file.delete()));

    // Deletar documentos
    const documentsPath = `${userId}/documents/`;
    const [documentFiles] = await bucket.getFiles({ prefix: documentsPath });
    await Promise.all(documentFiles.map(file => file.delete()));

    // Deletar imagens de produtos
    const productsPath = `${userId}/products/`;
    const [productFiles] = await bucket.getFiles({ prefix: productsPath });
    await Promise.all(productFiles.map(file => file.delete()));

    // Deletar imagens/vídeos de serviços
    const servicesPath = `${userId}/services/`;
    const [serviceFiles] = await bucket.getFiles({ prefix: servicesPath });
    await Promise.all(serviceFiles.map(file => file.delete()));

    // Deletar stories (imagens/vídeos)
    const storiesPath = `${userId}/stories/`;
    const [storyFiles] = await bucket.getFiles({ prefix: storiesPath });
    await Promise.all(storyFiles.map(file => file.delete()));

    // Deletar posts (imagens/vídeos)
    const postsPath = `${userId}/posts/`;
    const [postFiles] = await bucket.getFiles({ prefix: postsPath });
    await Promise.all(postFiles.map(file => file.delete()));

    functions.logger.info(`Arquivos do Storage deletados para usuário: ${userId}`);

    // 4. Deletar conta de autenticação (fazer ANTES de retornar sucesso para garantir que seja deletada)
    try {
      await admin.auth().deleteUser(userId);
      functions.logger.info(`Conta de autenticação deletada para usuário: ${userId}`);
    } catch (authError: unknown) {
      const errorMessage = authError instanceof Error ? authError.message : 'Erro desconhecido';
      functions.logger.error(`Erro ao deletar conta do Auth para usuário ${userId}:`, errorMessage);
      // Continuar mesmo se houver erro - os dados já foram deletados do Firestore/Storage
    }

    return {
      success: true,
      message: 'Conta deletada com sucesso'
    };
  } catch (error: unknown) {
    // Erro já tratado acima
    const errorMessage = error instanceof Error ? error.message : 'Erro desconhecido';
    const userId = context.auth?.uid || 'unknown';
    functions.logger.error(`Erro ao deletar conta do usuário ${userId}:`, error);
    throw new functions.https.HttpsError(
      'internal',
      `Erro ao deletar conta: ${errorMessage}`
    );
  }
});

