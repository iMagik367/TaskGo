import * as admin from 'firebase-admin';
import * as functions from 'firebase-functions';
import Stripe from 'stripe';
import {COLLECTIONS, PAYMENT_STATUS} from './utils/constants';
import {assertAuthenticated, handleError} from './utils/errors';

// Initialize Stripe
const stripe = new Stripe(process.env.STRIPE_SECRET_KEY || '', {
  apiVersion: '2023-10-16',
});

/**
 * Create a payment intent for an order
 */
export const createPaymentIntent = functions.https.onCall(async (data, context) => {
  try {
    assertAuthenticated(context);
    
    const db = admin.firestore();
    const {orderId} = data;

    if (!orderId) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'Order ID is required'
      );
    }

    // Get order details
    const orderDoc = await db.collection(COLLECTIONS.ORDERS).doc(orderId).get();
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

    if (order?.status !== 'accepted') {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'Order must be accepted before payment'
      );
    }

    // Get provider's Stripe Connect account ID
    const providerDoc = await db.collection('users').doc(order?.providerId).get();
    const provider = providerDoc.data();
    
    if (!provider?.stripeAccountId) {
      throw new functions.https.HttpsError(
        'failed-precondition',
        'Provider has not completed Stripe Connect onboarding'
      );
    }

    // Calculate total amount (in cents)
    const amount = Math.round((order?.budget || order?.proposalDetails?.price || 0) * 100);

    // Create Payment Intent with application fee
    const applicationFeePercent = 0.15; // 15% platform fee
    const applicationFeeAmount = Math.round(amount * applicationFeePercent);

    const paymentIntent = await stripe.paymentIntents.create({
      amount: amount + applicationFeeAmount, // Include platform fee in total
      currency: 'usd',
      application_fee_amount: applicationFeeAmount,
      transfer_data: {
        destination: provider.stripeAccountId,
      },
      metadata: {
        orderId: orderId,
        clientId: order.clientId,
        providerId: order.providerId,
      },
    });

    // Create payment document
    await db.collection(COLLECTIONS.PAYMENTS).add({
      orderId: orderId,
      clientId: order.clientId,
      providerId: order.providerId,
      amount: amount / 100, // Convert back to dollars
      applicationFee: applicationFeeAmount / 100,
      currency: 'usd',
      stripePaymentIntentId: paymentIntent.id,
      status: PAYMENT_STATUS.PENDING,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    // Update order status
    await db.collection(COLLECTIONS.ORDERS).doc(orderId).update({
      status: 'payment_pending',
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    functions.logger.info(`Payment intent created for order ${orderId}`);
    
    return {
      clientSecret: paymentIntent.client_secret,
      paymentIntentId: paymentIntent.id,
    };
  } catch (error) {
    functions.logger.error('Error creating payment intent:', error);
    throw handleError(error);
  }
});

/**
 * Confirm payment completion
 */
export const confirmPayment = functions.https.onCall(async (data, context) => {
  try {
    assertAuthenticated(context);
    
    const db = admin.firestore();
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
    const paymentsSnapshot = await db.collection(COLLECTIONS.PAYMENTS)
      .where('stripePaymentIntentId', '==', paymentIntentId)
      .get();

    if (paymentsSnapshot.empty) {
      throw new functions.https.HttpsError('not-found', 'Payment not found');
    }

    const paymentDoc = paymentsSnapshot.docs[0];
    const orderId = paymentDoc.data().orderId;

    // Update payment status
    await paymentDoc.ref.update({
      status: PAYMENT_STATUS.SUCCEEDED,
      stripePaymentIntentId: paymentIntentId,
      paidAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    // Update order status
    await db.collection(COLLECTIONS.ORDERS).doc(orderId).update({
      status: 'paid',
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    // Create notification for provider
    await db.collection(COLLECTIONS.NOTIFICATIONS).add({
      userId: paymentDoc.data().providerId,
      orderId: orderId,
      type: 'payment_received',
      title: 'Payment Received',
      message: 'Payment for your order has been confirmed',
      read: false,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    functions.logger.info(`Payment confirmed for ${paymentIntentId}`);
    return {success: true};
  } catch (error) {
    functions.logger.error('Error confirming payment:', error);
    throw handleError(error);
  }
});

/**
 * Request refund for an order
 */
export const requestRefund = functions.https.onCall(async (data, context) => {
  try {
    assertAuthenticated(context);
    
    const db = admin.firestore();
    const {orderId, reason} = data;

    if (!orderId) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'Order ID is required'
      );
    }

    const orderDoc = await db.collection(COLLECTIONS.ORDERS).doc(orderId).get();
    if (!orderDoc.exists) {
      throw new functions.https.HttpsError('not-found', 'Order not found');
    }

    const order = orderDoc.data();
    
    if (order?.clientId !== context.auth!.uid) {
      throw new functions.https.HttpsError(
        'permission-denied',
        'Only the order client can request refund'
      );
    }

    // Get payment info
    const paymentsSnapshot = await db.collection(COLLECTIONS.PAYMENTS)
      .where('orderId', '==', orderId)
      .where('status', '==', PAYMENT_STATUS.SUCCEEDED)
      .get();

    if (paymentsSnapshot.empty) {
      throw new functions.https.HttpsError(
        'failed-precondition',
        'No successful payment found for this order'
      );
    }

    // Update order to disputed
    await db.collection(COLLECTIONS.ORDERS).doc(orderId).update({
      status: 'disputed',
      disputeReason: reason,
      disputedAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    functions.logger.info(`Refund requested for order ${orderId}`);
    
    // TODO: Implement actual refund logic through Stripe
    // This requires admin review in production
    return {success: true, message: 'Refund request submitted for review'};
  } catch (error) {
    functions.logger.error('Error requesting refund:', error);
    throw handleError(error);
  }
});
