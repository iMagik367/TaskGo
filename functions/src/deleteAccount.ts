import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
import {validateAppCheck} from './security/appCheck';
import {getFirestore} from './utils/firestore';

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

    // 1. Deletar dados do Firestore
    const userRef = db.collection('users').doc(userId);
    
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
      'settings'
    ];
    
    functions.logger.info(`Deletando subcoleções do usuário ${userId}`);
    for (const subcollection of subcollections) {
      try {
        const subcollectionRef = userRef.collection(subcollection);
        const snapshot = await subcollectionRef.get();
        
        if (!snapshot.empty) {
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

