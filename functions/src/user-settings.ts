import * as admin from 'firebase-admin';
import {getFirestore} from './utils/firestore';
import * as functions from 'firebase-functions';
import {COLLECTIONS} from './utils/constants';
import {assertAuthenticated, handleError} from './utils/errors';
import {validateAppCheck} from './security/appCheck';

const booleanFields = (payload: Record<string, unknown>, requiredKeys: string[]): Record<string, boolean> => {
  const result: Record<string, boolean> = {};
  for (const key of requiredKeys) {
    const value = payload[key];
    if (typeof value !== 'boolean') {
      throw new functions.https.HttpsError(
          'invalid-argument',
          `Campo ${key} deve ser booleano`,
      );
    }
    result[key] = value;
  }
  return result;
};

export const updateNotificationSettings = functions.https.onCall(async (data, context) => {
  try {
    validateAppCheck(context);
    assertAuthenticated(context);

    const settings = booleanFields(data ?? {}, [
      'promos',
      'sound',
      'push',
      'lockscreen',
      'email',
      'sms',
    ]);

    const userId = context.auth!.uid;
    const db = getFirestore();
    await db
        .collection(COLLECTIONS.USERS)
        .doc(userId)
        .set({
          notificationSettings: settings,
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        }, {merge: true});

    functions.logger.info(`Notification settings updated for ${userId}`, settings);
    return {success: true};
  } catch (error) {
    functions.logger.error('Error updating notification settings:', error);
    throw handleError(error);
  }
});

export const updatePrivacySettings = functions.https.onCall(async (data, context) => {
  try {
    validateAppCheck(context);
    assertAuthenticated(context);

    const settings = booleanFields(data ?? {}, [
      'locationSharing',
      'profileVisible',
      'contactInfoSharing',
      'analytics',
      'personalizedAds',
      'dataCollection',
      'thirdPartySharing',
    ]);

    const userId = context.auth!.uid;
    const db = getFirestore();
    await db
        .collection(COLLECTIONS.USERS)
        .doc(userId)
        .set({
          privacySettings: settings,
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        }, {merge: true});

    functions.logger.info(`Privacy settings updated for ${userId}`, settings);
    return {success: true};
  } catch (error) {
    functions.logger.error('Error updating privacy settings:', error);
    throw handleError(error);
  }
});

export const updateLanguagePreference = functions.https.onCall(async (data, context) => {
  try {
    validateAppCheck(context);
    assertAuthenticated(context);

    const {language} = data ?? {};
    const allowed = ['pt', 'en', 'es', 'fr', 'it', 'de'];
    if (typeof language !== 'string' || !allowed.includes(language)) {
      throw new functions.https.HttpsError(
          'invalid-argument',
          'Idioma inválido.',
      );
    }

    const userId = context.auth!.uid;
    const db = getFirestore();
    await db
        .collection(COLLECTIONS.USERS)
        .doc(userId)
        .set({
          language,
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        }, {merge: true});

    functions.logger.info(`Language updated for ${userId} -> ${language}`);
    return {success: true};
  } catch (error) {
    functions.logger.error('Error updating language preference:', error);
    throw handleError(error);
  }
});

export const getUserSettings = functions.https.onCall(async (_data, context) => {
  try {
    validateAppCheck(context);
    assertAuthenticated(context);

    const userId = context.auth!.uid;
    const db = getFirestore();
    const snapshot = await db
        .collection(COLLECTIONS.USERS)
        .doc(userId)
        .get();

    if (!snapshot.exists) {
      throw new functions.https.HttpsError('not-found', 'User not found');
    }

    const userData = snapshot.data() ?? {};
    return {
      notificationSettings: userData.notificationSettings ?? {},
      privacySettings: userData.privacySettings ?? {},
      language: userData.language ?? 'pt',
      preferredCategories: userData.preferredCategories ?? [],
      biometricEnabled: userData.biometricEnabled ?? false,
    };
  } catch (error) {
    functions.logger.error('Error getting user settings:', error);
    throw handleError(error);
  }
});


