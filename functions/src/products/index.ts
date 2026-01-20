import * as admin from 'firebase-admin';
import {getFirestore} from '../utils/firestore';
import * as functions from 'firebase-functions';
import {AppError, handleError, assertAuthenticated} from '../utils/errors';
import {validateAppCheck} from '../security/appCheck';
import {getUserRole} from '../security/roles';
import {COLLECTIONS} from '../utils/constants';
import {getLocationCollection, getUserLocation, normalizeLocationId} from '../utils/location';

/**
 * Cria um novo produto
 * Apenas usuários com role "seller" ou "partner" podem criar produtos
 * Cloud Function é a autoridade - valida permissões e dados
 */
export const createProduct = functions.https.onCall(
  async (data, context) => {
    try {
      validateAppCheck(context);
      assertAuthenticated(context);

      const userId = context.auth!.uid;
      const db = getFirestore();

      // Verificar role do usuário (primeiro Custom Claims, depois documento)
      let userRole: string;
      try {
        userRole = getUserRole(context);
      } catch {
        // Se não tiver em Custom Claims, verificar no documento
        const userDoc = await db.collection(COLLECTIONS.USERS).doc(userId).get();
        if (!userDoc.exists) {
          throw new AppError('not-found', 'User not found', 404);
        }
        userRole = userDoc.data()?.role || 'user';
      }

      // Apenas sellers/partners/providers podem criar produtos
      const allowedRoles = ['seller', 'partner', 'provider'];
      if (!allowedRoles.includes(userRole)) {
        throw new AppError(
          'permission-denied',
          `Only sellers, partners, and providers can create products. Current role: ${userRole}`,
          403,
        );
      }

      // Validar dados de entrada
      const {
        title,
        description,
        category,
        price,
        images = [],
        stock,
        active = true,
      } = data;

      if (!title || typeof title !== 'string' || title.trim().length === 0) {
        throw new AppError('invalid-argument', 'title is required and must be non-empty', 400);
      }

      if (!description || typeof description !== 'string' || description.trim().length === 0) {
        throw new AppError('invalid-argument', 'description is required and must be non-empty', 400);
      }

      if (!category || typeof category !== 'string' || category.trim().length === 0) {
        throw new AppError('invalid-argument', 'category is required and must be non-empty', 400);
      }

      if (!price || typeof price !== 'number' || price <= 0) {
        throw new AppError('invalid-argument', 'price is required and must be a positive number', 400);
      }

      if (!Array.isArray(images)) {
        throw new AppError('invalid-argument', 'images must be an array', 400);
      }

      if (stock !== undefined && (typeof stock !== 'number' || stock < 0)) {
        throw new AppError('invalid-argument', 'stock must be a non-negative number', 400);
      }

      // Validar que o usuário existe e tem permissão
      const userDoc = await db.collection(COLLECTIONS.USERS).doc(userId).get();
      if (!userDoc.exists) {
        throw new AppError('not-found', 'User not found', 404);
      }

      const userData = userDoc.data();
      const userDocRole = userData?.role;

      // Verificar consistência entre Custom Claims e documento
      if (userDocRole && !allowedRoles.includes(userDocRole)) {
        throw new AppError(
          'permission-denied',
          'User role does not allow creating products',
          403,
        );
      }

      // CRÍTICO: Obter localização do usuário para organizar por região
      const userLocation = await getUserLocation(db, userId);
      const {city, state} = userLocation;

      // 📍 LOCATION TRACE OBRIGATÓRIO - Rastreamento de localização
      const locationId = normalizeLocationId(city || 'unknown', state || 'unknown');
      const firestorePath = `locations/${locationId}/products`;
      
      functions.logger.info('📍 LOCATION TRACE', {
        function: 'createProduct',
        userId,
        city: city || 'unknown',
        state: state || 'unknown',
        locationId,
        firestorePath,
        rawCity: city || '',
        rawState: state || '',
        timestamp: new Date().toISOString(),
      });

      if (!city || !state) {
        functions.logger.warn(
          `User ${userId} does not have location information. ` +
          'Product will be saved in \'unknown\' location.'
        );
      }

      // Criar dados do produto
      const productData = {
        sellerId: userId,
        title: title.trim(),
        description: description.trim(),
        category: category.trim(),
        price,
        images: Array.isArray(images) ? images : [],
        stock: stock !== undefined ? stock : null,
        active: active === true,
        status: 'active', // Apenas produtos com status "active" são públicos
        city: city || '', // Adicionar cidade explicitamente
        state: state || '', // Adicionar estado explicitamente
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      };

      // CRÍTICO: Salvar na coleção pública por localização
      const locationProductsCollection = getLocationCollection(
        db,
        COLLECTIONS.PRODUCTS || 'products',
        city || 'unknown',
        state || 'unknown'
      );
      const productRef = await locationProductsCollection.add(productData);
      const productId = productRef.id;

      // 📍 PROOF: Logar path REAL onde o dado foi gravado
      functions.logger.info('📍 BACKEND WRITE PROOF', {
        function: 'createProduct',
        productId,
        actualFirestorePath: `locations/${locationId}/products/${productId}`,
        collectionId: locationProductsCollection.id,
        documentId: productId,
        timestamp: new Date().toISOString(),
      });

      // Também salvar na coleção global para compatibilidade (será removido futuramente)
      await db.collection(COLLECTIONS.PRODUCTS || 'products').doc(productId).set(productData);

      // Criar também na subcoleção do usuário (para organização - dados privados)
      await db
        .collection(COLLECTIONS.USERS)
        .doc(userId)
        .collection('products')
        .doc(productId)
        .set(productData);

      functions.logger.info(`Product created: ${productId}`, {
        productId,
        sellerId: userId,
        category,
        price,
        location: `${city || 'unknown'}, ${state || 'unknown'}`,
        locationCollection: `locations/${normalizeLocationId(city || 'unknown', state || 'unknown')}/products`,
        timestamp: new Date().toISOString(),
      });

      return {
        success: true,
        productId,
        message: 'Product created successfully',
      };
    } catch (error) {
      functions.logger.error('Error creating product:', error);
      throw handleError(error);
    }
  },
);

