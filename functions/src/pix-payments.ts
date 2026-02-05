import * as admin from 'firebase-admin';
import {getFirestore} from './utils/firestore';
import * as functions from 'firebase-functions';
import {COLLECTIONS, PAYMENT_STATUS} from './utils/constants';
import {assertAuthenticated, handleError} from './utils/errors';
import {validateAppCheck} from './security/appCheck';
import {purchaseOrdersPath, getUserLocationId} from './utils/firestorePaths';
import * as crypto from 'crypto';

/**
 * Create PIX payment and generate QR code data
 */
export const createPixPayment = functions.https.onCall(async (data, context) => {
  try {
    validateAppCheck(context);
    assertAuthenticated(context);

    const db = getFirestore();
    const {orderId} = data;

    if (!orderId) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'Order ID is required'
      );
    }

    // CRÍTICO: Buscar order na coleção por localização
    const userId = context.auth!.uid;
    const locationId = await getUserLocationId(db, userId);
    const locationOrdersCollection = purchaseOrdersPath(db, locationId);
    const orderDoc = await locationOrdersCollection.doc(orderId).get();
    if (!orderDoc.exists) {
      throw new functions.https.HttpsError('not-found', 'Order not found');
    }

    const order = orderDoc.data();

    if (order?.clientId !== context.auth!.uid) {
      throw new functions.https.HttpsError(
        'permission-denied',
        'Only the order client can create payment'
      );
    }

    if (order?.status !== 'PENDING_PAYMENT') {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'Order must be in PENDING_PAYMENT status'
      );
    }

    const totalAmount = order?.total || 0;

    // Generate PIX key (random key for this payment)
    const pixKey = generatePixKey();

    // Generate PIX QR code data (EMV format)
    const qrCodeData = generatePixQrCode({
      pixKey: pixKey,
      amount: totalAmount,
      description: `Pedido ${orderId.substring(0, 8)}`,
      merchantName: 'TaskGo',
      merchantCity: 'SAO PAULO',
    });

    // Create payment document
    const paymentRef = await db.collection('product_payments').add({
      orderId: orderId,
      clientId: order.clientId,
      sellerId: order.storeId || null,
      amount: totalAmount,
      applicationFee: totalAmount * 0.02, // 2% commission
      netAmountToSeller: totalAmount * 0.98,
      currency: 'BRL',
      paymentMethod: 'PIX',
      pixKey: pixKey,
      pixQrCode: qrCodeData,
      status: PAYMENT_STATUS.PENDING,
      transferStatus: 'PENDING_SHIPMENT',
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    // Update order status - na coleção por localização
    await locationOrdersCollection.doc(orderId).update({
      status: 'PAYMENT_PENDING',
      paymentId: paymentRef.id,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    functions.logger.info(`PIX payment created for order ${orderId}`);

    return {
      paymentId: paymentRef.id,
      pixKey: pixKey,
      qrCodeData: qrCodeData,
      amount: totalAmount,
      expiresAt: Date.now() + (30 * 60 * 1000), // 30 minutes
    };
  } catch (error) {
    functions.logger.error('Error creating PIX payment:', error);
    throw handleError(error);
  }
});

/**
 * Generate random PIX key
 */
function generatePixKey(): string {
  // Generate random UUID-based key
  const randomBytes = crypto.randomBytes(16);
  return randomBytes.toString('hex').toUpperCase();
}

/**
 * Generate PIX QR Code in EMV format
 * Simplified version - in production, use a proper PIX library
 */
function generatePixQrCode(params: {
  pixKey: string;
  amount: number;
  description: string;
  merchantName: string;
  merchantCity: string;
}): string {
  // EMV QR Code format for PIX
  // This is a simplified version - in production, use proper PIX library
  const payload = [
    '00020126', // Payload Format Indicator
    '01', // Point of Initiation Method
    '02', // Merchant Account Information
    '26', // GUI
    '0014BR.GOV.BCB.PIX',
    '01', // Key
    `${String(params.pixKey.length).padStart(2, '0')}${params.pixKey}`,
    '52', // Merchant Category Code
    '0000',
    '53', // Transaction Currency
    '03', // BRL
    '54', // Transaction Amount
    `${String(params.amount.toFixed(2).length).padStart(2, '0')}${params.amount.toFixed(2)}`,
    '58', // Country Code
    '02BR',
    '59', // Merchant Name
    `${String(params.merchantName.length).padStart(2, '0')}${params.merchantName}`,
    '60', // Merchant City
    `${String(params.merchantCity.length).padStart(2, '0')}${params.merchantCity}`,
    '62', // Additional Data Field Template
    '05', // Reference Label
    `${String(params.description.length).padStart(2, '0')}${params.description}`,
    '6304', // CRC16
  ].join('');

  // Calculate CRC16 (simplified - use proper CRC16 calculation)
  const crc = calculateCRC16(payload);
  return payload + crc;
}

/**
 * Calculate CRC16 for PIX QR Code
 */
function calculateCRC16(data: string): string {
  // Simplified CRC16 calculation
  // In production, use proper CRC16-CCITT implementation
  let crc = 0xFFFF;
  for (let i = 0; i < data.length; i++) {
    crc ^= data.charCodeAt(i) << 8;
    for (let j = 0; j < 8; j++) {
      if (crc & 0x8000) {
        crc = (crc << 1) ^ 0x1021;
      } else {
        crc <<= 1;
      }
    }
  }
  return (crc & 0xFFFF).toString(16).toUpperCase().padStart(4, '0');
}

/**
 * Verify PIX payment status
 * This function should be called periodically to check if PIX payment was completed
 * In production, integrate with a PIX payment gateway (Mercado Pago, PagSeguro, etc.)
 */
export const verifyPixPayment = functions.https.onCall(async (data, context) => {
  try {
    validateAppCheck(context);
    assertAuthenticated(context);

    const db = getFirestore();
    const {paymentId} = data;

    if (!paymentId) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'Payment ID is required'
      );
    }

    // Get payment document
    const paymentDoc = await db.collection('product_payments').doc(paymentId).get();
    if (!paymentDoc.exists) {
      throw new functions.https.HttpsError('not-found', 'Payment not found');
    }

    const payment = paymentDoc.data();

    if (payment?.clientId !== context.auth!.uid) {
      throw new functions.https.HttpsError(
        'permission-denied',
        'Only the payment owner can verify payment'
      );
    }

    if (payment?.status === PAYMENT_STATUS.SUCCEEDED) {
      return {
        status: 'succeeded',
        paid: true,
      };
    }

    // TODO: In production, integrate with PIX payment gateway API
    // For now, we'll use a manual confirmation system
    // The payment status should be updated manually by an admin or via webhook
    // when the gateway confirms the payment

    // Check if payment expired
    const createdAt = payment?.createdAt?.toMillis();
    const expiresAt = createdAt ? createdAt + (30 * 60 * 1000) : null;
    const now = Date.now();

    if (expiresAt && now > expiresAt) {
      // Payment expired - use FAILED status
      await paymentDoc.ref.update({
        status: PAYMENT_STATUS.FAILED,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });

      // CRÍTICO: Buscar order na coleção por localização
      const clientId = payment.clientId;
      const locationId = await getUserLocationId(db, clientId);
      const locationOrdersCollection = purchaseOrdersPath(db, locationId);
      
      // Update order status - na coleção por localização
      await locationOrdersCollection.doc(payment.orderId).update({
        status: 'PENDING_PAYMENT',
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });

      return {
        status: 'expired',
        paid: false,
      };
    }

    return {
      status: payment?.status || 'pending',
      paid: false,
    };
  } catch (error) {
    functions.logger.error('Error verifying PIX payment:', error);
    throw handleError(error);
  }
});

