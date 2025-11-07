import * as admin from 'firebase-admin';
import * as functions from 'firebase-functions';
import {COLLECTIONS, ORDER_STATUS} from './utils/constants';
import {assertAuthenticated, handleError} from './utils/errors';

/**
 * Create a new order
 */
export const createOrder = functions.https.onCall(async (data, context) => {
  try {
    assertAuthenticated(context);
    
    const db = admin.firestore();
    const {serviceId, details, location, budget, dueDate} = data;

    if (!serviceId || !details || !location) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'Service ID, details, and location are required'
      );
    }

    // Verify service exists
    const serviceDoc = await db.collection(COLLECTIONS.SERVICES).doc(serviceId).get();
    if (!serviceDoc.exists) {
      throw new functions.https.HttpsError('not-found', 'Service not found');
    }

    const service = serviceDoc.data();
    if (service?.providerId === context.auth!.uid) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'Cannot create order for your own service'
      );
    }

    const orderData = {
      clientId: context.auth!.uid,
      providerId: service?.providerId,
      serviceId: serviceId,
      details,
      location,
      budget,
      dueDate,
      status: ORDER_STATUS.PENDING,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    };

    const orderRef = await db.collection(COLLECTIONS.ORDERS).add(orderData);
    
    // Create notification for provider
    await db.collection(COLLECTIONS.NOTIFICATIONS).add({
      userId: service?.providerId,
      orderId: orderRef.id,
      type: 'order_created',
      title: 'New Order Received',
      message: `You have a new order for ${service?.title || 'your service'}`,
      read: false,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    functions.logger.info(`Order created: ${orderRef.id}`);
    return {orderId: orderRef.id};
  } catch (error) {
    functions.logger.error('Error creating order:', error);
    throw handleError(error);
  }
});

/**
 * Update order status
 */
export const updateOrderStatus = functions.https.onCall(async (data, context) => {
  try {
    assertAuthenticated(context);
    
    const db = admin.firestore();
    const {orderId, status, proposalDetails} = data;

    if (!orderId || !status) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'Order ID and status are required'
      );
    }

    const orderDoc = await db.collection(COLLECTIONS.ORDERS).doc(orderId).get();
    if (!orderDoc.exists) {
      throw new functions.https.HttpsError('not-found', 'Order not found');
    }

    const order = orderDoc.data();
    const isProvider = context.auth!.uid === order?.providerId;
    const isClient = context.auth!.uid === order?.clientId;

    // Verify permissions
    if (!isProvider && !isClient) {
      throw new functions.https.HttpsError('permission-denied', 'Insufficient permissions');
    }

    // Validate status transition
    const allowedTransitions: Record<string, string[]> = {
      [ORDER_STATUS.PENDING]: [ORDER_STATUS.PROPOSED, ORDER_STATUS.CANCELLED],
      [ORDER_STATUS.PROPOSED]: [ORDER_STATUS.ACCEPTED, ORDER_STATUS.CANCELLED],
      [ORDER_STATUS.ACCEPTED]: [ORDER_STATUS.PAYMENT_PENDING, ORDER_STATUS.CANCELLED],
      [ORDER_STATUS.PAYMENT_PENDING]: [ORDER_STATUS.PAID, ORDER_STATUS.CANCELLED],
      [ORDER_STATUS.PAID]: [ORDER_STATUS.IN_PROGRESS, ORDER_STATUS.CANCELLED],
      [ORDER_STATUS.IN_PROGRESS]: [ORDER_STATUS.COMPLETED],
    };

    if (!allowedTransitions[order?.status]?.includes(status)) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        `Invalid status transition from ${order?.status} to ${status}`
      );
    }

    const updateData: Record<string, unknown> = {
      status,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    };

    if (status === ORDER_STATUS.PROPOSED && proposalDetails && isProvider) {
      updateData.proposalDetails = proposalDetails;
      updateData.proposedAt = admin.firestore.FieldValue.serverTimestamp();
    }

    if (status === ORDER_STATUS.ACCEPTED && isClient) {
      updateData.acceptedAt = admin.firestore.FieldValue.serverTimestamp();
    }

    await db.collection(COLLECTIONS.ORDERS).doc(orderId).update(updateData);

    // Create appropriate notification
    const notifications = [];
    if (status === ORDER_STATUS.PROPOSED) {
      notifications.push({
        userId: order?.clientId,
        orderId: orderId,
        type: 'order_accepted',
        title: 'Proposal Received',
        message: 'Provider has sent a proposal for your order',
        read: false,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
      });
    }

    if (status === ORDER_STATUS.COMPLETED) {
      notifications.push({
        userId: order?.clientId,
        orderId: orderId,
        type: 'order_completed',
        title: 'Order Completed',
        message: 'Your order has been marked as completed',
        read: false,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
      });
    }

    for (const notification of notifications) {
      await db.collection(COLLECTIONS.NOTIFICATIONS).add(notification);
    }

    functions.logger.info(`Order ${orderId} status updated to ${status}`);
    return {success: true};
  } catch (error) {
    functions.logger.error('Error updating order status:', error);
    throw handleError(error);
  }
});

/**
 * Get orders for current user
 */
export const getMyOrders = functions.https.onCall(async (data, context) => {
  try {
    assertAuthenticated(context);
    
    const db = admin.firestore();
    const {role, status} = data;

    const ordersQuery = db.collection(COLLECTIONS.ORDERS);
    
    let query: admin.firestore.Query;
    if (role === 'client') {
      query = ordersQuery.where('clientId', '==', context.auth!.uid);
    } else if (role === 'provider') {
      query = ordersQuery.where('providerId', '==', context.auth!.uid);
    } else {
      // Get all orders for user (both client and provider)
      const clientOrders = await ordersQuery
        .where('clientId', '==', context.auth!.uid)
        .get();
      const providerOrders = await ordersQuery
        .where('providerId', '==', context.auth!.uid)
        .get();
      
      const orders = [
        ...clientOrders.docs.map(doc => ({id: doc.id, ...doc.data()})),
        ...providerOrders.docs.map(doc => ({id: doc.id, ...doc.data()})),
      ];
      
      return {orders};
    }

    if (status) {
      query = query.where('status', '==', status);
    }

    const snapshot = await query.orderBy('createdAt', 'desc').get();
    const orders = snapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data(),
    }));

    return {orders};
  } catch (error) {
    functions.logger.error('Error fetching orders:', error);
    throw handleError(error);
  }
});
