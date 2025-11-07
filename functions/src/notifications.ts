import * as admin from 'firebase-admin';
import * as functions from 'firebase-functions';
import {COLLECTIONS} from './utils/constants';
import {assertAuthenticated, handleError} from './utils/errors';

/**
 * Send push notification
 */
export const sendPushNotification = functions.https.onCall(async (data, context) => {
  try {
    assertAuthenticated(context);
    
    const db = admin.firestore();
    const {userId, title, message, data: notificationData} = data;

    if (!userId || !title || !message) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'User ID, title, and message are required'
      );
    }

    // Check permissions (admin or sending to self)
    if (context.auth!.uid !== userId) {
      const adminDoc = await db.collection('users').doc(context.auth!.uid).get();
      if (!adminDoc.exists || adminDoc.data()?.role !== 'admin') {
        throw new functions.https.HttpsError(
          'permission-denied',
          'Cannot send notifications to other users'
        );
      }
    }

    // Get user's FCM token(s)
    const userDoc = await db.collection('users').doc(userId).get();
    const fcmTokens = userDoc.data()?.fcmTokens || [];

    if (fcmTokens.length === 0) {
      functions.logger.warn(`No FCM tokens found for user ${userId}`);
      return {success: false, message: 'No FCM tokens registered'};
    }

    // Send to all registered devices
    const messages = fcmTokens.map((token: string) => ({
      notification: {
        title,
        body: message,
      },
      data: notificationData || {},
      token,
    }));

    const batchResponse = await admin.messaging().sendAll(messages);

    // Save notification to Firestore
    await db.collection(COLLECTIONS.NOTIFICATIONS).add({
      userId,
      title,
      message,
      data: notificationData || {},
      read: false,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    functions.logger.info(`Sent ${batchResponse.successCount}/${messages.length} notifications`);

    return {
      success: true,
      successCount: batchResponse.successCount,
      failureCount: batchResponse.failureCount,
    };
  } catch (error) {
    functions.logger.error('Error sending push notification:', error);
    throw handleError(error);
  }
});

/**
 * Get user's notifications
 */
export const getMyNotifications = functions.https.onCall(async (data, context) => {
  try {
    assertAuthenticated(context);
    
    const db = admin.firestore();
    const {limit = 50, unreadOnly = false} = data;

    let query: admin.firestore.Query = db.collection(COLLECTIONS.NOTIFICATIONS)
      .where('userId', '==', context.auth!.uid);

    if (unreadOnly) {
      query = query.where('read', '==', false);
    }

    const snapshot = await query
      .orderBy('createdAt', 'desc')
      .limit(limit)
      .get();

    const notifications = snapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data(),
    }));

    return {notifications};
  } catch (error) {
    functions.logger.error('Error fetching notifications:', error);
    throw handleError(error);
  }
});

/**
 * Mark notification as read
 */
export const markNotificationRead = functions.https.onCall(async (data, context) => {
  try {
    assertAuthenticated(context);
    
    const db = admin.firestore();
    const {notificationId} = data;

    if (!notificationId) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'Notification ID is required'
      );
    }

    const notificationDoc = await db.collection(COLLECTIONS.NOTIFICATIONS).doc(notificationId).get();
    
    if (!notificationDoc.exists) {
      throw new functions.https.HttpsError('not-found', 'Notification not found');
    }

    if (notificationDoc.data()?.userId !== context.auth!.uid) {
      throw new functions.https.HttpsError(
        'permission-denied',
        'Cannot read other users\' notifications'
      );
    }

    await notificationDoc.ref.update({
      read: true,
      readAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    return {success: true};
  } catch (error) {
    functions.logger.error('Error marking notification as read:', error);
    throw handleError(error);
  }
});

/**
 * Mark all notifications as read
 */
export const markAllNotificationsRead = functions.https.onCall(async (data, context) => {
  try {
    assertAuthenticated(context);
    
    const db = admin.firestore();

    const snapshot = await db.collection(COLLECTIONS.NOTIFICATIONS)
      .where('userId', '==', context.auth!.uid)
      .where('read', '==', false)
      .get();

    const batch = db.batch();
    const timestamp = admin.firestore.FieldValue.serverTimestamp();

    snapshot.forEach((doc) => {
      batch.update(doc.ref, {
        read: true,
        readAt: timestamp,
      });
    });

    await batch.commit();

    functions.logger.info(`Marked ${snapshot.size} notifications as read for user ${context.auth!.uid}`);

    return {success: true, count: snapshot.size};
  } catch (error) {
    functions.logger.error('Error marking all notifications as read:', error);
    throw handleError(error);
  }
});

/**
 * Trigger: Send notification when order status changes
 */
export const onOrderStatusChange = functions.firestore
  .document('orders/{orderId}')
  .onUpdate(async (change, context) => {
    const before = change.before.data();
    const after = change.after.data();
    const orderId = context.params.orderId;

    // Skip if status didn't change
    if (before?.status === after?.status) {
      return null;
    }

    const db = admin.firestore();

    // Determine notification recipients based on status change
    const notifications: Array<{userId: string; title: string; message: string}> = [];

    switch (after?.status) {
      case 'proposed':
        notifications.push({
          userId: after.clientId,
          title: 'Proposal Received',
          message: 'Provider has sent a proposal for your order',
        });
        break;

      case 'accepted':
        notifications.push({
          userId: after.providerId,
          title: 'Order Accepted',
          message: 'Client has accepted your proposal',
        });
        break;

      case 'completed':
        notifications.push({
          userId: after.clientId,
          title: 'Order Completed',
          message: 'Your order has been marked as completed',
        });
        break;

      case 'cancelled': {
        const recipientId = before?.clientId === after?.clientId ? after?.providerId : after?.clientId;
        notifications.push({
          userId: recipientId,
          title: 'Order Cancelled',
          message: 'The order has been cancelled',
        });
        break;
      }
    }

    // Send notifications
    for (const notification of notifications) {
      await db.collection(COLLECTIONS.NOTIFICATIONS).add({
        userId: notification.userId,
        orderId: orderId,
        title: notification.title,
        message: notification.message,
        read: false,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
      });
    }

    functions.logger.info(`Sent notifications for order ${orderId} status change`);
    return null;
  });
