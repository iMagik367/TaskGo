import * as admin from 'firebase-admin';
import * as functions from 'firebase-functions';

/**
 * Triggered when a new user is created in Firebase Auth
 * Creates corresponding user document in Firestore
 */
export const onUserCreate = functions.auth.user().onCreate(async (user) => {
  const db = admin.firestore();
  
  try {
    const userData = {
      uid: user.uid,
      email: user.email,
      displayName: user.displayName,
      photoURL: user.photoURL,
      role: 'client', // Default role
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      profileComplete: false,
      verified: false,
    };

    await db.collection('users').doc(user.uid).set(userData);
    
    functions.logger.info(`User document created for ${user.uid}`);
    return null;
  } catch (error) {
    functions.logger.error('Error creating user document:', error);
    throw error;
  }
});

/**
 * Triggered when a user is deleted from Firebase Auth
 * Removes user data from Firestore
 */
export const onUserDelete = functions.auth.user().onDelete(async (user) => {
  const db = admin.firestore();
  const batch = db.batch();
  
  try {
    // Delete user document
    const userRef = db.collection('users').doc(user.uid);
    batch.delete(userRef);

    // Delete user's orders (soft delete by updating status)
    const ordersSnapshot = await db.collection('orders')
      .where('clientId', '==', user.uid)
      .get();
    
    ordersSnapshot.forEach((doc) => {
      batch.update(doc.ref, {
        status: 'cancelled',
        deleted: true,
        deletedAt: admin.firestore.FieldValue.serverTimestamp(),
      });
    });

    await batch.commit();
    functions.logger.info(`User data deleted for ${user.uid}`);
    return null;
  } catch (error) {
    functions.logger.error('Error deleting user data:', error);
    throw error;
  }
});

/**
 * Function to promote user to provider
 */
export const promoteToProvider = functions.https.onCall(async (data, context) => {
  const db = admin.firestore();
  
  if (!context.auth) {
    throw new functions.https.HttpsError(
      'unauthenticated',
      'User must be authenticated'
    );
  }

  try {
    await db.collection('users').doc(context.auth.uid).update({
      role: 'provider',
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    functions.logger.info(`User ${context.auth.uid} promoted to provider`);
    return {success: true};
  } catch (error) {
    functions.logger.error('Error promoting user to provider:', error);
    throw new functions.https.HttpsError(
      'internal',
      'Failed to promote user'
    );
  }
});

/**
 * Function to approve provider documents
 */
export const approveProviderDocuments = functions.https.onCall(async (data, context) => {
  const db = admin.firestore();
  
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'User must be authenticated');
  }

  // Check if user is admin
  const adminDoc = await db.collection('users').doc(context.auth.uid).get();
  if (!adminDoc.exists || adminDoc.data()?.role !== 'admin') {
    throw new functions.https.HttpsError('permission-denied', 'Admin access required');
  }

  const {providerId, documents} = data;

  if (!providerId || !documents) {
    throw new functions.https.HttpsError(
      'invalid-argument',
      'Provider ID and documents are required'
    );
  }

  try {
    await db.collection('users').doc(providerId).update({
      documents: documents,
      documentsApproved: true,
      documentsApprovedAt: admin.firestore.FieldValue.serverTimestamp(),
      documentsApprovedBy: context.auth.uid,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    // Send notification to provider
    await db.collection('notifications').add({
      userId: providerId,
      type: 'system_alert',
      title: 'Documents Approved',
      message: 'Your documents have been approved. You can now accept orders!',
      read: false,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    functions.logger.info(`Documents approved for provider ${providerId}`);
    return {success: true};
  } catch (error) {
    functions.logger.error('Error approving documents:', error);
    throw new functions.https.HttpsError('internal', 'Failed to approve documents');
  }
});
