import * as admin from 'firebase-admin';
import {getFirestore} from '../utils/firestore';
import * as functions from 'firebase-functions';
import {validateAppCheck} from '../security/appCheck';

/**
 * Script de migração: Atualiza Custom Claims de todos os usuários existentes
 * 
 * IMPORTANTE:
 * - Executar uma vez para migrar usuários existentes
 * - Este script deve ser executado manualmente via Cloud Function ou script Node.js
 * - Não executar em produção sem testar primeiro
 * 
 * Como usar:
 * 1. Deploy esta função como callable
 * 2. Chamar via Firebase Console ou Admin SDK
 * 3. OU executar via script Node.js local
 */

/**
 * Cloud Function para migrar usuários existentes
 * SOMENTE ADMINS podem executar
 */
export const migrateExistingUsersToCustomClaims = functions.https.onCall(
  async (data, context) => {
    try {
      validateAppCheck(context);
      
      // Verificar se é admin (através de Custom Claims ou documento)
      if (!context.auth) {
        throw new functions.https.HttpsError(
          'unauthenticated',
          'User must be authenticated',
        );
      }

      const db = getFirestore();
      const adminDoc = await db.collection('users').doc(context.auth.uid).get();
      const adminData = adminDoc.data();

      // Verificar se é admin (fallback para documento se não houver Custom Claims)
      const isAdminFromDoc = adminData?.role === 'admin';
      const isAdminFromClaims = context.auth.token.role === 'admin';

      if (!isAdminFromDoc && !isAdminFromClaims) {
        throw new functions.https.HttpsError(
          'permission-denied',
          'Admin access required',
        );
      }

      const {batchSize = 100, dryRun = false} = data;

      functions.logger.info('Starting migration of existing users to Custom Claims', {
        adminId: context.auth.uid,
        batchSize,
        dryRun,
        timestamp: new Date().toISOString(),
      });

      // Listar todos os usuários (em batches)
      let nextPageToken: string | undefined;
      let totalProcessed = 0;
      let totalUpdated = 0;
      let totalSkipped = 0;
      let totalErrors = 0;

      do {
        const listUsersResult = await admin.auth().listUsers(batchSize, nextPageToken);
        nextPageToken = listUsersResult.pageToken;

        // Processar batch
        for (const userRecord of listUsersResult.users) {
          try {
            totalProcessed++;

            // Verificar se já tem Custom Claims
            const existingClaims = userRecord.customClaims || {};
            const existingRole = existingClaims.role;

            // Se já tem role válido em Custom Claims, pular
            const validRolesList = ['user', 'admin', 'moderator', 'provider', 'seller', 'partner'];
            if (existingRole && validRolesList.includes(existingRole)) {
              totalSkipped++;
              continue;
            }

            // Buscar role no documento Firestore
            const userDoc = await db.collection('users').doc(userRecord.uid).get();
            const userData = userDoc.data();
            const firestoreRole = userData?.role;

            // Mapear role do Firestore para Custom Claims
            let customClaimRole = 'user'; // Default

            if (firestoreRole) {
              // Mapear roles legados
              if (firestoreRole === 'client') {
                customClaimRole = 'user';
              } else if (['admin', 'moderator'].includes(firestoreRole)) {
                customClaimRole = firestoreRole;
              } else if (['provider', 'seller', 'partner'].includes(firestoreRole)) {
                // Manter roles legados para compatibilidade
                customClaimRole = firestoreRole;
              } else {
                // Role desconhecido, usar default
                customClaimRole = 'user';
              }
            }

            // Atualizar Custom Claims (se não for dry run)
            if (!dryRun) {
              await admin.auth().setCustomUserClaims(userRecord.uid, {
                ...existingClaims,
                role: customClaimRole,
              });

              // Sincronizar role no documento Firestore (se diferente)
              if (firestoreRole !== customClaimRole && firestoreRole !== 'client') {
                await db.collection('users').doc(userRecord.uid).update({
                  role: customClaimRole,
                  roleMigratedAt: admin.firestore.FieldValue.serverTimestamp(),
                  updatedAt: admin.firestore.FieldValue.serverTimestamp(),
                });
              }
            }

            totalUpdated++;

            functions.logger.info(`User migrated: ${userRecord.uid}`, {
              uid: userRecord.uid,
              email: userRecord.email,
              oldRole: firestoreRole || 'none',
              newRole: customClaimRole,
              dryRun,
            });
          } catch (error) {
            totalErrors++;
            functions.logger.error(`Error migrating user ${userRecord.uid}:`, error);
          }
        }
      } while (nextPageToken);

      functions.logger.info('Migration completed', {
        totalProcessed,
        totalUpdated,
        totalSkipped,
        totalErrors,
        dryRun,
        timestamp: new Date().toISOString(),
      });

      return {
        success: true,
        summary: {
          totalProcessed,
          totalUpdated,
          totalSkipped,
          totalErrors,
          dryRun,
        },
        message: dryRun
          ? `Dry run completed. Would update ${totalUpdated} users.`
          : `Migration completed. Updated ${totalUpdated} users.`,
      };
    } catch (error) {
      functions.logger.error('Error in migration:', error);
      throw new functions.https.HttpsError(
        'internal',
        error instanceof Error ? error.message : 'Migration failed',
      );
    }
  },
);

