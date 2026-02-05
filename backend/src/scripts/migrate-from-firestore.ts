/**
 * Script de migração de dados do Firestore para PostgreSQL
 * 
 * Uso:
 * npm run migrate:firestore
 */

import * as admin from 'firebase-admin';
import { UserRepository } from '../repositories/UserRepository';
import { LocationRepository } from '../repositories/LocationRepository';
import { ProductRepository } from '../repositories/ProductRepository';
import { query } from '../database/connection';

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
const userRepo = new UserRepository();
const locationRepo = new LocationRepository();
const productRepo = new ProductRepository();

/**
 * Migra usuários do Firestore para PostgreSQL
 */
async function migrateUsers(): Promise<void> {
  console.log('🔄 Migrando usuários...');
  
  const usersSnapshot = await db.collection('users').get();
  let migrated = 0;
  let errors = 0;

  for (const doc of usersSnapshot.docs) {
    try {
      const firestoreUser = doc.data();
      
      // Buscar cidade baseada em city/state antigo
      let currentCityId: number | undefined;
      if (firestoreUser.city && firestoreUser.state) {
        const city = await locationRepo.findCityByNameAndState(
          firestoreUser.city,
          firestoreUser.state
        );
        currentCityId = city?.id;
      }

      // Criar usuário no PostgreSQL
      await userRepo.create({
        id: doc.id,
        firebase_uid: doc.id,
        email: firestoreUser.email || '',
        role: firestoreUser.role || 'client',
        display_name: firestoreUser.displayName,
        phone: firestoreUser.phone,
        photo_url: firestoreUser.photoURL,
        current_city_id: currentCityId,
        cpf: firestoreUser.cpf,
        cnpj: firestoreUser.cnpj,
        rg: firestoreUser.rg,
        birth_date: firestoreUser.birthDate?.toDate(),
        document_front_url: firestoreUser.documentFront,
        document_back_url: firestoreUser.documentBack,
        selfie_url: firestoreUser.selfie,
        address_proof_url: firestoreUser.addressProof,
        profile_complete: firestoreUser.profileComplete || false,
        verified: firestoreUser.verified || false,
        verified_at: firestoreUser.verifiedAt?.toDate(),
        stripe_account_id: firestoreUser.stripeAccountId,
        stripe_charges_enabled: firestoreUser.stripeChargesEnabled || false,
        stripe_payouts_enabled: firestoreUser.stripePayoutsEnabled || false,
        rating: firestoreUser.rating || 0.00
      });

      // Migrar categorias preferidas (se for parceiro)
      if (firestoreUser.role === 'partner' && firestoreUser.preferredCategories) {
        // Buscar IDs das categorias
        const categoryIds: number[] = [];
        for (const categoryName of firestoreUser.preferredCategories) {
          const categories = await locationRepo.getAllCategories('service');
          const category = categories.find(c => c.name === categoryName);
          if (category) {
            categoryIds.push(category.id);
          }
        }
        
        if (categoryIds.length > 0) {
          await userRepo.setPreferredCategories(doc.id, categoryIds);
        }
      }

      // Criar localização inicial se houver city/state
      if (currentCityId && firestoreUser.latitude && firestoreUser.longitude) {
        await userRepo.updateLocation(
          doc.id,
          firestoreUser.latitude,
          firestoreUser.longitude,
          currentCityId
        );
      }

      migrated++;
      if (migrated % 100 === 0) {
        console.log(`  ✅ ${migrated} usuários migrados...`);
      }
    } catch (error: any) {
      console.error(`  ❌ Erro ao migrar usuário ${doc.id}:`, error.message);
      errors++;
    }
  }

  console.log(`✅ Migração de usuários concluída: ${migrated} migrados, ${errors} erros`);
}

/**
 * Migra produtos do Firestore para PostgreSQL
 */
async function migrateProducts(): Promise<void> {
  console.log('🔄 Migrando produtos...');
  
  // Buscar produtos de todas as localizações
  const locationsSnapshot = await db.collection('locations').get();
  let migrated = 0;
  let errors = 0;

  for (const locationDoc of locationsSnapshot.docs) {
    const productsSnapshot = await locationDoc.ref.collection('products').get();
    
    for (const doc of productsSnapshot.docs) {
      try {
        const firestoreProduct = doc.data();
        
        // Extrair city/state do locationId (formato: "city_state")
        const locationId = locationDoc.id;
        const [cityName, stateCode] = locationId.split('_');
        
        // Buscar cidade
        const city = await locationRepo.findCityByNameAndState(cityName, stateCode);
        if (!city) {
          console.warn(`  ⚠️ Cidade não encontrada: ${cityName}, ${stateCode}`);
          continue;
        }

        // Criar produto no PostgreSQL
        await productRepo.create({
          id: doc.id,
          seller_id: firestoreProduct.sellerId,
          created_in_city_id: city.id, // FIXO - cidade onde foi criado
          title: firestoreProduct.title || '',
          description: firestoreProduct.description,
          price: parseFloat(firestoreProduct.price || 0),
          category: firestoreProduct.category,
          active: firestoreProduct.active !== false,
          featured: firestoreProduct.featured || false,
          discount_percentage: firestoreProduct.discountPercentage,
          rating: firestoreProduct.rating || 0.00
        });

        // Migrar imagens
        if (firestoreProduct.images && Array.isArray(firestoreProduct.images)) {
          for (let i = 0; i < firestoreProduct.images.length; i++) {
            await productRepo.addImage(doc.id, firestoreProduct.images[i], i);
          }
        }

        migrated++;
        if (migrated % 100 === 0) {
          console.log(`  ✅ ${migrated} produtos migrados...`);
        }
      } catch (error: any) {
        console.error(`  ❌ Erro ao migrar produto ${doc.id}:`, error.message);
        errors++;
      }
    }
  }

  console.log(`✅ Migração de produtos concluída: ${migrated} migrados, ${errors} erros`);
}

/**
 * Função principal
 */
async function main(): Promise<void> {
  console.log('🚀 Iniciando migração do Firestore para PostgreSQL...\n');

  try {
    await migrateUsers();
    console.log('');
    await migrateProducts();
    console.log('\n✅ Migração concluída!');
  } catch (error) {
    console.error('❌ Erro na migração:', error);
    process.exit(1);
  } finally {
    process.exit(0);
  }
}

// Executar se chamado diretamente
if (require.main === module) {
  main();
}

export { migrateUsers, migrateProducts };
