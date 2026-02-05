/**
 * Script para validar dados migrados do Firestore
 * 
 * Uso:
 * npm run validate:migration
 */

import { query } from '../database/connection';
import * as admin from 'firebase-admin';

// Inicializar Firebase Admin
if (!admin.apps.length) {
  admin.initializeApp({
    credential: admin.credential.cert({
      projectId: process.env.FIREBASE_PROJECT_ID,
      clientEmail: process.env.FIREBASE_CLIENT_EMAIL,
      privateKey: process.env.FIREBASE_PRIVATE_KEY?.replace(/\\n/g, '\n')
    })
  });
}

const db = admin.firestore();

interface ValidationResult {
  table: string;
  firestoreCount: number;
  postgresCount: number;
  match: boolean;
  errors?: string[];
}

/**
 * Valida migração de usuários
 */
async function validateUsers(): Promise<ValidationResult> {
  console.log('🔍 Validando usuários...');
  
  const firestoreUsers = await db.collection('users').get();
  const postgresResult = await query('SELECT COUNT(*) as count FROM users');
  const postgresCount = parseInt(postgresResult.rows[0].count);
  
  const match = firestoreUsers.size === postgresCount;
  const errors: string[] = [];
  
  if (!match) {
    errors.push(`Contagem não corresponde: Firestore=${firestoreUsers.size}, PostgreSQL=${postgresCount}`);
  }
  
  // Validar alguns usuários específicos
  let validated = 0;
  for (const doc of firestoreUsers.docs.slice(0, 10)) {
    const firestoreUser = doc.data();
    const postgresUser = await query('SELECT * FROM users WHERE firebase_uid = $1', [doc.id]);
    
    if (postgresUser.rows.length === 0) {
      errors.push(`Usuário ${doc.id} não encontrado no PostgreSQL`);
    } else {
      const pg = postgresUser.rows[0];
      if (pg.email !== firestoreUser.email) {
        errors.push(`Email não corresponde para usuário ${doc.id}`);
      }
      if (pg.role !== firestoreUser.role) {
        errors.push(`Role não corresponde para usuário ${doc.id}`);
      }
      validated++;
    }
  }
  
  return {
    table: 'users',
    firestoreCount: firestoreUsers.size,
    postgresCount,
    match,
    errors: errors.length > 0 ? errors : undefined
  };
}

/**
 * Valida migração de produtos
 */
async function validateProducts(): Promise<ValidationResult> {
  console.log('🔍 Validando produtos...');
  
  let firestoreCount = 0;
  const locationsSnapshot = await db.collection('locations').get();
  
  for (const locationDoc of locationsSnapshot.docs) {
    const productsSnapshot = await locationDoc.ref.collection('products').get();
    firestoreCount += productsSnapshot.size;
  }
  
  const postgresResult = await query('SELECT COUNT(*) as count FROM products');
  const postgresCount = parseInt(postgresResult.rows[0].count);
  
  const match = firestoreCount === postgresCount;
  const errors: string[] = [];
  
  if (!match) {
    errors.push(`Contagem não corresponde: Firestore=${firestoreCount}, PostgreSQL=${postgresCount}`);
  }
  
  return {
    table: 'products',
    firestoreCount,
    postgresCount,
    match,
    errors: errors.length > 0 ? errors : undefined
  };
}

/**
 * Valida estrutura do banco
 */
async function validateStructure(): Promise<void> {
  console.log('🔍 Validando estrutura do banco...');
  
  const tables = [
    'users', 'user_locations', 'products', 'posts', 'stories',
    'service_orders', 'purchase_orders', 'notifications',
    'stripe_accounts', 'bank_accounts', 'conversations'
  ];
  
  for (const table of tables) {
    try {
      const result = await query(`SELECT COUNT(*) as count FROM ${table}`);
      console.log(`  ✅ Tabela ${table}: ${result.rows[0].count} registros`);
    } catch (error: any) {
      console.error(`  ❌ Erro ao validar tabela ${table}:`, error.message);
    }
  }
}

/**
 * Função principal
 */
async function main(): Promise<void> {
  console.log('🚀 Iniciando validação da migração...\n');
  
  try {
    await validateStructure();
    console.log('');
    
    const userValidation = await validateUsers();
    console.log(`\n📊 Usuários:`);
    console.log(`   Firestore: ${userValidation.firestoreCount}`);
    console.log(`   PostgreSQL: ${userValidation.postgresCount}`);
    console.log(`   Status: ${userValidation.match ? '✅ OK' : '❌ ERRO'}`);
    if (userValidation.errors) {
      userValidation.errors.forEach(err => console.log(`   ⚠️ ${err}`));
    }
    
    const productValidation = await validateProducts();
    console.log(`\n📊 Produtos:`);
    console.log(`   Firestore: ${productValidation.firestoreCount}`);
    console.log(`   PostgreSQL: ${productValidation.postgresCount}`);
    console.log(`   Status: ${productValidation.match ? '✅ OK' : '❌ ERRO'}`);
    if (productValidation.errors) {
      productValidation.errors.forEach(err => console.log(`   ⚠️ ${err}`));
    }
    
    console.log('\n✅ Validação concluída!');
  } catch (error) {
    console.error('❌ Erro na validação:', error);
    process.exit(1);
  } finally {
    process.exit(0);
  }
}

if (require.main === module) {
  main();
}

export { validateUsers, validateProducts, validateStructure };
