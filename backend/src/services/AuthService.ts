import bcrypt from 'bcrypt';
import jwt from 'jsonwebtoken';
import { v4 as uuidv4 } from 'uuid';
import { pool, query, transaction } from '../database/connection';
import { User } from '../models/User';
import { RefreshToken, PasswordResetToken, EmailVerificationToken } from '../models/Auth';
import { UUID } from '../types';

const JWT_SECRET = process.env.JWT_SECRET || 'your-secret-key-change-in-production';
const JWT_REFRESH_SECRET = process.env.JWT_REFRESH_SECRET || 'your-refresh-secret-key-change-in-production';
const JWT_EXPIRES_IN = process.env.JWT_EXPIRES_IN || '15m'; // 15 minutos
const JWT_REFRESH_EXPIRES_IN = process.env.JWT_REFRESH_EXPIRES_IN || '7d'; // 7 dias
const MAX_LOGIN_ATTEMPTS = 5;
const LOCK_TIME = 30 * 60 * 1000; // 30 minutos em milissegundos

export class AuthService {
  /**
   * Hash de senha usando bcrypt
   */
  static async hashPassword(password: string): Promise<string> {
    const saltRounds = 12;
    return await bcrypt.hash(password, saltRounds);
  }

  /**
   * Verificar senha
   */
  static async verifyPassword(password: string, hash: string): Promise<boolean> {
    return await bcrypt.compare(password, hash);
  }

  /**
   * Gerar JWT access token
   */
  static generateAccessToken(userId: UUID, email: string, role: string): string {
    return jwt.sign(
      { userId, email, role },
      JWT_SECRET,
      { expiresIn: JWT_EXPIRES_IN }
    );
  }

  /**
   * Gerar JWT refresh token
   */
  static generateRefreshToken(): string {
    return uuidv4() + '-' + uuidv4(); // Token único
  }

  /**
   * Verificar e decodificar JWT token
   */
  static verifyToken(token: string): { userId: UUID; email: string; role: string } | null {
    try {
      const decoded = jwt.verify(token, JWT_SECRET) as any;
      return {
        userId: decoded.userId,
        email: decoded.email,
        role: decoded.role
      };
    } catch (error) {
      return null;
    }
  }

  /**
   * Salvar refresh token no banco
   */
  static async saveRefreshToken(
    userId: UUID,
    token: string,
    expiresAt: Date
  ): Promise<void> {
    await query(
      `INSERT INTO refresh_tokens (user_id, token, expires_at)
       VALUES ($1, $2, $3)`,
      [userId, token, expiresAt]
    );
  }

  /**
   * Validar refresh token
   */
  static async validateRefreshToken(token: string): Promise<RefreshToken | null> {
    const result = await query(
      `SELECT * FROM refresh_tokens
       WHERE token = $1 AND revoked = false AND expires_at > NOW()`,
      [token]
    );

    if (result.rows.length === 0) {
      return null;
    }

    return result.rows[0] as RefreshToken;
  }

  /**
   * Revogar refresh token
   */
  static async revokeRefreshToken(token: string): Promise<void> {
    await query(
      `UPDATE refresh_tokens SET revoked = true WHERE token = $1`,
      [token]
    );
  }

  /**
   * Revogar todos os refresh tokens de um usuário
   */
  static async revokeAllUserTokens(userId: UUID): Promise<void> {
    await query(
      `UPDATE refresh_tokens SET revoked = true WHERE user_id = $1`,
      [userId]
    );
  }

  /**
   * Verificar se usuário está bloqueado
   */
  static async isUserLocked(userId: UUID): Promise<boolean> {
    const result = await query(
      `SELECT locked_until FROM users WHERE id = $1`,
      [userId]
    );

    if (result.rows.length === 0) {
      return false;
    }

    const user = result.rows[0];
    if (!user.locked_until) {
      return false;
    }

    const lockedUntil = new Date(user.locked_until);
    if (lockedUntil > new Date()) {
      return true; // Ainda bloqueado
    }

    // Desbloquear se o tempo expirou
    await query(
      `UPDATE users SET locked_until = NULL, failed_login_attempts = 0 WHERE id = $1`,
      [userId]
    );

    return false;
  }

