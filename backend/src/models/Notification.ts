import { UUID } from 'crypto';

export interface Notification {
  id: UUID;
  user_id: UUID;
  type: string; // new_service_order_available, order_accepted, etc.
  title: string;
  message: string;
  data?: Record<string, any>; // JSONB
  read: boolean;
  read_at?: Date;
  created_at: Date;
}
