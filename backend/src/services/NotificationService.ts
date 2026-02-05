import { query } from '../database/connection';
import { Notification } from '../models/Notification';
import { v4 as uuidv4 } from 'uuid';
import { Server as SocketIOServer } from 'socket.io';
import { PoolClient } from 'pg';

export class NotificationService {
  private io?: SocketIOServer;
  private pgClient?: PoolClient;

  constructor(io?: SocketIOServer) {
    this.io = io;
  }

  /**
   * Configura o cliente PostgreSQL para escutar NOTIFY
   */
  async setupPostgreSQLListener(client: PoolClient): Promise<void> {
    this.pgClient = client;
    
    // Escutar canal de novas ordens de serviço
    await client.query('LISTEN new_service_order');
    
    client.on('notification', (msg) => {
      if (msg.channel === 'new_service_order') {
        this.handleNewServiceOrderNotification(msg.payload);
      }
    });
  }

  /**
   * Cria uma notificação no banco de dados
   */
  async createNotification(notification: Partial<Notification>): Promise<Notification> {
    const id = notification.id || uuidv4();
    const result = await query(
      `INSERT INTO notifications (id, user_id, type, title, message, data, read)
       VALUES ($1, $2, $3, $4, $5, $6, $7)
       RETURNING *`,
      [
        id,
        notification.user_id,
        notification.type,
        notification.title,
        notification.message,
        notification.data ? JSON.stringify(notification.data) : null,
        notification.read || false
      ]
    );
    return result.rows[0];
  }

  /**
   * Marca notificação como lida
   */
  async markAsRead(notificationId: string, userId: string): Promise<void> {
    await query(
      `UPDATE notifications 
       SET read = true, read_at = CURRENT_TIMESTAMP
       WHERE id = $1 AND user_id = $2`,
      [notificationId, userId]
    );
  }

  /**
   * Obtém notificações não lidas do usuário
   */
  async getUnreadNotifications(userId: string, limit: number = 50): Promise<Notification[]> {
    const result = await query(
      `SELECT * FROM notifications
       WHERE user_id = $1 AND read = false
       ORDER BY created_at DESC
       LIMIT $2`,
      [userId, limit]
    );
    return result.rows;
  }

  /**
   * Obtém todas as notificações do usuário
   */
  async getUserNotifications(userId: string, limit: number = 50): Promise<Notification[]> {
    const result = await query(
      `SELECT * FROM notifications
       WHERE user_id = $1
       ORDER BY created_at DESC
       LIMIT $2`,
      [userId, limit]
    );
    return result.rows;
  }

  /**
   * Envia notificação via WebSocket em tempo real
   */
  async sendRealtimeNotification(userId: string, notification: Notification): Promise<void> {
    if (this.io) {
      this.io.to(`user:${userId}`).emit('notification', notification);
    }
  }

  /**
   * Notifica parceiros sobre nova ordem de serviço
   */
  async notifyPartnersAboutNewOrder(
    orderId: string,
    cityId: number,
    category: string
  ): Promise<void> {
    // Buscar parceiros na cidade com a categoria
    const partners = await query(
      `SELECT DISTINCT u.id, u.firebase_uid FROM users u
       INNER JOIN user_preferred_categories upc ON u.id = upc.user_id
       INNER JOIN categories c ON upc.category_id = c.id
       WHERE u.role = 'partner'
         AND u.current_city_id = $1
         AND c.name = $2
         AND u.verified = true`,
      [cityId, category]
    );

    // Criar notificação para cada parceiro
    for (const partner of partners.rows) {
      const notification = await this.createNotification({
        user_id: partner.id,
        type: 'new_service_order_available',
        title: 'Nova Ordem de Serviço Disponível',
        message: `Uma nova ordem de serviço na categoria ${category} está disponível na sua cidade.`,
        data: {
          orderId,
          category,
          cityId
        }
      });

      // Enviar via WebSocket
      await this.sendRealtimeNotification(partner.id, notification);
    }
  }

  /**
   * Handler para notificações do PostgreSQL LISTEN
   */
  private async handleNewServiceOrderNotification(payload: string): Promise<void> {
    try {
      const data = JSON.parse(payload);
      const { order_id, city_id, category } = data;
      
      await this.notifyPartnersAboutNewOrder(order_id, city_id, category);
    } catch (error) {
      console.error('Erro ao processar notificação do PostgreSQL:', error);
    }
  }
}
