import * as admin from 'firebase-admin';
import {getFirestore} from './utils/firestore';
import * as functions from 'firebase-functions';
import Stripe from 'stripe';
import {assertAuthenticated, handleError} from './utils/errors';
import {validateAppCheck} from './security/appCheck';

// Initialize Stripe
const stripe = new Stripe(process.env.STRIPE_SECRET_KEY || '', {
  apiVersion: '2023-10-16',
});

/**
 * Create Stripe Connect onboarding link for provider
 */
export const createOnboardingLink = functions.https.onCall(async (data, context) => {
  try {
    validateAppCheck(context);
    assertAuthenticated(context);
    
    const db = getFirestore();
    
    // Check if user is a provider
    const userDoc = await db.collection('users').doc(context.auth!.uid).get();
    if (!userDoc.exists) {
      throw new functions.https.HttpsError('not-found', 'User not found');
    }

    const userData = userDoc.data();
    
    if (userData?.role !== 'provider') {
      throw new functions.https.HttpsError(
        'failed-precondition',
        'Only providers can create Stripe Connect accounts'
      );
    }

    // Check if documents are approved
    if (!userData?.documentsApproved) {
      throw new functions.https.HttpsError(
        'failed-precondition',
        'Documents must be approved before Stripe onboarding'
      );
    }

    let stripeAccountId = userData?.stripeAccountId;

    // Create Stripe Connect account if it doesn't exist
    if (!stripeAccountId) {
      const account = await stripe.accounts.create({
        type: 'express',
        country: 'US', // TODO: Get from user profile
        email: userData?.email,
        business_type: 'individual',
        capabilities: {
          card_payments: {requested: true},
          transfers: {requested: true},
        },
      });

      stripeAccountId = account.id;

      // Save Stripe account ID to user document
      await db.collection('users').doc(context.auth!.uid).update({
        stripeAccountId: stripeAccountId,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });
    }

    // Create onboarding link
    const accountLink = await stripe.accountLinks.create({
      account: stripeAccountId,
      refresh_url: process.env.STRIPE_REFRESH_URL || 'https://taskgo.app/settings',
      return_url: process.env.STRIPE_RETURN_URL || 'https://taskgo.app/settings',
      type: 'account_onboarding',
    });

    functions.logger.info(`Onboarding link created for ${context.auth!.uid}`);
    
    return {
      onboardingUrl: accountLink.url,
      stripeAccountId: stripeAccountId,
    };
  } catch (error) {
    functions.logger.error('Error creating onboarding link:', error);
    throw handleError(error);
  }
});

/**
 * Check Stripe Connect account status
 */
export const getAccountStatus = functions.https.onCall(async (data, context) => {
  try {
    validateAppCheck(context);
    assertAuthenticated(context);
    
    const db = getFirestore();
    
    const userDoc = await db.collection('users').doc(context.auth!.uid).get();
    if (!userDoc.exists) {
      throw new functions.https.HttpsError('not-found', 'User not found');
    }

    const userData = userDoc.data();
    const stripeAccountId = userData?.stripeAccountId;

    if (!stripeAccountId) {
      return {
        connected: false,
        message: 'No Stripe account linked',
      };
    }

    try {
      const account = await stripe.accounts.retrieve(stripeAccountId);
      
      return {
        connected: true,
        chargesEnabled: account.charges_enabled,
        payoutsEnabled: account.payouts_enabled,
        detailsSubmitted: account.details_submitted,
        requirements: account.requirements,
      };
    } catch (error) {
      functions.logger.error('Error retrieving Stripe account:', error);
      return {
        connected: true,
        error: 'Unable to retrieve account details',
      };
    }
  } catch (error) {
    functions.logger.error('Error checking account status:', error);
    throw handleError(error);
  }
});

/**
 * Create Login Link for dashboard access
 */
export const createDashboardLink = functions.https.onCall(async (data, context) => {
  try {
    validateAppCheck(context);
    assertAuthenticated(context);
    
    const db = getFirestore();
    
    const userDoc = await db.collection('users').doc(context.auth!.uid).get();
    if (!userDoc.exists) {
      throw new functions.https.HttpsError('not-found', 'User not found');
    }

    const stripeAccountId = userDoc.data()?.stripeAccountId;

    if (!stripeAccountId) {
      throw new functions.https.HttpsError(
        'failed-precondition',
        'No Stripe account linked'
      );
    }

    const loginLink = await stripe.accounts.createLoginLink(stripeAccountId);

    functions.logger.info(`Dashboard link created for ${context.auth!.uid}`);
    
    return {
      dashboardUrl: loginLink.url,
    };
  } catch (error) {
    functions.logger.error('Error creating dashboard link:', error);
    throw handleError(error);
  }
});
