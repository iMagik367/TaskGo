import { query } from '../database/connection';
import {
  ServiceOrder,
  Proposal,
  PurchaseOrder,
  OrderItem,
  OrderTrackingEvent
} from '../models/Order';
import { v4 as uuidv4 } from 'uuid';

export class ServiceOrderRepository {
  async findById(id: string): Promise<ServiceOrder | null> {
    const result = await query(
      'SELECT * FROM service_orders WHERE id = $1',
      [id]
    );
    return result.rows[0] || null;
  }

  async findByCityAndCategory(
    cityId: number,
    category: string,
    limit: number = 50
  ): Promise<ServiceOrder[]> {
    const result = await query(
      `SELECT * FROM service_orders
       WHERE created_in_city_id = $1 
         AND category = $2
         AND status = 'pending'
         AND partner_id IS NULL
       ORDER BY created_at DESC
       LIMIT $3`,
      [cityId, category, limit]
    );
    return result.rows;
  }

  async findByClient(clientId: string): Promise<ServiceOrder[]> {
    const result = await query(
      'SELECT * FROM service_orders WHERE client_id = $1 ORDER BY created_at DESC',
      [clientId]
    );
    return result.rows;
  }

  async findByPartner(partnerId: string): Promise<ServiceOrder[]> {
    const result = await query(
      'SELECT * FROM service_orders WHERE partner_id = $1 ORDER BY created_at DESC',
      [partnerId]
    );
    return result.rows;
  }

  async create(order: Partial<ServiceOrder>): Promise<ServiceOrder> {
    const id = order.id || uuidv4();
    const result = await query(
      `INSERT INTO service_orders (
        id, client_id, partner_id, created_in_city_id, category, details,
        address, latitude, longitude, budget_min, budget_max, status
      ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)
      RETURNING *`,
      [
        id, order.client_id, order.partner_id || null, order.created_in_city_id,
        order.category, order.details, order.address, order.latitude,
        order.longitude, order.budget_min, order.budget_max,
        order.status || 'pending'
      ]
    );
    return result.rows[0];
  }

  async acceptOrder(orderId: string, partnerId: string): Promise<ServiceOrder> {
    const result = await query(
      `UPDATE service_orders 
       SET partner_id = $1, status = 'accepted', accepted_at = CURRENT_TIMESTAMP
       WHERE id = $2 AND status = 'pending' AND partner_id IS NULL
       RETURNING *`,
      [partnerId, orderId]
    );
    return result.rows[0];
  }

  async createProposal(proposal: Partial<Proposal>): Promise<Proposal> {
    const id = proposal.id || uuidv4();
    const result = await query(
      `INSERT INTO proposals (id, order_id, partner_id, amount, message, estimated_time, status)
       VALUES ($1, $2, $3, $4, $5, $6, $7)
       RETURNING *`,
      [
        id, proposal.order_id, proposal.partner_id, proposal.amount,
        proposal.message, proposal.estimated_time, proposal.status || 'pending'
      ]
    );
    return result.rows[0];
  }

  async getProposals(orderId: string): Promise<Proposal[]> {
    const result = await query(
      'SELECT * FROM proposals WHERE order_id = $1 ORDER BY created_at DESC',
      [orderId]
    );
    return result.rows;
  }
}

export class PurchaseOrderRepository {
  async findById(id: string): Promise<PurchaseOrder | null> {
    const result = await query(
      'SELECT * FROM purchase_orders WHERE id = $1',
      [id]
    );
    return result.rows[0] || null;
  }

  async findByOrderNumber(orderNumber: string): Promise<PurchaseOrder | null> {
    const result = await query(
      'SELECT * FROM purchase_orders WHERE order_number = $1',
      [orderNumber]
    );
    return result.rows[0] || null;
  }