/**
 * Script Node.js para executar migração localmente
 * 
 * Como executar:
 * 1. cd functions
 * 2. npm run build
 * 3. node -e "require('./lib/scripts/migrateExistingUsers').migrateLocal()"
 * 
 * OU criar arquivo separado e executar:
 * node scripts/run-migration.js
 */
export async function migrateLocal() {
  try {
    // Verificar se já está inicializado (pode estar em contexto de Cloud Function)
    try {
      admin.firestore();
    } catch {
      admin.initializeApp();
    }

    const db = getFirestore();

    const batchSize = 100;
    let nextPageToken: string | undefined;
    let totalProcessed = 0;
    let totalUpdated = 0;
    let totalSkipped = 0;
    let totalErrors = 0;

    console.log('🚀 Iniciando migração local de Custom Claims...');
    console.log('');

    do {
      const listUsersResult = await admin.auth().listUsers(batchSize, nextPageToken);
      nextPageToken = listUsersResult.pageToken;

      for (const userRecord of listUsersResult.users) {
        try {
          totalProcessed++;

          const existingClaims = userRecord.customClaims || {};
          const existingRole = existingClaims.role;

          // Se já tem role válido, pular
          const validRoles = ['user', 'admin', 'moderator', 'provider', 'seller', 'partner'];
          if (existingRole && validRoles.includes(existingRole)) {
            totalSkipped++;
            console.log(`⊘ ${userRecord.email || userRecord.uid}: já tem role=${existingRole}`);
            continue;
          }

          // Buscar role no Firestore
          const userDoc = await db.collection('users').doc(userRecord.uid).get();
          const userData = userDoc.data();
          const firestoreRole = userData?.role || 'user';

          // Mapear role
          let customClaimRole = firestoreRole;
          if (firestoreRole === 'client') {
            customClaimRole = 'user';
          }

          // Definir Custom Claims
          await admin.auth().setCustomUserClaims(userRecord.uid, {
            ...existingClaims,
            role: customClaimRole,
          });

          totalUpdated++;
          console.log(`✓ ${userRecord.email || userRecord.uid}: ${firestoreRole} → ${customClaimRole}`);
        } catch (error) {
          totalErrors++;
          const errorMsg = error instanceof Error ? error.message : String(error);
          console.error(`✗ ${userRecord.email || userRecord.uid}: ${errorMsg}`);
        }
      }

      // Pequeno delay para não sobrecarregar a API
      if (nextPageToken) {
        await new Promise((resolve) => setTimeout(resolve, 100));
      }
    } while (nextPageToken);

    console.log('');
    console.log('═══════════════════════════════════════');
    console.log('✅ Migração concluída!');
    console.log(`   Total processado: ${totalProcessed}`);
    console.log(`   Atualizados: ${totalUpdated}`);
    console.log(`   Pulados (já tinham role): ${totalSkipped}`);
    console.log(`   Erros: ${totalErrors}`);
    console.log('═══════════════════════════════════════');

    return {
      totalProcessed,
      totalUpdated,
      totalSkipped,
      totalErrors,
    };
  } catch (error) {
    console.error('❌ Erro fatal na migração:', error);
    throw error;
  }
}
