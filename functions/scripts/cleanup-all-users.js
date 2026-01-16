/**
 * Script para limpar TODOS os usuários do Firebase Auth e dados relacionados
 * 
 * ⚠️ ATENÇÃO: Esta ação é IRREVERSÍVEL!
 * 
 * Como usar:
 * cd functions
 * node scripts/cleanup-all-users.js
 */

const admin = require('firebase-admin');
const path = require('path');
const fs = require('fs');

// Carregar service account key
const serviceAccountPath = path.join(__dirname, '../../task-go-ee85f-firebase-adminsdk-fbsvc-5ec279b7e7.json');

if (!fs.existsSync(serviceAccountPath)) {
  console.error(`❌ Arquivo de credenciais não encontrado: ${serviceAccountPath}`);
  console.error('Verifique se o arquivo task-go-ee85f-firebase-adminsdk-fbsvc-5ec279b7e7.json está na raiz do projeto.');
  process.exit(1);
}

const serviceAccount = require(serviceAccountPath);

// Inicializar Firebase Admin com credenciais
const existingApps = admin.apps;
if (existingApps.length > 0) {
  for (const app of existingApps) {
    if (app) {
      app.delete().catch(() => {});
    }
  }
}

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

console.log('✅ Firebase Admin inicializado com credenciais do service account\n');

const db = admin.firestore();

async function cleanupAllUsers() {
  try {
    console.log('⚠️  INICIANDO LIMPEZA COMPLETA DE USUÁRIOS');
    console.log('⚠️  Esta ação é IRREVERSÍVEL!\n');
    
    const batchSize = 100;
    let nextPageToken = undefined;
    let totalDeleted = 0;
    let totalErrors = 0;
    const deletedUserIds = [];

    console.log('📋 Listando e excluindo usuários do Firebase Auth...\n');

    do {
      const listUsersResult = await admin.auth().listUsers(batchSize, nextPageToken);
      nextPageToken = listUsersResult.pageToken;

      console.log(`📦 Processando batch: ${listUsersResult.users.length} usuários`);

      for (const userRecord of listUsersResult.users) {
        try {
          // Excluir usuário do Firebase Auth
          await admin.auth().deleteUser(userRecord.uid);
          totalDeleted++;
          deletedUserIds.push(userRecord.uid);
          
          console.log(`✓ Excluído: ${userRecord.email || userRecord.uid}`);
        } catch (error) {
          totalErrors++;
          const errorMsg = error instanceof Error ? error.message : String(error);
          console.error(`✗ Erro ao excluir ${userRecord.email || userRecord.uid}: ${errorMsg}`);
        }
      }

      // Pequeno delay para não sobrecarregar a API
      if (nextPageToken) {
        await new Promise((resolve) => setTimeout(resolve, 100));
      }
    } while (nextPageToken);

    console.log('\n🔍 Verificando documentos órfãos no Firestore...\n');

    // Limpar documentos órfãos na coleção users (se existirem)
    let orphanDocsDeleted = 0;
    if (deletedUserIds.length > 0) {
      // Processar em batches para não sobrecarregar
      const batches = [];
      for (let i = 0; i < deletedUserIds.length; i += 500) {
        const batch = deletedUserIds.slice(i, i + 500);
        batches.push(batch);
      }

      for (const batch of batches) {
        const userDocs = await db.collection('users').where(admin.firestore.FieldPath.documentId(), 'in', batch).get();
        
        if (!userDocs.empty) {
          const deleteBatch = db.batch();
          let batchCount = 0;
          
          userDocs.forEach((doc) => {
            deleteBatch.delete(doc.ref);
            batchCount++;
          });
          
          if (batchCount > 0) {
            await deleteBatch.commit();
            orphanDocsDeleted += batchCount;
            console.log(`✓ ${batchCount} documentos órfãos excluídos do Firestore`);
          }
        }
      }
    }

    console.log('');
    console.log('═══════════════════════════════════════');
    console.log('✅ Limpeza concluída!');
    console.log(`   Usuários excluídos do Auth: ${totalDeleted}`);
    console.log(`   Documentos órfãos excluídos: ${orphanDocsDeleted}`);
    console.log(`   Erros: ${totalErrors}`);
    console.log('═══════════════════════════════════════');
    console.log('');
    console.log('✅ Ambiente limpo e pronto para testes do zero!');
    console.log('   Novos usuários criarão Custom Claims automaticamente via onUserCreate');
    console.log('');

    process.exit(0);
  } catch (error) {
    console.error('❌ Erro fatal na limpeza:', error);
    if (error.stack) {
      console.error('Stack trace:', error.stack);
    }
    process.exit(1);
  }
}

// Executar limpeza
cleanupAllUsers();
