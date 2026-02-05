import express from 'express';
import { ServiceOrderRepository, PurchaseOrderRepository } from '../repositories/OrderRepository';

const router = express.Router();
const serviceOrderRepo = new ServiceOrderRepository();
const purchaseOrderRepo = new PurchaseOrderRepository();

// ========== SERVICE ORDERS ==========

/**
 * POST /api/orders/service
 * Cria nova ordem de serviço
 */
router.post('/service', async (req, res, next) => {
  try {
    const order = await serviceOrderRepo.create(req.body);
    res.status(201).json(order);
  } catch (error: any) {
    next(error);
  }
});

/**
 * GET /api/orders/service/city/:cityId/category/:category
 * Obtém ordens de serviço pendentes de uma cidade e categoria
 */
router.get('/service/city/:cityId/category/:category', async (req, res, next) => {
  try {
    const { cityId, category } = req.params;
    const { limit = 50 } = req.query;
    
    const orders = await serviceOrderRepo.findByCityAndCategory(
      parseInt(cityId),
      category,
      parseInt(limit as string)
    );
    
    res.json(orders);
  } catch (error: any) {
    next(error);
  }
});

/**
 * POST /api/orders/service/:orderId/accept
 * Aceita ordem de serviço
 */
router.post('/service/:orderId/accept', async (req, res, next) => {
  try {
    const { orderId } = req.params;
    const { partnerId } = req.body;
    
    const order = await serviceOrderRepo.acceptOrder(orderId, partnerId);
    res.json(order);
  } catch (error: any) {
    next(error);
  }
});

// ========== PURCHASE ORDERS ==========

/**
 * POST /api/orders/purchase
 * Cria novo pedido de produto
 */
router.post('/purchase', async (req, res, next) => {
  try {
    const order = await purchaseOrderRepo.create(req.body);
    res.status(201).json(order);
  } catch (error: any) {
    next(error);
  }
});

/**
 * GET /api/orders/purchase/:id
 * Obtém pedido por ID
 */
router.get('/purchase/:id', async (req, res, next) => {
  try {
    const { id } = req.params;
    const order = await purchaseOrderRepo.findById(id);
    
    if (!order) {
      return res.status(404).json({ error: 'Pedido não encontrado' });
    }
    
    res.json(order);
  } catch (error: any) {
    next(error);
  }
});

export default router;
