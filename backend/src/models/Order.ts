import { UUID } from 'crypto';

// Ordem de Serviço
export interface ServiceOrder {
  id: UUID;
  client_id: UUID;
  partner_id?: UUID; // NULL até ser aceita
  created_in_city_id: number; // FIXO
  category: string;
  details: string;
  address?: string;
  latitude?: number;
  longitude?: number;
  budget_min?: number;
  budget_max?: number;
  status: 'pending' | 'accepted' | 'completed' | 'cancelled';
  accepted_at?: Date;
  completed_at?: Date;
  created_at: Date;
  updated_at: Date;
}

export interface Proposal {
  id: UUID;
  order_id: UUID;
  partner_id: UUID;
  amount: number;
  message?: string;
  estimated_time?: string;
  status: 'pending' | 'accepted' | 'rejected';
  created_at: Date;
}

// Pedido de Produto
export interface PurchaseOrder {
  id: UUID;
  order_number: string;
  client_id: UUID;
  seller_id: UUID;
  total: number;
  subtotal?: number;
  delivery_fee?: number;
  platform_fee?: number; // 5% do total
  escrow_amount?: number;
  escrow_released_at?: Date;
  escrow_released_to?: UUID;
  status: 'pending' | 'paid' | 'shipped' | 'delivered' | 'cancelled';
  payment_method?: string; // pix, credit, debit
  payment_status?: string;
  stripe_payment_intent_id?: string;
  delivery_address?: string;
  delivery_city_id?: number;
  tracking_code?: string;
  courier?: string;
  shipped_at?: Date;
  shipped_confirmed_by_seller: boolean;
  delivered_at?: Date;
  delivered_confirmed_by_client: boolean;
  delivered_confirmed_by_seller: boolean;
  created_at: Date;
  updated_at: Date;
}

export interface OrderItem {
  id: number;
  order_id: UUID;
  product_id?: UUID;
  product_name: string;
  product_image_url?: string;
  quantity: number;
  unit_price: number;
  total_price: number;
  created_at: Date;
}

export interface OrderTrackingEvent {
  id: number;
  order_id: UUID;
  status: string; // PREPARING, SHIPPED, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED
  description?: string;
  location?: string;
  event_date: Date;
  created_at: Date;
}
