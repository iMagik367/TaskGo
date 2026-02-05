import { UUID } from '../types';

export interface Story {
  id: UUID;
  user_id: UUID;
  created_in_city_id: number; // FIXO
  media_url: string;
  media_type: 'image' | 'video';
  expires_at: Date; // created_at + 24h
  views_count: number;
  created_at: Date;
}
