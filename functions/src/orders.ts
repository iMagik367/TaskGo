import * as admin from 'firebase-admin';
import {getFirestore} from './utils/firestore';
import * as functions from 'firebase-functions';
import {COLLECTIONS, ORDER_STATUS} from './utils/constants';
import {assertAuthenticated, handleError} from './utils/errors';
import {validateAppCheck} from './security/appCheck';
import {getUserLocation, normalizeLocationId, validateCityAndState} from './utils/location';
import {
  ordersPath,
  servicesPath,
  getUserLocationId,
  createStandardPayload,
  createUpdatePayload,
} from './utils/firestorePaths';

type OrderDocument = {
  clientId: string;
  details: unknown;
  location: unknown;
  budget: number | null;
  dueDate: string | null;
  status: string;
  createdAt: admin.firestore.FieldValue;
  updatedAt: admin.firestore.FieldValue;
  providerId?: string | null;
  serviceId?: string;
  category?: string;
  city?: string;
  state?: string;
  locationCollection?: string;
};

/**
 * Create a new order
 * Can be created with either:
 * - serviceId: Direct order to a specific service (notifies only that provider)
 * - category: Open order for any provider in that category (notifies all matching providers)
 */
export const createOrder = functions.https.onCall(async (data, context) => {
  try {
    // Validar App Check
    validateAppCheck(context);
    
    // Verificar autenticação
    assertAuthenticated(context);
    
    const userId = context.auth!.uid;
    const db = getFirestore();
    const {serviceId, category, details, location, budget, dueDate} = data;
    
    // CRÍTICO: Apenas clientes podem criar ordens de serviço
    const userDoc = await db.collection(COLLECTIONS.USERS).doc(userId).get();
    if (!userDoc.exists) {
      throw new functions.https.HttpsError('not-found', 'User not found');
    }
    
    const userData = userDoc.data();
    const userRole = userData?.role || 'user';
    const accountType = userData?.accountType || 'CLIENTE';
    
    // Apenas clientes podem criar ordens
    if (userRole === 'partner' ||
        accountType === 'PARCEIRO' || accountType === 'PRESTADOR') {
      throw new functions.https.HttpsError(
        'permission-denied',
        'Only clients can create service orders. Partners cannot create orders.'
      );
    }
    
    functions.logger.info(`Creating order for user ${userId}`, {
      serviceId: serviceId || null,
      category: category || null,
      hasDetails: !!details,
      hasLocation: !!location,
      budget: budget || null,
      dueDate: dueDate || null,
    });

    // Either serviceId or category must be provided
    if (!serviceId && !category) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'Either serviceId or category is required'
      );
    }

    if (!details) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'Details are required'
      );
    }
    
    // CRÍTICO: Usar APENAS city/state do perfil do usuário (cadastro) - LEI MÁXIMA DO TASKGO
    // GPS (latitude/longitude) é usado APENAS para coordenadas no mapa, NÃO para determinar city/state
    let finalCity: string;
    let finalState: string;
    let locationId: string;
    
    // PRIORIDADE 1: Usar city/state enviados pelo frontend (vêm do perfil do usuário)
    if (data.city && data.state) {
      const validated = validateCityAndState(data.city, data.state);
      if (!validated.valid) {
        throw new functions.https.HttpsError(
          'invalid-argument',
          `Invalid location data: ${validated.error}`,
        );
      }
      finalCity = validated.city!;
      finalState = validated.state!;
      locationId = normalizeLocationId(finalCity, finalState);
      
      functions.logger.info('📍 createOrder: Usando city/state do perfil (enviado pelo frontend)', {
        userId,
        city: finalCity,
        state: finalState,
        locationId,
        latitude: data.latitude, // GPS apenas para coordenadas
        longitude: data.longitude, // GPS apenas para coordenadas
      });
    } else {
      // FALLBACK: Obter do perfil do usuário no Firestore (se frontend não enviou)
      functions.logger.warn('📍 createOrder: Frontend não enviou city/state, obtendo do perfil do usuário', {userId});
      const userLocation = await getUserLocation(db, userId);
      finalCity = userLocation.city;
      finalState = userLocation.state;
      
      if (!finalCity || !finalState) {
        functions.logger.warn('📍 createOrder: Usuário sem localização no perfil', {
          userId,
          city: finalCity || 'empty',
          state: finalState || 'empty',
        });
        throw new functions.https.HttpsError(
          'failed-precondition',
          'Location not available. User must have city and state in their profile.'
        );
      }

      // Obter locationId
      locationId = await getUserLocationId(db, userId);
      
      functions.logger.info('📍 createOrder: Usando city/state do perfil do Firestore', {
        userId,
        city: finalCity,
        state: finalState,
        locationId,
      });
    }
    const firestorePath = `locations/${locationId}/orders`;
    
    // CRÍTICO: Validar que city e state estão presentes e válidos
    if (!finalCity || !finalState || finalCity.trim() === '' || finalState.trim() === '') {
      const errorMsg = `User ${userId} does not have valid location information ` +
        `(city='${finalCity}', state='${finalState}'). ` +
        'Cannot create order without valid location.';
      functions.logger.error(errorMsg);
      throw new functions.https.HttpsError('failed-precondition', errorMsg);
    }
    
    // 📍 LOCATION TRACE OBRIGATÓRIO - Rastreamento de localização
    functions.logger.info('📍 LOCATION TRACE', {
      function: 'createOrder',
      userId,
      city: finalCity,
      state: finalState,
      locationId,
      firestorePath,
      rawCity: finalCity,
      rawState: finalState,
      originalLocation: location || '',
      timestamp: new Date().toISOString(),
    });

    // Criar dados do pedido usando payload padrão (orders não têm campo active)
    const orderDataRaw: Record<string, unknown> = {
      clientId: context.auth!.uid,
      details,
      location,
      city: finalCity, // Adicionar cidade explicitamente
      state: finalState, // Adicionar estado explicitamente
      locationId: locationId, // CRÍTICO: Adicionar locationId para busca eficiente (SSR, reviews, etc)
      budget: budget || null,
      dueDate: dueDate || null,
      status: ORDER_STATUS.PENDING,
    };

    const orderData = createStandardPayload(orderDataRaw, undefined); // Orders não têm campo active
    // Remover active do payload se foi adicionado
    delete (orderData as Record<string, unknown>).active;

    // If serviceId is provided, create order for specific service
    if (serviceId) {
      // Verify service exists - buscar na coleção por localização
      const locationServicesCollection = servicesPath(db, locationId);
      const serviceDoc = await locationServicesCollection.doc(serviceId).get();
      if (!serviceDoc.exists) {
        throw new functions.https.HttpsError('not-found', 'Service not found');
      }

      const service = serviceDoc.data();
      if (service?.providerId === context.auth!.uid) {
        throw new functions.https.HttpsError(
          'invalid-argument',
          'Cannot create order for your own service'
        );
      }

      orderData.providerId = service?.providerId;
      orderData.serviceId = serviceId;
      orderData.category = service?.category || category;

      // CRÍTICO: Salvar APENAS na coleção pública por localização
      const locationOrdersCollection = ordersPath(db, locationId);
      const orderRef = await locationOrdersCollection.add(orderData);
      
      // 📍 PROOF: Logar path REAL onde o dado foi gravado
      functions.logger.info('📍 BACKEND WRITE PROOF', {
        function: 'createOrder (specific service)',
        orderId: orderRef.id,
        actualFirestorePath: `locations/${locationId}/orders/${orderRef.id}`,
        collectionId: locationOrdersCollection.id,
        documentId: orderRef.id,
        timestamp: new Date().toISOString(),
      });
      
      functions.logger.info(`Order document created in Firestore: ${orderRef.id}`, {
        orderId: orderRef.id,
        clientId: userId,
        providerId: (orderData as OrderDocument).providerId || null,
        serviceId: serviceId,
        status: ORDER_STATUS.PENDING,
        location: `${finalCity}, ${finalState}`,
        locationCollection: `locations/${locationId}/orders`,
      });
      
      // Create notification for specific provider
      if (service?.providerId) {
        await db.collection(COLLECTIONS.NOTIFICATIONS).add({
          userId: service.providerId,
          orderId: orderRef.id,
          type: 'order_created',
          title: 'New Order Received',
          message: `You have a new order for ${service?.title || 'your service'}`,
          read: false,
          createdAt: admin.firestore.FieldValue.serverTimestamp(),
        });
        functions.logger.info(`Notification created for provider ${service.providerId}`);
      }

      functions.logger.info(`✅ Order created successfully for specific service: ${orderRef.id}`);
      return {orderId: orderRef.id};
    }

    // If category is provided, create open order (no providerId)
    // The onServiceOrderCreated trigger will handle notifying all matching providers
    if (category) {
      orderData.category = category;
      // No providerId - this is an open order
      orderData.providerId = null;

      // CRÍTICO: Salvar APENAS na coleção pública por localização
      const locationOrdersCollection = ordersPath(db, locationId);
      const orderRef = await locationOrdersCollection.add(orderData);
      
      // 📍 PROOF: Logar path REAL onde o dado foi gravado
      functions.logger.info('📍 BACKEND WRITE PROOF', {
        function: 'createOrder (open order)',
        orderId: orderRef.id,
        actualFirestorePath: `locations/${locationId}/orders/${orderRef.id}`,
        collectionId: locationOrdersCollection.id,
        documentId: orderRef.id,
        timestamp: new Date().toISOString(),
      });

      functions.logger.info(`✅ Open order created successfully for category ${category}: ${orderRef.id}`, {
        orderId: orderRef.id,
        clientId: userId,
        category: category,
        status: ORDER_STATUS.PENDING,
        location: `${finalCity}, ${finalState}`,
        locationCollection: `locations/${locationId}/orders`,
      });
      return {orderId: orderRef.id};
    }

    throw new functions.https.HttpsError(
      'invalid-argument',
      'Either serviceId or category must be provided'
    );
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : 'Error occurred';
    const errorStack = error instanceof Error ? error.stack : undefined;
    functions.logger.error('❌ Error creating order:', {
      error: errorMessage,
      stack: errorStack,
      userId: context.auth?.uid || '',
      data: data,
    });
    throw handleError(error);
  }
});

