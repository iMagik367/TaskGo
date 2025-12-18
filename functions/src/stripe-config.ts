import * as functions from 'firebase-functions';

/**
 * Get Stripe publishable key
 * This is safe to expose to clients
 */
export const getStripePublishableKey = functions.https.onCall(async (_data, _context) => {
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
});

