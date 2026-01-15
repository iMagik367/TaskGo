import * as admin from 'firebase-admin';
import * as functions from 'firebase-functions';

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