/**
 * Update order status
 */
export const updateOrderStatus = functions.https.onCall(async (data, context) => {
  try {
    // Validar App Check
    validateAppCheck(context);
    
    // Verificar autenticação
    assertAuthenticated(context);
    
    const db = getFirestore();
    const {orderId, status, proposalDetails} = data;

    if (!orderId || !status) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'Order ID and status are required'
      );
    }

    // Buscar pedido - precisa procurar em todas as localizações
    // Primeiro, obter localização do usuário para buscar no path correto
    const userId = context.auth!.uid;
    const locationId = await getUserLocationId(db, userId);
    
    const locationOrdersCollection = ordersPath(db, locationId);
    const orderDoc = await locationOrdersCollection.doc(orderId).get();
    
    if (!orderDoc.exists) {
      throw new functions.https.HttpsError('not-found', 'Order not found');
    }

    const order = orderDoc.data();
    const isProvider = context.auth!.uid === order?.providerId;
    const isClient = context.auth!.uid === order?.clientId;

    // Verify permissions
    if (!isProvider && !isClient) {
      throw new functions.https.HttpsError('permission-denied', 'Insufficient permissions');
    }

    // Validate status transition
    const allowedTransitions: Record<string, string[]> = {
      [ORDER_STATUS.PENDING]: [ORDER_STATUS.PROPOSED, ORDER_STATUS.CANCELLED],
      [ORDER_STATUS.PROPOSED]: [ORDER_STATUS.ACCEPTED, ORDER_STATUS.CANCELLED],
      [ORDER_STATUS.ACCEPTED]: [ORDER_STATUS.PAYMENT_PENDING, ORDER_STATUS.CANCELLED],
      [ORDER_STATUS.PAYMENT_PENDING]: [ORDER_STATUS.PAID, ORDER_STATUS.CANCELLED],
      [ORDER_STATUS.PAID]: [ORDER_STATUS.IN_PROGRESS, ORDER_STATUS.CANCELLED],
      [ORDER_STATUS.IN_PROGRESS]: [ORDER_STATUS.COMPLETED],
    };

    if (!allowedTransitions[order?.status]?.includes(status)) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        `Invalid status transition from ${order?.status} to ${status}`
      );
    }

    const updateDataRaw: Record<string, unknown> = {
      status,
    };

    if (status === ORDER_STATUS.PROPOSED && proposalDetails && isProvider) {
      updateDataRaw.proposalDetails = proposalDetails;
      updateDataRaw.proposedAt = admin.firestore.FieldValue.serverTimestamp();
    }

    if (status === ORDER_STATUS.ACCEPTED && isClient) {
      updateDataRaw.acceptedAt = admin.firestore.FieldValue.serverTimestamp();
    }

    // Criar payload de atualização padrão
    const updateData = createUpdatePayload(updateDataRaw);

    // Atualizar APENAS na coleção pública por localização
    await locationOrdersCollection.doc(orderId).update(updateData);

    // Create appropriate notification
    const notifications = [];
    if (status === ORDER_STATUS.PROPOSED) {
      notifications.push({
        userId: order?.clientId,
        orderId: orderId,
        type: 'order_accepted',
        title: 'Proposal Received',
        message: 'Provider has sent a proposal for your order',
        read: false,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
      });
    }

    if (status === ORDER_STATUS.COMPLETED) {
      notifications.push({
        userId: order?.clientId,
        orderId: orderId,
        type: 'order_completed',
        title: 'Order Completed',
        message: 'Your order has been marked as completed',
        read: false,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
      });
    }

    for (const notification of notifications) {
      await db.collection(COLLECTIONS.NOTIFICATIONS).add(notification);
    }

    functions.logger.info(`Order ${orderId} status updated to ${status}`);
    return {success: true};
  } catch (error) {
    functions.logger.error('Error updating order status:', error);
    throw handleError(error);
  }
});

