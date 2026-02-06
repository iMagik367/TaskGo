import speakeasy from 'speakeasy';
import QRCode from 'qrcode';
import { pool, query } from '../database/connection';
import { TwoFactorSecret } from '../models/Auth';
import { UUID } from '../types';
import { EmailService } from './EmailService';

export class TwoFactorService {
  /**
   * Gerar secret para TOTP (authenticator apps)
   */
  static generateSecret(userEmail: string, serviceName: string = 'TaskGo'): string {
    return speakeasy.generateSecret({
      name: `${serviceName} (${userEmail})`,
      length: 32,
    }).base32;
  }

  /**
   * Gerar QR Code URL para authenticator apps
   */
  static async generateQRCode(secret: string, userEmail: string, serviceName: string = 'TaskGo'): Promise<string> {
    const otpauthUrl = speakeasy.otpauthURL({
      secret,
      label: userEmail,
      issuer: serviceName,
      encoding: 'base32',
    });

    try {
      return await QRCode.toDataURL(otpauthUrl);
    } catch (error) {
      console.error('Erro ao gerar QR Code:', error);
      throw new Error('Erro ao gerar QR Code');
    }
  }

  /**
   * Gerar códigos de backup
   */
  static generateBackupCodes(count: number = 10): string[] {
    const codes: string[] = [];
    for (let i = 0; i < count; i++) {
      // Gerar código de 8 dígitos
      const code = Math.floor(10000000 + Math.random() * 90000000).toString();
      codes.push(code);
    }
    return codes;
  }

  /**
   * Verificar código TOTP
   */
  static verifyTOTP(secret: string, token: string): boolean {
    return speakeasy.totp.verify({
      secret,
      encoding: 'base32',
      token,
      window: 2, // Permitir 2 períodos de 30s antes e depois
    });
  }

  /**
   * Gerar código numérico aleatório (para SMS/Email)
   */
  static generateNumericCode(length: number = 6): string {
    const min = Math.pow(10, length - 1);
    const max = Math.pow(10, length) - 1;
    return Math.floor(Math.random() * (max - min + 1) + min).toString();
  }

  /**
   * Buscar configuração 2FA do usuário
   */
  static async getTwoFactorSecret(userId: UUID): Promise<TwoFactorSecret | null> {
    const result = await query(
      `SELECT * FROM two_factor_secrets WHERE user_id = $1`,
      [userId]
    );

    if (result.rows.length === 0) {
      return null;
    }

    return result.rows[0] as TwoFactorSecret;
  }

  /**
   * Habilitar 2FA para usuário
   */
  static async enable2FA(
    userId: UUID,
    method: 'sms' | 'email' | 'authenticator',
    secret?: string,
    phone?: string
  ): Promise<{ secret?: string; qr_code_url?: string; backup_codes: string[] }> {
    const backupCodes = this.generateBackupCodes(10);

    let finalSecret = secret;
    if (method === 'authenticator' && !secret) {
      // Buscar email do usuário para gerar secret
      const userResult = await query(`SELECT email FROM users WHERE id = $1`, [userId]);
      const userEmail = userResult.rows[0]?.email || '';
      finalSecret = this.generateSecret(userEmail);
    }

    // Salvar no banco
    await query(
      `INSERT INTO two_factor_secrets (user_id, secret, backup_codes, method, phone_verified)
       VALUES ($1, $2, $3, $4, $5)
       ON CONFLICT (user_id) DO UPDATE
       SET secret = $2, backup_codes = $3, method = $4, phone_verified = $5, updated_at = NOW()`,
      [
        userId,
        finalSecret || null,
        backupCodes,
        method,
        method === 'sms' ? false : null, // phone_verified será true apenas após verificação
      ]
    );

    // Atualizar user_settings
    await query(
      `UPDATE user_settings SET two_factor_enabled = true, two_factor_method = $1 WHERE user_id = $2`,
      [method, userId]
    );

    let qrCodeUrl: string | undefined;
    if (method === 'authenticator' && finalSecret) {
      const userResult = await query(`SELECT email FROM users WHERE id = $1`, [userId]);
      const userEmail = userResult.rows[0]?.email || '';
      qrCodeUrl = await this.generateQRCode(finalSecret, userEmail);
    }

    return {
      secret: method === 'authenticator' ? finalSecret : undefined,
      qr_code_url: qrCodeUrl,
      backup_codes: backupCodes,
    };
  }

  /**
   * Verificar código 2FA
   */
  static async verify2FA(userId: UUID, code: string): Promise<boolean> {
    const twoFactorSecret = await this.getTwoFactorSecret(userId);
    if (!twoFactorSecret || !twoFactorSecret.method) {
      return false;
    }

    // Verificar código de backup primeiro
    if (twoFactorSecret.backup_codes && twoFactorSecret.backup_codes.includes(code)) {
      // Remover código de backup usado
      const updatedCodes = twoFactorSecret.backup_codes.filter((c) => c !== code);
      await query(
        `UPDATE two_factor_secrets SET backup_codes = $1 WHERE user_id = $2`,
        [updatedCodes, userId]
      );
      return true;
    }

    // Verificar TOTP (authenticator)
    if (twoFactorSecret.method === 'authenticator' && twoFactorSecret.secret) {
      return this.verifyTOTP(twoFactorSecret.secret, code);
    }

    // Para SMS/Email, o código é enviado e verificado em memória (cache)
    // Por enquanto, retornar false - implementar cache Redis se necessário
    return false;
  }

  /**
   * Enviar código 2FA via SMS (placeholder - integrar com serviço SMS)
   */
  static async sendSMSCode(userId: UUID, phone: string): Promise<string> {
    const code = this.generateNumericCode(6);
    // TODO: Integrar com serviço SMS (Twilio, etc.)
    console.log(`[2FA SMS] Código para ${phone}: ${code}`);
    return code;
  }

  /**
   * Enviar código 2FA via Email
   */
  static async sendEmailCode(userId: UUID, email: string): Promise<string> {
    const code = this.generateNumericCode(6);
    await EmailService.send2FACode(email, code);
    return code;
  }

  /**
   * Desabilitar 2FA
   */
  static async disable2FA(userId: UUID): Promise<void> {
    await query(`DELETE FROM two_factor_secrets WHERE user_id = $1`, [userId]);
    await query(
      `UPDATE user_settings SET two_factor_enabled = false, two_factor_method = NULL WHERE user_id = $1`,
      [userId]
    );
  }

  /**
   * Verificar se 2FA está habilitado
   */
  static async is2FAEnabled(userId: UUID): Promise<boolean> {
    const result = await query(
      `SELECT two_factor_enabled FROM user_settings WHERE user_id = $1`,
      [userId]
    );

    if (result.rows.length === 0) {
      return false;
    }

    return result.rows[0].two_factor_enabled === true;
  }
}
