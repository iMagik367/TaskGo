import {getFirestore} from '../utils/firestore';
import * as functions from 'firebase-functions';
import {AppError, handleError, assertAuthenticated} from '../utils/errors';
import {validateAppCheck} from '../security/appCheck';
import {COLLECTIONS} from '../utils/constants';
import {getUserLocation, validateCityAndState, normalizeLocationId} from '../utils/location';
import {servicesPath, getUserLocationId, createStandardPayload, createUpdatePayload} from '../utils/firestorePaths';

/**
 * Cria um novo serviço
 * Apenas usuários com role "partner" podem criar serviços
 * Cloud Function é a autoridade - valida permissões e dados
 */
export const createService = functions.https.onCall(
  async (data, context) => {
    try {
      // Validar App Check
      validateAppCheck(context);

      // Verificar autenticação
      assertAuthenticated(context);

      const userId = context.auth!.uid;
      const db = getFirestore();

      // REMOVIDO: Parceiros não podem mais criar serviços individuais
      // Parceiros apenas definem preferredCategories no perfil
      // Esta função está desabilitada para parceiros
      throw new AppError(
        'permission-denied',
        'Partners can no longer create individual services. ' +
        'Please define your service categories in your profile (preferredCategories).',
        403,
      );

      // Validar dados de entrada
      const {
        title,
        description,
        category,
        price,
        latitude,
        longitude,
        active = true,
      } = data;

      if (!title || typeof title !== 'string' || title.trim().length === 0) {
        throw new AppError('invalid-argument', 'title is required and must be non-empty', 400);
      }

      if (!description || typeof description !== 'string' || description.trim().length === 0) {
        throw new AppError('invalid-argument', 'description is required and must be non-empty', 400);
      }

      if (!category || typeof category !== 'string' || category.trim().length === 0) {
        throw new AppError('invalid-argument', 'category is required and must be non-empty', 400);
      }

      if (price !== undefined && (typeof price !== 'number' || price < 0)) {
        throw new AppError('invalid-argument', 'price must be a non-negative number', 400);
      }

      // Validar que o usuário existe e tem permissão
      const userDoc = await db.collection(COLLECTIONS.USERS).doc(userId).get();
      if (!userDoc.exists) {
        throw new AppError('not-found', 'User not found', 404);
      }

      // Código removido - função desabilitada para parceiros

      // CRÍTICO: Usar APENAS city/state do perfil do usuário (cadastro) - LEI MÁXIMA DO TASKGO
      // GPS (latitude/longitude) é usado APENAS para coordenadas no mapa, NÃO para determinar city/state
      let city: string;
      let state: string;
      let locationId: string;
      
      // PRIORIDADE 1: Usar city/state enviados pelo frontend (vêm do perfil do usuário)
      if (data.city && data.state) {
        const validated = validateCityAndState(data.city, data.state);
        if (!validated.valid) {
          throw new functions.https.HttpsError(
            'invalid-argument',
            `Invalid location data: ${validated.error}`,
          );
        }
        city = validated.city!;
        state = validated.state!;
        locationId = normalizeLocationId(city, state);
        
        functions.logger.info('📍 createService: Usando city/state do perfil (enviado pelo frontend)', {
          userId,
          city,
          state,
          locationId,
          latitude: data.latitude, // GPS apenas para coordenadas
          longitude: data.longitude, // GPS apenas para coordenadas
        });
      } else {
        // FALLBACK: Obter do perfil do usuário no Firestore (se frontend não enviou)
        functions.logger.warn(
          '📍 createService: Frontend não enviou city/state, obtendo do perfil',
          {userId}
        );
        const userLocation = await getUserLocation(db, userId);
        city = userLocation.city;
        state = userLocation.state;
        
        if (!city || !state) {
          throw new functions.https.HttpsError(
            'failed-precondition',
            'Location not available. User must have city and state in their profile.',
          );
        }
        
        locationId = await getUserLocationId(db, userId);
        
        functions.logger.info(
          '📍 createService: Usando city/state do perfil do Firestore',
          {
          userId,
          city,
          state,
          locationId,
        });
      }

      // 📍 LOCATION TRACE OBRIGATÓRIO - Rastreamento de localização
      const firestorePath = `locations/${locationId}/services`;
      
      functions.logger.info('📍 LOCATION TRACE', {
        function: 'createService',
        userId,
        city: city,
        state: state,
        locationId,
        firestorePath,
        source: 'users/{userId} root fields (city, state)',
        timestamp: new Date().toISOString(),
      });

      // CRÍTICO: Validar que city e state estão presentes e válidos
      if (!city || !state || city.trim() === '' || state.trim() === '') {
        const errorMsg = `User ${userId} does not have valid location information ` +
          `(city='${city}', state='${state}'). ` +
          'Cannot create service without valid location.';
        functions.logger.error(errorMsg);
        throw new functions.https.HttpsError('failed-precondition', errorMsg);
      }

      // Criar dados do serviço usando payload padrão
      const serviceData = createStandardPayload({
        providerId: userId,
        title: title.trim(),
        description: description.trim(),
        category: category.trim(),
        price: price || null,
        latitude: latitude || null,
        longitude: longitude || null,
        city: city || '', // Adicionar cidade explicitamente
        state: state || '', // Adicionar estado explicitamente
        locationId: locationId, // CRÍTICO: Adicionar locationId para busca eficiente (SSR, reviews, etc)
      }, active === true);

      // CRÍTICO: Salvar APENAS na coleção pública por localização
      const locationServicesCollection = servicesPath(db, locationId);
      const serviceRef = await locationServicesCollection.add(serviceData);
      const serviceId = serviceRef.id;

      // 📍 PROOF: Logar path REAL onde o dado foi gravado
      functions.logger.info('📍 BACKEND WRITE PROOF', {
        function: 'createService',
        serviceId,
        actualFirestorePath: `locations/${locationId}/services/${serviceId}`,
        collectionId: locationServicesCollection.id,
        documentId: serviceId,
        timestamp: new Date().toISOString(),
      });

      functions.logger.info(`Service created: ${serviceId}`, {
        serviceId,
        providerId: userId,
        category,
        location: `${city}, ${state}`,
        locationCollection: `locations/${locationId}/services`,
        timestamp: new Date().toISOString(),
      });

      return {
        success: true,
        serviceId,
        message: 'Service created successfully',
      };
    } catch (error) {
      functions.logger.error('Error creating service:', error);
      throw handleError(error);
    }
  },
);

