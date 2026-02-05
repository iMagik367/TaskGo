import * as admin from 'firebase-admin';
import * as functions from 'firebase-functions';
import {AppError, handleError, assertAuthenticated} from './utils/errors';
import {validateAppCheck} from './security/appCheck';
import {getFirestore} from './utils/firestore';
import {getUserLocation, validateCityAndState, normalizeLocationId} from './utils/location';
import {storiesPath, getUserLocationId, createStandardPayload} from './utils/firestorePaths';

/**
 * Cloud Function para criar uma nova story
 * Backend como autoridade - valida autenticação, autorização e dados
 */
export const createStory = functions.https.onCall(
  async (data, context) => {
    try {
      validateAppCheck(context);
      assertAuthenticated(context);

      const userId = context.auth!.uid;
      const db = getFirestore();

      // Validar dados de entrada
      const {
        mediaUrl,
        mediaType = 'image',
        caption,
        thumbnailUrl,
        location,
        expiresAt,
      } = data;

      if (!mediaUrl || typeof mediaUrl !== 'string' || mediaUrl.trim().length === 0) {
        throw new AppError('invalid-argument', 'mediaUrl is required and must be non-empty', 400);
      }

      if (!mediaType || typeof mediaType !== 'string') {
        throw new AppError('invalid-argument', 'mediaType must be a string', 400);
      }

      // Validar que o usuário existe
      const userDoc = await db.collection('users').doc(userId).get();
      if (!userDoc.exists) {
        throw new AppError('not-found', 'User not found', 404);
      }

      const userData = userDoc.data();
      const userName = userData?.name || userData?.displayName || 'Usuário';
      const userAvatarUrl = userData?.avatarUrl || userData?.photoURL || null;
      const userRole = userData?.role || 'user'; // Role do autor da story

      // CRÍTICO: Usar APENAS city/state do perfil do usuário (cadastro) - LEI MÁXIMA DO TASKGO
      // GPS (latitude/longitude) é usado APENAS para coordenadas no mapa, NÃO para determinar city/state
      let storyCity: string;
      let storyState: string;
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
        storyCity = validated.city!;
        storyState = validated.state!;
        locationId = normalizeLocationId(storyCity, storyState);
        
        functions.logger.info('📍 createStory: Usando city/state do perfil (enviado pelo frontend)', {
          userId,
          city: storyCity,
          state: storyState,
          locationId,
          latitude: data.latitude, // GPS apenas para coordenadas
          longitude: data.longitude, // GPS apenas para coordenadas
        });
      } else {
        // FALLBACK: Obter do perfil do usuário no Firestore (se frontend não enviou)
        functions.logger.warn('📍 createStory: Frontend não enviou city/state, obtendo do perfil do usuário', {userId});
        const userLocation = await getUserLocation(db, userId);
        storyCity = userLocation.city;
        storyState = userLocation.state;

        // CRÍTICO: Validar que city e state estão presentes e válidos
        if (!storyCity || !storyState || storyCity.trim() === '' || storyState.trim() === '') {
          const errorMsg = `User ${userId} does not have valid location information ` +
            `(city='${storyCity}', state='${storyState}'). ` +
            'Cannot create story without valid location. ' +
            'User must have city and state in their profile.';
          functions.logger.error(errorMsg);
          throw new functions.https.HttpsError('failed-precondition', errorMsg);
        }

        // Obter locationId
        locationId = await getUserLocationId(db, userId);
        
        functions.logger.info('📍 createStory: Usando city/state do perfil do Firestore', {
          userId,
          city: storyCity,
          state: storyState,
          locationId,
        });
      }
      const firestorePath = `locations/${locationId}/stories`;
      
      // 📍 LOCATION TRACE OBRIGATÓRIO - Rastreamento de localização
      functions.logger.info('📍 LOCATION TRACE', {
        function: 'createStory',
        userId,
        city: storyCity,
        state: storyState,
        locationId,
        firestorePath,
        source: 'users/{userId} root fields (city, state)',
        timestamp: new Date().toISOString(),
      });

      // Calcular expiresAt se não fornecido (24 horas a partir de agora)
      let expiresAtTimestamp: admin.firestore.Timestamp;
      if (expiresAt) {
        if (expiresAt instanceof admin.firestore.Timestamp) {
          expiresAtTimestamp = expiresAt;
        } else if (typeof expiresAt === 'number') {
          expiresAtTimestamp = admin.firestore.Timestamp.fromMillis(expiresAt);
        } else {
          throw new AppError('invalid-argument', 'expiresAt must be a Timestamp or number', 400);
        }
      } else {
        const now = admin.firestore.Timestamp.now();
        expiresAtTimestamp = admin.firestore.Timestamp.fromMillis(
          now.toMillis() + 24 * 60 * 60 * 1000
        );
      }

      // Validar location se fornecido (apenas para latitude/longitude, NÃO para city/state)
      // Lei 9.3: city e state NUNCA vêm do cliente, apenas de users/{userId}
      let locationData: Record<string, unknown> | null = null;
      if (location) {
        if (typeof location !== 'object') {
          throw new AppError('invalid-argument', 'location must be an object', 400);
        }
        // Usar apenas latitude e longitude do cliente (se fornecido)
        // city e state vêm EXCLUSIVAMENTE de users/{userId}
        locationData = {
          city: storyCity, // SEMPRE do users/{userId}
          state: storyState, // SEMPRE do users/{userId}
          latitude: typeof location.latitude === 'number' ? location.latitude : 0,
          longitude: typeof location.longitude === 'number' ? location.longitude : 0,
        };
      }

      // Criar dados da story usando payload padrão (stories não têm campo active)
      const storyData = createStandardPayload({
        userId,
        userName,
        userAvatarUrl,
        userRole: userRole, // CRÍTICO: Role do autor para filtrar stories de parceiros para clientes
        mediaUrl: mediaUrl.trim(),
        mediaType: mediaType.trim(),
        caption: caption && typeof caption === 'string' ? caption.trim() : '',
        thumbnailUrl: thumbnailUrl && typeof thumbnailUrl === 'string' ? thumbnailUrl.trim() : null,
        location: locationData,
        city: storyCity || '', // Adicionar cidade explicitamente
        state: storyState || '', // Adicionar estado explicitamente
        locationId: locationId, // CRÍTICO: Adicionar locationId para busca eficiente (SSR, etc)
        expiresAt: expiresAtTimestamp,
        viewsCount: 0,
      }, undefined); // Stories não têm campo active

      // Remover active do payload se foi adicionado (stories não têm active)
      delete (storyData as Record<string, unknown>).active;

      // CRÍTICO: Salvar APENAS na coleção pública por localização
      const locationStoriesCollection = storiesPath(db, locationId);
      const storyRef = await locationStoriesCollection.add(storyData);
      const storyId = storyRef.id;

      // 📍 PROOF: Logar path REAL onde o dado foi gravado
      functions.logger.info('📍 BACKEND WRITE PROOF', {
        function: 'createStory',
        storyId,
        actualFirestorePath: `locations/${locationId}/stories/${storyId}`,
        collectionId: locationStoriesCollection.id,
        documentId: storyId,
        timestamp: new Date().toISOString(),
      });

      functions.logger.info(`Story created: ${storyId}`, {
        storyId,
        userId,
        mediaType,
        location: `${storyCity}, ${storyState}`,
        locationCollection: `locations/${locationId}/stories`,
        timestamp: new Date().toISOString(),
      });

      return {
        success: true,
        storyId,
        message: 'Story created successfully',
      };
    } catch (error) {
      functions.logger.error('Error creating story:', error);
      throw handleError(error);
    }
  },
);

