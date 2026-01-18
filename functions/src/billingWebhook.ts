import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';

import {getFirestore} from './utils/firestore';
/**
 * Webhook para receber notificações do Google Play Billing
 * Este endpoint deve ser configurado no Google Play Console
 */
export const googlePlayBillingWebhook = functions.https.onRequest(async (req, res) => {
  try {
    const { notificationType, purchaseToken, subscriptionId, productId } = req.body;

    if (!notificationType || !purchaseToken) {
      res.status(400).json({ error: 'Missing required fields' });
      return;
    }

    const db = getFirestore();

    switch (notificationType) {
      case 1: // SUBSCRIPTION_RECOVERED
      case 2: // SUBSCRIPTION_RENEWED
      case 3: // SUBSCRIPTION_CANCELED
      case 4: // SUBSCRIPTION_PURCHASED
      case 5: // SUBSCRIPTION_ON_HOLD
      case 6: // SUBSCRIPTION_IN_GRACE_PERIOD
      case 7: // SUBSCRIPTION_RESTARTED
      case 8: // SUBSCRIPTION_PRICE_CHANGE_CONFIRMED
      case 9: // SUBSCRIPTION_DEFERRED
      case 10: // SUBSCRIPTION_PAUSED
      case 11: // SUBSCRIPTION_PAUSE_SCHEDULE_CHANGED
      case 12: // SUBSCRIPTION_REVOKED
      case 13: // SUBSCRIPTION_EXPIRED
        // Processar notificação de assinatura
        await handleSubscriptionNotification(
          notificationType,
          purchaseToken,
          subscriptionId,
          productId,
          db
        );
        break;

      case 20: // ONE_TIME_PRODUCT_PURCHASED
        // Processar compra de produto único
        await handleOneTimePurchase(purchaseToken, productId, db);
        break;

      default:
        functions.logger.warn(`Unhandled notification type: ${notificationType}`);
    }

    res.status(200).json({ success: true });
  } catch (error) {
    functions.logger.error('Error processing billing webhook:', error);
    const message = error instanceof Error ? error.message : 'Internal server error';
    res.status(500).json({ error: message });
  }
});

/**
 * Processa notificação de assinatura
 */
async function handleSubscriptionNotification(
  notificationType: number,
  purchaseToken: string,
  subscriptionId: string | undefined,
  productId: string | undefined,
  db: admin.firestore.Firestore
) {
  // Buscar compra no Firestore
  const purchasesSnapshot = await db.collection('purchases')
    .where('purchaseToken', '==', purchaseToken)
    .get();

  if (purchasesSnapshot.empty) {
    functions.logger.warn(`No purchase found for token: ${purchaseToken}`);
    return;
  }

  const purchaseDoc = purchasesSnapshot.docs[0];
  const purchaseData = purchaseDoc.data();

  // Atualizar status da compra
  await purchaseDoc.ref.update({
    notificationType,
    lastNotification: admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });

  // Criar notificação para o usuário
  await db.collection('notifications').add({
    userId: purchaseData.userId,
    type: 'billing_update',
    title: 'Atualização de Assinatura',
    message: getNotificationMessage(notificationType),
    read: false,
    createdAt: admin.firestore.FieldValue.serverTimestamp()
  });

  functions.logger.info(`Subscription notification processed: ${notificationType} for ${purchaseToken}`);
}

/**
 * Processa compra de produto único
 */
async function handleOneTimePurchase(
  purchaseToken: string,
  productId: string | undefined,
  db: admin.firestore.Firestore
) {
  // Buscar compra no Firestore
  const purchasesSnapshot = await db.collection('purchases')
    .where('purchaseToken', '==', purchaseToken)
    .get();

  if (purchasesSnapshot.empty) {
    functions.logger.warn(`No purchase found for token: ${purchaseToken}`);
    return;
  }

  const purchaseDoc = purchasesSnapshot.docs[0];
  const purchaseData = purchaseDoc.data();

  // Atualizar status da compra
  await purchaseDoc.ref.update({
    status: 'completed',
    completedAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  });

  // Criar notificação para o usuário
  await db.collection('notifications').add({
    userId: purchaseData.userId,
    type: 'purchase_completed',
    title: 'Compra Confirmada',
    message: 'Sua compra foi confirmada com sucesso!',
    read: false,
    createdAt: admin.firestore.FieldValue.serverTimestamp()
  });

  functions.logger.info(`One-time purchase processed: ${purchaseToken}`);
}

/**
 * Retorna mensagem de notificação baseada no tipo
 */
function getNotificationMessage(notificationType: number): string {
  const messages: { [key: number]: string } = {
    1: 'Sua assinatura foi recuperada',
    2: 'Sua assinatura foi renovada',
    3: 'Sua assinatura foi cancelada',
    4: 'Sua assinatura foi ativada',
    5: 'Sua assinatura está em espera',
    6: 'Sua assinatura está em período de graça',
    7: 'Sua assinatura foi reiniciada',
    8: 'Mudança de preço confirmada',
    9: 'Sua assinatura foi adiada',
    10: 'Sua assinatura foi pausada',
    11: 'Agendamento de pausa alterado',
    12: 'Sua assinatura foi revogada',
    13: 'Sua assinatura expirou'
  };

  return messages[notificationType] || 'Atualização na sua assinatura';
}