/**
 * Atualiza um produto existente
 * Apenas o dono do produto pode atualizar
 */
export const updateProduct = functions.https.onCall(
  async (data, context) => {
    try {
      validateAppCheck(context);
      assertAuthenticated(context);

      const userId = context.auth!.uid;
      const db = getFirestore();
      const {productId, updates} = data;

      if (!productId || typeof productId !== 'string') {
        throw new AppError('invalid-argument', 'productId is required', 400);
      }

      if (!updates || typeof updates !== 'object') {
        throw new AppError('invalid-argument', 'updates is required and must be an object', 400);
      }

      // Buscar produto
      const productDoc = await db.collection(COLLECTIONS.PRODUCTS || 'products').doc(productId).get();
      if (!productDoc.exists) {
        throw new AppError('not-found', 'Product not found', 404);
      }

      const productData = productDoc.data();

      // Verificar propriedade
      if (productData?.sellerId !== userId) {
        throw new AppError('permission-denied', 'Only product owner can update product', 403);
      }

      // Validar campos permitidos para atualização
      const allowedFields = ['title', 'description', 'category', 'price', 'images', 'stock', 'active', 'status'];
      const updateData: Record<string, unknown> = {
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      };

      for (const field of allowedFields) {
        if (updates[field] !== undefined) {
          // Validações específicas por campo
          if (field === 'title' && (typeof updates[field] !== 'string' || updates[field].trim().length === 0)) {
            throw new AppError('invalid-argument', 'title must be a non-empty string', 400);
          }
          if (field === 'description' && (typeof updates[field] !== 'string' || updates[field].trim().length === 0)) {
            throw new AppError('invalid-argument', 'description must be a non-empty string', 400);
          }
          if (field === 'category' && (typeof updates[field] !== 'string' || updates[field].trim().length === 0)) {
            throw new AppError('invalid-argument', 'category must be a non-empty string', 400);
          }
          if (field === 'price' && (typeof updates[field] !== 'number' || updates[field] <= 0)) {
            throw new AppError('invalid-argument', 'price must be a positive number', 400);
          }
          if (field === 'images' && !Array.isArray(updates[field])) {
            throw new AppError('invalid-argument', 'images must be an array', 400);
          }
          if (field === 'stock' && 
              updates[field] !== null && 
              (typeof updates[field] !== 'number' || updates[field] < 0)) {
            throw new AppError(
              'invalid-argument',
              'stock must be a non-negative number or null',
              400
            );
          }
          if (field === 'active' && typeof updates[field] !== 'boolean') {
            throw new AppError('invalid-argument', 'active must be a boolean', 400);
          }
          if (field === 'status' && updates[field] !== 'active' && updates[field] !== 'inactive') {
            throw new AppError('invalid-argument', 'status must be "active" or "inactive"', 400);
          }

          updateData[field] = updates[field];
        }
      }

      // Atualizar na coleção pública
      await db.collection(COLLECTIONS.PRODUCTS || 'products').doc(productId).update(updateData);

      // Atualizar na subcoleção do usuário
      await db
        .collection(COLLECTIONS.USERS)
        .doc(userId)
        .collection('products')
        .doc(productId)
        .update(updateData);

      functions.logger.info(`Product updated: ${productId}`, {
        productId,
        sellerId: userId,
        updatedFields: Object.keys(updateData),
        timestamp: new Date().toISOString(),
      });

      return {
        success: true,
        message: 'Product updated successfully',
      };
    } catch (error) {
      functions.logger.error('Error updating product:', error);
      throw handleError(error);
    }
  },
);

/**
 * Deleta um produto
 * Apenas o dono do produto pode deletar
 */
export const deleteProduct = functions.https.onCall(
  async (data, context) => {
    try {
      validateAppCheck(context);
      assertAuthenticated(context);

      const userId = context.auth!.uid;
      const db = getFirestore();
      const {productId} = data;

      if (!productId || typeof productId !== 'string') {
        throw new AppError('invalid-argument', 'productId is required', 400);
      }

      // Buscar produto
      const productDoc = await db.collection(COLLECTIONS.PRODUCTS || 'products').doc(productId).get();
      if (!productDoc.exists) {
        throw new AppError('not-found', 'Product not found', 404);
      }

      const productData = productDoc.data();

      // Verificar propriedade
      if (productData?.sellerId !== userId) {
        throw new AppError('permission-denied', 'Only product owner can delete product', 403);
      }

      // Deletar da coleção pública
      await db.collection(COLLECTIONS.PRODUCTS || 'products').doc(productId).delete();

      // Deletar da subcoleção do usuário
      await db
        .collection(COLLECTIONS.USERS)
        .doc(userId)
        .collection('products')
        .doc(productId)
        .delete();

      functions.logger.info(`Product deleted: ${productId}`, {
        productId,
        sellerId: userId,
        timestamp: new Date().toISOString(),
      });

      return {
        success: true,
        message: 'Product deleted successfully',
      };
    } catch (error) {
      functions.logger.error('Error deleting product:', error);
      throw handleError(error);
    }
  },
);