/**
 * Cloud Function para limpar stories expiradas automaticamente
 * Executa diariamente para remover stories com mais de 24 horas
 */
export const cleanupExpiredStories = functions.pubsub
  .schedule('every 24 hours')
  .timeZone('America/Sao_Paulo')
  .onRun(async (_context) => {
    const db = getFirestore();
    const now = admin.firestore.Timestamp.now();
    const twentyFourHoursAgo = admin.firestore.Timestamp.fromMillis(
      now.toMillis() - 24 * 60 * 60 * 1000
    );

    try {
      functions.logger.info('Iniciando limpeza de stories expiradas...');
      
      // CRÍTICO: Limpar stories de todas as localizações
      // Buscar todas as localizações primeiro
      const locationsSnapshot = await db.collection('locations').get();
      let totalDeletedCount = 0;

      if (locationsSnapshot.empty) {
        functions.logger.info('Nenhuma localização encontrada.');
        return { deletedCount: 0 };
      }

      // Processar cada localização
      for (const locationDoc of locationsSnapshot.docs) {
        const locationId = locationDoc.id;
        const storiesCollection = locationDoc.ref.collection('stories');
        
        // Buscar stories expiradas nesta localização
        const expiredStoriesQuery = storiesCollection
          .where('expiresAt', '<=', twentyFourHoursAgo)
          .limit(500); // Processar em lotes de 500

        const snapshot = await expiredStoriesQuery.get();
        
        if (!snapshot.empty) {
          const batch = db.batch();
          snapshot.docs.forEach((doc) => {
            batch.delete(doc.ref);
            totalDeletedCount++;
          });
          await batch.commit();
          functions.logger.info(
            `Limpeza concluída para ${locationId}: ` +
            `${snapshot.docs.length} stories expiradas removidas.`
          );
        }
      }

      // Limpeza concluída - não há mais coleção global

      functions.logger.info(`Limpeza geral concluída: ${totalDeletedCount} stories expiradas removidas.`);

      return { deletedCount: totalDeletedCount };
    } catch (error) {
      functions.logger.error('Erro ao limpar stories expiradas:', error);
      throw error;
    }
  });
