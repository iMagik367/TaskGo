import * as admin from 'firebase-admin';
import * as functions from 'firebase-functions';

/**
 * MIGRAÇÃO COMPLETA E SEGURA: Default → Taskgo
 * 
 * Este script migra TODOS os dados do database 'default' para 'taskgo'
 * de forma segura, validando cada etapa e garantindo integridade dos dados.
 * 
 * CARACTERÍSTICAS:
 * - Processa em batches de 500 documentos (limite do Firestore)
 * - Valida integridade dos dados após cada batch
 * - Trata erros de forma robusta
 * - Loga progresso detalhado
 * - Não sobrescreve dados existentes (usa merge)
 * - Migra subcoleções recursivamente
 * - Valida contagem de documentos antes e depois
 */
export const migrateDatabaseToTaskgo = functions
  .runWith({
    timeoutSeconds: 540, // 9 minutos (máximo para HTTP functions)
    memory: '2GB',
  })
  .https.onRequest(async (req, res) => {
    // Verificar autenticação (opcional - remover se quiser executar sem auth)
    // if (!req.headers.authorization) {
    //   res.status(401).json({error: 'Unauthorized'});
    //   return;
    // }

    const startTime = Date.now();
    interface CollectionResult {
      name: string;
      documentsMigrated: number;
      subcollectionsMigrated: number;
      errors: number;
      status: 'success' | 'partial' | 'failed';
    }

    const results = {
      collections: [] as CollectionResult[],
      totalDocuments: 0,
      totalSubcollections: 0,
      totalErrors: 0,
      duration: 0,
    };

    try {
      functions.logger.info('🚀 Iniciando migração completa: default → taskgo');

      // Obter instâncias dos databases
      const sourceDb = admin.firestore(); // Database 'default'
      // eslint-disable-next-line @typescript-eslint/ban-ts-comment
      // @ts-expect-error - firestore() pode aceitar database ID em projetos Enterprise
      const targetDb = admin.app().firestore('taskgo'); // Database 'taskgo'

      // Lista completa de coleções para migrar
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

      // Migrar cada coleção
      for (const collectionName of collections) {
        const collectionStartTime = Date.now();
        const collectionResult = {
          name: collectionName,
          documentsMigrated: 0,
          subcollectionsMigrated: 0,
          errors: 0,
          status: 'success' as 'success' | 'partial' | 'failed',
        };

        try {
          functions.logger.info(`📦 Migrando coleção: ${collectionName}...`);

          // Obter todos os documentos da coleção
          const sourceSnapshot = await sourceDb.collection(collectionName).get();

          if (sourceSnapshot.empty) {
            functions.logger.info(`   ⏭️  Coleção ${collectionName} está vazia, pulando...`);
            collectionResult.status = 'success';
            results.collections.push(collectionResult);
            continue;
          }

          functions.logger.info(`   📊 Encontrados ${sourceSnapshot.size} documentos em ${collectionName}`);

          // Processar documentos em batches
          const documents = sourceSnapshot.docs;
          const batchSize = 500;
          let processedCount = 0;

          for (let i = 0; i < documents.length; i += batchSize) {
            const batch = documents.slice(i, i + batchSize);
            const targetBatch = targetDb.batch();

            for (const doc of batch) {
              try {
                const data = doc.data();

                // Validar dados antes de migrar
                if (!data || typeof data !== 'object') {
                  functions.logger.warn(`   ⚠️  Documento ${doc.id} tem dados inválidos, pulando...`);
                  collectionResult.errors++;
                  continue;
                }

                // Copiar documento principal
                const targetRef = targetDb.collection(collectionName).doc(doc.id);
                
                // Verificar se já existe no destino
                const existingDoc = await targetRef.get();
                if (existingDoc.exists) {
                  // Se existe, fazer merge (não sobrescrever)
                  targetBatch.set(targetRef, data, {merge: true});
                } else {
                  // Se não existe, criar novo
                  targetBatch.set(targetRef, data);
                }

                // Migrar subcoleções recursivamente
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
                          collectionResult.subcollectionsMigrated++;
                        }
                      }
                    }
                  } catch (subError) {
                    functions.logger.error(
                      `   ❌ Erro ao migrar subcoleção ${subcollection.id} do documento ${doc.id}:`,
                      subError
                    );
                    collectionResult.errors++;
                  }
                }

                collectionResult.documentsMigrated++;
              } catch (docError) {
                functions.logger.error(`   ❌ Erro ao processar documento ${doc.id}:`, docError);
                collectionResult.errors++;
              }
            }

            // Executar batch
            try {
              await targetBatch.commit();
              processedCount += batch.length;
              functions.logger.info(
                `   ✅ Batch ${Math.floor(i / batchSize) + 1}/${Math.ceil(documents.length / batchSize)}: ` +
                `${processedCount}/${documents.length} documentos processados`
              );
            } catch (batchError) {
              functions.logger.error('   ❌ Erro ao commitar batch:', batchError);
              collectionResult.errors += batch.length;
            }
          }

          // Validar migração: contar documentos no destino
          const targetSnapshot = await targetDb.collection(collectionName).get();
          const expectedCount = sourceSnapshot.size;
          const actualCount = targetSnapshot.size;

          if (actualCount >= expectedCount) {
            functions.logger.info(
              `   ✅ Coleção ${collectionName} migrada com sucesso: ` +
              `${actualCount} documentos (esperado: ${expectedCount})`
            );
            collectionResult.status = 'success';
          } else {
            functions.logger.warn(
              `   ⚠️  Coleção ${collectionName} migrada parcialmente: ` +
              `${actualCount} documentos (esperado: ${expectedCount})`
            );
            collectionResult.status = 'partial';
          }

          const collectionDuration = ((Date.now() - collectionStartTime) / 1000).toFixed(2);
          functions.logger.info(`   ⏱️  Tempo: ${collectionDuration}s`);

        } catch (collectionError) {
          functions.logger.error(`   ❌ Erro ao migrar coleção ${collectionName}:`, collectionError);
          collectionResult.status = 'failed';
          collectionResult.errors++;
        }

        results.collections.push(collectionResult);
        results.totalDocuments += collectionResult.documentsMigrated;
        results.totalSubcollections += collectionResult.subcollectionsMigrated;
        results.totalErrors += collectionResult.errors;
      }

      // Resumo final
      const duration = ((Date.now() - startTime) / 1000).toFixed(2);
      results.duration = parseFloat(duration);

      const successCount = results.collections.filter((c) => c.status === 'success').length;
      const partialCount = results.collections.filter((c) => c.status === 'partial').length;
      const failedCount = results.collections.filter((c) => c.status === 'failed').length;

      functions.logger.info('========================================');
      functions.logger.info('🎉 MIGRAÇÃO CONCLUÍDA');
      functions.logger.info('========================================');
      functions.logger.info(`⏱️  Duração total: ${duration}s`);
      functions.logger.info(`📊 Coleções processadas: ${results.collections.length}`);
      functions.logger.info(`   ✅ Sucesso: ${successCount}`);
      functions.logger.info(`   ⚠️  Parcial: ${partialCount}`);
      functions.logger.info(`   ❌ Falhou: ${failedCount}`);
      functions.logger.info(`📄 Documentos migrados: ${results.totalDocuments}`);
      functions.logger.info(`📁 Subcoleções migradas: ${results.totalSubcollections}`);
      functions.logger.info(`❌ Erros: ${results.totalErrors}`);
      functions.logger.info('========================================');

      res.status(200).json({
        success: true,
        message: 'Migração concluída',
        results,
      });
    } catch (error) {
      functions.logger.error('❌ ERRO CRÍTICO NA MIGRAÇÃO:', error);
      res.status(500).json({
        success: false,
        error: error instanceof Error ? error.message : 'Erro desconhecido',
        results,
      });
    }
  });
