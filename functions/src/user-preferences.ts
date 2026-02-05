import * as admin from 'firebase-admin';
import {getFirestore} from './utils/firestore';
import * as functions from 'firebase-functions';
import {COLLECTIONS} from './utils/constants';
import {assertAuthenticated, handleError} from './utils/errors';
import {validateAppCheck} from './security/appCheck';
import {getUserLocationId} from './utils/firestorePaths';

/**
 * Update user preferences (categories)
 * This syncs preferences from local storage to Firestore
 */
export const updateUserPreferences = functions.https.onCall(async (data, context) => {
  try {
    validateAppCheck(context);
    assertAuthenticated(context);
    
    const db = getFirestore();
    const {categories} = data; // Array of category names: ["Montagem", "Reforma", ...]

    if (!Array.isArray(categories)) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'Categories must be an array'
      );
    }

    const userId = context.auth!.uid;
    
    // CRÍTICO: Buscar city/state do usuário para salvar em locations/{locationId}/users
    const userDoc = await db.collection(COLLECTIONS.USERS).doc(userId).get();
    if (!userDoc.exists) {
      throw new functions.https.HttpsError('not-found', 'User not found');
    }
    
    const userData = userDoc.data();
    const userCity = userData?.city;
    const userState = userData?.state;
    
    if (!userCity || !userState) {
      throw new functions.https.HttpsError(
        'failed-precondition',
        'User must have city and state defined in profile'
      );
    }
    
    const locationId = await getUserLocationId(db, userId);
    
    // Salvar em users global (compatibilidade)
    const userRef = db.collection(COLLECTIONS.USERS).doc(userId);
    await userRef.update({
      preferredCategories: categories,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
    
    // Salvar em locations/{locationId}/users/{userId}
    await db
        .collection('locations')
        .doc(locationId)
        .collection('users')
        .doc(userId)
        .update({
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
    validateAppCheck(context);
    assertAuthenticated(context);
    
    const db = getFirestore();
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

