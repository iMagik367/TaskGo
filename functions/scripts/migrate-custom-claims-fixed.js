/**
 * Script JavaScript para executar migração de Custom Claims
 * 
 * Como usar:
 * cd functions
 * node scripts/migrate-custom-claims-fixed.js
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
// Deletar apps existentes se houver (sem await no nível superior)
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

async function migrateLocal() {
  try {
    const batchSize = 100;
    let nextPageToken = undefined;
    let totalProcessed = 0;
    let totalUpdated = 0;
    let totalSkipped = 0;
    let totalErrors = 0;

    console.log('🚀 Iniciando migração local de Custom Claims...');
    console.log('');

    do {
      const listUsersResult = await admin.auth().listUsers(batchSize, nextPageToken);
      console.log(`📋 Processando batch: ${listUsersResult.users.length} usuários`);
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
          const firestoreRole = (userData && userData.role) ? userData.role : 'user';

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

    process.exit(0);
  } catch (error) {
    console.error('❌ Erro fatal na migração:', error);
    if (error.stack) {
      console.error('Stack trace:', error.stack);
    }
    process.exit(1);
  }
}

// Executar migração
migrateLocal();