/**
 * Get orders for current user
 */
export const getMyOrders = functions.https.onCall(async (data, context) => {
  try {
    // Validar App Check
    validateAppCheck(context);
    
    // Verificar autenticação
    assertAuthenticated(context);
    
    const db = getFirestore();
    const {role, status} = data;

    // CRÍTICO: Obter localização do usuário para buscar orders na coleção correta
    const userId = context.auth!.uid;
    const locationId = await getUserLocationId(db, userId);
    const locationOrdersCollection = ordersPath(db, locationId);
    
    let query: admin.firestore.Query;
    if (role === 'client') {
      query = locationOrdersCollection.where('clientId', '==', userId);
    } else if (role === 'partner') {
      query = locationOrdersCollection.where('providerId', '==', userId);
    } else {
      // Get all orders for user (both client and provider)
      const clientOrders = await locationOrdersCollection
        .where('clientId', '==', userId)
        .get();
      const providerOrders = await locationOrdersCollection
        .where('providerId', '==', userId)
        .get();
      
      const orders = [
        ...clientOrders.docs.map(doc => ({id: doc.id, ...doc.data()})),
        ...providerOrders.docs.map(doc => ({id: doc.id, ...doc.data()})),
      ];
      
      return {orders};
    }

    if (status) {
      query = query.where('status', '==', status);
    }

    const snapshot = await query.orderBy('createdAt', 'desc').get();
    const orders = snapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data(),
    }));

    return {orders};
  } catch (error) {
    functions.logger.error('Error fetching orders:', error);
    throw handleError(error);
  }
});

