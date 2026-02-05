import { LocationRepository } from '../repositories/LocationRepository';
import { UserRepository } from '../repositories/UserRepository';
import { City } from '../models/Location';

export interface LocationUpdateRequest {
  userId: string;
  latitude: number;
  longitude: number;
  accuracy?: number;
  timestamp?: Date;
}

export interface LocationUpdateResult {
  cityChanged: boolean;
  previousCityId?: number;
  currentCityId: number;
  city: City;
}

export class LocationService {
  constructor(
    private locationRepo: LocationRepository,
    private userRepo: UserRepository
  ) {}

  /**
   * Atualiza a localização do usuário baseado em GPS
   * Se a cidade mudou, atualiza o histórico e a localização atual
   */
  async updateUserLocation(
    request: LocationUpdateRequest
  ): Promise<LocationUpdateResult> {
    const { userId, latitude, longitude, accuracy } = request;

    // Buscar cidade mais próxima
    const nearestCity = await this.locationRepo.findNearestCity(
      latitude,
      longitude,
      50 // raio de 50km
    );

    if (!nearestCity) {
      throw new Error('Nenhuma cidade encontrada próxima à localização fornecida');
    }

    // Buscar usuário atual
    const user = await this.userRepo.findById(userId);
    if (!user) {
      throw new Error('Usuário não encontrado');
    }

    const previousCityId = user.current_city_id;
    const cityChanged = previousCityId !== nearestCity.id;

    // Se a cidade mudou, atualizar histórico e localização atual
    if (cityChanged) {
      // Marcar localização anterior como não atual
      if (previousCityId) {
        await this.userRepo.updateLocation(
          userId,
          latitude,
          longitude,
          nearestCity.id
        );
      } else {
        // Primeira localização - apenas atualizar
        await this.userRepo.updateLocation(
          userId,
          latitude,
          longitude,
          nearestCity.id
        );
      }
    } else {
      // Mesma cidade - apenas atualizar coordenadas
      await this.userRepo.update(
        userId,
        {
          current_latitude: latitude,
          current_longitude: longitude,
          last_location_update: new Date()
        }
      );
    }

    return {
      cityChanged,
      previousCityId: previousCityId || undefined,
      currentCityId: nearestCity.id,
      city: nearestCity
    };
  }

  /**
   * Busca cidade por nome e estado
   */
  async findCityByNameAndState(
    cityName: string,
    stateCode: string
  ): Promise<City | null> {
    return await this.locationRepo.findCityByNameAndState(cityName, stateCode);
  }

  /**
   * Busca cidade mais próxima das coordenadas
   */
  async findNearestCity(
    latitude: number,
    longitude: number,
    radiusKm: number = 50
  ): Promise<City | null> {
    return await this.locationRepo.findNearestCity(latitude, longitude, radiusKm);
  }

  /**
   * Obtém todas as cidades de um estado
   */
  async getCitiesByState(stateId: number): Promise<City[]> {
    return await this.locationRepo.getCitiesByState(stateId);
  }
}
