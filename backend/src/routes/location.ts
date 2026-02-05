import express from 'express';
import { LocationService } from '../services/LocationService';
import { LocationRepository } from '../repositories/LocationRepository';
import { UserRepository } from '../repositories/UserRepository';

const router = express.Router();
const locationRepo = new LocationRepository();
const userRepo = new UserRepository();
const locationService = new LocationService(locationRepo, userRepo);

/**
 * POST /api/location/update
 * Atualiza localização do usuário via GPS
 */
router.post('/update', async (req, res, next) => {
  try {
    const { userId, latitude, longitude, accuracy } = req.body;

    if (!userId || latitude === undefined || longitude === undefined) {
      return res.status(400).json({
        error: 'userId, latitude e longitude são obrigatórios'
      });
    }

    const result = await locationService.updateUserLocation({
      userId,
      latitude: parseFloat(latitude),
      longitude: parseFloat(longitude),
      accuracy: accuracy ? parseFloat(accuracy) : undefined
    });

    res.json({
      success: true,
      cityChanged: result.cityChanged,
      previousCityId: result.previousCityId,
      currentCityId: result.currentCityId,
      city: result.city
    });
  } catch (error: any) {
    next(error);
  }
});

/**
 * GET /api/location/cities/:stateId
 * Obtém cidades de um estado
 */
router.get('/cities/:stateId', async (req, res, next) => {
  try {
    const { stateId } = req.params;
    const cities = await locationService.getCitiesByState(parseInt(stateId));
    res.json(cities);
  } catch (error: any) {
    next(error);
  }
});

/**
 * GET /api/location/nearest
 * Busca cidade mais próxima das coordenadas
 */
router.get('/nearest', async (req, res, next) => {
  try {
    const { latitude, longitude, radiusKm } = req.query;

    if (!latitude || !longitude) {
      return res.status(400).json({
        error: 'latitude e longitude são obrigatórios'
      });
    }

    const city = await locationService.findNearestCity(
      parseFloat(latitude as string),
      parseFloat(longitude as string),
      radiusKm ? parseFloat(radiusKm as string) : 50
    );

    if (!city) {
      return res.status(404).json({
        error: 'Nenhuma cidade encontrada próxima à localização fornecida'
      });
    }

    res.json(city);
  } catch (error: any) {
    next(error);
  }
});

export default router;
