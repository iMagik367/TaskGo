import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';

const db = admin.firestore();
const rtdb = admin.database();
const storage = admin.storage();

/**
 * Cloud Function para exclusão de conta do usuário
 * Atende aos requisitos do Google Play Store para exclusão de dados
 */
export const deleteUserAccount = functions.https.onCall(async (data, context) => {
  // Verificar autenticação
  if (!context.auth) {
    throw new functions.https.HttpsError(
      'unauthenticated',
      'Usuário não autenticado'
    );
  }

  const userId = context.auth.uid;

  try {
    functions.logger.info(`Iniciando exclusão de conta para usuário: ${userId}`);

    // 1. Deletar dados do Firestore
    const batch = db.batch();

    // Deletar perfil do usuário
    const userRef = db.collection('users').doc(userId);
    batch.delete(userRef);

    // Deletar produtos do usuário
    const productsSnapshot = await db.collection('products')
      .where('sellerId', '==', userId)
      .get();
    productsSnapshot.forEach((doc: admin.firestore.QueryDocumentSnapshot) => batch.delete(doc.ref));

    // Deletar serviços do usuário
    const servicesSnapshot = await db.collection('services')
      .where('providerId', '==', userId)
      .get();
    servicesSnapshot.forEach((doc: admin.firestore.QueryDocumentSnapshot) => batch.delete(doc.ref));

    // Deletar pedidos relacionados
    const ordersSnapshot = await db.collection('orders')
      .where('clientId', '==', userId)
      .get();
    ordersSnapshot.forEach((doc: admin.firestore.QueryDocumentSnapshot) => batch.delete(doc.ref));

    const providerOrdersSnapshot = await db.collection('orders')
      .where('providerId', '==', userId)
      .get();
    providerOrdersSnapshot.forEach((doc: admin.firestore.QueryDocumentSnapshot) => batch.delete(doc.ref));

    // Deletar pedidos de compra
    const purchaseOrdersSnapshot = await db.collection('purchase_orders')
      .where('clientId', '==', userId)
      .get();
    purchaseOrdersSnapshot.forEach((doc: admin.firestore.QueryDocumentSnapshot) => batch.delete(doc.ref));

    const sellerOrdersSnapshot = await db.collection('purchase_orders')
      .where('sellerId', '==', userId)
      .get();
    sellerOrdersSnapshot.forEach((doc: admin.firestore.QueryDocumentSnapshot) => batch.delete(doc.ref));

    // Deletar avaliações
    const reviewsSnapshot = await db.collection('reviews')
      .where('clientId', '==', userId)
      .get();
    reviewsSnapshot.forEach((doc: admin.firestore.QueryDocumentSnapshot) => batch.delete(doc.ref));

    // Deletar notificações
    const notificationsSnapshot = await db.collection('notifications')
      .where('userId', '==', userId)
      .get();
    notificationsSnapshot.forEach((doc: admin.firestore.QueryDocumentSnapshot) => batch.delete(doc.ref));

    // Deletar conversas
    const conversationsSnapshot = await db.collection('conversations')
      .where('userId', '==', userId)
      .get();
    conversationsSnapshot.forEach((doc: admin.firestore.QueryDocumentSnapshot) => batch.delete(doc.ref));

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

    // 4. Deletar conta de autenticação
    await admin.auth().deleteUser(userId);
    functions.logger.info(`Conta de autenticação deletada para usuário: ${userId}`);

    return {
      success: true,
      message: 'Conta deletada com sucesso'
    };
  } catch (error: unknown) {
    const errorMessage = error instanceof Error ? error.message : 'Erro desconhecido';
    functions.logger.error(`Erro ao deletar conta do usuário ${userId}:`, error);
    throw new functions.https.HttpsError(
      'internal',
      `Erro ao deletar conta: ${errorMessage}`
    );
  }
});

