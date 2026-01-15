import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';

/**
 * Migra usuários existentes de provider/seller para partner (Parceiro)
 * Esta função pode ser executada on-demand ou via HTTP trigger
 * 
 * Migrações realizadas:
 * - role: "provider" → "partner"
 * - role: "seller" → "partner"
 * - Preserva preferredCategories existentes
 * - Atualiza accountType no Firestore (se existir campo separado)
 */
export const migrateToPartner = functions.https.onCall(async (data, context) => {
  // Verificar se é admin ou autenticado (ajustar conforme necessário)
  if (!context.auth) {
    throw new functions.https.HttpsError(
      'unauthenticated',
      'Usuário não autenticado'
    );
  }

  const batchSize = data.batchSize || 100; // Processar em lotes
  const dryRun = data.dryRun !== false; // Por padrão, apenas simular (dry run)

  const db = admin.firestore();
  const usersCollection = db.collection('users');

  try {
    let migratedCount = 0;
    const errorCount = 0; // Nunca incrementado, mantido para compatibilidade
    const errors: string[] = [];

    // Migrar usuários com role "provider"
    functions.logger.info('Iniciando migração de providers para partners...');
    let providerSnapshot = await usersCollection
      .where('role', '==', 'provider')
      .limit(batchSize)
      .get();

    while (!providerSnapshot.empty) {
      const batch = db.batch();
      let hasUpdates = false;
      
      providerSnapshot.forEach((doc) => {
        const userData = doc.data();
        if (dryRun) {
          functions.logger.info(`[DRY RUN] Migraria usuário ${doc.id} de provider para partner`);
          migratedCount++;
        } else {
          batch.update(doc.ref, {
            role: 'partner',
            updatedAt: admin.firestore.FieldValue.serverTimestamp(),
            // Preservar preferredCategories se existir
            ...(userData.preferredCategories && {
              preferredCategories: userData.preferredCategories
            })
          });
          hasUpdates = true;
          migratedCount++;
        }
      });

      if (!dryRun && hasUpdates) {
        await batch.commit();
        functions.logger.info(`Migrados ${providerSnapshot.size} providers para partners`);
      }

      // Se há mais documentos, continuar
      if (providerSnapshot.size === batchSize) {
        const lastDoc = providerSnapshot.docs[providerSnapshot.docs.length - 1];
        providerSnapshot = await usersCollection
          .where('role', '==', 'provider')
          .startAfter(lastDoc)
          .limit(batchSize)
          .get();
      } else {
        break;
      }
    }

    // Migrar usuários com role "seller"
    functions.logger.info('Iniciando migração de sellers para partners...');
    let sellerSnapshot = await usersCollection
      .where('role', '==', 'seller')
      .limit(batchSize)
      .get();

    while (!sellerSnapshot.empty) {
      const batch = db.batch();
      let hasUpdates = false;
      
      sellerSnapshot.forEach((doc) => {
        const userData = doc.data();
        if (dryRun) {
          functions.logger.info(`[DRY RUN] Migraria usuário ${doc.id} de seller para partner`);
          migratedCount++;
        } else {
          batch.update(doc.ref, {
            role: 'partner',
            updatedAt: admin.firestore.FieldValue.serverTimestamp(),
            // Preservar preferredCategories se existir, ou criar lista vazia se não existir
            preferredCategories: userData.preferredCategories || []
          });
          hasUpdates = true;
          migratedCount++;
        }
      });

      if (!dryRun && hasUpdates) {
        await batch.commit();
        functions.logger.info(`Migrados ${sellerSnapshot.size} sellers para partners`);
      }

      // Se há mais documentos, continuar
      if (sellerSnapshot.size === batchSize) {
        const lastDoc = sellerSnapshot.docs[sellerSnapshot.docs.length - 1];
        sellerSnapshot = await usersCollection
          .where('role', '==', 'seller')
          .startAfter(lastDoc)
          .limit(batchSize)
          .get();
      } else {
        break;
      }
    }

    const result = {
      success: true,
      dryRun,
      migratedCount,
      errorCount,
      errors: errors.slice(0, 10), // Limitar erros retornados
      message: dryRun 
        ? `[DRY RUN] Simulação completa. ${migratedCount} usuários seriam migrados.`
        : `Migração completa. ${migratedCount} usuários migrados para partner.`
    };

    functions.logger.info('Migração concluída', result);
    return result;

  } catch (error: unknown) {
    const errorMessage = error instanceof Error ? error.message : 'Erro desconhecido';
    functions.logger.error('Erro durante migração:', error);
    throw new functions.https.HttpsError(
      'internal',
      `Erro ao migrar usuários: ${errorMessage}`
    );
  }
});

