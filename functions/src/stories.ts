import * as admin from 'firebase-admin';
import * as functions from 'firebase-functions';
import {AppError, handleError, assertAuthenticated} from './utils/errors';
import {validateAppCheck} from './security/appCheck';

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
      const db = admin.firestore();

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
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        expiresAt: expiresAtTimestamp,
        viewsCount: 0,
      };

      // Criar story no Firestore
      const storyRef = await db.collection('stories').add(storyData);
      const storyId = storyRef.id;

      functions.logger.info(`Story created: ${storyId}`, {
        storyId,
        userId,
        mediaType,
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
    const db = admin.firestore();
    const now = admin.firestore.Timestamp.now();
    const twentyFourHoursAgo = admin.firestore.Timestamp.fromMillis(
      now.toMillis() - 24 * 60 * 60 * 1000
    );

    try {
      functions.logger.info('Iniciando limpeza de stories expiradas...');
      
      // Buscar stories expiradas
      const expiredStoriesQuery = db.collection('stories')
        .where('expiresAt', '<=', twentyFourHoursAgo)
        .limit(500); // Processar em lotes de 500

      const snapshot = await expiredStoriesQuery.get();
      
      if (snapshot.empty) {
        functions.logger.info('Nenhuma story expirada encontrada.');
        return null;
      }

      const batch = db.batch();
      let deletedCount = 0;

      snapshot.docs.forEach((doc) => {
        batch.delete(doc.ref);
        deletedCount++;
      });

      await batch.commit();
      functions.logger.info(`Limpeza concluída: ${deletedCount} stories expiradas removidas.`);

      return { deletedCount };
    } catch (error) {
      functions.logger.error('Erro ao limpar stories expiradas:', error);
      throw error;
    }
  });
