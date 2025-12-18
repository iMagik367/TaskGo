import * as admin from 'firebase-admin';
import * as functions from 'firebase-functions';
import {COLLECTIONS} from './utils/constants';
import {handleError} from './utils/errors';

/**
 * Trigger: Send notification when product order status changes
 * Monitors purchase_orders collection for status changes
 */
export const onProductOrderStatusChange = functions.firestore
  .document('purchase_orders/{orderId}')
  .onUpdate(async (change, context) => {
    const before = change.before.data();
    const after = change.after.data();
    const orderId = context.params.orderId;

    // Skip if status didn't change
    if (before?.status === after?.status) {
      return null;
    }

    const db = admin.firestore();

    // Determine notification recipients and messages based on status change
    const notifications: Array<{userId: string; title: string; message: string; type: string}> = [];

    switch (after?.status) {
      case 'PAID':
        // Notify store that payment was confirmed
        if (after?.storeId) {
          notifications.push({
            userId: after.storeId,
            title: 'Pagamento Confirmado',
            message: `Pedido #${after.orderNumber || orderId} - Pagamento confirmado! Prepare o pedido.`,
            type: 'order_paid',
          });
        }
        // Notify client that payment was processed
        notifications.push({
          userId: after.clientId,
          title: 'Pagamento Confirmado',
          message: `Seu pedido #${after.orderNumber || orderId} foi confirmado! Aguardando preparo.`,
          type: 'order_paid',
        });
        break;

      case 'PREPARING':
        notifications.push({
          userId: after.clientId,
          title: 'Pedido em Preparo',
          message: `Seu pedido #${after.orderNumber || orderId} está sendo preparado!`,
          type: 'order_preparing',
        });
        break;

      case 'SHIPPED': {
        // Create tracking event
        const trackingCode = after.trackingCode || `TG${orderId.substring(orderId.length - 9).padStart(9, '0')}BR`;
        await db.collection('tracking_events').add({
          orderId: orderId,
          type: 'SHIPPED',
          description: 'Pedido enviado pela loja',
          timestamp: admin.firestore.FieldValue.serverTimestamp(),
          done: true,
        });

        notifications.push({
          userId: after.clientId,
          title: 'Pedido Enviado!',
          message: `Seu pedido #${after.orderNumber || orderId} foi enviado! Código de rastreamento: ${trackingCode}`,
          type: 'order_shipped',
        });
        break;
      }

      case 'IN_TRANSIT':
        await db.collection('tracking_events').add({
          orderId: orderId,
          type: 'IN_TRANSIT',
          description: 'Pedido em trânsito',
          timestamp: admin.firestore.FieldValue.serverTimestamp(),
          done: true,
        });

        notifications.push({
          userId: after.clientId,
          title: 'Pedido em Trânsito',
          message: `Seu pedido #${after.orderNumber || orderId} está a caminho!`,
          type: 'order_in_transit',
        });
        break;

      case 'OUT_FOR_DELIVERY':
        await db.collection('tracking_events').add({
          orderId: orderId,
          type: 'OUT_FOR_DELIVERY',
          description: 'Pedido saiu para entrega',
          timestamp: admin.firestore.FieldValue.serverTimestamp(),
          done: true,
        });

        notifications.push({
          userId: after.clientId,
          title: 'Saiu para Entrega',
          message: `Seu pedido #${after.orderNumber || orderId} saiu para entrega! Chegando em breve.`,
          type: 'order_out_for_delivery',
        });
        break;

      case 'DELIVERED':
        await db.collection('tracking_events').add({
          orderId: orderId,
          type: 'DELIVERED',
          description: 'Pedido entregue',
          timestamp: admin.firestore.FieldValue.serverTimestamp(),
          done: true,
        });

        // Update order with delivered timestamp
        await change.after.ref.update({
          deliveredAt: admin.firestore.FieldValue.serverTimestamp(),
        });

        notifications.push({
          userId: after.clientId,
          title: 'Pedido Entregue!',
          message: `Seu pedido #${after.orderNumber || orderId} foi entregue! Avalie sua experiência.`,
          type: 'order_delivered',
        });
        break;

      case 'CANCELLED':
        notifications.push({
          userId: after.clientId,
          title: 'Pedido Cancelado',
          message: `Seu pedido #${after.orderNumber || orderId} foi cancelado.`,
          type: 'order_cancelled',
        });
        break;
    }

    // Send notifications
    for (const notification of notifications) {
      await db.collection(COLLECTIONS.NOTIFICATIONS).add({
        userId: notification.userId,
        orderId: orderId,
        type: notification.type,
        title: notification.title,
        message: notification.message,
        read: false,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
      });
    }

    functions.logger.info(
      `Sent notifications for product order ${orderId} status change: ${before?.status} -> ${after?.status}`
    );
    return null;
  });

