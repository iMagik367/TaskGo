import * as functions from 'firebase-functions';
import {validateAppCheck} from './security/appCheck';

/**
 * Get Stripe publishable key
 * This is safe to expose to clients
 */
export const getStripePublishableKey = functions.https.onCall(async (_data, _context) => {
  try {
    validateAppCheck(_context);
  const publishableKey = process.env.STRIPE_PUBLISHABLE_KEY;
  
  if (!publishableKey) {
    throw new functions.https.HttpsError(
      'failed-precondition',
      'Stripe publishable key not configured'
    );
  }
  
    return {
      publishableKey: publishableKey,
    };
  } catch (error) {
    functions.logger.error('Error getting Stripe publishable key:', error);
    throw error instanceof functions.https.HttpsError 
      ? error 
      : new functions.https.HttpsError('internal', 'Error getting Stripe key');
  }
});

