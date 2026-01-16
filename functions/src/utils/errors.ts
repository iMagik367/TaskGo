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
  // Não logar dados sensíveis
  const errorMessage = error instanceof Error ? error.message : 'An unknown error occurred';
  const errorCode = error instanceof AppError ? error.code : 'internal';

  // Log estruturado sem dados sensíveis
  functions.logger.error('Error occurred', {
    code: errorCode,
    message: errorMessage,
    timestamp: new Date().toISOString(),
  });

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
