import * as admin from 'firebase-admin';
import {getFirestore} from './utils/firestore';
import * as functions from 'firebase-functions';
import Stripe from 'stripe';
import {COLLECTIONS, PAYMENT_STATUS} from './utils/constants';
import {assertAuthenticated, handleError} from './utils/errors';
import {validateAppCheck} from './security/appCheck';
import {ordersPath, getUserLocationId} from './utils/firestorePaths';

// Initialize Stripe
const stripe = new Stripe(process.env.STRIPE_SECRET_KEY || '', {
  apiVersion: '2023-10-16',
});

/**
 * Create a payment intent for an order
 */
export const createPaymentIntent = functions.https.onCall(async (data, context) => {
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

    // Get order details - buscar na coleção por localização
    const userId = context.auth!.uid;
    const locationId = await getUserLocationId(db, userId);
    const locationOrdersCollection = ordersPath(db, locationId);
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

    // Update order status - na coleção por localização
    await locationOrdersCollection.doc(orderId).update({
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
    const paymentsSnapshot = await db.collection(COLLECTIONS.PAYMENTS)
      .where('stripePaymentIntentId', '==', paymentIntentId)
      .get();

    if (paymentsSnapshot.empty) {
      throw new functions.https.HttpsError('not-found', 'Payment not found');
    }

    const paymentDoc = paymentsSnapshot.docs[0];
    const payment = paymentDoc.data();
    const orderId = payment.orderId;

    // Update payment status
    await paymentDoc.ref.update({
      status: PAYMENT_STATUS.SUCCEEDED,
      stripePaymentIntentId: paymentIntentId,
      paidAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    // Update order status - na coleção por localização
    // Obter locationId do clientId do payment
    const clientId = payment.clientId;
    const locationId = await getUserLocationId(db, clientId);
    const locationOrdersCollection = ordersPath(db, locationId);
    await locationOrdersCollection.doc(orderId).update({
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
 * Process Google Pay payment via Stripe
 */
export const processGooglePayPayment = functions.https.onCall(async (data, context) => {
  try {
    validateAppCheck(context);
    assertAuthenticated(context);
    
    const db = getFirestore();
    const {orderId, googlePayToken, amount, currency = 'brl'} = data;

    if (!orderId || !googlePayToken || !amount) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'Order ID, Google Pay token, and amount are required'
      );
    }

    // Get order details - buscar na coleção por localização
    const userId = context.auth!.uid;
    const locationId = await getUserLocationId(db, userId);
    const locationOrdersCollection = ordersPath(db, locationId);
    const orderDoc = await locationOrdersCollection.doc(orderId).get();
    if (!orderDoc.exists) {
      throw new functions.https.HttpsError('not-found', 'Order not found');
    }

    const order = orderDoc.data();
    
    if (order?.clientId !== userId) {
      throw new functions.https.HttpsError(
        'permission-denied',
        'Only the order client can process payment'
      );
    }

    // Get provider's Stripe Connect account ID (if applicable)
    let providerStripeAccountId: string | undefined;
    if (order?.providerId) {
      const providerDoc = await db.collection('users').doc(order.providerId).get();
      providerStripeAccountId = providerDoc.data()?.stripeAccountId;
    }

    // Calculate amount in cents
    const amountCents = Math.round(amount * 100);
    const applicationFeePercent = 0.15; // 15% platform fee
    const applicationFeeAmount = Math.round(amountCents * applicationFeePercent);

    // Create Payment Method from Google Pay token first
    const paymentMethod = await stripe.paymentMethods.create({
      type: 'card',
      card: {
        token: googlePayToken,
      },
    });

    // Create Payment Intent with Google Pay payment method
    const paymentIntentParams: Stripe.PaymentIntentCreateParams = {
      amount: amountCents + applicationFeeAmount,
      currency: currency.toLowerCase(),
      payment_method: paymentMethod.id,
      confirm: true,
      return_url: 'taskgo://payment-return',
      metadata: {
        orderId: orderId,
        clientId: order.clientId,
        paymentMethod: 'google_pay',
      },
    };

    // Add Stripe Connect destination if provider has account
    if (providerStripeAccountId) {
      paymentIntentParams.application_fee_amount = applicationFeeAmount;
      paymentIntentParams.transfer_data = {
        destination: providerStripeAccountId,
      };
    }

    const paymentIntent = await stripe.paymentIntents.create(paymentIntentParams);

    // Create payment document
    await db.collection(COLLECTIONS.PAYMENTS).add({
      orderId: orderId,
      clientId: order.clientId,
      providerId: order.providerId || null,
      amount: amount,
      applicationFee: applicationFeeAmount / 100,
      currency: currency.toLowerCase(),
      stripePaymentIntentId: paymentIntent.id,
      paymentMethod: 'google_pay',
      status: paymentIntent.status === 'succeeded' ? PAYMENT_STATUS.SUCCEEDED : PAYMENT_STATUS.PROCESSING,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    // Update order status - na coleção por localização
    await locationOrdersCollection.doc(orderId).update({
      status: paymentIntent.status === 'succeeded' ? 'paid' : 'payment_pending',
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    functions.logger.info(`Google Pay payment processed for order ${orderId}`);
    
    return {
      success: paymentIntent.status === 'succeeded',
      paymentIntentId: paymentIntent.id,
      status: paymentIntent.status,
    };
  } catch (error) {
    functions.logger.error('Error processing Google Pay payment:', error);
    throw handleError(error);
  }
});

/**
 * Request refund for an order
 */
export const requestRefund = functions.https.onCall(async (data, context) => {
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

    // Get order details - buscar na coleção por localização
    const userId = context.auth!.uid;
    const locationId = await getUserLocationId(db, userId);
    const locationOrdersCollection = ordersPath(db, locationId);
    const orderDoc = await locationOrdersCollection.doc(orderId).get();
    if (!orderDoc.exists) {
      throw new functions.https.HttpsError('not-found', 'Order not found');
    }

    const order = orderDoc.data();
    
    if (order?.clientId !== userId) {
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

    // Update order to disputed - na coleção por localização
    await locationOrdersCollection.doc(orderId).update({
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
