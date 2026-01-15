/**
 * Script para limpar TODOS os dados do Firebase
 * ATENÇÃO: Este script deleta TUDO - usuários, dados do Firestore e Storage
 * 
 * Uso: node scripts/clear-firebase-data.js
 */

const admin = require('firebase-admin');
const path = require('path');

// Inicializar Firebase Admin
// Tenta carregar as credenciais do ambiente ou do arquivo de service account
if (!admin.apps.length) {
  try {
    // Se tiver GOOGLE_APPLICATION_CREDENTIALS definido, usa ele
    if (process.env.GOOGLE_APPLICATION_CREDENTIALS) {
      const serviceAccount = require(process.env.GOOGLE_APPLICATION_CREDENTIALS);
      admin.initializeApp({
        credential: admin.credential.cert(serviceAccount)
      });
    } else {
      // Tenta usar as credenciais padrão (se estiver configurado no Firebase CLI)
      admin.initializeApp();
    }
  } catch (error) {
    console.error('Erro ao inicializar Firebase Admin:', error);
    console.error('Certifique-se de que as credenciais estão configuradas.');
    process.exit(1);
  }
}

const db = admin.firestore();
const auth = admin.auth();
const storage = admin.storage();

/**
 * Deletar todas as coleções do Firestore
 */
async function deleteAllCollections() {
  console.log('\n=== DELETANDO COLECÕES DO FIRESTORE ===\n');
  
  const collections = await db.listCollections();
  console.log(`Encontradas ${collections.length} coleções`);
  
  for (const collection of collections) {
    console.log(`\nDeletando coleção: ${collection.id}`);
    
    try {
      // Para coleções públicas (services, products, orders, posts, etc.)
      const snapshot = await collection.get();
      const batch = db.batch();
      let count = 0;
      
      snapshot.forEach((doc) => {
        batch.delete(doc.ref);
        count++;
        
        // Firestore permite no máximo 500 operações por batch
        if (count >= 500) {
          batch.commit();
          count = 0;
        }
      });
      
      if (count > 0) {
        await batch.commit();
      }
      
      console.log(`  ✓ ${snapshot.size} documentos deletados`);
    } catch (error) {
      console.error(`  ✗ Erro ao deletar coleção ${collection.id}:`, error.message);
    }
  }
  
  // Deletar subcoleções de usuários
  console.log('\nDeletando subcoleções de usuários...');
  const usersSnapshot = await db.collection('users').get();
  
  for (const userDoc of usersSnapshot.docs) {
    const userId = userDoc.id;
    console.log(`  Deletando subcoleções do usuário: ${userId}`);
    
    // Listar todas as subcoleções conhecidas
    const subcollections = [
      'services', 'products', 'orders', 'purchase_orders',
      'reviews', 'notifications', 'conversations',
      'preferences', 'settings', 'posts', 'stories'
    ];
    
    for (const subcollectionName of subcollections) {
      try {
        const subcollectionRef = db.collection('users').doc(userId).collection(subcollectionName);
        const subSnapshot = await subcollectionRef.get();
        
        if (!subSnapshot.empty) {
          const batch = db.batch();
          subSnapshot.forEach((doc) => {
            batch.delete(doc.ref);
          });
          await batch.commit();
          console.log(`    ✓ ${subcollectionName}: ${subSnapshot.size} documentos`);
        }
      } catch (error) {
        // Ignorar se a subcoleção não existir
      }
    }
  }
  
  // Deletar documentos da coleção users também
  console.log('\nDeletando documentos da coleção users...');
  const usersBatch = db.batch();
  let usersCount = 0;
  usersSnapshot.forEach((doc) => {
    usersBatch.delete(doc.ref);
    usersCount++;
    if (usersCount >= 500) {
      usersBatch.commit();
      usersCount = 0;
    }
  });
  if (usersCount > 0) {
    await usersBatch.commit();
  }
  console.log(`  ✓ ${usersSnapshot.size} documentos de usuários deletados`);
}

/**
 * Deletar todos os usuários do Authentication
 */
async function deleteAllUsers() {
  console.log('\n=== DELETANDO USUÁRIOS DO AUTHENTICATION ===\n');
  
  let totalDeleted = 0;
  let nextPageToken;
  
  do {
    const listUsersResult = await auth.listUsers(1000, nextPageToken);
    
    const deletePromises = listUsersResult.users.map(async (userRecord) => {
      try {
        await auth.deleteUser(userRecord.uid);
        console.log(`  ✓ Usuário deletado: ${userRecord.uid} (${userRecord.email || 'sem email'})`);
        return true;
      } catch (error) {
        console.error(`  ✗ Erro ao deletar usuário ${userRecord.uid}:`, error.message);
        return false;
      }
    });
    
    const results = await Promise.all(deletePromises);
    totalDeleted += results.filter(r => r).length;
    
    nextPageToken = listUsersResult.pageToken;
  } while (nextPageToken);
  
  console.log(`\nTotal de usuários deletados: ${totalDeleted}`);
}

/**
 * Deletar todos os arquivos do Storage
 */
async function deleteAllStorageFiles() {
  console.log('\n=== DELETANDO ARQUIVOS DO STORAGE ===\n');
  
  try {
    const bucket = storage.bucket();
    const [files] = await bucket.getFiles();
    
    console.log(`Encontrados ${files.length} arquivos`);
    
    if (files.length === 0) {
      console.log('Nenhum arquivo para deletar');
      return;
    }
    
    // Deletar em lotes de 100
    for (let i = 0; i < files.length; i += 100) {
      const batch = files.slice(i, i + 100);
      await Promise.all(batch.map(file => file.delete()));
      console.log(`  ✓ ${Math.min(i + 100, files.length)}/${files.length} arquivos deletados`);
    }
    
    console.log(`\nTotal de arquivos deletados: ${files.length}`);
  } catch (error) {
    console.error('Erro ao deletar arquivos do Storage:', error.message);
  }
}

/**
 * Função principal
 */
async function main() {
  console.log('========================================');
  console.log('LIMPEZA COMPLETA DO FIREBASE');
  console.log('========================================');
  console.log('\n⚠️  ATENÇÃO: Este script vai deletar TODOS os dados!');
  console.log('⚠️  - Todos os usuários do Authentication');
  console.log('⚠️  - Todas as coleções do Firestore');
  console.log('⚠️  - Todos os arquivos do Storage');
  console.log('\nPressione Ctrl+C para cancelar...');
  
  // Aguardar 5 segundos para dar tempo de cancelar
  await new Promise(resolve => setTimeout(resolve, 5000));
  
  try {
    // 1. Deletar dados do Firestore primeiro (para evitar problemas de referência)
    await deleteAllCollections();
    
    // 2. Deletar arquivos do Storage
    await deleteAllStorageFiles();
    
    // 3. Deletar usuários do Authentication por último
    await deleteAllUsers();
    
    console.log('\n========================================');
    console.log('✅ LIMPEZA CONCLUÍDA COM SUCESSO!');
    console.log('========================================\n');
  } catch (error) {
    console.error('\n========================================');
    console.error('❌ ERRO DURANTE A LIMPEZA:');
    console.error('========================================');
    console.error(error);
    process.exit(1);
  }
}

// Executar
main().then(() => {
  process.exit(0);
}).catch((error) => {
  console.error('Erro fatal:', error);
  process.exit(1);
});
