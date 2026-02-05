import * as admin from 'firebase-admin';
import {getFirestore} from './utils/firestore';
import * as functions from 'firebase-functions';
import Stripe from 'stripe';
import {COLLECTIONS, PAYMENT_STATUS} from './utils/constants';
import {assertAuthenticated, handleError} from './utils/errors';
import {validateAppCheck} from './security/appCheck';
import {purchaseOrdersPath, getUserLocationId} from './utils/firestorePaths';

// Initialize Stripe
const stripe = new Stripe(process.env.STRIPE_SECRET_KEY || '', {
  apiVersion: '2023-10-16',
});

/**
 * Create payment intent for product purchase with 2% platform fee
 */
export const createProductPaymentIntent = functions.https.onCall(async (data, context) => {
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
    
    if (order?.clientId !== userId) {
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

    // Get seller's Stripe Connect account ID
    const sellerId = order?.storeId || null;
    if (!sellerId) {
      throw new functions.https.HttpsError(
        'failed-precondition',
        'Order must have a seller/store ID'
      );
    }

    const sellerDoc = await db.collection('users').doc(sellerId).get();
    const seller = sellerDoc.data();
    
    if (!seller?.stripeAccountId) {
      throw new functions.https.HttpsError(
        'failed-precondition',
        'Seller has not completed Stripe Connect onboarding'
      );
    }

    // Calculate amounts (in cents for BRL)
    const totalAmount = Math.round(order.total * 100); // Total em centavos
    const platformFeePercent = 0.02; // 2% comissão
    const platformFeeAmount = Math.round(totalAmount * platformFeePercent);
    const sellerAmount = totalAmount - platformFeeAmount; // 98% para o vendedor

    // Create Payment Intent WITHOUT immediate transfer
    // Transfer will happen only after shipment confirmation
    const paymentIntent = await stripe.paymentIntents.create({
      amount: totalAmount,
      currency: 'brl',
      // NO transfer_data here - we'll transfer manually after shipment confirmation
      // NO application_fee_amount here - we'll handle fees manually
      metadata: {
        orderId: orderId,
        clientId: order.clientId,
        sellerId: sellerId,
        sellerStripeAccountId: seller.stripeAccountId,
        type: 'product_payment',
        platformFeeAmount: platformFeeAmount.toString(),
        sellerAmount: sellerAmount.toString(),
      },
      capture_method: 'automatic', // Automatically capture payment
    });

    // Create payment document
    await db.collection('product_payments').add({
      orderId: orderId,
      clientId: order.clientId,
      sellerId: sellerId,
      storeId: order.storeId || null,
      totalAmount: order.total,
      sellerAmount: sellerAmount / 100, // Convert back to reais
      platformFee: platformFeeAmount / 100,
      currency: 'BRL',
      paymentMethod: order.paymentMethod || 'UNKNOWN',
      stripePaymentIntentId: paymentIntent.id,
      stripeChargeId: null, // Will be set when payment is confirmed
      stripeTransferId: null, // Will be set when shipment is confirmed
      status: PAYMENT_STATUS.PENDING,
      transferStatus: 'PENDING_SHIPMENT', // PENDING_SHIPMENT, TRANSFERRED, REFUNDED
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    // Update order status - na coleção por localização
    await locationOrdersCollection.doc(orderId).update({
      status: 'PAYMENT_PENDING',
      paymentIntentId: paymentIntent.id,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    functions.logger.info(`Product payment intent created for order ${orderId}`);
    
    return {
      clientSecret: paymentIntent.client_secret,
      paymentIntentId: paymentIntent.id,
    };
  } catch (error) {
    functions.logger.error('Error creating product payment intent:', error);
    throw handleError(error);
  }
});

/**
 * Confirm product payment completion
 */
export const confirmProductPayment = functions.https.onCall(async (data, context) => {
  try {
    validateAppCheck(context);
    assertAuthenticated(context);
    
    const db = getFirestore();
    const {paymentIntentId} = data;

    if (!paymentIntentId) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'Payment Intent ID is required'
      );
    }

    // Retrieve payment intent from Stripe
    const paymentIntent = await stripe.paymentIntents.retrieve(paymentIntentId);

    if (paymentIntent.status !== 'succeeded') {
      throw new functions.https.HttpsError(
        'failed-precondition',
        `Payment not succeeded. Status: ${paymentIntent.status}`
      );
    }

    // Update payment document
    const paymentsSnapshot = await db.collection('product_payments')
      .where('stripePaymentIntentId', '==', paymentIntentId)
      .get();

    if (paymentsSnapshot.empty) {
      throw new functions.https.HttpsError('not-found', 'Payment not found');
    }

    const paymentDoc = paymentsSnapshot.docs[0];
    const paymentData = paymentDoc.data();
    const orderId = paymentData.orderId;

    // Get charge ID from payment intent
    let chargeId: string | null = null;
    try {
      const charges = await stripe.charges.list({
        payment_intent: paymentIntentId,
        limit: 1,
      });
      if (charges.data.length > 0) {
        chargeId = charges.data[0].id;
      }
    } catch (error) {
      functions.logger.warn('Could not retrieve charge ID:', error);
    }

    // Update payment status - payment confirmed but NOT transferred yet
    // Transfer will happen only after shipment confirmation
    await paymentDoc.ref.update({
      status: PAYMENT_STATUS.SUCCEEDED,
      stripeChargeId: chargeId,
      transferStatus: 'PENDING_SHIPMENT', // Waiting for shipment confirmation
      paidAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    // CRÍTICO: Buscar order na coleção por localização
    const clientId = paymentData.clientId;
    const locationId = await getUserLocationId(db, clientId);
    const locationOrdersCollection = purchaseOrdersPath(db, locationId);
    
    // Update order status - na coleção por localização
    await locationOrdersCollection.doc(orderId).update({
      status: 'PAID',
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    // Create notification for seller
    await db.collection(COLLECTIONS.NOTIFICATIONS).add({
      userId: paymentData.sellerId,
      orderId: orderId,
      type: 'payment_received',
      title: 'Pagamento Confirmado - Aguardando Envio',
      message: `Pagamento confirmado para o pedido #${orderId.substring(0, 8)}. ` +
        'Confirme o envio para receber o pagamento.',
      read: false,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    // Create notification for client
    await db.collection(COLLECTIONS.NOTIFICATIONS).add({
      userId: paymentData.clientId,
      orderId: orderId,
      type: 'payment_confirmed',
      title: 'Pagamento Confirmado',
      message: 'Seu pagamento foi confirmado! O vendedor será notificado para enviar o produto.',
      read: false,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    functions.logger.info(`Product payment confirmed for ${paymentIntentId}`);
    return {success: true};
  } catch (error) {
    functions.logger.error('Error confirming product payment:', error);
    throw handleError(error);
  }
});

/**
 * Transfer payment to seller after shipment confirmation
 * This function should be called when seller confirms shipment
 */
export const transferPaymentToSeller = functions.https.onCall(async (data, context) => {
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
    
    // Verify that the caller is the seller
    if (order?.storeId !== userId) {
      throw new functions.https.HttpsError(
        'permission-denied',
        'Only the seller can confirm shipment and receive payment'
      );
    }

    // Check if order has been shipped
    const shipmentSnapshot = await db.collection('shipments')
      .where('orderId', '==', orderId)
      .where('status', 'in', ['SHIPPED', 'IN_TRANSIT', 'OUT_FOR_DELIVERY', 'DELIVERED'])
      .limit(1)
      .get();

    if (shipmentSnapshot.empty) {
      throw new functions.https.HttpsError(
        'failed-precondition',
        'Order must be shipped before payment can be transferred'
      );
    }

    // Get payment document
    const paymentsSnapshot = await db.collection('product_payments')
      .where('orderId', '==', orderId)
      .limit(1)
      .get();

    if (paymentsSnapshot.empty) {
      throw new functions.https.HttpsError('not-found', 'Payment not found');
    }

    const paymentDoc = paymentsSnapshot.docs[0];
    const paymentData = paymentDoc.data();

    // Check if payment was already transferred
    if (paymentData.transferStatus === 'TRANSFERRED') {
      throw new functions.https.HttpsError(
        'failed-precondition',
        'Payment has already been transferred'
      );
    }

    if (paymentData.status !== PAYMENT_STATUS.SUCCEEDED) {
      throw new functions.https.HttpsError(
        'failed-precondition',
        'Payment must be succeeded before transfer'
      );
    }

    // Get seller's Stripe account
    const sellerDoc = await db.collection('users').doc(order.storeId).get();
    const seller = sellerDoc.data();

    if (!seller?.stripeAccountId) {
      throw new functions.https.HttpsError(
        'failed-precondition',
        'Seller has not completed Stripe Connect onboarding'
      );
    }

    // Get payment intent to retrieve charge
    const paymentIntent = await stripe.paymentIntents.retrieve(
      paymentData.stripePaymentIntentId
    );

    if (paymentIntent.status !== 'succeeded') {
      throw new functions.https.HttpsError(
        'failed-precondition',
        'Payment intent must be succeeded'
      );
    }

    // Get charge
    const charges = await stripe.charges.list({
      payment_intent: paymentData.stripePaymentIntentId,
      limit: 1,
    });

    if (charges.data.length === 0) {
      throw new functions.https.HttpsError(
        'failed-precondition',
        'Charge not found for payment intent'
      );
    }

    const charge = charges.data[0];
    
    // Calculate amounts (from metadata or recalculate)
    const totalAmount = Math.round(paymentData.totalAmount * 100); // in cents
    const platformFeeAmount = Math.round(totalAmount * 0.02); // 2%
    const sellerAmount = totalAmount - platformFeeAmount; // 98%

    // Create transfer to seller (98% of the amount)
    const transfer = await stripe.transfers.create({
      amount: sellerAmount,
      currency: 'brl',
      destination: seller.stripeAccountId,
      source_transaction: charge.id, // Transfer from the charge
      metadata: {
        orderId: orderId,
        sellerId: order.storeId,
        type: 'product_payment_transfer',
      },
    });

    // Update payment document
    await paymentDoc.ref.update({
      stripeTransferId: transfer.id,
      transferStatus: 'TRANSFERRED',
      transferredAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    // Update order status - na coleção por localização
    await locationOrdersCollection.doc(orderId).update({
      status: 'SHIPPED',
      paymentTransferred: true,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    // Create notification for seller
    await db.collection(COLLECTIONS.NOTIFICATIONS).add({
      userId: order.storeId,
      orderId: orderId,
      type: 'payment_transferred',
      title: 'Pagamento Transferido',
      message: `Pagamento de R$ ${(sellerAmount / 100).toFixed(2)} foi transferido para sua conta!`,
      read: false,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    // Create notification for client
    await db.collection(COLLECTIONS.NOTIFICATIONS).add({
      userId: order.clientId,
      orderId: orderId,
      type: 'order_shipped',
      title: 'Pedido Enviado',
      message: `Seu pedido #${orderId.substring(0, 8)} foi enviado! Você pode rastrear o envio.`,
      read: false,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    functions.logger.info(`Payment transferred to seller for order ${orderId}`);
    return {
      success: true,
      transferId: transfer.id,
      amount: sellerAmount / 100,
    };
  } catch (error) {
    functions.logger.error('Error transferring payment to seller:', error);
    throw handleError(error);
  }
});

/**
 * Refund payment if order is cancelled before shipment
 */
export const refundProductPayment = functions.https.onCall(async (data, context) => {
  try {
    validateAppCheck(context);
    assertAuthenticated(context);
    
    const db = getFirestore();
    const {orderId, reason} = data;

    if (!orderId) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'Order ID is required'
      );
    }

    // CRÍTICO: Buscar order na coleção por localização
    // Precisamos buscar em todas as localizações pois não sabemos qual é
    const userId = context.auth!.uid;
    const locationsSnapshot = await db.collection('locations').limit(100).get();
    let orderDoc: admin.firestore.DocumentSnapshot | null = null;
    let locationOrdersCollection: admin.firestore.CollectionReference | null = null;
    
    for (const locationDoc of locationsSnapshot.docs) {
      const locOrdersCollection = purchaseOrdersPath(db, locationDoc.id);
      const doc = await locOrdersCollection.doc(orderId).get();
      if (doc.exists) {
        orderDoc = doc;
        locationOrdersCollection = locOrdersCollection;
        break;
      }
    }
    
    if (!orderDoc || !orderDoc.exists) {
      throw new functions.https.HttpsError('not-found', 'Order not found');
    }

    const order = orderDoc.data();
    
    // Only client or seller can request refund (before shipment)
    if (order?.clientId !== userId && order?.storeId !== userId) {
      throw new functions.https.HttpsError(
        'permission-denied',
        'Only the client or seller can request refund'
      );
    }

    // Check if order was already shipped
    const shipmentSnapshot = await db.collection('shipments')
      .where('orderId', '==', orderId)
      .where('status', 'in', ['SHIPPED', 'IN_TRANSIT', 'OUT_FOR_DELIVERY', 'DELIVERED'])
      .limit(1)
      .get();

    if (!shipmentSnapshot.empty) {
      throw new functions.https.HttpsError(
        'failed-precondition',
        'Cannot refund order that has already been shipped'
      );
    }

    // Get payment document
    const paymentsSnapshot = await db.collection('product_payments')
      .where('orderId', '==', orderId)
      .limit(1)
      .get();

    if (paymentsSnapshot.empty) {
      throw new functions.https.HttpsError('not-found', 'Payment not found');
    }

    const paymentDoc = paymentsSnapshot.docs[0];
    const paymentData = paymentDoc.data();

    if (paymentData.transferStatus === 'TRANSFERRED') {
      throw new functions.https.HttpsError(
        'failed-precondition',
        'Cannot refund payment that has already been transferred'
      );
    }

    if (paymentData.status !== PAYMENT_STATUS.SUCCEEDED) {
      throw new functions.https.HttpsError(
        'failed-precondition',
        'Payment must be succeeded to refund'
      );
    }

    // Get charge
    const charges = await stripe.charges.list({
      payment_intent: paymentData.stripePaymentIntentId,
      limit: 1,
    });

    if (charges.data.length === 0) {
      throw new functions.https.HttpsError('not-found', 'Charge not found');
    }

    const charge = charges.data[0];

    // Create refund
    const refund = await stripe.refunds.create({
      charge: charge.id,
      amount: Math.round(paymentData.totalAmount * 100), // Full refund in cents
      metadata: {
        orderId: orderId,
        reason: reason || 'Order cancelled',
      },
    });

    // Update payment document
    await paymentDoc.ref.update({
      transferStatus: 'REFUNDED',
      stripeRefundId: refund.id,
      refundedAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    // Update order status - na coleção por localização
    if (locationOrdersCollection) {
      await locationOrdersCollection.doc(orderId).update({
        status: 'CANCELLED',
        refunded: true,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });
    }

    // Create notifications
    await db.collection(COLLECTIONS.NOTIFICATIONS).add({
      userId: order.clientId,
      orderId: orderId,
      type: 'refund_processed',
      title: 'Reembolso Processado',
      message: `Reembolso de R$ ${paymentData.totalAmount.toFixed(2)} foi processado ` +
        `para o pedido #${orderId.substring(0, 8)}`,
      read: false,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    functions.logger.info(`Payment refunded for order ${orderId}`);
    return {
      success: true,
      refundId: refund.id,
      amount: paymentData.totalAmount,
    };
  } catch (error) {
    functions.logger.error('Error refunding payment:', error);
    throw handleError(error);
  }
});

