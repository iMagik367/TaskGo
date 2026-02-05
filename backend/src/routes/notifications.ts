import express from 'express';
import { NotificationService } from '../services/NotificationService';
import { Server as SocketIOServer } from 'socket.io';

const router = express.Router();

// TODO: Injetar io do app
const notificationService = new NotificationService();

/**
 * GET /api/notifications/:userId
 * Obtém notificações do usuário
 */
router.get('/:userId', async (req, res, next) => {
  try {
    const { userId } = req.params;
    const { unread } = req.query;
    
    const notifications = unread === 'true'
      ? await notificationService.getUnreadNotifications(userId)
      : await notificationService.getUserNotifications(userId);
    
    res.json(notifications);
  } catch (error: any) {
    next(error);
  }
});

/**
 * PUT /api/notifications/:id/read
 * Marca notificação como lida
 */
router.put('/:id/read', async (req, res, next) => {
  try {
    const { id } = req.params;
    const { userId } = req.body;
    
    await notificationService.markAsRead(id, userId);
    res.json({ success: true });
  } catch (error: any) {
    next(error);
  }
});

export default router;
