import { UUID } from '../types';

export interface RefreshToken {
  id: UUID;
  user_id: UUID;
  token: string;
  expires_at: Date;
  created_at: Date;
  revoked: boolean;
}

export interface TwoFactorSecret {
  user_id: UUID;
  secret?: string; // Para TOTP (authenticator apps)
  backup_codes?: string[]; // Códigos de backup
  method?: 'sms' | 'email' | 'authenticator';
  phone_verified: boolean;
  created_at: Date;
  updated_at: Date;
}

export interface PasswordResetToken {
  id: UUID;
  user_id: UUID;
  token: string;
  expires_at: Date;
  used: boolean;
  created_at: Date;
}

export interface EmailVerificationToken {
  id: UUID;
  user_id: UUID;
  token: string;
  expires_at: Date;
  used: boolean;
  created_at: Date;
}

// DTOs para requisições
export interface RegisterRequest {
  email: string;
  password: string;
  display_name?: string;
  phone?: string;
  role: 'client' | 'partner';
}

export interface LoginRequest {
  email: string;
  password: string;
  two_factor_code?: string;
}

export interface GoogleLoginRequest {
  id_token: string; // Google ID token
}

export interface RefreshTokenRequest {
  refresh_token: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  token: string;
  new_password: string;
}

export interface ChangePasswordRequest {
  current_password: string;
  new_password: string;
}

export interface VerifyEmailRequest {
  token: string;
}

export interface Enable2FARequest {
  method: 'sms' | 'email' | 'authenticator';
  phone?: string; // Obrigatório se method = 'sms'
}

export interface Verify2FARequest {
  code: string;
}

// DTOs para respostas
export interface AuthResponse {
  user: {
    id: string;
    email: string;
    role: string;
    display_name?: string;
    phone?: string;
    photo_url?: string;
    email_verified: boolean;
    two_factor_enabled: boolean;
  };
  access_token: string;
  refresh_token: string;
  expires_in: number; // Segundos até expiração
}

export interface TwoFactorSetupResponse {
  qr_code_url?: string; // Para authenticator apps
  backup_codes: string[];
  secret?: string; // Para authenticator apps
}