/**
 * Confirm PIX payment manually (for testing/admin use)
 * In production, this should be called by a webhook from the PIX gateway
 */
export const confirmPixPayment = functions.https.onCall(async (data, context) => {
  try {
    validateAppCheck(context);
    assertAuthenticated(context);

    const db = getFirestore();
    const {paymentId} = data;

    if (!paymentId) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'Payment ID is required'
      );
    }

    // Get payment document
    const paymentDoc = await db.collection('product_payments').doc(paymentId).get();
    if (!paymentDoc.exists) {
      throw new functions.https.HttpsError('not-found', 'Payment not found');
    }

    const payment = paymentDoc.data();

    if (payment?.status !== PAYMENT_STATUS.PENDING) {
      throw new functions.https.HttpsError(
        'failed-precondition',
        `Payment is not pending. Current status: ${payment?.status}`
      );
    }

    // Update payment status
    await paymentDoc.ref.update({
      status: PAYMENT_STATUS.SUCCEEDED,
      paidAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    // CRÍTICO: Buscar order na coleção por localização
    const clientId = payment.clientId;
    const locationId = await getUserLocationId(db, clientId);
    const locationOrdersCollection = purchaseOrdersPath(db, locationId);
    
    // Update order status - na coleção por localização
    await locationOrdersCollection.doc(payment.orderId).update({
      status: 'PAID',
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    // Create notification for seller
    await db.collection(COLLECTIONS.NOTIFICATIONS).add({
      userId: payment.sellerId,
      orderId: payment.orderId,
      type: 'payment_received',
      title: 'Pagamento PIX Confirmado - Aguardando Envio',
      message: `Pagamento PIX confirmado para o pedido #${payment.orderId.substring(0, 8)}. ` +
        'Confirme o envio para receber o pagamento.',
      read: false,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    // Create notification for client
    await db.collection(COLLECTIONS.NOTIFICATIONS).add({
      userId: payment.clientId,
      orderId: payment.orderId,
      type: 'payment_confirmed',
      title: 'Pagamento PIX Confirmado',
      message: 'Seu pagamento PIX foi confirmado! O vendedor será notificado para enviar o produto.',
      read: false,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    functions.logger.info(`PIX payment confirmed for payment ${paymentId}`);

    return {success: true};
  } catch (error) {
    functions.logger.error('Error confirming PIX payment:', error);
    throw handleError(error);
  }
});

