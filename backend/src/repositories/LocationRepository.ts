import { query } from '../database/connection';
import { State, City, Category } from '../models/Location';

export class LocationRepository {
  async findStateByCode(code: string): Promise<State | null> {
    const result = await query(
      'SELECT * FROM states WHERE code = $1',
      [code]
    );
    return result.rows[0] || null;
  }

  async findCityById(id: number): Promise<City | null> {
    const result = await query(
      'SELECT * FROM cities WHERE id = $1',
      [id]
    );
    return result.rows[0] || null;
  }

  async findCityByNameAndState(cityName: string, stateCode: string): Promise<City | null> {
    const normalizedName = cityName.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '');
    const result = await query(
      `SELECT c.* FROM cities c
       INNER JOIN states s ON c.state_id = s.id
       WHERE c.normalized_name = $1 AND s.code = $2`,
      [normalizedName, stateCode]
    );
    return result.rows[0] || null;
  }

  async findNearestCity(latitude: number, longitude: number, radiusKm: number = 50): Promise<City | null> {
    // Usar PostGIS se disponível, senão usar cálculo de distância simples
    const result = await query(
      `SELECT *, 
        (6371 * acos(
          cos(radians($1)) * cos(radians(latitude)) *
          cos(radians(longitude) - radians($2)) +
          sin(radians($1)) * sin(radians(latitude))
        )) AS distance
       FROM cities
       WHERE (6371 * acos(
         cos(radians($1)) * cos(radians(latitude)) *
         cos(radians(longitude) - radians($2)) +
         sin(radians($1)) * sin(radians(latitude))
       )) <= $3
       ORDER BY distance
       LIMIT 1`,
      [latitude, longitude, radiusKm]
    );
    return result.rows[0] || null;
  }

  async getAllStates(): Promise<State[]> {
    const result = await query('SELECT * FROM states ORDER BY name');
    return result.rows;
  }

  async getCitiesByState(stateId: number): Promise<City[]> {
    const result = await query(
      'SELECT * FROM cities WHERE state_id = $1 ORDER BY name',
      [stateId]
    );
    return result.rows;
  }

  async getAllCategories(type?: 'service' | 'product'): Promise<Category[]> {
    let queryText = 'SELECT * FROM categories';
    const params: any[] = [];
    
    if (type) {
      queryText += ' WHERE type = $1';
      params.push(type);
    }
    
    queryText += ' ORDER BY name';
    const result = await query(queryText, params);
    return result.rows;
  }
}