  async findByClient(clientId: string): Promise<PurchaseOrder[]> {
    const result = await query(
      'SELECT * FROM purchase_orders WHERE client_id = $1 ORDER BY created_at DESC',
      [clientId]
    );
    return result.rows;
  }

  async findBySeller(sellerId: string): Promise<PurchaseOrder[]> {
    const result = await query(
      'SELECT * FROM purchase_orders WHERE seller_id = $1 ORDER BY created_at DESC',
      [sellerId]
    );
    return result.rows;
  }

  async create(order: Partial<PurchaseOrder>): Promise<PurchaseOrder> {
    const id = order.id || uuidv4();
    const orderNumber = order.order_number || `ORD-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
    
    const result = await query(
      `INSERT INTO purchase_orders (
        id, order_number, client_id, seller_id, total, subtotal, delivery_fee,
        platform_fee, escrow_amount, status, payment_method, delivery_address,
        delivery_city_id
      ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13)
      RETURNING *`,
      [
        id, orderNumber, order.client_id, order.seller_id, order.total,
        order.subtotal, order.delivery_fee, order.platform_fee,
        order.escrow_amount, order.status || 'pending', order.payment_method,
        order.delivery_address, order.delivery_city_id
      ]
    );
    return result.rows[0];
  }

  async updateStatus(
    id: string,
    status: PurchaseOrder['status'],
    updates?: Partial<PurchaseOrder>
  ): Promise<PurchaseOrder> {
    const fields: string[] = ['status = $1'];
    const values: any[] = [status];
    let paramCount = 2;

    if (updates) {
      Object.entries(updates).forEach(([key, value]) => {
        if (value !== undefined && key !== 'id' && key !== 'status') {
          fields.push(`${key} = $${paramCount}`);
          values.push(value);
          paramCount++;
        }
      });
    }

    values.push(id);
    const result = await query(
      `UPDATE purchase_orders SET ${fields.join(', ')} WHERE id = $${paramCount} RETURNING *`,
      values
    );
    return result.rows[0];
  }

  async addItem(item: Partial<OrderItem>): Promise<OrderItem> {
    const result = await query(
      `INSERT INTO order_items (
        order_id, product_id, product_name, product_image_url,
        quantity, unit_price, total_price
      ) VALUES ($1, $2, $3, $4, $5, $6, $7)
      RETURNING *`,
      [
        item.order_id, item.product_id, item.product_name,
        item.product_image_url, item.quantity || 1,
        item.unit_price, item.total_price
      ]
    );
    return result.rows[0];
  }

  async getItems(orderId: string): Promise<OrderItem[]> {
    const result = await query(
      'SELECT * FROM order_items WHERE order_id = $1 ORDER BY created_at',
      [orderId]
    );
    return result.rows;
  }

  async addTrackingEvent(event: Partial<OrderTrackingEvent>): Promise<OrderTrackingEvent> {
    const result = await query(
      `INSERT INTO order_tracking_events (
        order_id, status, description, location, event_date
      ) VALUES ($1, $2, $3, $4, $5)
      RETURNING *`,
      [
        event.order_id, event.status, event.description,
        event.location, event.event_date || new Date()
      ]
    );
    return result.rows[0];
  }

  async getTrackingEvents(orderId: string): Promise<OrderTrackingEvent[]> {
    const result = await query(
      'SELECT * FROM order_tracking_events WHERE order_id = $1 ORDER BY event_date DESC',
      [orderId]
    );
    return result.rows;
  }

  async confirmDelivery(
    orderId: string,
    confirmedBy: 'client' | 'seller'
  ): Promise<PurchaseOrder> {
    const field = confirmedBy === 'client'
      ? 'delivered_confirmed_by_client'
      : 'delivered_confirmed_by_seller';
    
    const result = await query(
      `UPDATE purchase_orders 
       SET ${field} = true, delivered_at = CURRENT_TIMESTAMP
       WHERE id = $1
       RETURNING *`,
      [orderId]
    );
    return result.rows[0];
  }
}
