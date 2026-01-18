import * as admin from 'firebase-admin';
import {getFirestore} from './utils/firestore';
import * as functions from 'firebase-functions';

const db = getFirestore();
const auth = admin.auth();
const storage = admin.storage();

/**
 * ATENÇÃO: Esta função deleta TODOS os dados do Firebase
 * 
 * Função temporária para limpar todos os dados:
 * - Todos os usuários do Authentication
 * - Todas as coleções do Firestore (incluindo subcoleções)
 * - Todos os arquivos do Storage
 * 
 * Para segurança, esta função requer um parâmetro de confirmação
 */
export const clearAllData = functions.https.onRequest(async (req, res) => {
  // Requer parâmetro de confirmação para segurança
  const confirm = req.query.confirm as string;
  
  if (confirm !== 'DELETE_ALL_DATA_CONFIRMED') {
    res.status(400).json({
      error: 'Esta função requer confirmação explícita',
      message: 'Adicione ?confirm=DELETE_ALL_DATA_CONFIRMED à URL para confirmar',
      warning: '⚠️ Esta ação é IRREVERSÍVEL e deleta TODOS os dados!'
    });
    return;
  }

  try {
    const results: {
      firestore: { collections: number; documents: number; users: number; userSubcollections: number };
      storage: { files: number };
      auth: { users: number };
    } = {
      firestore: { collections: 0, documents: 0, users: 0, userSubcollections: 0 },
      storage: { files: 0 },
      auth: { users: 0 }
    };

    // 1. Deletar dados do Firestore
    console.log('=== DELETANDO FIRESTORE ===');
    
    // Deletar coleções públicas
    const collections = await db.listCollections();
    console.log(`Encontradas ${collections.length} coleções públicas`);
    
    for (const collection of collections) {
      if (collection.id === 'users') {
        // Pular users por enquanto, vamos deletar depois
        continue;
      }
      
      console.log(`Deletando coleção: ${collection.id}`);
      const snapshot = await collection.get();
      
      if (!snapshot.empty) {
        // Deletar em batches de 500
        const batches: FirebaseFirestore.WriteBatch[] = [];
        let currentBatch = db.batch();
        let count = 0;
        
        snapshot.forEach((doc) => {
          currentBatch.delete(doc.ref);
          count++;
          
          if (count >= 500) {
            batches.push(currentBatch);
            currentBatch = db.batch();
            count = 0;
          }
        });
        
        if (count > 0) {
          batches.push(currentBatch);
        }
        
        for (const batch of batches) {
          await batch.commit();
        }
        
        results.firestore.collections++;
        results.firestore.documents += snapshot.size;
        console.log(`  ✓ ${snapshot.size} documentos deletados`);
      }
    }
    
    // Deletar subcoleções de usuários
    console.log('\nDeletando subcoleções de usuários...');
    const usersSnapshot = await db.collection('users').get();
    
    const subcollections = [
      'services', 'products', 'orders', 'purchase_orders',
      'reviews', 'notifications', 'conversations',
      'preferences', 'settings', 'posts', 'stories'
    ];
    
    for (const userDoc of usersSnapshot.docs) {
      const userId = userDoc.id;
      
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
            results.firestore.userSubcollections += subSnapshot.size;
          }
        } catch (error) {
          // Ignorar se não existir
        }
      }
    }
    
    // Deletar documentos da coleção users
    console.log('\nDeletando documentos da coleção users...');
    if (!usersSnapshot.empty) {
      const batches: FirebaseFirestore.WriteBatch[] = [];
      let currentBatch = db.batch();
      let count = 0;
      
      usersSnapshot.forEach((doc) => {
        currentBatch.delete(doc.ref);
        count++;
        
        if (count >= 500) {
          batches.push(currentBatch);
          currentBatch = db.batch();
          count = 0;
        }
      });
      
      if (count > 0) {
        batches.push(currentBatch);
      }
      
      for (const batch of batches) {
        await batch.commit();
      }
      
      results.firestore.users = usersSnapshot.size;
      console.log(`  ✓ ${usersSnapshot.size} documentos deletados`);
    }
    
    // 2. Deletar arquivos do Storage
    console.log('\n=== DELETANDO STORAGE ===');
    try {
      const bucket = storage.bucket();
      const [files] = await bucket.getFiles();
      
      console.log(`Encontrados ${files.length} arquivos`);
      
      if (files.length > 0) {
        // Deletar em lotes de 100
        for (let i = 0; i < files.length; i += 100) {
          const batch = files.slice(i, i + 100);
          await Promise.all(batch.map(file => file.delete()));
          console.log(`  ✓ ${Math.min(i + 100, files.length)}/${files.length} arquivos deletados`);
        }
        
        results.storage.files = files.length;
      }
    } catch (error: unknown) {
      const errorMessage = error instanceof Error ? error.message : 'Erro desconhecido';
      console.error('Erro ao deletar arquivos do Storage:', errorMessage);
    }
    
    // 3. Deletar usuários do Authentication
    console.log('\n=== DELETANDO USUÁRIOS DO AUTH ===');
    let totalDeleted = 0;
    let nextPageToken;
    
    do {
      const listUsersResult = await auth.listUsers(1000, nextPageToken);
      
      const deletePromises = listUsersResult.users.map(async (userRecord) => {
        try {
          await auth.deleteUser(userRecord.uid);
          console.log(`  ✓ Usuário deletado: ${userRecord.uid}`);
          return true;
        } catch (error: unknown) {
          const errorMessage = error instanceof Error ? error.message : 'Erro desconhecido';
          console.error(`  ✗ Erro ao deletar usuário ${userRecord.uid}:`, errorMessage);
          return false;
        }
      });
      
      const results = await Promise.all(deletePromises);
      totalDeleted += results.filter(r => r).length;
      
      nextPageToken = listUsersResult.pageToken;
    } while (nextPageToken);
    
    results.auth.users = totalDeleted;
    console.log(`\nTotal de usuários deletados: ${totalDeleted}`);
    
    console.log('\n=== LIMPEZA CONCLUÍDA ===');
    
    res.json({
      success: true,
      message: 'Todos os dados foram deletados com sucesso',
      results: results
    });
  } catch (error: unknown) {
    const errorMessage = error instanceof Error ? error.message : 'Erro desconhecido';
    console.error('Erro durante a limpeza:', error);
    res.status(500).json({
      error: 'Erro durante a limpeza',
      message: errorMessage
    });
  }
});
