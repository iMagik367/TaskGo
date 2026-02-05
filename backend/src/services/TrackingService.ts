import { PurchaseOrderRepository } from '../repositories/OrderRepository';
import { OrderTrackingEvent } from '../models/Order';
import { NotificationService } from './NotificationService';

export interface TrackingStatus {
  status: 'PREPARING' | 'SHIPPED' | 'IN_TRANSIT' | 'OUT_FOR_DELIVERY' | 'DELIVERED';
  description: string;
  location?: string;
}

export class TrackingService {
  constructor(
    private orderRepo: PurchaseOrderRepository,
    private notificationService: NotificationService
  ) {}

  /**
   * Adiciona evento de rastreamento ao pedido
   */
  async addTrackingEvent(
    orderId: string,
    trackingStatus: TrackingStatus
  ): Promise<OrderTrackingEvent> {
    const event = await this.orderRepo.addTrackingEvent({
      order_id: orderId,
      status: trackingStatus.status,
      description: trackingStatus.description,
      location: trackingStatus.location,
      event_date: new Date()
    });

    // Atualizar status do pedido se necessário
    await this.updateOrderStatusFromTracking(orderId, trackingStatus.status);

    // Notificar cliente
    const order = await this.orderRepo.findById(orderId);
    if (order) {
      await this.notificationService.createNotification({
        user_id: order.client_id,
        type: 'order_tracking_update',
        title: 'Atualização do Pedido',
        message: trackingStatus.description,
        data: {
          orderId,
          status: trackingStatus.status
        }
      });
    }

    return event;
  }

  /**
   * Atualiza status do pedido baseado no rastreamento
   */
  private async updateOrderStatusFromTracking(
    orderId: string,
    trackingStatus: string
  ): Promise<void> {
    let orderStatus: 'pending' | 'paid' | 'shipped' | 'delivered' | 'cancelled' = 'paid';

    switch (trackingStatus) {
      case 'SHIPPED':
        orderStatus = 'shipped';
        await this.orderRepo.updateStatus(orderId, 'shipped', {
          shipped_at: new Date(),
          shipped_confirmed_by_seller: true
        });
        break;
      case 'DELIVERED':
        orderStatus = 'delivered';
        await this.orderRepo.updateStatus(orderId, 'delivered', {
          delivered_at: new Date()
        });
        break;
      default:
        // Manter status atual
        break;
    }
  }

  /**
   * Obtém histórico completo de rastreamento
   */
  async getTrackingHistory(orderId: string): Promise<OrderTrackingEvent[]> {
    return await this.orderRepo.getTrackingEvents(orderId);
  }

  /**
   * Confirma entrega pelo cliente
   */
  async confirmDeliveryByClient(orderId: string): Promise<void> {
    await this.orderRepo.confirmDelivery(orderId, 'client');
    
    // Verificar se ambos confirmaram para liberar escrow
    const order = await this.orderRepo.findById(orderId);
    if (order?.delivered_confirmed_by_client && order?.delivered_confirmed_by_seller) {
      await this.releaseEscrow(orderId);
    }
  }

  /**
   * Confirma entrega pelo vendedor
   */
  async confirmDeliveryBySeller(orderId: string): Promise<void> {
    await this.orderRepo.confirmDelivery(orderId, 'seller');
    
    // Verificar se ambos confirmaram para liberar escrow
    const order = await this.orderRepo.findById(orderId);
    if (order?.delivered_confirmed_by_client && order?.delivered_confirmed_by_seller) {
      await this.releaseEscrow(orderId);
    }
  }

  /**
   * Libera escrow após confirmação de ambos
   */
  private async releaseEscrow(orderId: string): Promise<void> {
    const order = await this.orderRepo.findById(orderId);
    if (!order || !order.escrow_amount) {
      return;
    }

    // Atualizar pedido
    await this.orderRepo.updateStatus(orderId, 'delivered', {
      escrow_released_at: new Date(),
      escrow_released_to: order.seller_id
    });

    // Notificar vendedor
    await this.notificationService.createNotification({
      user_id: order.seller_id,
      type: 'escrow_released',
      title: 'Pagamento Liberado',
      message: `O pagamento do pedido ${order.order_number} foi liberado.`,
      data: {
        orderId,
        amount: order.escrow_amount
      }
    });
  }
}
