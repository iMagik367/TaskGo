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
export * from './identityVerification';
export * from './billingWebhook';

// Health check endpoint
export const health = functions.https.onRequest((req, res) => {
  res.json({status: 'ok', timestamp: new Date().toISOString()});
});
