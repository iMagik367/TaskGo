import * as functions from 'firebase-functions';

import {getFirestore} from './utils/firestore';
import {
  productsPath,
  servicesPath,
  ordersPath,
  feedPath,
  getUserLocationId,
  createStandardPayload,
} from './utils/firestorePaths';
const db = getFirestore();

/**
 * Sincronização bidirecional entre coleções públicas e subcoleções do usuário
 * Garante que dados públicos (services, products, orders, posts) sempre estejam
 * sincronizados com a subcoleção do usuário (fonte de verdade)
 */

// ==================== SERVICES ====================

/**
 * Quando um serviço é criado/atualizado/deletado na subcoleção do usuário,
 * sincroniza com a coleção pública por localização
 */
export const syncServiceFromUserCollection = functions.firestore
  .document('users/{userId}/services/{serviceId}')
  .onWrite(async (change, context) => {
    const userId = context.params.userId as string;
    const serviceId = context.params.serviceId as string;
    
    try {
      // Obter localização do usuário
      const locationId = await getUserLocationId(db, userId);
      const locationServicesCollection = servicesPath(db, locationId);
      const publicServiceRef = locationServicesCollection.doc(serviceId);
      
      // Se foi deletado na subcoleção do usuário, deletar também da coleção pública
      if (!change.after.exists) {
        await publicServiceRef.delete();
        functions.logger.info(
          `Serviço ${serviceId} deletado da coleção pública ` +
          `após deleção na subcoleção do usuário ${userId}`
        );
        return;
      }
      
      // Se foi criado/atualizado, sincronizar com a coleção pública
      const serviceData = change.after.data();
      if (serviceData) {
        // Garantir que tenha timestamps e active
        const standardizedData = createStandardPayload(serviceData, serviceData.active !== false);
        await publicServiceRef.set(standardizedData, { merge: true });
        functions.logger.info(
          'Serviço ' + serviceId + ' sincronizado da subcoleção do usuário ' + userId + ' ' +
          'para coleção pública em locations/' + locationId + '/services'
        );
      }
    } catch (error) {
      functions.logger.error(
        `Erro ao sincronizar serviço ${serviceId} da subcoleção do usuário:`,
        error
      );
    }
  });

/**
 * REMOVIDO: syncServiceToUserCollection
 * Não sincronizamos mais da coleção pública para subcoleção do usuário
 * A fonte de verdade é sempre a subcoleção do usuário, que sincroniza para locations/{locationId}/services
 */

// ==================== PRODUCTS ====================

/**
 * Quando um produto é criado/atualizado/deletado na subcoleção do usuário,
 * sincroniza com a coleção pública por localização
 */
export const syncProductFromUserCollection = functions.firestore
  .document('users/{userId}/products/{productId}')
  .onWrite(async (change, context) => {
    const userId = context.params.userId as string;
    const productId = context.params.productId as string;
    
    try {
      // Obter localização do usuário
      const locationId = await getUserLocationId(db, userId);
      const locationProductsCollection = productsPath(db, locationId);
      const publicProductRef = locationProductsCollection.doc(productId);
      
      // Se foi deletado na subcoleção do usuário, deletar também da coleção pública
      if (!change.after.exists) {
        await publicProductRef.delete();
        functions.logger.info(
          'Produto ' + productId + ' deletado da coleção pública ' +
          'após deleção na subcoleção do usuário ' + userId
        );
        return;
      }
      
      // Se foi criado/atualizado, sincronizar com a coleção pública
      const productData = change.after.data();
      if (productData) {
        // Garantir que tenha timestamps e active
        const standardizedData = createStandardPayload(productData, productData.active !== false);
        await publicProductRef.set(standardizedData, { merge: true });
        functions.logger.info(
          'Produto ' + productId + ' sincronizado da subcoleção do usuário ' + userId + ' ' +
          'para coleção pública em locations/' + locationId + '/products'
        );
      }
    } catch (error) {
      functions.logger.error(`Erro ao sincronizar produto ${productId} da subcoleção do usuário:`, error);
    }
  });

