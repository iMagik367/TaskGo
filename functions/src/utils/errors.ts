import * as admin from 'firebase-admin';
import * as functions from 'firebase-functions';

export class AppError extends Error {
  constructor(
    public code: string,
    public message: string,
    public statusCode: number = 500,
  ) {
    super(message);
    this.name = 'AppError';
  }
}

export const handleError = (error: unknown): functions.https.HttpsError => {
  functions.logger.error('Error occurred:', error);

  if (error instanceof AppError) {
    return new functions.https.HttpsError(
      error.code as functions.https.FunctionsErrorCode,
      error.message,
    );
  }

  if (error instanceof Error) {
    return new functions.https.HttpsError('internal', error.message);
  }

  return new functions.https.HttpsError('internal', 'An unknown error occurred');
};

export const assertAuthenticated = (context: functions.https.CallableContext) => {
  if (!context.auth) {
    throw new AppError('unauthenticated', 'User must be authenticated', 401);
  }
};

export const assertAdmin = async (context: functions.https.CallableContext) => {
  assertAuthenticated(context);
  
  const db = admin.firestore();
  const userDoc = await db.collection('users').doc(context.auth!.uid).get();
  
  if (!userDoc.exists || userDoc.data()?.role !== 'admin') {
    throw new AppError('permission-denied', 'Admin access required', 403);
  }
};
