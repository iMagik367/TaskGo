import { query } from '../database/connection';
import { Product, ProductImage } from '../models/Product';
import { v4 as uuidv4 } from 'uuid';

export class ProductRepository {
  async findById(id: string): Promise<Product | null> {
    const result = await query(
      'SELECT * FROM products WHERE id = $1',
      [id]
    );
    return result.rows[0] || null;
  }

  async findByCity(cityId: number, limit: number = 50, offset: number = 0): Promise<Product[]> {
    const result = await query(
      `SELECT * FROM products 
       WHERE created_in_city_id = $1 AND active = true
       ORDER BY created_at DESC
       LIMIT $2 OFFSET $3`,
      [cityId, limit, offset]
    );
    return result.rows;
  }

  async findBySeller(sellerId: string): Promise<Product[]> {
    const result = await query(
      'SELECT * FROM products WHERE seller_id = $1 ORDER BY created_at DESC',
      [sellerId]
    );
    return result.rows;
  }

  async create(product: Partial<Product>): Promise<Product> {
    const id = product.id || uuidv4();
    const result = await query(
      `INSERT INTO products (
        id, seller_id, created_in_city_id, title, description, price,
        category, active, featured, discount_percentage, rating
      ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11)
      RETURNING *`,
      [
        id, product.seller_id, product.created_in_city_id, product.title,
        product.description, product.price, product.category,
        product.active !== false, product.featured || false,
        product.discount_percentage, product.rating || 0.00
      ]
    );
    return result.rows[0];
  }

  async update(id: string, updates: Partial<Product>): Promise<Product> {
    const fields: string[] = [];
    const values: any[] = [];
    let paramCount = 1;

    Object.entries(updates).forEach(([key, value]) => {
      if (value !== undefined && key !== 'id' && key !== 'created_in_city_id') {
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
      `UPDATE products SET ${fields.join(', ')} WHERE id = $${paramCount} RETURNING *`,
      values
    );
    return result.rows[0];
  }

  async delete(id: string): Promise<void> {
    await query('DELETE FROM products WHERE id = $1', [id]);
  }

  async getImages(productId: string): Promise<ProductImage[]> {
    const result = await query(
      'SELECT * FROM product_images WHERE product_id = $1 ORDER BY position',
      [productId]
    );
    return result.rows;
  }

  async addImage(productId: string, imageUrl: string, position: number = 0): Promise<ProductImage> {
    const result = await query(
      `INSERT INTO product_images (product_id, image_url, position)
       VALUES ($1, $2, $3) RETURNING *`,
      [productId, imageUrl, position]
    );
    return result.rows[0];
  }

  async removeImage(imageId: number): Promise<void> {
    await query('DELETE FROM product_images WHERE id = $1', [imageId]);
  }
}
