import { UUID } from 'crypto';

export interface BankAccount {
  id: UUID;
  user_id: UUID; // Apenas para role='partner'
  account_holder_name: string;
  bank_code: string; // 001=BB, 341=Itau, etc.
  bank_name: string;
  account_type: 'checking' | 'savings' | 'payment';
  agency?: string;
  account_number: string;
  account_digit?: string;
  cpf_cnpj: string;
  pix_key?: string;
  pix_key_type?: 'cpf' | 'cnpj' | 'email' | 'phone' | 'random';
  is_primary: boolean;
  verified: boolean;
  created_at: Date;
  updated_at: Date;
}
