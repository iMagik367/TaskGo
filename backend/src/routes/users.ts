import express from 'express';
import { UserRepository } from '../repositories/UserRepository';

const router = express.Router();
const userRepo = new UserRepository();

/**
 * GET /api/users/:id
 * Obtém usuário por ID
 */
router.get('/:id', async (req, res, next) => {
  try {
    const { id } = req.params;
    const user = await userRepo.findById(id);
    
    if (!user) {
      return res.status(404).json({ error: 'Usuário não encontrado' });
    }
    
    res.json(user);
  } catch (error: any) {
    next(error);
  }
});

/**
 * GET /api/users/firebase/:firebaseUid
 * Obtém usuário por Firebase UID
 */
router.get('/firebase/:firebaseUid', async (req, res, next) => {
  try {
    const { firebaseUid } = req.params;
    const user = await userRepo.findByFirebaseUid(firebaseUid);
    
    if (!user) {
      return res.status(404).json({ error: 'Usuário não encontrado' });
    }
    
    res.json(user);
  } catch (error: any) {
    next(error);
  }
});

/**
 * POST /api/users
 * Cria novo usuário
 */
router.post('/', async (req, res, next) => {
  try {
    const user = await userRepo.create(req.body);
    res.status(201).json(user);
  } catch (error: any) {
    next(error);
  }
});

/**
 * PUT /api/users/:id
 * Atualiza usuário
 */
router.put('/:id', async (req, res, next) => {
  try {
    const { id } = req.params;
    const user = await userRepo.update(id, req.body);
    res.json(user);
  } catch (error: any) {
    next(error);
  }
});

/**
 * GET /api/users/:id/settings
 * Obtém configurações do usuário
 */
router.get('/:id/settings', async (req, res, next) => {
  try {
    const { id } = req.params;
    const settings = await userRepo.getSettings(id);
    
    if (!settings) {
      // Criar configurações padrão
      const defaultSettings = await userRepo.updateSettings(id, {});
      return res.json(defaultSettings);
    }
    
    res.json(settings);
  } catch (error: any) {
    next(error);
  }
});

/**
 * PUT /api/users/:id/settings
 * Atualiza configurações do usuário
 */
router.put('/:id/settings', async (req, res, next) => {
  try {
    const { id } = req.params;
    const settings = await userRepo.updateSettings(id, req.body);
    res.json(settings);
  } catch (error: any) {
    next(error);
  }
});

export default router;