/**
 * Trigger: Create initial tracking event when product order is created
 */
export const onProductOrderCreated = functions.firestore
  .document('purchase_orders/{orderId}')
  .onCreate(async (snapshot, context) => {
    const orderId = context.params.orderId;
    const orderData = snapshot.data();
    const db = admin.firestore();

    try {
      // Generate tracking code
      const trackingCode = `TG${orderId.substring(orderId.length - 9).padStart(9, '0')}BR`;

      // Update order with tracking code
      await snapshot.ref.update({
        trackingCode: trackingCode,
        status: 'PENDING_PAYMENT',
      });

      // Create initial tracking event
      await db.collection('tracking_events').add({
        orderId: orderId,
        type: 'PENDING_PAYMENT',
        description: 'Pedido criado - Aguardando pagamento',
        timestamp: admin.firestore.FieldValue.serverTimestamp(),
        done: true,
      });

      // Notify store about new order
      if (orderData?.storeId) {
        await db.collection(COLLECTIONS.NOTIFICATIONS).add({
          userId: orderData.storeId,
          orderId: orderId,
          type: 'new_order',
          title: 'Novo Pedido Recebido',
          message: `Novo pedido #${orderData.orderNumber || orderId} recebido!`,
          read: false,
          createdAt: admin.firestore.FieldValue.serverTimestamp(),
        });
      }

      functions.logger.info(`Created tracking for product order ${orderId}`);
    } catch (error) {
      functions.logger.error(`Error creating tracking for order ${orderId}:`, error);
    }

    return null;
  });

/**
 * Update order status (for stores to mark as shipped/delivered)
 */
export const updateProductOrderStatus = functions.https.onCall(async (data, context) => {
  try {
    if (!context.auth) {
      throw new functions.https.HttpsError('unauthenticated', 'User must be authenticated');
    }

    const db = admin.firestore();
    const {orderId, status} = data;

    if (!orderId || !status) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'Order ID and status are required'
      );
    }

    const orderDoc = await db.collection('purchase_orders').doc(orderId).get();
    if (!orderDoc.exists) {
      throw new functions.https.HttpsError('not-found', 'Order not found');
    }

    const orderData = orderDoc.data();
    const userId = context.auth.uid;

    // Verify permissions (store owner or client)
    if (orderData?.storeId !== userId && orderData?.clientId !== userId) {
      throw new functions.https.HttpsError('permission-denied', 'Insufficient permissions');
    }

    const updateData: Record<string, unknown> = {
      status: status,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    };

    // Add timestamps based on status
    if (status === 'SHIPPED') {
      updateData.shippedAt = admin.firestore.FieldValue.serverTimestamp();
    } else if (status === 'DELIVERED') {
      updateData.deliveredAt = admin.firestore.FieldValue.serverTimestamp();
    }

    await orderDoc.ref.update(updateData);

    functions.logger.info(`Product order ${orderId} status updated to ${status}`);
    return {success: true};
  } catch (error) {
    functions.logger.error('Error updating product order status:', error);
    throw handleError(error);
  }
});

