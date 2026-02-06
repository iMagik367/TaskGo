import { OAuth2Client } from 'google-auth-library';
import { pool, query } from '../database/connection';
import { User } from '../models/User';
import { UUID } from '../types';

const GOOGLE_CLIENT_ID = process.env.GOOGLE_CLIENT_ID || '';

export class GoogleAuthService {
  private static client: OAuth2Client;

  static initialize() {
    this.client = new OAuth2Client(GOOGLE_CLIENT_ID);
  }

  /**
   * Verificar e validar ID token do Google
   */
  static async verifyIdToken(idToken: string): Promise<{
    googleId: string;
    email: string;
    name?: string;
    picture?: string;
  } | null> {
    try {
      if (!GOOGLE_CLIENT_ID) {
        throw new Error('GOOGLE_CLIENT_ID não configurado');
      }

      const ticket = await this.client.verifyIdToken({
        idToken,
        audience: GOOGLE_CLIENT_ID,
      });

      const payload = ticket.getPayload();
      if (!payload) {
        return null;
      }

      return {
        googleId: payload.sub,
        email: payload.email || '',
        name: payload.name,
        picture: payload.picture,
      };
    } catch (error) {
      console.error('Erro ao verificar token do Google:', error);
      return null;
    }
  }

  /**
   * Buscar usuário por Google ID
   */
  static async findUserByGoogleId(googleId: string): Promise<User | null> {
    const result = await query(
      `SELECT * FROM users WHERE google_id = $1`,
      [googleId]
    );

    if (result.rows.length === 0) {
      return null;
    }

    return result.rows[0] as User;
  }

  /**
   * Buscar usuário por email
   */
  static async findUserByEmail(email: string): Promise<User | null> {
    const result = await query(
      `SELECT * FROM users WHERE email = $1`,
      [email]
    );

    if (result.rows.length === 0) {
      return null;
    }

    return result.rows[0] as User;
  }

  /**
   * Criar ou atualizar usuário com Google
   */
  static async createOrUpdateUserWithGoogle(
    googleId: string,
    email: string,
    name?: string,
    picture?: string
  ): Promise<User> {
    // Verificar se já existe usuário com este Google ID
    let user = await this.findUserByGoogleId(googleId);

    if (user) {
      // Atualizar dados se necessário
      if (name && name !== user.display_name) {
        await query(
          `UPDATE users SET display_name = $1, photo_url = $2, updated_at = NOW() WHERE id = $3`,
          [name, picture, user.id]
        );
        user.display_name = name;
        user.photo_url = picture;
      }
      return user;
    }

    // Verificar se já existe usuário com este email
    const existingUser = await this.findUserByEmail(email);
    if (existingUser) {
      // Vincular Google ID ao usuário existente
      await query(
        `UPDATE users SET google_id = $1, email_verified = true, email_verified_at = NOW(), updated_at = NOW() WHERE id = $2`,
        [googleId, existingUser.id]
      );
      existingUser.google_id = googleId;
      existingUser.email_verified = true;
      return existingUser;
    }

    // Criar novo usuário
    const result = await query(
      `INSERT INTO users (
        email, google_id, display_name, photo_url, role, email_verified, email_verified_at
      ) VALUES ($1, $2, $3, $4, $5, $6, NOW())
      RETURNING *`,
      [email, googleId, name, picture, 'client', true]
    );

    // Criar configurações padrão
    await query(
      `INSERT INTO user_settings (user_id) VALUES ($1)`,
      [result.rows[0].id]
    );

    return result.rows[0] as User;
  }
}

// Inicializar quando o módulo for carregado
if (GOOGLE_CLIENT_ID) {
  GoogleAuthService.initialize();
}
