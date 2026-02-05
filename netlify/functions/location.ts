import { Handler, HandlerEvent, HandlerContext } from '@netlify/functions';
import { LocationService } from '../../backend/src/services/LocationService';
import { LocationRepository } from '../../backend/src/repositories/LocationRepository';
import { UserRepository } from '../../backend/src/repositories/UserRepository';

const locationRepo = new LocationRepository();
const userRepo = new UserRepository();
const locationService = new LocationService(locationRepo, userRepo);

export const handler: Handler = async (event: HandlerEvent, context: HandlerContext) => {
  const headers = {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Headers': 'Content-Type, Authorization',
    'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
  };

  if (event.httpMethod === 'OPTIONS') {
    return { statusCode: 200, headers, body: '' };
  }

  try {
    const { httpMethod, body, queryStringParameters } = event;

    if (httpMethod === 'POST' && event.path.includes('/update')) {
      const { userId, latitude, longitude, accuracy } = JSON.parse(body || '{}');

      if (!userId || latitude === undefined || longitude === undefined) {
        return {
          statusCode: 400,
          headers,
          body: JSON.stringify({ error: 'userId, latitude e longitude são obrigatórios' }),
        };
      }

      const result = await locationService.updateUserLocation({
        userId,
        latitude: parseFloat(latitude),
        longitude: parseFloat(longitude),
        accuracy: accuracy ? parseFloat(accuracy) : undefined,
      });

      return { statusCode: 200, headers, body: JSON.stringify(result) };
    }

    if (httpMethod === 'GET' && event.path.includes('/nearest')) {
      const { latitude, longitude, radiusKm } = queryStringParameters || {};

      if (!latitude || !longitude) {
        return {
          statusCode: 400,
          headers,
          body: JSON.stringify({ error: 'latitude e longitude são obrigatórios' }),
        };
      }

      const city = await locationService.findNearestCity(
        parseFloat(latitude),
        parseFloat(longitude),
        radiusKm ? parseFloat(radiusKm) : 50
      );

      if (!city) {
        return {
          statusCode: 404,
          headers,
          body: JSON.stringify({ error: 'Nenhuma cidade encontrada' }),
        };
      }

      return { statusCode: 200, headers, body: JSON.stringify(city) };
    }

    return { statusCode: 404, headers, body: JSON.stringify({ error: 'Rota não encontrada' }) };
  } catch (error: any) {
    console.error('Erro:', error);
    return {
      statusCode: 500,
      headers,
      body: JSON.stringify({ error: error.message || 'Erro interno do servidor' }),
    };
  }
};
