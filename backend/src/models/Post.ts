import { UUID } from '../types';

export interface Post {
  id: UUID;
  user_id: UUID;
  created_in_city_id: number; // FIXO
  content: string;
  media_urls?: string[];
  media_types?: string[];
  likes_count: number;
  comments_count: number;
  created_at: Date;
  updated_at: Date;
}

export interface PostLike {
  id: number;
  post_id: UUID;
  user_id: UUID;
  created_at: Date;
}

export interface PostComment {
  id: number;
  post_id: UUID;
  user_id: UUID;
  content: string;
  created_at: Date;
}
