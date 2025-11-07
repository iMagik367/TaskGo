import * as admin from 'firebase-admin';
import * as functions from 'firebase-functions';
import Stripe from 'stripe';
import {COLLECTIONS, PAYMENT_STATUS} from './utils/constants';

// Initialize Stripe
const stripe = new Stripe(process.env.STRIPE_SECRET_KEY || '', {
  apiVersion: '2023-10-16',
});

/**
 * Stripe webhook endpoint
 */
export const stripeWebhook = functions.https.onRequest(async (req, res) => {
  const sig = req.headers['stripe-signature'] as string;
  const webhookSecret = process.env.STRIPE_WEBHOOK_SECRET;

  if (!webhookSecret) {
    functions.logger.error('Stripe webhook secret not configured');
    res.status(400).send('Webhook secret not configured');
    return;
  }

  let event: Stripe.Event;

  try {
    event = stripe.webhooks.constructEvent(req.rawBody, sig, webhookSecret);
  } catch (err) {
    functions.logger.error('Webhook signature verification failed:', err);
    res.status(400).send(`Webhook Error: ${err}`);
    return;
  }

  const db = admin.firestore();

  try {
    switch (event.type) {
      case 'payment_intent.succeeded':
        await handlePaymentIntentSucceeded(event.data.object as Stripe.PaymentIntent, db);
        break;

      case 'payment_intent.payment_failed':
        await handlePaymentIntentFailed(event.data.object as Stripe.PaymentIntent, db);
        break;

      case 'account.updated':
        await handleAccountUpdated(event.data.object as Stripe.Account, db);
        break;

      case 'transfer.created':
        await handleTransferCreated(event.data.object as Stripe.Transfer, db);
        break;

      default:
        functions.logger.log(`Unhandled event type: ${event.type}`);
    }

    res.json({received: true});
  } catch (error) {
    functions.logger.error('Error processing webhook:', error);
    res.status(500).send('Webhook processing failed');
  }
});

/**
 * Handle successful payment intent
 */
async function handlePaymentIntentSucceeded(
  paymentIntent: Stripe.PaymentIntent,
  db: admin.firestore.Firestore,
) {
  const orderId = paymentIntent.metadata?.orderId;

  if (!orderId) {
    functions.logger.error('No order ID in payment intent metadata');
    return;
  }

  // Find payment document
  const paymentsSnapshot = await db.collection(COLLECTIONS.PAYMENTS)
    .where('stripePaymentIntentId', '==', paymentIntent.id)
    .get();

  if (paymentsSnapshot.empty) {
    functions.logger.error(`No payment document found for ${paymentIntent.id}`);
    return;
  }

  const paymentDoc = paymentsSnapshot.docs[0];
  const payment = paymentDoc.data();

  // Update payment status
  await paymentDoc.ref.update({
    status: PAYMENT_STATUS.SUCCEEDED,
    paidAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  });

  // Update order status
  const orderRef = db.collection(COLLECTIONS.ORDERS).doc(orderId);
  await orderRef.update({
    status: 'paid',
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  });

  // Create notification for provider
  await db.collection(COLLECTIONS.NOTIFICATIONS).add({
    userId: payment.providerId,
    orderId: orderId,
    type: 'payment_received',
    title: 'Payment Received',
    message: 'Payment for your order has been confirmed',
    read: false,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
  });

  // Create notification for client
  await db.collection(COLLECTIONS.NOTIFICATIONS).add({
    userId: payment.clientId,
    orderId: orderId,
    type: 'payment_received',
    title: 'Payment Confirmed',
    message: 'Your payment has been processed successfully',
    read: false,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
  });

  functions.logger.info(`Payment succeeded for order ${orderId}`);
}

/**
 * Handle failed payment intent
 */
async function handlePaymentIntentFailed(
  paymentIntent: Stripe.PaymentIntent,
  db: admin.firestore.Firestore,
) {
  const orderId = paymentIntent.metadata?.orderId;

  if (!orderId) {
    functions.logger.error('No order ID in payment intent metadata');
    return;
  }

  // Find payment document
  const paymentsSnapshot = await db.collection(COLLECTIONS.PAYMENTS)
    .where('stripePaymentIntentId', '==', paymentIntent.id)
    .get();

  if (!paymentsSnapshot.empty) {
    const paymentDoc = paymentsSnapshot.docs[0];
    await paymentDoc.ref.update({
      status: PAYMENT_STATUS.FAILED,
      failedAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    // Notify client
    const payment = paymentDoc.data();
    await db.collection(COLLECTIONS.NOTIFICATIONS).add({
      userId: payment.clientId,
      orderId: orderId,
      type: 'system_alert',
      title: 'Payment Failed',
      message: 'Your payment could not be processed. Please try again.',
      read: false,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });
  }

  functions.logger.info(`Payment failed for order ${orderId}`);
}

/**
 * Handle account updated event
 */
async function handleAccountUpdated(
  account: Stripe.Account,
  db: admin.firestore.Firestore,
) {
  // Find user with this Stripe account ID
  const usersSnapshot = await db.collection('users')
    .where('stripeAccountId', '==', account.id)
    .get();

  if (usersSnapshot.empty) {
    functions.logger.error(`No user found for Stripe account ${account.id}`);
    return;
  }

  const userDoc = usersSnapshot.docs[0];

  // Update user document with account capabilities
  await userDoc.ref.update({
    stripeChargesEnabled: account.charges_enabled,
    stripePayoutsEnabled: account.payouts_enabled,
    stripeDetailsSubmitted: account.details_submitted,
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  });

  functions.logger.info(`Stripe account updated for user ${userDoc.id}`);
}

/**
 * Handle transfer created event
 */
async function handleTransferCreated(
  transfer: Stripe.Transfer,
  db: admin.firestore.Firestore,
) {
  const metadata = transfer.metadata || {};
  const orderId = metadata.orderId;

  if (!orderId) {
    functions.logger.warn('Transfer created without order ID in metadata');
    return;
  }

  // Log transfer for accounting purposes
  await db.collection('transfers').add({
    orderId: orderId,
    stripeTransferId: transfer.id,
    destination: transfer.destination as string,
    amount: transfer.amount / 100,
    currency: transfer.currency,
    created: admin.firestore.FieldValue.serverTimestamp(),
  });

  functions.logger.info(`Transfer created for order ${orderId}`);
}
