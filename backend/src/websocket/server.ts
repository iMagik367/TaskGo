import { Server as HTTPServer } from 'http';
import { Server as SocketIOServer, Socket } from 'socket.io';
import { pool, query } from '../database/connection';
import { NotificationService } from '../services/NotificationService';

export class WebSocketServer {
  private io: SocketIOServer;
  private notificationService: NotificationService;
  private pgClient: any;

  constructor(httpServer: HTTPServer) {
    this.io = new SocketIOServer(httpServer, {
      cors: {
        origin: process.env.CORS_ORIGIN || '*',
        methods: ['GET', 'POST']
      }
    });

    this.notificationService = new NotificationService(this.io);
    this.setupEventHandlers();
    this.setupPostgreSQLListener();
  }

  private setupEventHandlers(): void {
    this.io.on('connection', (socket: Socket) => {
      console.log(`✅ Cliente conectado: ${socket.id}`);

      // Autenticar usuário
      socket.on('authenticate', async (data: { userId: string; token?: string }) => {
        // TODO: Validar token JWT/Firebase
        const { userId } = data;
        socket.join(`user:${userId}`);
        console.log(`👤 Usuário ${userId} autenticado`);
      });

      // Entrar em sala de cidade/categoria (para parceiros)
      socket.on('join_city_category', (data: { cityId: number; categoryId: number }) => {
        const { cityId, categoryId } = data;
        socket.join(`city:${cityId}:category:${categoryId}`);
        console.log(`📍 Socket ${socket.id} entrou em city:${cityId}:category:${categoryId}`);
      });

      // Sair de sala
      socket.on('leave_city_category', (data: { cityId: number; categoryId: number }) => {
        const { cityId, categoryId } = data;
        socket.leave(`city:${cityId}:category:${categoryId}`);
      });

      // Desconexão
      socket.on('disconnect', () => {
        console.log(`❌ Cliente desconectado: ${socket.id}`);
      });
    });
  }

  /**
   * Configura listener do PostgreSQL para NOTIFY
   */
  private async setupPostgreSQLListener(): Promise<void> {
    try {
      // Obter cliente dedicado para LISTEN
      this.pgClient = await pool.connect();
      
      // Escutar canal de novas ordens de serviço
      await this.pgClient.query('LISTEN new_service_order');
      
      console.log('✅ PostgreSQL LISTEN configurado para new_service_order');

      // Handler de notificações
      this.pgClient.on('notification', async (msg: any) => {
        if (msg.channel === 'new_service_order') {
          await this.handleNewServiceOrderNotification(msg.payload);
        }
      });

      // Handler de erros
      this.pgClient.on('error', (err: Error) => {
        console.error('❌ Erro no cliente PostgreSQL:', err);
        // Tentar reconectar
        setTimeout(() => this.setupPostgreSQLListener(), 5000);
      });
    } catch (error) {
      console.error('❌ Erro ao configurar PostgreSQL LISTEN:', error);
      // Tentar reconectar após 5 segundos
      setTimeout(() => this.setupPostgreSQLListener(), 5000);
    }
  }

  /**
   * Processa notificação de nova ordem de serviço
   */
  private async handleNewServiceOrderNotification(payload: string): Promise<void> {
    try {
      const data = JSON.parse(payload);
      const { order_id, city_id, category } = data;

      console.log(`📢 Nova ordem de serviço: ${order_id} em city:${city_id}, category:${category}`);

      // Buscar parceiros na cidade com a categoria
      const partners = await query(
        `SELECT DISTINCT u.id, u.firebase_uid FROM users u
         INNER JOIN user_preferred_categories upc ON u.id = upc.user_id
         INNER JOIN categories c ON upc.category_id = c.id
         WHERE u.role = 'partner'
           AND u.current_city_id = $1
           AND c.name = $2
           AND u.verified = true`,
        [city_id, category]
      );

      // Emitir para parceiros conectados na sala
      this.io.to(`city:${city_id}:category:${category}`).emit('new_service_order', {
        orderId: order_id,
        cityId: city_id,
        category,
        timestamp: new Date().toISOString()
      });

      // Criar notificações no banco para parceiros
      for (const partner of partners.rows) {
        await this.notificationService.createNotification({
          user_id: partner.id,
          type: 'new_service_order_available',
          title: 'Nova Ordem de Serviço Disponível',
          message: `Uma nova ordem de serviço na categoria ${category} está disponível na sua cidade.`,
          data: {
            orderId: order_id,
            category,
            cityId: city_id
          }
        });

        // Enviar notificação em tempo real
        this.io.to(`user:${partner.id}`).emit('notification', {
          type: 'new_service_order_available',
          orderId: order_id,
          category,
          cityId: city_id
        });
      }
    } catch (error) {
      console.error('❌ Erro ao processar notificação:', error);
    }
  }

  /**
   * Emite evento para usuário específico
   */
  emitToUser(userId: string, event: string, data: any): void {
    this.io.to(`user:${userId}`).emit(event, data);
  }

  /**
   * Emite evento para todos na sala
   */
  emitToRoom(room: string, event: string, data: any): void {
    this.io.to(room).emit(event, data);
  }

  /**
   * Fecha conexões
   */
  async close(): Promise<void> {
    if (this.pgClient) {
      await this.pgClient.release();
    }
    this.io.close();
  }
}