/**
 * Atualiza um serviço existente
 * Apenas o dono do serviço pode atualizar
 */
export const updateService = functions.https.onCall(
  async (data, context) => {
    try {
      validateAppCheck(context);
      assertAuthenticated(context);

      const userId = context.auth!.uid;
      const db = getFirestore();
      const {serviceId, updates} = data;

      if (!serviceId || typeof serviceId !== 'string') {
        throw new AppError('invalid-argument', 'serviceId is required', 400);
      }

      if (!updates || typeof updates !== 'object') {
        throw new AppError('invalid-argument', 'updates is required and must be an object', 400);
      }

      // Buscar serviço - precisa procurar em todas as localizações
      // Obter locationId do usuário para buscar no path correto
      const locationId = await getUserLocationId(db, userId);
      
      const locationServicesCollection = servicesPath(db, locationId);
      const serviceDoc = await locationServicesCollection.doc(serviceId).get();
      
      if (!serviceDoc.exists) {
        throw new AppError('not-found', 'Service not found', 404);
      }

      const serviceData = serviceDoc.data();

      // Verificar propriedade
      if (serviceData?.providerId !== userId) {
        throw new AppError('permission-denied', 'Only service owner can update service', 403);
      }

      // Validar campos permitidos para atualização
      const allowedFields = ['title', 'description', 'category', 'price', 'latitude', 'longitude', 'active'];
      const updateDataRaw: Record<string, unknown> = {};

      for (const field of allowedFields) {
        if (updates[field] !== undefined) {
          // Validações específicas por campo
          if (field === 'title' && (typeof updates[field] !== 'string' || updates[field].trim().length === 0)) {
            throw new AppError('invalid-argument', 'title must be a non-empty string', 400);
          }
          if (field === 'description' && (typeof updates[field] !== 'string' || updates[field].trim().length === 0)) {
            throw new AppError('invalid-argument', 'description must be a non-empty string', 400);
          }
          if (field === 'category' && (typeof updates[field] !== 'string' || updates[field].trim().length === 0)) {
            throw new AppError('invalid-argument', 'category must be a non-empty string', 400);
          }
          if (field === 'price' && 
              updates[field] !== null && 
              (typeof updates[field] !== 'number' || updates[field] < 0)) {
            throw new AppError(
              'invalid-argument',
              'price must be a non-negative number or null',
              400
            );
          }
          if (field === 'active' && typeof updates[field] !== 'boolean') {
            throw new AppError('invalid-argument', 'active must be a boolean', 400);
          }

          updateDataRaw[field] = updates[field];
        }
      }

      // Criar payload de atualização padrão
      const updateData = createUpdatePayload(updateDataRaw);

      // Atualizar APENAS na coleção pública por localização
      await locationServicesCollection.doc(serviceId).update(updateData);

      functions.logger.info(`Service updated: ${serviceId}`, {
        serviceId,
        providerId: userId,
        updatedFields: Object.keys(updateData),
        timestamp: new Date().toISOString(),
      });

      return {
        success: true,
        message: 'Service updated successfully',
      };
    } catch (error) {
      functions.logger.error('Error updating service:', error);
      throw handleError(error);
    }
  },
);

/**
 * Deleta um serviço
 * Apenas o dono do serviço pode deletar
 */
export const deleteService = functions.https.onCall(
  async (data, context) => {
    try {
      validateAppCheck(context);
      assertAuthenticated(context);

      const userId = context.auth!.uid;
      const db = getFirestore();
      const {serviceId} = data;

      if (!serviceId || typeof serviceId !== 'string') {
        throw new AppError('invalid-argument', 'serviceId is required', 400);
      }

      // Buscar serviço - precisa procurar em todas as localizações
      // Obter locationId do usuário para buscar no path correto
      const locationId = await getUserLocationId(db, userId);
      
      const locationServicesCollection = servicesPath(db, locationId);
      const serviceDoc = await locationServicesCollection.doc(serviceId).get();
      
      if (!serviceDoc.exists) {
        throw new AppError('not-found', 'Service not found', 404);
      }

      const serviceData = serviceDoc.data();

      // Verificar propriedade
      if (serviceData?.providerId !== userId) {
        throw new AppError('permission-denied', 'Only service owner can delete service', 403);
      }

      // Deletar APENAS da coleção pública por localização
      await locationServicesCollection.doc(serviceId).delete();

      functions.logger.info(`Service deleted: ${serviceId}`, {
        serviceId,
        providerId: userId,
        timestamp: new Date().toISOString(),
      });

      return {
        success: true,
        message: 'Service deleted successfully',
      };
    } catch (error) {
      functions.logger.error('Error deleting service:', error);
      throw handleError(error);
    }
  },
);