  /**
   * Incrementar tentativas de login falhadas
   */
  static async incrementFailedLoginAttempts(userId: UUID): Promise<void> {
    const result = await query(
      `UPDATE users
       SET failed_login_attempts = failed_login_attempts + 1
       WHERE id = $1
       RETURNING failed_login_attempts`,
      [userId]
    );

    const attempts = result.rows[0].failed_login_attempts;

    // Bloquear se exceder limite
    if (attempts >= MAX_LOGIN_ATTEMPTS) {
      const lockUntil = new Date(Date.now() + LOCK_TIME);
      await query(
        `UPDATE users SET locked_until = $1 WHERE id = $2`,
        [lockUntil, userId]
      );
    }
  }

  /**
   * Resetar tentativas de login falhadas
   */
  static async resetFailedLoginAttempts(userId: UUID): Promise<void> {
    await query(
      `UPDATE users
       SET failed_login_attempts = 0, locked_until = NULL, last_login = NOW()
       WHERE id = $1`,
      [userId]
    );
  }

  /**
   * Criar token de reset de senha
   */
  static async createPasswordResetToken(userId: UUID): Promise<string> {
    const token = uuidv4();
    const expiresAt = new Date(Date.now() + 60 * 60 * 1000); // 1 hora

    // Revogar tokens anteriores
    await query(
      `UPDATE password_reset_tokens SET used = true WHERE user_id = $1 AND used = false`,
      [userId]
    );

    // Criar novo token
    await query(
      `INSERT INTO password_reset_tokens (user_id, token, expires_at)
       VALUES ($1, $2, $3)`,
      [userId, token, expiresAt]
    );

    return token;
  }

  /**
   * Validar token de reset de senha
   */
  static async validatePasswordResetToken(token: string): Promise<PasswordResetToken | null> {
    const result = await query(
      `SELECT * FROM password_reset_tokens
       WHERE token = $1 AND used = false AND expires_at > NOW()`,
      [token]
    );

    if (result.rows.length === 0) {
      return null;
    }

    return result.rows[0] as PasswordResetToken;
  }

  /**
   * Marcar token de reset como usado
   */
  static async markPasswordResetTokenAsUsed(token: string): Promise<void> {
    await query(
      `UPDATE password_reset_tokens SET used = true WHERE token = $1`,
      [token]
    );
  }

  /**
   * Criar token de verificação de email
   */
  static async createEmailVerificationToken(userId: UUID): Promise<string> {
    const token = uuidv4();
    const expiresAt = new Date(Date.now() + 24 * 60 * 60 * 1000); // 24 horas

    // Revogar tokens anteriores
    await query(
      `UPDATE email_verification_tokens SET used = true WHERE user_id = $1 AND used = false`,
      [userId]
    );

    // Criar novo token
    await query(
      `INSERT INTO email_verification_tokens (user_id, token, expires_at)
       VALUES ($1, $2, $3)`,
      [userId, token, expiresAt]
    );

    return token;
  }

  /**
   * Validar token de verificação de email
   */
  static async validateEmailVerificationToken(token: string): Promise<EmailVerificationToken | null> {
    const result = await query(
      `SELECT * FROM email_verification_tokens
       WHERE token = $1 AND used = false AND expires_at > NOW()`,
      [token]
    );

    if (result.rows.length === 0) {
      return null;
    }

    return result.rows[0] as EmailVerificationToken;
  }

  /**
   * Marcar token de verificação como usado e verificar email
   */
  static async verifyEmail(token: string): Promise<boolean> {
    const verificationToken = await this.validateEmailVerificationToken(token);
    if (!verificationToken) {
      return false;
    }

    await transaction(async (client) => {
      // Marcar token como usado
      await client.query(
        `UPDATE email_verification_tokens SET used = true WHERE token = $1`,
        [token]
      );

      // Verificar email do usuário
      await client.query(
        `UPDATE users SET email_verified = true, email_verified_at = NOW() WHERE id = $1`,
        [verificationToken.user_id]
      );
    });

    return true;
  }

  /**
   * Calcular data de expiração do refresh token
   */
  static getRefreshTokenExpiration(): Date {
    const days = parseInt(JWT_REFRESH_EXPIRES_IN.replace('d', '')) || 7;
    return new Date(Date.now() + days * 24 * 60 * 60 * 1000);
  }
}
