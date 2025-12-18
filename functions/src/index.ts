import * as admin from 'firebase-admin';
import * as functions from 'firebase-functions';

admin.initializeApp();

// Import all function modules
export * from './auth';
export * from './orders';
export * from './payments';
export * from './stripe-connect';
export * from './webhooks';
export * from './ai-chat';
export * from './notifications';
export * from './gradualNotifications';
export * from './identityVerification';
export * from './faceRecognitionVerification';
export * from './billingWebhook';
export * from './user-preferences';
export * from './user-settings';
export * from './product-orders';
export * from './deleteAccount';
export * from './account-change';
export * from './product-payments';
export * from './tracking';
export * from './pix-payments';
export * from './stripe-config';
export * from './auto-refund';

// Health check endpoint
export const health = functions.https.onRequest((req, res) => {
  res.json({status: 'ok', timestamp: new Date().toISOString()});
});
