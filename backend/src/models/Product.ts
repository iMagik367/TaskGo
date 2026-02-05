import { UUID } from 'crypto';

export interface Product {
  id: UUID;
  seller_id: UUID;
  created_in_city_id: number; // FIXO - cidade onde foi criado
  title: string;
  description?: string;
  price: number;
  category?: string;
  active: boolean;
  featured: boolean;
  discount_percentage?: number;
  rating: number;
  created_at: Date;
  updated_at: Date;
}

export interface ProductImage {
  id: number;
  product_id: UUID;
  image_url: string;
  position: number;
  created_at: Date;
}
