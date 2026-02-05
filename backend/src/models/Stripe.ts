import { UUID } from 'crypto';

export interface StripeAccount {
  id: UUID;
  user_id: UUID;
  stripe_account_id: string;
  charges_enabled: boolean;
  payouts_enabled: boolean;
  details_submitted: boolean;
  platform_fee_percentage: number; // 5.00 para 5%
  application_fee_percentage?: number;
  transfer_data_destination?: string;
  on_behalf_of?: string;
  country: string; // BR
  default_currency: string; // BRL
  created_at: Date;
  updated_at: Date;
}

export interface StripePaymentIntent {
  id: UUID;
  stripe_payment_intent_id: string;
  purchase_order_id: UUID;
  amount: number;
  currency: string; // BRL
  status: string; // succeeded, pending, failed
  client_secret?: string;
  application_fee_amount?: number;
  transfer_data?: Record<string, any>; // JSONB
  created_at: Date;
  updated_at: Date;
}
