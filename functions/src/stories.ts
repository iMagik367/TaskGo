import * as admin from 'firebase-admin';
import * as functions from 'firebase-functions';
import {AppError, handleError, assertAuthenticated} from './utils/errors';
import {validateAppCheck} from './security/appCheck';
import {getFirestore} from './utils/firestore';
import {getLocationCollection, getUserLocation, normalizeLocationId} from './utils/location';

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

      // CRÍTICO: Obter localização do usuário para organizar por região
      let storyCity = '';
      let storyState = '';
      
      // Tentar obter da localização fornecida primeiro
      if (location && typeof location === 'object') {
        storyCity = location.city || '';
        storyState = location.state || '';
      }
      
      // Se não tiver na localização, obter do perfil do usuário
      if (!storyCity || !storyState) {
        const userLocation = await getUserLocation(db, userId);
        storyCity = storyCity || userLocation.city;
        storyState = storyState || userLocation.state;
      }

      // 📍 LOCATION TRACE OBRIGATÓRIO - Rastreamento de localização
      const locationId = normalizeLocationId(storyCity || 'unknown', storyState || 'unknown');
      const firestorePath = `locations/${locationId}/stories`;
      
      functions.logger.info('📍 LOCATION TRACE', {
        function: 'createStory',
        userId,
        city: storyCity || 'unknown',
        state: storyState || 'unknown',
        locationId,
        firestorePath,
        rawCity: storyCity || '',
        rawState: storyState || '',
        timestamp: new Date().toISOString(),
      });

      if (!storyCity || !storyState) {
        functions.logger.warn(
          `User ${userId} does not have location information. ` +
          'Story will be saved in \'unknown\' location.'
        );
      }

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

      // Validar location se fornecido
      let locationData: Record<string, unknown> | null = null;
      if (location) {
        if (typeof location !== 'object') {
          throw new AppError('invalid-argument', 'location must be an object', 400);
        }
        locationData = {
          city: location.city || '',
          state: location.state || '',
          latitude: typeof location.latitude === 'number' ? location.latitude : 0,
          longitude: typeof location.longitude === 'number' ? location.longitude : 0,
        };
      }

      // Criar dados da story
      const storyData = {
        userId,
        userName,
        userAvatarUrl,
        mediaUrl: mediaUrl.trim(),
        mediaType: mediaType.trim(),
        caption: caption && typeof caption === 'string' ? caption.trim() : '',
        thumbnailUrl: thumbnailUrl && typeof thumbnailUrl === 'string' ? thumbnailUrl.trim() : null,
        location: locationData,
        city: storyCity || '', // Adicionar cidade explicitamente
        state: storyState || '', // Adicionar estado explicitamente
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        expiresAt: expiresAtTimestamp,
        viewsCount: 0,
      };

      // CRÍTICO: Salvar na coleção pública por localização
      const locationStoriesCollection = getLocationCollection(
        db,
        'stories',
        storyCity || 'unknown',
        storyState || 'unknown'
      );
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

      // Também salvar na coleção global para compatibilidade (será removido futuramente)
      await db.collection('stories').doc(storyId).set(storyData);

      functions.logger.info(`Story created: ${storyId}`, {
        storyId,
        userId,
        mediaType,
        location: `${storyCity || 'unknown'}, ${storyState || 'unknown'}`,
        locationCollection: `locations/${normalizeLocationId(storyCity || 'unknown', storyState || 'unknown')}/stories`,
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

      // Também limpar da coleção global (compatibilidade)
      const globalExpiredStoriesQuery = db.collection('stories')
        .where('expiresAt', '<=', twentyFourHoursAgo)
        .limit(500);

      const globalSnapshot = await globalExpiredStoriesQuery.get();
      if (!globalSnapshot.empty) {
        const batch = db.batch();
        globalSnapshot.docs.forEach((doc) => {
          batch.delete(doc.ref);
          totalDeletedCount++;
        });
        await batch.commit();
      }

      functions.logger.info(`Limpeza geral concluída: ${totalDeletedCount} stories expiradas removidas.`);

      return { deletedCount: totalDeletedCount };
    } catch (error) {
      functions.logger.error('Erro ao limpar stories expiradas:', error);
      throw error;
    }
  });