/**
 * Função HTTP alternativa para execução via URL (útil para cron jobs ou webhooks)
 */
export const migrateToPartnerHttp = functions.https.onRequest(async (req, res) => {
  try {
    const dryRun = req.query.dryRun !== 'false'; // Por padrão, dry run
    const batchSize = parseInt(req.query.batchSize as string) || 100;

    const db = admin.firestore();
    const usersCollection = db.collection('users');

    let migratedCount = 0;
    const errors: string[] = [];

    // Migrar providers
    let providerSnapshot = await usersCollection
      .where('role', '==', 'provider')
      .limit(batchSize)
      .get();

    while (!providerSnapshot.empty) {
      const batch = db.batch();
      let hasUpdates = false;
      
      providerSnapshot.forEach((doc) => {
        const userData = doc.data();
        if (dryRun) {
          migratedCount++;
        } else {
          batch.update(doc.ref, {
            role: 'partner',
            updatedAt: admin.firestore.FieldValue.serverTimestamp(),
            // Preservar preferredCategories se existir
            ...(userData.preferredCategories && {
              preferredCategories: userData.preferredCategories
            })
          });
          hasUpdates = true;
          migratedCount++;
        }
      });

      if (!dryRun && hasUpdates) {
        await batch.commit();
      }

      if (providerSnapshot.size === batchSize) {
        const lastDoc = providerSnapshot.docs[providerSnapshot.docs.length - 1];
        providerSnapshot = await usersCollection
          .where('role', '==', 'provider')
          .startAfter(lastDoc)
          .limit(batchSize)
          .get();
      } else {
        break;
      }
    }

    // Migrar sellers
    let sellerSnapshot = await usersCollection
      .where('role', '==', 'seller')
      .limit(batchSize)
      .get();

    while (!sellerSnapshot.empty) {
      const batch = db.batch();
      let hasUpdates = false;
      
      sellerSnapshot.forEach((doc) => {
        const userData = doc.data();
        if (dryRun) {
          migratedCount++;
        } else {
          batch.update(doc.ref, {
            role: 'partner',
            updatedAt: admin.firestore.FieldValue.serverTimestamp(),
            preferredCategories: userData.preferredCategories || []
          });
          hasUpdates = true;
          migratedCount++;
        }
      });

      if (!dryRun && hasUpdates) {
        await batch.commit();
      }

      if (sellerSnapshot.size === batchSize) {
        const lastDoc = sellerSnapshot.docs[sellerSnapshot.docs.length - 1];
        sellerSnapshot = await usersCollection
          .where('role', '==', 'seller')
          .startAfter(lastDoc)
          .limit(batchSize)
          .get();
      } else {
        break;
      }
    }

    res.json({
      success: true,
      dryRun,
      migratedCount,
      errors: errors.slice(0, 10),
      message: dryRun 
        ? `[DRY RUN] ${migratedCount} usuários seriam migrados.`
        : `${migratedCount} usuários migrados para partner.`
    });
  } catch (error: unknown) {
    const errorMessage = error instanceof Error ? error.message : 'Erro desconhecido';
    functions.logger.error('Erro durante migração HTTP:', error);
    res.status(500).json({
      success: false,
      error: errorMessage
    });
  }
});
