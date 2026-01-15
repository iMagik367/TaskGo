import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';

const db = admin.firestore();

/**
 * Sincronização bidirecional entre coleções públicas e subcoleções do usuário
 * Garante que dados públicos (services, products, orders, posts) sempre estejam
 * sincronizados com a subcoleção do usuário (fonte de verdade)
 */

// ==================== SERVICES ====================

/**
 * Quando um serviço é criado/atualizado/deletado na subcoleção do usuário,
 * sincroniza com a coleção pública
 */
export const syncServiceFromUserCollection = functions.firestore
  .document('users/{userId}/services/{serviceId}')
  .onWrite(async (change, context) => {
    const userId = context.params.userId as string;
    const serviceId = context.params.serviceId as string;
    
    try {
      const publicServiceRef = db.collection('services').doc(serviceId);
      
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
        await publicServiceRef.set(serviceData, { merge: true });
        functions.logger.info(
          'Serviço ' + serviceId + ' sincronizado da subcoleção do usuário ' + userId + ' ' +
          'para coleção pública'
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
 * Quando um serviço é criado/atualizado/deletado na coleção pública,
 * sincroniza com a subcoleção do usuário (fonte de verdade)
 * NOTA: Esta função é uma segurança, mas a fonte de verdade deve ser a subcoleção do usuário
 */
export const syncServiceToUserCollection = functions.firestore
  .document('services/{serviceId}')
  .onWrite(async (change, context) => {
    const serviceId = context.params.serviceId as string;
    
    try {
      const serviceData = change.after.exists ? change.after.data() : null;
      
      if (!serviceData) {
        // Foi deletado da coleção pública - verificar se ainda existe na subcoleção
        // Se existir, recriar na coleção pública (fonte de verdade é a subcoleção)
        return;
      }
      
      const providerId = serviceData.providerId as string | undefined;
      if (!providerId) {
        functions.logger.warn(`Serviço ${serviceId} sem providerId, ignorando sincronização`);
        return;
      }
      
      const userServiceRef = db.collection('users').doc(providerId).collection('services').doc(serviceId);
      
      // Verificar se já existe na subcoleção
      const userServiceDoc = await userServiceRef.get();
      
      // Se não existe na subcoleção, criar (fonte de verdade deve estar na subcoleção)
      if (!userServiceDoc.exists) {
        await userServiceRef.set(serviceData, { merge: true });
        functions.logger.info(
          'Serviço ' + serviceId + ' criado na subcoleção do usuário ' + providerId + ' ' +
          'a partir da coleção pública'
        );
      } else {
        // Se existe, atualizar apenas se a coleção pública for mais recente
        // (Isso é uma medida de segurança, mas normalmente a subcoleção é atualizada primeiro)
        const userServiceData = userServiceDoc.data();
        const publicUpdatedAt = serviceData.updatedAt;
        const userUpdatedAt = userServiceData?.updatedAt;
        
        if (publicUpdatedAt && userUpdatedAt) {
          // Comparar timestamps - se público for mais recente, atualizar subcoleção
          const publicTime = publicUpdatedAt instanceof admin.firestore.Timestamp 
            ? publicUpdatedAt.toMillis() 
            : (publicUpdatedAt as number);
          const userTime = userUpdatedAt instanceof admin.firestore.Timestamp 
            ? userUpdatedAt.toMillis() 
            : (userUpdatedAt as number);
          
          if (publicTime > userTime) {
            await userServiceRef.set(serviceData, { merge: true });
            functions.logger.info(
              'Serviço ' + serviceId + ' atualizado na subcoleção do usuário ' + providerId + ' ' +
              'a partir da coleção pública'
            );
          }
        }
      }
    } catch (error) {
      functions.logger.error(`Erro ao sincronizar serviço ${serviceId} para subcoleção do usuário:`, error);
    }
  });

// ==================== PRODUCTS ====================

/**
 * Quando um produto é criado/atualizado/deletado na subcoleção do usuário,
 * sincroniza com a coleção pública
 */
export const syncProductFromUserCollection = functions.firestore
  .document('users/{userId}/products/{productId}')
  .onWrite(async (change, context) => {
    const userId = context.params.userId as string;
    const productId = context.params.productId as string;
    
    try {
      const publicProductRef = db.collection('products').doc(productId);
      
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
        await publicProductRef.set(productData, { merge: true });
        functions.logger.info(
          'Produto ' + productId + ' sincronizado da subcoleção do usuário ' + userId + ' ' +
          'para coleção pública'
        );
      }
    } catch (error) {
      functions.logger.error(`Erro ao sincronizar produto ${productId} da subcoleção do usuário:`, error);
    }
  });

/**
 * Quando um produto é criado/atualizado/deletado na coleção pública,
 * sincroniza com a subcoleção do usuário (fonte de verdade)
 */
export const syncProductToUserCollection = functions.firestore
  .document('products/{productId}')
  .onWrite(async (change, context) => {
    const productId = context.params.productId as string;
    
    try {
      const productData = change.after.exists ? change.after.data() : null;
      
      if (!productData) {
        return;
      }
      
      const sellerId = productData.sellerId as string | undefined;
      if (!sellerId) {
        functions.logger.warn(`Produto ${productId} sem sellerId, ignorando sincronização`);
        return;
      }
      
      const userProductRef = db.collection('users').doc(sellerId).collection('products').doc(productId);
      
      // Verificar se já existe na subcoleção
      const userProductDoc = await userProductRef.get();
      
      // Se não existe na subcoleção, criar
      if (!userProductDoc.exists) {
        await userProductRef.set(productData, { merge: true });
        functions.logger.info(
          'Produto ' + productId + ' criado na subcoleção do usuário ' + sellerId + ' ' +
          'a partir da coleção pública'
        );
      } else {
        // Se existe, atualizar apenas se a coleção pública for mais recente
        const userProductData = userProductDoc.data();
        const publicUpdatedAt = productData.updatedAt;
        const userUpdatedAt = userProductData?.updatedAt;
        
        if (publicUpdatedAt && userUpdatedAt) {
          const publicTime = publicUpdatedAt instanceof admin.firestore.Timestamp 
            ? publicUpdatedAt.toMillis() 
            : (publicUpdatedAt as number);
          const userTime = userUpdatedAt instanceof admin.firestore.Timestamp 
            ? userUpdatedAt.toMillis() 
            : (userUpdatedAt as number);
          
          if (publicTime > userTime) {
            await userProductRef.set(productData, { merge: true });
            functions.logger.info(
              'Produto ' + productId + ' atualizado na subcoleção do usuário ' + sellerId + ' ' +
              'a partir da coleção pública'
            );
          }
        }
      }
    } catch (error) {
      functions.logger.error(`Erro ao sincronizar produto ${productId} para subcoleção do usuário:`, error);
    }
  });

// ==================== ORDERS ====================

/**
 * Quando uma ordem é criada/atualizada/deletada na subcoleção do usuário,
 * sincroniza com a coleção pública
 */
export const syncOrderFromUserCollection = functions.firestore
  .document('users/{userId}/orders/{orderId}')
  .onWrite(async (change, context) => {
    const userId = context.params.userId as string;
    const orderId = context.params.orderId as string;
    
    try {
      const publicOrderRef = db.collection('orders').doc(orderId);
      
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
        await publicOrderRef.set(orderData, { merge: true });
        functions.logger.info(
          'Ordem ' + orderId + ' sincronizada da subcoleção do usuário ' + userId + ' ' +
          'para coleção pública'
        );
      }
    } catch (error) {
      functions.logger.error(`Erro ao sincronizar ordem ${orderId} da subcoleção do usuário:`, error);
    }
  });

/**
 * Quando uma ordem é criada/atualizada/deletada na coleção pública,
 * sincroniza com a subcoleção do usuário (cliente que criou)
 */
export const syncOrderToUserCollection = functions.firestore
  .document('orders/{orderId}')
  .onWrite(async (change, context) => {
    const orderId = context.params.orderId as string;
    
    try {
      const orderData = change.after.exists ? change.after.data() : null;
      
      if (!orderData) {
        return;
      }
      
      const clientId = orderData.clientId as string | undefined;
      if (!clientId) {
        functions.logger.warn(`Ordem ${orderId} sem clientId, ignorando sincronização`);
        return;
      }
      
      const userOrderRef = db.collection('users').doc(clientId).collection('orders').doc(orderId);
      
      // Verificar se já existe na subcoleção
      const userOrderDoc = await userOrderRef.get();
      
      // Se não existe na subcoleção, criar
      if (!userOrderDoc.exists) {
        await userOrderRef.set(orderData, { merge: true });
        functions.logger.info(
          'Ordem ' + orderId + ' criada na subcoleção do usuário ' + clientId + ' ' +
          'a partir da coleção pública'
        );
      } else {
        // Se existe, atualizar apenas se a coleção pública for mais recente
        const userOrderData = userOrderDoc.data();
        const publicUpdatedAt = orderData.updatedAt;
        const userUpdatedAt = userOrderData?.updatedAt;
        
        if (publicUpdatedAt && userUpdatedAt) {
          const publicTime = publicUpdatedAt instanceof admin.firestore.Timestamp 
            ? publicUpdatedAt.toMillis() 
            : (publicUpdatedAt as number);
          const userTime = userUpdatedAt instanceof admin.firestore.Timestamp 
            ? userUpdatedAt.toMillis() 
            : (userUpdatedAt as number);
          
          if (publicTime > userTime) {
            await userOrderRef.set(orderData, { merge: true });
            functions.logger.info(
              'Ordem ' + orderId + ' atualizada na subcoleção do usuário ' + clientId + ' ' +
              'a partir da coleção pública'
            );
          }
        }
      }
    } catch (error) {
      functions.logger.error(`Erro ao sincronizar ordem ${orderId} para subcoleção do usuário:`, error);
    }
  });

// ==================== POSTS ====================

/**
 * Quando um post é criado/atualizado/deletado na subcoleção do usuário,
 * sincroniza com a coleção pública
 */
export const syncPostFromUserCollection = functions.firestore
  .document('users/{userId}/posts/{postId}')
  .onWrite(async (change, context) => {
    const userId = context.params.userId as string;
    const postId = context.params.postId as string;
    
    try {
      const publicPostRef = db.collection('posts').doc(postId);
      
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
        await publicPostRef.set(postData, { merge: true });
        functions.logger.info(
          'Post ' + postId + ' sincronizado da subcoleção do usuário ' + userId + ' ' +
          'para coleção pública'
        );
      }
    } catch (error) {
      functions.logger.error(`Erro ao sincronizar post ${postId} da subcoleção do usuário:`, error);
    }
  });

/**
 * Quando um post é criado/atualizado/deletado na coleção pública,
 * sincroniza com a subcoleção do usuário (fonte de verdade)
 */
export const syncPostToUserCollection = functions.firestore
  .document('posts/{postId}')
  .onWrite(async (change, context) => {
    const postId = context.params.postId as string;
    
    try {
      const postData = change.after.exists ? change.after.data() : null;
      
      if (!postData) {
        return;
      }
      
      const userId = postData.userId as string | undefined;
      if (!userId) {
        functions.logger.warn(`Post ${postId} sem userId, ignorando sincronização`);
        return;
      }
      
      const userPostRef = db.collection('users').doc(userId).collection('posts').doc(postId);
      
      // Verificar se já existe na subcoleção
      const userPostDoc = await userPostRef.get();
      
      // Se não existe na subcoleção, criar
      if (!userPostDoc.exists) {
        await userPostRef.set(postData, { merge: true });
        functions.logger.info(
          'Post ' + postId + ' criado na subcoleção do usuário ' + userId + ' ' +
          'a partir da coleção pública'
        );
      } else {
        // Se existe, atualizar apenas se a coleção pública for mais recente
        const userPostData = userPostDoc.data();
        const publicUpdatedAt = postData.updatedAt;
        const userUpdatedAt = userPostData?.updatedAt;
        
        if (publicUpdatedAt && userUpdatedAt) {
          const publicTime = publicUpdatedAt instanceof admin.firestore.Timestamp 
            ? publicUpdatedAt.toMillis() 
            : (publicUpdatedAt as number);
          const userTime = userUpdatedAt instanceof admin.firestore.Timestamp 
            ? userUpdatedAt.toMillis() 
            : (userUpdatedAt as number);
          
          if (publicTime > userTime) {
            await userPostRef.set(postData, { merge: true });
            functions.logger.info(
              'Post ' + postId + ' atualizado na subcoleção do usuário ' + userId + ' ' +
              'a partir da coleção pública'
            );
          }
        }
      }
    } catch (error) {
      functions.logger.error(`Erro ao sincronizar post ${postId} para subcoleção do usuário:`, error);
    }
  });
