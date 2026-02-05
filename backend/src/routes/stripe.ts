import express from 'express';
import { StripeService } from '../services/StripeService';

const router = express.Router();
const stripeService = new StripeService(process.env.STRIPE_SECRET_KEY || '');

/**
 * POST /api/stripe/webhook
 * Webhook do Stripe
 */
router.post('/webhook', express.raw({ type: 'application/json' }), async (req, res, next) => {
  try {
    const sig = req.headers['stripe-signature'];
    if (!sig) {
      return res.status(400).json({ error: 'Missing stripe-signature header' });
    }

    // TODO: Verificar assinatura do webhook
    const event = JSON.parse(req.body.toString());
    
    await stripeService.handleWebhook(event);
    
    res.json({ received: true });
  } catch (error: any) {
    next(error);
  }
});

/**
 * POST /api/stripe/accounts
 * Cria ou atualiza conta Stripe
 */
router.post('/accounts', async (req, res, next) => {
  try {
    const { userId, email, country, type } = req.body;
    
    const account = await stripeService.createOrUpdateStripeAccount(userId, {
      email,
      country,
      type
    });
    
    res.json(account);
  } catch (error: any) {
    next(error);
  }
});

/**
 * POST /api/stripe/payment-intents
 * Cria Payment Intent
 */
router.post('/payment-intents', async (req, res, next) => {
  try {
    const { purchaseOrderId, amount, sellerStripeAccountId, platformFeePercentage } = req.body;
    
    const paymentIntent = await stripeService.createProductPaymentIntent(
      purchaseOrderId,
      amount,
      sellerStripeAccountId,
      platformFeePercentage || 5.0
    );
    
    res.json(paymentIntent);
  } catch (error: any) {
    next(error);
  }
});

export default router;
