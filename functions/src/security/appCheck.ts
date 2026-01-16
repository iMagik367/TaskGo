import * as functions from 'firebase-functions';

/**
 * Middleware para validar App Check token
 * Garante que apenas requests de apps legítimos sejam processados
 */
export const validateAppCheck = (
  context: functions.https.CallableContext,
): void => {
  // Em produção, App Check deve estar habilitado
  // Em desenvolvimento/emulador, permitir sem token
  if (
    process.env.FUNCTIONS_EMULATOR === 'true' ||
    process.env.NODE_ENV === 'development'
  ) {
    return;
  }

  // App Check token está em context.app
  // Se não houver token válido, context.app será undefined
  if (!context.app) {
    functions.logger.warn('App Check token missing', {
      uid: context.auth?.uid,
      timestamp: new Date().toISOString(),
    });
    throw new functions.https.HttpsError(
      'failed-precondition',
      'App Check validation failed. This request must come from a legitimate app.',
    );
  }
};
