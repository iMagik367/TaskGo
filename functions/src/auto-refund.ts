import * as admin from 'firebase-admin';
import {getFirestore} from './utils/firestore';
import * as functions from 'firebase-functions';
import Stripe from 'stripe';
import {COLLECTIONS, PAYMENT_STATUS} from './utils/constants';
import {purchaseOrdersPath} from './utils/firestorePaths';

// Initialize Stripe lazily
function getStripe(): Stripe {
  const secretKey = process.env.STRIPE_SECRET_KEY;
  if (!secretKey) {
    throw new Error('STRIPE_SECRET_KEY not configured');
  }
  return new Stripe(secretKey, {
    apiVersion: '2023-10-16',
  });
}

/**
 * Scheduled function to automatically refund orders that haven't been shipped
 * within 30 minutes of payment confirmation
 * Runs every 5 minutes
 */
export const checkAndRefundUnshippedOrders = functions.pubsub
  .schedule('every 5 minutes')
  .onRun(async (_context) => {
    const db = getFirestore();
    const now = Date.now();
    const thirtyMinutesAgo = now - (30 * 60 * 1000); // 30 minutes in milliseconds

    try {
      functions.logger.info('Checking for unshipped orders to refund...');

      // CRÍTICO: Buscar orders em todas as localizações (não há coleção global)
      // Find all orders that:
      // 1. Status is PAID
      // 2. Paid more than 30 minutes ago
      // 3. Don't have a confirmed shipment
      const locationsSnapshot = await db.collection('locations').limit(100).get();
      
      let refundedCount = 0;
      let errorCount = 0;
      const allPaidOrders: Array<{orderDoc: admin.firestore.DocumentSnapshot; locationId: string}> = [];

      // Coletar todos os orders PAID de todas as localizações
      for (const locationDoc of locationsSnapshot.docs) {
        const locationId = locationDoc.id;
        const locationOrdersCollection = purchaseOrdersPath(db, locationId);
        const paidOrdersSnapshot = await locationOrdersCollection
          .where('status', '==', 'PAID')
          .get();
        
        for (const orderDoc of paidOrdersSnapshot.docs) {
          allPaidOrders.push({orderDoc, locationId});
        }
      }

      for (const {orderDoc, locationId} of allPaidOrders) {
        const order = orderDoc.data();
        if (!order) {
          continue; // Skip if no data
        }
        const orderId = orderDoc.id;

        // Check if order has paidAt timestamp
        const paidAt = order.paidAt?.toMillis();
        if (!paidAt) {
          // Try to get from payment document
          const paymentsSnapshot = await db.collection('product_payments')
            .where('orderId', '==', orderId)
            .where('status', '==', PAYMENT_STATUS.SUCCEEDED)
            .limit(1)
            .get();

          if (paymentsSnapshot.empty) {
            continue; // No payment found, skip
          }

          const paymentData = paymentsSnapshot.docs[0].data();
          const paymentPaidAt = paymentData.paidAt?.toMillis();
          if (!paymentPaidAt || paymentPaidAt > thirtyMinutesAgo) {
            continue; // Not old enough or no timestamp
          }
        } else if (paidAt > thirtyMinutesAgo) {
          continue; // Not old enough
        }

        // Check if shipment exists and is confirmed
        const shipmentSnapshot = await db.collection('shipments')
          .where('orderId', '==', orderId)
          .limit(1)
          .get();

        if (!shipmentSnapshot.empty) {
          const shipment = shipmentSnapshot.docs[0].data();
          // If shipment exists and is confirmed, skip refund
          if (shipment.status === 'SHIPPED' || 
              shipment.status === 'IN_TRANSIT' || 
              shipment.status === 'OUT_FOR_DELIVERY' ||
              shipment.status === 'DELIVERED') {
            continue; // Already shipped, skip
          }
        }

        // Check if already refunded
        if (order.refunded || order.status === 'CANCELLED') {
          continue; // Already refunded
        }

        // Get payment document
        const paymentsSnapshot = await db.collection('product_payments')
          .where('orderId', '==', orderId)
          .limit(1)
          .get();

        if (paymentsSnapshot.empty) {
          functions.logger.warn(`No payment found for order ${orderId}`);
          continue;
        }

        const paymentDoc = paymentsSnapshot.docs[0];
        const paymentData = paymentDoc.data();

        // Check if payment was already transferred or refunded
        if (paymentData.transferStatus === 'TRANSFERRED' || 
            paymentData.transferStatus === 'REFUNDED') {
          continue; // Already processed
        }

        // Check if payment is PIX (different refund logic)
        if (paymentData.paymentMethod === 'PIX') {
          // For PIX, just mark as refunded (no Stripe refund needed)
          await paymentDoc.ref.update({
            transferStatus: 'REFUNDED',
            refundedAt: admin.firestore.FieldValue.serverTimestamp(),
            refundReason: 'Automatic refund: Seller did not confirm shipment within 30 minutes',
            updatedAt: admin.firestore.FieldValue.serverTimestamp(),
          });

          // CRÍTICO: Update order na coleção por localização
          const locationOrdersCollection = purchaseOrdersPath(db, locationId);
          await locationOrdersCollection.doc(orderId).update({
            status: 'CANCELLED',
            refunded: true,
            refundReason: 'Automatic refund: Seller did not confirm shipment within 30 minutes',
            updatedAt: admin.firestore.FieldValue.serverTimestamp(),
          });

          // Create notifications
          await db.collection(COLLECTIONS.NOTIFICATIONS).add({
            userId: order.clientId,
            orderId: orderId,
            type: 'refund_processed',
            title: 'Reembolso Automático Processado',
            message: `Seu pedido #${orderId.substring(0, 8)} foi reembolsado automaticamente ` +
              'pois o vendedor não confirmou o envio em até 30 minutos.',
            read: false,
            createdAt: admin.firestore.FieldValue.serverTimestamp(),
          });

          await db.collection(COLLECTIONS.NOTIFICATIONS).add({
            userId: order.storeId,
            orderId: orderId,
            type: 'order_cancelled',
            title: 'Pedido Cancelado Automaticamente',
            message: `O pedido #${orderId.substring(0, 8)} foi cancelado automaticamente ` +
              'pois você não confirmou o envio em até 30 minutos após o pagamento.',
            read: false,
            createdAt: admin.firestore.FieldValue.serverTimestamp(),
          });

          refundedCount++;
          functions.logger.info(`PIX payment refunded automatically for order ${orderId}`);
          continue;
        }

        // For Stripe payments, create actual refund
        try {
          const stripe = getStripe();
          // Get charge
          const charges = await stripe.charges.list({
            payment_intent: paymentData.stripePaymentIntentId,
            limit: 1,
          });

          if (charges.data.length === 0) {
            functions.logger.warn(`No charge found for payment ${paymentData.stripePaymentIntentId}`);
            errorCount++;
            continue;
          }

          const charge = charges.data[0];

          // Create refund
          const refund = await stripe.refunds.create({
            charge: charge.id,
            amount: Math.round(paymentData.amount * 100), // Full refund in cents
            metadata: {
              orderId: orderId,
              reason: 'Automatic refund: Seller did not confirm shipment within 30 minutes',
              automatic: 'true',
            },
          });

          // Update payment document
          await paymentDoc.ref.update({
            transferStatus: 'REFUNDED',
            stripeRefundId: refund.id,
            refundedAt: admin.firestore.FieldValue.serverTimestamp(),
            refundReason: 'Automatic refund: Seller did not confirm shipment within 30 minutes',
            updatedAt: admin.firestore.FieldValue.serverTimestamp(),
          });

          // CRÍTICO: Update order na coleção por localização
          const locationOrdersCollection = purchaseOrdersPath(db, locationId);
          await locationOrdersCollection.doc(orderId).update({
            status: 'CANCELLED',
            refunded: true,
            refundReason: 'Automatic refund: Seller did not confirm shipment within 30 minutes',
            updatedAt: admin.firestore.FieldValue.serverTimestamp(),
          });

          // Create notifications
          await db.collection(COLLECTIONS.NOTIFICATIONS).add({
            userId: order.clientId,
            orderId: orderId,
            type: 'refund_processed',
            title: 'Reembolso Automático Processado',
            message: `Seu pedido #${orderId.substring(0, 8)} foi reembolsado automaticamente ` +
              `(R$ ${paymentData.amount.toFixed(2)}) pois o vendedor não confirmou o envio em até 30 minutos.`,
            read: false,
            createdAt: admin.firestore.FieldValue.serverTimestamp(),
          });

          await db.collection(COLLECTIONS.NOTIFICATIONS).add({
            userId: order.storeId,
            orderId: orderId,
            type: 'order_cancelled',
            title: 'Pedido Cancelado Automaticamente',
            message: `O pedido #${orderId.substring(0, 8)} foi cancelado automaticamente ` +
              'pois você não confirmou o envio em até 30 minutos após o pagamento.',
            read: false,
            createdAt: admin.firestore.FieldValue.serverTimestamp(),
          });

          refundedCount++;
          functions.logger.info(`Payment refunded automatically for order ${orderId}, refund ID: ${refund.id}`);
        } catch (refundError) {
          functions.logger.error(`Error refunding order ${orderId}:`, refundError);
          errorCount++;
        }
      }

      functions.logger.info(
        `Auto-refund check completed. Refunded: ${refundedCount}, Errors: ${errorCount}`
      );

      return null;
    } catch (error) {
      functions.logger.error('Error in auto-refund check:', error);
      throw error;
    }
  });

