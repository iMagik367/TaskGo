import { UUID } from '../types';

export interface Review {
  id: UUID;
  reviewer_id: UUID;
  target_id: UUID; // Pode ser product, service_order, ou user
  target_type: 'product' | 'service' | 'partner';
  rating: number; // 1-5
  comment?: string;
  created_at: Date;
}
