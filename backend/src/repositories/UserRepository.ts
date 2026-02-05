import { query } from '../database/connection';
import { User, UserLocation, UserSettings, UserPreferredCategory } from '../models/User';
import { v4 as uuidv4 } from 'uuid';

export class UserRepository {
  async findById(id: string): Promise<User | null> {
    const result = await query(
      'SELECT * FROM users WHERE id = $1',
      [id]
    );
    return result.rows[0] || null;
  }

  async findByFirebaseUid(firebaseUid: string): Promise<User | null> {
    const result = await query(
      'SELECT * FROM users WHERE firebase_uid = $1',
      [firebaseUid]
    );
    return result.rows[0] || null;
  }

  async findByEmail(email: string): Promise<User | null> {
    const result = await query(
      'SELECT * FROM users WHERE email = $1',
      [email]
    );
    return result.rows[0] || null;
  }

  async create(user: Partial<User>): Promise<User> {
    const id = user.id || uuidv4();
    const result = await query(
      `INSERT INTO users (
        id, firebase_uid, email, role, display_name, phone, photo_url,
        current_latitude, current_longitude, current_city_id,
        cpf, cnpj, rg, birth_date,
        document_front_url, document_back_url, selfie_url, address_proof_url,
        profile_complete, verified, stripe_account_id,
        stripe_charges_enabled, stripe_payouts_enabled, rating
      ) VALUES (
        $1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14,
        $15, $16, $17, $18, $19, $20, $21, $22, $23, $24
      ) RETURNING *`,
      [
        id, user.firebase_uid, user.email, user.role, user.display_name,
        user.phone, user.photo_url, user.current_latitude, user.current_longitude,
        user.current_city_id, user.cpf, user.cnpj, user.rg, user.birth_date,
        user.document_front_url, user.document_back_url, user.selfie_url,
        user.address_proof_url, user.profile_complete || false,
        user.verified || false, user.stripe_account_id,
        user.stripe_charges_enabled || false, user.stripe_payouts_enabled || false,
        user.rating || 0.00
      ]
    );
    return result.rows[0];
  }

  async update(id: string, updates: Partial<User>): Promise<User> {
    const fields: string[] = [];
    const values: any[] = [];
    let paramCount = 1;

    Object.entries(updates).forEach(([key, value]) => {
      if (value !== undefined && key !== 'id') {
        fields.push(`${key} = $${paramCount}`);
        values.push(value);
        paramCount++;
      }
    });

    if (fields.length === 0) {
      throw new Error('Nenhum campo para atualizar');
    }

    values.push(id);
    const result = await query(
      `UPDATE users SET ${fields.join(', ')} WHERE id = $${paramCount} RETURNING *`,
      values
    );
    return result.rows[0];
  }

  async updateLocation(
    userId: string,
    latitude: number,
    longitude: number,
    cityId: number
  ): Promise<void> {
    // Atualizar localização atual do usuário
    await query(
      `UPDATE users 
       SET current_latitude = $1, current_longitude = $2, 
           current_city_id = $3, last_location_update = CURRENT_TIMESTAMP
       WHERE id = $4`,
      [latitude, longitude, cityId, userId]
    );

    // Marcar localização anterior como não atual
    await query(
      `UPDATE user_locations 
       SET is_current = false, exited_at = CURRENT_TIMESTAMP
       WHERE user_id = $1 AND is_current = true`,
      [userId]
    );

    // Criar nova localização atual
    await query(
      `INSERT INTO user_locations (user_id, city_id, latitude, longitude, is_current, entered_at)
       VALUES ($1, $2, $3, $4, true, CURRENT_TIMESTAMP)`,
      [userId, cityId, latitude, longitude]
    );
  }

  async getCurrentLocation(userId: string): Promise<UserLocation | null> {
    const result = await query(
      'SELECT * FROM user_locations WHERE user_id = $1 AND is_current = true',
      [userId]
    );
    return result.rows[0] || null;
  }

  async getLocationHistory(userId: string, limit: number = 50): Promise<UserLocation[]> {
    const result = await query(
      'SELECT * FROM user_locations WHERE user_id = $1 ORDER BY entered_at DESC LIMIT $2',
      [userId, limit]
    );
    return result.rows;
  }

  async getPreferredCategories(userId: string): Promise<number[]> {
    const result = await query(
      'SELECT category_id FROM user_preferred_categories WHERE user_id = $1',
      [userId]
    );
    return result.rows.map(row => row.category_id);
  }

  async setPreferredCategories(userId: string, categoryIds: number[]): Promise<void> {
    // Remover categorias antigas
    await query(
      'DELETE FROM user_preferred_categories WHERE user_id = $1',
      [userId]
    );

    // Inserir novas categorias
    if (categoryIds.length > 0) {
      const values = categoryIds.map((_, index) => 
        `($1, $${index + 2}, CURRENT_TIMESTAMP)`
      ).join(', ');
      const params = [userId, ...categoryIds];
      await query(
        `INSERT INTO user_preferred_categories (user_id, category_id, created_at) VALUES ${values}`,
        params
      );
    }
  }

  async getSettings(userId: string): Promise<UserSettings | null> {
    const result = await query(
      'SELECT * FROM user_settings WHERE user_id = $1',
      [userId]
    );
    return result.rows[0] || null;
  }

  async updateSettings(userId: string, settings: Partial<UserSettings>): Promise<UserSettings> {
    const existing = await this.getSettings(userId);
    
    if (!existing) {
      // Criar configurações padrão
      await query(
        `INSERT INTO user_settings (user_id, push_enabled, promo_enabled, sound_enabled,
         email_enabled, sms_enabled, location_sharing, profile_visible,
         contact_info_sharing, biometric_enabled, two_factor_enabled,
         two_factor_method, analytics)
         VALUES ($1, true, true, true, false, false, true, true, false, false, false, null, true)`,
        [userId]
      );
    }

    const fields: string[] = [];
    const values: any[] = [];
    let paramCount = 1;

    Object.entries(settings).forEach(([key, value]) => {
      if (value !== undefined && key !== 'user_id') {
        fields.push(`${key} = $${paramCount}`);
        values.push(value);
        paramCount++;
      }
    });

    if (fields.length === 0) {
      const result = await query(
        'SELECT * FROM user_settings WHERE user_id = $1',
        [userId]
      );
      return result.rows[0];
    }

    values.push(userId);
    const result = await query(
      `UPDATE user_settings SET ${fields.join(', ')}, updated_at = CURRENT_TIMESTAMP 
       WHERE user_id = $${paramCount} RETURNING *`,
      values
    );
    return result.rows[0];
  }

  async findPartnersByCityAndCategory(
    cityId: number,
    categoryId: number
  ): Promise<User[]> {
    const result = await query(
      `SELECT DISTINCT u.* FROM users u
       INNER JOIN user_preferred_categories upc ON u.id = upc.user_id
       WHERE u.role = 'partner'
         AND u.current_city_id = $1
         AND upc.category_id = $2
         AND u.verified = true`,
      [cityId, categoryId]
    );
    return result.rows;
  }
}
