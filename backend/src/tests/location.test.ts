/**
 * Testes para LocationService
 * 
 * Para executar: npm test
 */

import { LocationService } from '../services/LocationService';
import { LocationRepository } from '../repositories/LocationRepository';
import { UserRepository } from '../repositories/UserRepository';

describe('LocationService', () => {
  let locationService: LocationService;
  let locationRepo: LocationRepository;
  let userRepo: UserRepository;

  beforeEach(() => {
    locationRepo = new LocationRepository();
    userRepo = new UserRepository();
    locationService = new LocationService(locationRepo, userRepo);
  });

  describe('updateUserLocation', () => {
    it('deve atualizar localização do usuário', async () => {
      const userId = 'test-user-id';
      const latitude = -23.5505;
      const longitude = -46.6333;

      // Mock: criar usuário de teste
      await userRepo.create({
        id: userId,
        firebase_uid: userId,
        email: 'test@example.com',
        role: 'client'
      });

      const result = await locationService.updateUserLocation({
        userId,
        latitude,
        longitude
      });

      expect(result).toBeDefined();
      expect(result.currentCityId).toBeDefined();
      expect(result.city).toBeDefined();
    });

    it('deve detectar mudança de cidade', async () => {
      const userId = 'test-user-id-2';
      
      // Criar usuário em São Paulo
      await userRepo.create({
        id: userId,
        firebase_uid: userId,
        email: 'test2@example.com',
        role: 'client'
      });

      // Atualizar para São Paulo
      const result1 = await locationService.updateUserLocation({
        userId,
        latitude: -23.5505,
        longitude: -46.6333
      });

      // Atualizar para Rio de Janeiro
      const result2 = await locationService.updateUserLocation({
        userId,
        latitude: -22.9068,
        longitude: -43.1729
      });

      expect(result2.cityChanged).toBe(true);
      expect(result2.previousCityId).toBe(result1.currentCityId);
    });
  });

  describe('findNearestCity', () => {
    it('deve encontrar cidade mais próxima', async () => {
      const city = await locationService.findNearestCity(-23.5505, -46.6333, 50);
      expect(city).toBeDefined();
      expect(city?.name).toBe('São Paulo');
    });
  });
});