/**
 * Trigger: Notify providers when a new service order is created
 * This function is triggered when a new order is created in Firestore
 * It finds all providers in the same category and region, then sends push notifications
 * CRÍTICO: Trigger agora escuta em locations/{locationId}/orders/{orderId}
 */
export const onServiceOrderCreated = functions.firestore
  .document('locations/{locationId}/orders/{orderId}')
  .onCreate(async (snapshot, context) => {
    const orderId = context.params.orderId;
    const orderData = snapshot.data();
    const db = getFirestore();

    try {
      // Skip if order already has a providerId (it's a direct order to a specific provider)
      if (orderData?.providerId) {
        functions.logger.info(`Order ${orderId} already has providerId, skipping broadcast notification`);
        return null;
      }

      // Get order details
      const serviceId = orderData?.serviceId;
      const category = orderData?.category; // Category can be set directly on order
      const location = orderData?.location || '';
      const clientId = orderData?.clientId;

      let finalCategory = category;
      let serviceTitle = 'serviço';

      // If no category on order, try to get from service
      // CRÍTICO: Buscar serviço na coleção por localização
      if (!finalCategory && serviceId) {
        // Obter locationId do order (já tem city/state no orderData)
        const orderCity = orderData?.city || '';
        const orderState = orderData?.state || '';
        
        // CRÍTICO: Validar localização antes de buscar serviço
        if (!orderCity || !orderState || orderCity.trim() === '' || orderState.trim() === '') {
          functions.logger.warn(
            `Order ${orderId}: Cannot fetch service category - missing location ` +
            `(city='${orderCity}', state='${orderState}')`
          );
        } else {
          const orderLocationId = normalizeLocationId(orderCity, orderState);
          const locationServicesCollection = servicesPath(db, orderLocationId);
          const serviceDoc = await locationServicesCollection.doc(serviceId).get();
          if (serviceDoc.exists) {
            const service = serviceDoc.data();
            finalCategory = service?.category;
            serviceTitle = service?.title || serviceTitle;
          }
        }
      }
      
      if (!finalCategory) {
        functions.logger.warn(`Order ${orderId} has no category and no serviceId with category`);
        return null;
      }

      // Parse location to extract city and state
      // Location format can vary, we'll try to extract city and state
      // Common formats: "City, State" or "Address, City, State"
      const locationParts = location.split(',').map((s: string) => s.trim());
      let city = '';
      let state = '';

      if (locationParts.length >= 2) {
        // Assume last part is state, second to last is city
        state = locationParts[locationParts.length - 1];
        city = locationParts[locationParts.length - 2];
      } else if (locationParts.length === 1) {
        // Only city provided
        city = locationParts[0];
      }

      functions.logger.info(`Order ${orderId}: category=${finalCategory}, city=${city}, state=${state}`);

      // Find all providers with services in this category
      // CRÍTICO: Buscar serviços na coleção por localização
      if (!city || !state || city.trim() === '' || state.trim() === '') {
        functions.logger.error(
          `Order ${orderId}: Localização inválida (city='${city}', state='${state}'). ` +
          'Não é possível processar ordem sem localização válida.'
        );
        return null;
      }
      const orderLocationId = normalizeLocationId(city, state);
      const locationServicesCollection = servicesPath(db, orderLocationId);
      const servicesSnapshot = await locationServicesCollection
        .where('category', '==', finalCategory)
        .where('active', '==', true)
        .get();

      if (servicesSnapshot.empty) {
        functions.logger.info(`No active services found for category ${finalCategory}`);
        return null;
      }

      // Get unique provider IDs
      const providerIds = new Set<string>();
      servicesSnapshot.forEach((doc) => {
        const providerId = doc.data()?.providerId;
        if (providerId && providerId !== clientId) {
          providerIds.add(providerId);
        }
      });

      if (providerIds.size === 0) {
        functions.logger.info(`No providers found for category ${finalCategory}`);
        return null;
      }

      // Filter providers by location (city/state)
      // CRÍTICO: Buscar parceiros em locations/{locationId}/users
      const matchingProviders: string[] = [];
      
      // Buscar parceiros na coleção por localização
      const locationUsersCollection = db.collection('locations').doc(orderLocationId).collection('users');
      const providersSnapshot = await locationUsersCollection
        .where('role', '==', 'partner')
        .get();
      
      const providerMap = new Map<string, admin.firestore.DocumentData>();
      providersSnapshot.forEach((doc) => {
        providerMap.set(doc.id, doc.data());
      });
      
      for (const providerId of providerIds) {
        // Buscar primeiro em locations/{locationId}/users
        let userData = providerMap.get(providerId);
        
        // Fallback: buscar em users global (legacy)
        if (!userData) {
          const userDoc = await db.collection(COLLECTIONS.USERS).doc(providerId).get();
          if (!userDoc.exists) continue;
          userData = userDoc.data();
        }
        
        const userRole = userData?.role;
        
        // Only notify partners
        if (userRole !== 'partner') continue;

        // Check if provider has this category in their preferences
        const preferredCategories = userData?.preferredCategories || [];
        
        // If provider has preferences set, only notify if category matches
        // If no preferences set, notify all providers (backward compatibility)
        if (preferredCategories.length > 0) {
          // Normalize category names for comparison (case-insensitive)
          const normalizedOrderCategory = finalCategory.toLowerCase().trim();
          const normalizedPreferredCategories = preferredCategories.map((cat: string) => 
            cat.toLowerCase().trim()
          );
          
          // Check if order category matches any preferred category
          const categoryMatches = normalizedPreferredCategories.includes(normalizedOrderCategory);
          
          if (!categoryMatches) {
            functions.logger.info(
              `Provider ${providerId} skipped: category ${finalCategory} ` +
              `not in preferences [${preferredCategories.join(', ')}]`
            );
            continue; // Skip this provider
          }
        }

        // Lei 1: city e state DEVEM estar na raiz do documento users/{userId}
        // NÃO usar address.city ou address.state como fonte - isso viola a Lei 1
        const providerCity = userData?.city || '';
        const providerState = userData?.state || '';

        // Match by city and state if both are available
        // If only city is available, match by city
        // If neither is available, include the provider anyway (they might be willing to travel)
        let matches = false;
        
        if (city && state) {
          // Match by both city and state
          matches = providerCity.toLowerCase() === city.toLowerCase() && 
                   providerState.toLowerCase() === state.toLowerCase();
        } else if (city) {
          // Match by city only
          matches = providerCity.toLowerCase() === city.toLowerCase();
        } else {
          // No location filter, include all providers in category
          matches = true;
        }

        if (matches) {
          matchingProviders.push(providerId);
        }
      }

      if (matchingProviders.length === 0) {
        functions.logger.info(`No providers found in region for order ${orderId}`);
        return null;
      }

      functions.logger.info(`Found ${matchingProviders.length} providers to notify for order ${orderId}`);

      // Get client name for notification
      let clientName = 'Um cliente';
      if (clientId) {
        const clientDoc = await db.collection(COLLECTIONS.USERS).doc(clientId).get();
        if (clientDoc.exists) {
          clientName = clientDoc.data()?.displayName || clientDoc.data()?.name || clientName;
        }
      }

      // Prepare notification message
      const notificationTitle = 'Nova Ordem de Serviço Disponível';
      const notificationMessage = `${clientName} precisa de ${serviceTitle}${city ? ` em ${city}` : ''}`;

      // Send notifications to all matching providers
      type NotificationResult = admin.messaging.BatchResponse | {
        successCount: number;
        failureCount: number;
      };
      const notificationPromises: Array<Promise<NotificationResult>> = [];
      const batch = db.batch();

      for (const providerId of matchingProviders) {
        // Create notification document
        const notificationRef = db.collection(COLLECTIONS.NOTIFICATIONS).doc();
        batch.set(notificationRef, {
          userId: providerId,
          orderId: orderId,
          type: 'new_service_order_available',
          title: notificationTitle,
          message: notificationMessage,
          data: {
            orderId: orderId,
            category: finalCategory,
            location: location,
            serviceTitle: serviceTitle,
          },
          read: false,
          createdAt: admin.firestore.FieldValue.serverTimestamp(),
        });

        // Get provider's FCM tokens and send push notification
        const providerDoc = await db.collection(COLLECTIONS.USERS).doc(providerId).get();
        const fcmTokens = providerDoc.data()?.fcmTokens || [];

        if (fcmTokens.length > 0) {
          const messages = fcmTokens.map((token: string) => ({
            notification: {
              title: notificationTitle,
              body: notificationMessage,
            },
            data: {
              orderId: orderId,
              category: finalCategory,
              type: 'new_service_order_available',
            },
            token: token,
          }));

          notificationPromises.push(
            admin.messaging().sendAll(messages).catch((error) => {
              functions.logger.error(`Error sending push to provider ${providerId}:`, error);
              return {successCount: 0, failureCount: 0};
            })
          );
        }
      }

      // Commit all notification documents
      await batch.commit();

      // Send all push notifications
      const results = await Promise.all(notificationPromises);
      const totalSuccess = results.reduce((sum, r) => sum + (r.successCount || 0), 0);
      const totalFailure = results.reduce((sum, r) => sum + (r.failureCount || 0), 0);

      functions.logger.info(
        `Order ${orderId}: Notified ${matchingProviders.length} providers. ` +
        `Push notifications: ${totalSuccess} sent, ${totalFailure} failed`
      );

      return null;
    } catch (error) {
      functions.logger.error(`Error notifying providers for order ${orderId}:`, error);
      // Don't throw - we don't want to fail the order creation
      return null;
    }
  });