/**
 * REMOVIDO: syncProductToUserCollection
 * Não sincronizamos mais da coleção pública para subcoleção do usuário
 * A fonte de verdade é sempre a subcoleção do usuário, que sincroniza para locations/{locationId}/products
 */

// ==================== ORDERS ====================

/**
 * Quando uma ordem é criada/atualizada/deletada na subcoleção do usuário,
 * sincroniza com a coleção pública por localização
 */
export const syncOrderFromUserCollection = functions.firestore
  .document('users/{userId}/orders/{orderId}')
  .onWrite(async (change, context) => {
    const userId = context.params.userId as string;
    const orderId = context.params.orderId as string;
    
    try {
      // Obter localização do usuário
      const locationId = await getUserLocationId(db, userId);
      const locationOrdersCollection = ordersPath(db, locationId);
      const publicOrderRef = locationOrdersCollection.doc(orderId);
      
      // Se foi deletado na subcoleção do usuário, deletar também da coleção pública
      if (!change.after.exists) {
        await publicOrderRef.delete();
        functions.logger.info(
          'Ordem ' + orderId + ' deletada da coleção pública ' +
          'após deleção na subcoleção do usuário ' + userId
        );
        return;
      }
      
      // Se foi criado/atualizado, sincronizar com a coleção pública
      const orderData = change.after.data();
      if (orderData) {
        // Garantir que tenha timestamps (orders não têm active)
        const standardizedData = createStandardPayload(orderData, undefined);
        delete (standardizedData as Record<string, unknown>).active; // Remover active se foi adicionado
        await publicOrderRef.set(standardizedData, { merge: true });
        functions.logger.info(
          'Ordem ' + orderId + ' sincronizada da subcoleção do usuário ' + userId + ' ' +
          'para coleção pública em locations/' + locationId + '/orders'
        );
      }
    } catch (error) {
      functions.logger.error(`Erro ao sincronizar ordem ${orderId} da subcoleção do usuário:`, error);
    }
  });

/**
 * REMOVIDO: syncOrderToUserCollection
 * Não sincronizamos mais da coleção pública para subcoleção do usuário
 * A fonte de verdade é sempre a subcoleção do usuário, que sincroniza para locations/{locationId}/orders
 */

// ==================== POSTS ====================

/**
 * Quando um post é criado/atualizado/deletado na subcoleção do usuário,
 * sincroniza com a coleção pública por localização
 */
export const syncPostFromUserCollection = functions.firestore
  .document('users/{userId}/posts/{postId}')
  .onWrite(async (change, context) => {
    const userId = context.params.userId as string;
    const postId = context.params.postId as string;
    
    try {
      // Obter localização do usuário
      const locationId = await getUserLocationId(db, userId);
      const locationPostsCollection = feedPath(db, locationId);
      const publicPostRef = locationPostsCollection.doc(postId);
      
      // Se foi deletado na subcoleção do usuário, deletar também da coleção pública
      if (!change.after.exists) {
        await publicPostRef.delete();
        functions.logger.info(
          'Post ' + postId + ' deletado da coleção pública ' +
          'após deleção na subcoleção do usuário ' + userId
        );
        return;
      }
      
      // Se foi criado/atualizado, sincronizar com a coleção pública
      const postData = change.after.data();
      if (postData) {
        // Garantir que tenha timestamps (posts podem ter active dependendo do caso)
        const standardizedData = createStandardPayload(postData, postData.active !== false);
        await publicPostRef.set(standardizedData, { merge: true });
        functions.logger.info(
          'Post ' + postId + ' sincronizado da subcoleção do usuário ' + userId + ' ' +
          'para coleção pública em locations/' + locationId + '/posts'
        );
      }
    } catch (error) {
      functions.logger.error(`Erro ao sincronizar post ${postId} da subcoleção do usuário:`, error);
    }
  });

/**
 * REMOVIDO: syncPostToUserCollection
 * Não sincronizamos mais da coleção pública para subcoleção do usuário
 * A fonte de verdade é sempre a subcoleção do usuário, que sincroniza para locations/{locationId}/posts
 */
