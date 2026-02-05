import express from 'express';
import { TrackingService } from '../services/TrackingService';
import { PurchaseOrderRepository } from '../repositories/OrderRepository';
import { NotificationService } from '../services/NotificationService';

const router = express.Router();
const orderRepo = new PurchaseOrderRepository();
const notificationService = new NotificationService();
const trackingService = new TrackingService(orderRepo, notificationService);

/**
 * POST /api/tracking/:orderId/event
 * Adiciona evento de rastreamento
 */
router.post('/:orderId/event', async (req, res, next) => {
  try {
    const { orderId } = req.params;
    const { status, description, location } = req.body;
    
    const event = await trackingService.addTrackingEvent(orderId, {
      status,
      description,
      location
    });
    
    res.json(event);
  } catch (error: any) {
    next(error);
  }
});

/**
 * GET /api/tracking/:orderId
 * Obtém histórico de rastreamento
 */
router.get('/:orderId', async (req, res, next) => {
  try {
    const { orderId } = req.params;
    const events = await trackingService.getTrackingHistory(orderId);
    res.json(events);
  } catch (error: any) {
    next(error);
  }
});

/**
 * POST /api/tracking/:orderId/confirm-delivery
 * Confirma entrega
 */
router.post('/:orderId/confirm-delivery', async (req, res, next) => {
  try {
    const { orderId } = req.params;
    const { confirmedBy } = req.body; // 'client' ou 'seller'
    
    if (confirmedBy === 'client') {
      await trackingService.confirmDeliveryByClient(orderId);
    } else if (confirmedBy === 'seller') {
      await trackingService.confirmDeliveryBySeller(orderId);
    } else {
      return res.status(400).json({ error: 'confirmedBy deve ser "client" ou "seller"' });
    }
    
    res.json({ success: true });
  } catch (error: any) {
    next(error);
  }
});

export default router;
