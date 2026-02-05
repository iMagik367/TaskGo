import express from 'express';
import { ProductRepository } from '../repositories/ProductRepository';

const router = express.Router();
const productRepo = new ProductRepository();

/**
 * GET /api/products/city/:cityId
 * Obtém produtos de uma cidade
 */
router.get('/city/:cityId', async (req, res, next) => {
  try {
    const { cityId } = req.params;
    const { limit = 50, offset = 0 } = req.query;
    
    const products = await productRepo.findByCity(
      parseInt(cityId),
      parseInt(limit as string),
      parseInt(offset as string)
    );
    
    res.json(products);
  } catch (error: any) {
    next(error);
  }
});

/**
 * GET /api/products/seller/:sellerId
 * Obtém produtos de um vendedor
 */
router.get('/seller/:sellerId', async (req, res, next) => {
  try {
    const { sellerId } = req.params;
    const products = await productRepo.findBySeller(sellerId);
    res.json(products);
  } catch (error: any) {
    next(error);
  }
});

/**
 * POST /api/products
 * Cria novo produto
 */
router.post('/', async (req, res, next) => {
  try {
    const product = await productRepo.create(req.body);
    res.status(201).json(product);
  } catch (error: any) {
    next(error);
  }
});

/**
 * PUT /api/products/:id
 * Atualiza produto
 */
router.put('/:id', async (req, res, next) => {
  try {
    const { id } = req.params;
    const product = await productRepo.update(id, req.body);
    res.json(product);
  } catch (error: any) {
    next(error);
  }
});

export default router;
