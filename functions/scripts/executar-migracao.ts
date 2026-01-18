/**
 * Script standalone para executar migração completa
 * 
 * USO:
 *   npx ts-node functions/scripts/executar-migracao.ts
 * 
 * OU via Firebase CLI:
 *   firebase functions:shell
 *   migrateDatabaseToTaskgo()
 */

import * as admin from 'firebase-admin';

// Inicializar Firebase Admin
if (!admin.apps.length) {
  admin.initializeApp();
}

async function executarMigracao() {
  const startTime = Date.now();
  console.log('🚀 Iniciando migração completa: default → taskgo');
  console.log('========================================\n');

  try {
    // Obter instâncias dos databases
    const sourceDb = admin.firestore(); // Database 'default'
    // @ts-ignore - firestore() pode aceitar database ID em projetos Enterprise
    const targetDb = admin.app().firestore('taskgo'); // Database 'taskgo'

    // Lista completa de coleções
    const collections = [
      'users',
      'products',
      'services',
      'orders',
      'conversations',
      'stories',
      'posts',
      'notifications',
      'categories',
      'reviews',
      'ai_usage',
      'moderation_logs',
      'shipments',
      'purchase_orders',
      'account_change_requests',
      'identity_verifications',
      'two_factor_codes',
    ];

    let totalDocuments = 0;
    let totalSubcollections = 0;
    let totalErrors = 0;

    for (const collectionName of collections) {
      const collectionStartTime = Date.now();
      console.log(`📦 Migrando coleção: ${collectionName}...`);

      try {
        const sourceSnapshot = await sourceDb.collection(collectionName).get();

        if (sourceSnapshot.empty) {
          console.log(`   ⏭️  Vazia, pulando...\n`);
          continue;
        }

        console.log(`   📊 ${sourceSnapshot.size} documentos encontrados`);

        const documents = sourceSnapshot.docs;
        const batchSize = 500;
        let processedCount = 0;

        for (let i = 0; i < documents.length; i += batchSize) {
          const batch = documents.slice(i, i + batchSize);
          const targetBatch = targetDb.batch();

          for (const doc of batch) {
            try {
              const data = doc.data();
              if (!data || typeof data !== 'object') {
                console.warn(`   ⚠️  Documento ${doc.id} inválido, pulando...`);
                totalErrors++;
                continue;
              }

              const targetRef = targetDb.collection(collectionName).doc(doc.id);
              const existingDoc = await targetRef.get();
              
              if (existingDoc.exists) {
                targetBatch.set(targetRef, data, {merge: true});
              } else {
                targetBatch.set(targetRef, data);
              }

              // Migrar subcoleções
              const subcollections = await doc.ref.listCollections();
              for (const subcollection of subcollections) {
                try {
                  const subSnapshot = await subcollection.get();
                  if (!subSnapshot.empty) {
                    for (const subDoc of subSnapshot.docs) {
                      const subData = subDoc.data();
                      if (subData && typeof subData === 'object') {
                        targetBatch.set(
                          targetRef.collection(subcollection.id).doc(subDoc.id),
                          subData
                        );
                        totalSubcollections++;
                      }
                    }
                  }
                } catch (subError) {
                  console.error(`   ❌ Erro em subcoleção ${subcollection.id}:`, subError);
                  totalErrors++;
                }
              }

              totalDocuments++;
            } catch (docError) {
              console.error(`   ❌ Erro no documento ${doc.id}:`, docError);
              totalErrors++;
            }
          }

          await targetBatch.commit();
          processedCount += batch.length;
          console.log(`   ✅ ${processedCount}/${documents.length} processados`);
        }

        // Validar
        const targetSnapshot = await targetDb.collection(collectionName).get();
        const duration = ((Date.now() - collectionStartTime) / 1000).toFixed(2);
        
        if (targetSnapshot.size >= sourceSnapshot.size) {
          console.log(`   ✅ Migrada: ${targetSnapshot.size} documentos (${duration}s)\n`);
        } else {
          console.warn(`   ⚠️  Parcial: ${targetSnapshot.size}/${sourceSnapshot.size} (${duration}s)\n`);
        }
      } catch (collectionError) {
        console.error(`   ❌ Erro na coleção ${collectionName}:`, collectionError);
        totalErrors++;
      }
    }

    const duration = ((Date.now() - startTime) / 1000).toFixed(2);
    console.log('========================================');
    console.log('🎉 MIGRAÇÃO CONCLUÍDA');
    console.log('========================================');
    console.log(`⏱️  Duração: ${duration}s`);
    console.log(`📄 Documentos: ${totalDocuments}`);
    console.log(`📁 Subcoleções: ${totalSubcollections}`);
    console.log(`❌ Erros: ${totalErrors}`);
    console.log('========================================');

    process.exit(0);
  } catch (error) {
    console.error('❌ ERRO CRÍTICO:', error);
    process.exit(1);
  }
}

// Executar
executarMigracao();
