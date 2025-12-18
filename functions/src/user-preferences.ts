import * as admin from 'firebase-admin';
import * as functions from 'firebase-functions';
import {COLLECTIONS} from './utils/constants';
import {assertAuthenticated, handleError} from './utils/errors';

/**
 * Update user preferences (categories)
 * This syncs preferences from local storage to Firestore
 */
export const updateUserPreferences = functions.https.onCall(async (data, context) => {
  try {
    assertAuthenticated(context);
    
    const db = admin.firestore();
    const {categories} = data; // Array of category names: ["Montagem", "Reforma", ...]

    if (!Array.isArray(categories)) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'Categories must be an array'
      );
    }

    const userId = context.auth!.uid;
    const userRef = db.collection(COLLECTIONS.USERS).doc(userId);
    
    // Update user document with preferred categories
    await userRef.update({
      preferredCategories: categories,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    functions.logger.info(`User ${userId} preferences updated: ${categories.join(', ')}`);
    return {success: true};
  } catch (error) {
    functions.logger.error('Error updating user preferences:', error);
    throw handleError(error);
  }
});

/**
 * Get user preferences
 */
export const getUserPreferences = functions.https.onCall(async (data, context) => {
  try {
    assertAuthenticated(context);
    
    const db = admin.firestore();
    const userId = context.auth!.uid;
    
    const userDoc = await db.collection(COLLECTIONS.USERS).doc(userId).get();
    if (!userDoc.exists) {
      throw new functions.https.HttpsError('not-found', 'User not found');
    }

    const userData = userDoc.data();
    return {
      preferredCategories: userData?.preferredCategories || [],
    };
  } catch (error) {
    functions.logger.error('Error getting user preferences:', error);
    throw handleError(error);
  }
});

