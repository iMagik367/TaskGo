import Stripe from 'stripe';
import { query } from '../database/connection';
import { StripeAccount, StripePaymentIntent } from '../models/Stripe';
import { v4 as uuidv4 } from 'uuid';

export class StripeService {
  private stripe: Stripe;

  constructor(apiKey: string) {
    this.stripe = new Stripe(apiKey, {
      apiVersion: '2023-10-16'
    });
  }

  /**
   * Cria ou atualiza conta Stripe Connect para parceiro
   */
  async createOrUpdateStripeAccount(
    userId: string,
    accountData: {
      email: string;
      country?: string;
      type?: 'express' | 'standard';
    }
  ): Promise<StripeAccount> {
    // Verificar se já existe conta
    const existing = await query(
      'SELECT * FROM stripe_accounts WHERE user_id = $1',
      [userId]
    );

    let stripeAccountId: string;
    let stripeAccount: Stripe.Account;

    if (existing.rows.length > 0) {
      // Atualizar conta existente
      stripeAccountId = existing.rows[0].stripe_account_id;
      stripeAccount = await this.stripe.accounts.update(stripeAccountId, {
        email: accountData.email
      });
    } else {
      // Criar nova conta
      stripeAccount = await this.stripe.accounts.create({
        type: accountData.type || 'express',
        country: accountData.country || 'BR',
        email: accountData.email,
        capabilities: {
          card_payments: { requested: true },
          transfers: { requested: true }
        }
      });
      stripeAccountId = stripeAccount.id;
    }

    // Salvar/atualizar no banco
    const result = await query(
      `INSERT INTO stripe_accounts (
        id, user_id, stripe_account_id, charges_enabled, payouts_enabled,
        details_submitted, country, default_currency
      ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
      ON CONFLICT (user_id) DO UPDATE SET
        charges_enabled = EXCLUDED.charges_enabled,
        payouts_enabled = EXCLUDED.payouts_enabled,
        details_submitted = EXCLUDED.details_submitted,
        updated_at = CURRENT_TIMESTAMP
      RETURNING *`,
      [
        uuidv4(),
        userId,
        stripeAccountId,
        stripeAccount.capabilities?.card_payments === 'active',
        stripeAccount.capabilities?.transfers === 'active',
        stripeAccount.details_submitted || false,
        stripeAccount.country || 'BR',
        'BRL'
      ]
    );

    return result.rows[0];
  }

  /**
   * Obtém configurações da conta Stripe
   */
  async getStripeAccount(userId: string): Promise<StripeAccount | null> {
    const result = await query(
      'SELECT * FROM stripe_accounts WHERE user_id = $1',
      [userId]
    );
    return result.rows[0] || null;
  }

  /**
   * Atualiza configurações do gateway Stripe
   */
  async updateStripeAccountSettings(
    userId: string,
    settings: {
      platform_fee_percentage?: number;
      application_fee_percentage?: number;
      transfer_data_destination?: string;
    }
  ): Promise<StripeAccount> {
    const fields: string[] = [];
    const values: any[] = [];
    let paramCount = 1;

    Object.entries(settings).forEach(([key, value]) => {
      if (value !== undefined) {
        fields.push(`${key} = $${paramCount}`);
        values.push(value);
        paramCount++;
      }
    });

    if (fields.length === 0) {
      throw new Error('Nenhum campo para atualizar');
    }

    values.push(userId);
    const result = await query(
      `UPDATE stripe_accounts SET ${fields.join(', ')}, updated_at = CURRENT_TIMESTAMP
       WHERE user_id = $${paramCount} RETURNING *`,
      values
    );
    return result.rows[0];
  }

  /**
   * Cria Payment Intent para pedido de produto
   */
  async createProductPaymentIntent(
    purchaseOrderId: string,
    amount: number,
    sellerStripeAccountId: string,
    platformFeePercentage: number = 5.0
  ): Promise<StripePaymentIntent> {
    const applicationFeeAmount = Math.round(amount * (platformFeePercentage / 100));

    const paymentIntent = await this.stripe.paymentIntents.create({
      amount: Math.round(amount * 100), // Converter para centavos
      currency: 'brl',
      application_fee_amount: Math.round(applicationFeeAmount * 100),
      transfer_data: {
        destination: sellerStripeAccountId
      },
      metadata: {
        purchase_order_id: purchaseOrderId
      }
    });

    // Salvar no banco
    const result = await query(
      `INSERT INTO stripe_payment_intents (
        id, stripe_payment_intent_id, purchase_order_id, amount, currency,
        status, client_secret, application_fee_amount, transfer_data
      ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)
      RETURNING *`,
      [
        uuidv4(),
        paymentIntent.id,
        purchaseOrderId,
        amount,
        'BRL',
        paymentIntent.status,
        paymentIntent.client_secret,
        applicationFeeAmount,
        JSON.stringify(paymentIntent.transfer_data)
      ]
    );

    return result.rows[0];
  }

  /**
   * Atualiza status do Payment Intent após webhook
   */
  async updatePaymentIntentStatus(
    stripePaymentIntentId: string,
    status: string
  ): Promise<StripePaymentIntent> {
    const result = await query(
      `UPDATE stripe_payment_intents
       SET status = $1, updated_at = CURRENT_TIMESTAMP
       WHERE stripe_payment_intent_id = $2
       RETURNING *`,
      [status, stripePaymentIntentId]
    );
    return result.rows[0];
  }

  /**
   * Processa webhook do Stripe
   */
  async handleWebhook(event: Stripe.Event): Promise<void> {
    switch (event.type) {
      case 'payment_intent.succeeded':
        const paymentIntent = event.data.object as Stripe.PaymentIntent;
        await this.updatePaymentIntentStatus(paymentIntent.id, 'succeeded');
        
        // Atualizar status do pedido
        const paymentIntentRecord = await query(
          'SELECT purchase_order_id FROM stripe_payment_intents WHERE stripe_payment_intent_id = $1',
          [paymentIntent.id]
        );
        
        if (paymentIntentRecord.rows.length > 0) {
          await query(
            'UPDATE purchase_orders SET status = $1 WHERE id = $2',
            ['paid', paymentIntentRecord.rows[0].purchase_order_id]
          );
        }
        break;

      case 'payment_intent.payment_failed':
        const failedPaymentIntent = event.data.object as Stripe.PaymentIntent;
        await this.updatePaymentIntentStatus(failedPaymentIntent.id, 'failed');
        break;

      case 'account.updated':
        const account = event.data.object as Stripe.Account;
        await query(
          `UPDATE stripe_accounts
           SET charges_enabled = $1, payouts_enabled = $2, details_submitted = $3,
               updated_at = CURRENT_TIMESTAMP
           WHERE stripe_account_id = $4`,
          [
            account.capabilities?.card_payments === 'active',
            account.capabilities?.transfers === 'active',
            account.details_submitted || false,
            account.id
          ]
        );
        break;
    }
  }
}
