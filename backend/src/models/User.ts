import { UUID } from '../types';

export interface User {
  id: UUID;
  firebase_uid?: string; // Opcional agora (pode ser NULL para novos usuários)
  email: string;
  role: 'client' | 'partner' | 'admin';
  display_name?: string;
  phone?: string;
  photo_url?: string;
  
  // Autenticação
  password_hash?: string;
  email_verified: boolean;
  email_verified_at?: Date;
  google_id?: string;
  last_login?: Date;
  failed_login_attempts: number;
  locked_until?: Date;
  
  // Localização atual (dinâmica via GPS)
  current_latitude?: number;
  current_longitude?: number;
  current_city_id?: number;
  last_location_update?: Date;
  
  // Verificação de identidade
  cpf?: string;
  cnpj?: string;
  rg?: string;
  birth_date?: Date;
  document_front_url?: string;
  document_back_url?: string;
  selfie_url?: string;
  address_proof_url?: string;
  
  // Status de verificação
  profile_complete: boolean;
  verified: boolean;
  verified_at?: Date;
  verified_by?: UUID;
  
  // Stripe (legacy)
  stripe_account_id?: string;
  stripe_charges_enabled: boolean;
  stripe_payouts_enabled: boolean;
  
  // Rating
  rating: number;
  
  // Timestamps
  created_at: Date;
  updated_at: Date;
}

export interface UserLocation {
  id: number;
  user_id: UUID;
  city_id: number;
  latitude: number;
  longitude: number;
  is_current: boolean;
  accuracy?: number;
  source: string;
  entered_at: Date;
  exited_at?: Date;
}

export interface UserPreferredCategory {
  user_id: UUID;
  category_id: number;
  created_at: Date;
}

export interface UserSettings {
  user_id: UUID;
  // Notificações
  push_enabled: boolean;
  promo_enabled: boolean;
  sound_enabled: boolean;
  email_enabled: boolean;
  sms_enabled: boolean;
  // Privacidade
  location_sharing: boolean;
  profile_visible: boolean;
  contact_info_sharing: boolean;
  // Segurança
  biometric_enabled: boolean;
  two_factor_enabled: boolean;
  two_factor_method?: 'sms' | 'email' | 'authenticator';
  // Analytics
  analytics: boolean;
  updated_at: Date;
}
