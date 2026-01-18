import * as admin from 'firebase-admin';
import {getFirestore} from '../utils/firestore';
import * as functions from 'firebase-functions';
import {AppError, handleError, assertAuthenticated} from '../utils/errors';
import {validateAppCheck} from '../security/appCheck';
import {getUserRole} from '../security/roles';
import {COLLECTIONS} from '../utils/constants';

/**
 * Cria um novo serviço
 * Apenas usuários com role "provider" ou "partner" podem criar serviços
 * Cloud Function é a autoridade - valida permissões e dados
 */
export const createService = functions.https.onCall(
  async (data, context) => {
    try {
      // Validar App Check
      validateAppCheck(context);

      // Verificar autenticação
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

      // Apenas providers/partners podem criar serviços
      const allowedRoles = ['provider', 'partner'];
      if (!allowedRoles.includes(userRole)) {
        throw new AppError(
          'permission-denied',
          `Only providers and partners can create services. Current role: ${userRole}`,
          403,
        );
      }

      // Validar dados de entrada
      const {
        title,
        description,
        category,
        price,
        latitude,
        longitude,
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

      if (price !== undefined && (typeof price !== 'number' || price < 0)) {
        throw new AppError('invalid-argument', 'price must be a non-negative number', 400);
      }

      // Validar que o usuário existe e tem permissão
      const userDoc = await db.collection(COLLECTIONS.USERS).doc(userId).get();
      if (!userDoc.exists) {
        throw new AppError('not-found', 'User not found', 404);
      }

      const userData = userDoc.data();
      const userDocRole = userData?.role;

      // Verificar consistência entre Custom Claims e documento
      // Em produção, Custom Claims são a autoridade
      if (userDocRole && !allowedRoles.includes(userDocRole)) {
        throw new AppError(
          'permission-denied',
          'User role does not allow creating services',
          403,
        );
      }

      // Criar dados do serviço
      const serviceData = {
        providerId: userId,
        title: title.trim(),
        description: description.trim(),
        category: category.trim(),
        price: price || null,
        latitude: latitude || null,
        longitude: longitude || null,
        active: active === true,
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      };

      // Criar serviço na coleção pública (para queries eficientes)
      const serviceRef = await db.collection(COLLECTIONS.SERVICES).add(serviceData);
      const serviceId = serviceRef.id;

      // Criar também na subcoleção do usuário (para organização)
      await db
        .collection(COLLECTIONS.USERS)
        .doc(userId)
        .collection('services')
        .doc(serviceId)
        .set(serviceData);

      functions.logger.info(`Service created: ${serviceId}`, {
        serviceId,
        providerId: userId,
        category,
        timestamp: new Date().toISOString(),
      });

      return {
        success: true,
        serviceId,
        message: 'Service created successfully',
      };
    } catch (error) {
      functions.logger.error('Error creating service:', error);
      throw handleError(error);
    }
  },
);

/**
 * Atualiza um serviço existente
 * Apenas o dono do serviço pode atualizar
 */
export const updateService = functions.https.onCall(
  async (data, context) => {
    try {
      validateAppCheck(context);
      assertAuthenticated(context);

      const userId = context.auth!.uid;
      const db = getFirestore();
      const {serviceId, updates} = data;

      if (!serviceId || typeof serviceId !== 'string') {
        throw new AppError('invalid-argument', 'serviceId is required', 400);
      }

      if (!updates || typeof updates !== 'object') {
        throw new AppError('invalid-argument', 'updates is required and must be an object', 400);
      }

      // Buscar serviço
      const serviceDoc = await db.collection(COLLECTIONS.SERVICES).doc(serviceId).get();
      if (!serviceDoc.exists) {
        throw new AppError('not-found', 'Service not found', 404);
      }

      const serviceData = serviceDoc.data();

      // Verificar propriedade
      if (serviceData?.providerId !== userId) {
        throw new AppError('permission-denied', 'Only service owner can update service', 403);
      }

      // Validar campos permitidos para atualização
      const allowedFields = ['title', 'description', 'category', 'price', 'latitude', 'longitude', 'active'];
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
          if (field === 'price' && 
              updates[field] !== null && 
              (typeof updates[field] !== 'number' || updates[field] < 0)) {
            throw new AppError(
              'invalid-argument',
              'price must be a non-negative number or null',
              400
            );
          }
          if (field === 'active' && typeof updates[field] !== 'boolean') {
            throw new AppError('invalid-argument', 'active must be a boolean', 400);
          }

          updateData[field] = updates[field];
        }
      }

      // Atualizar na coleção pública
      await db.collection(COLLECTIONS.SERVICES).doc(serviceId).update(updateData);

      // Atualizar na subcoleção do usuário
      await db
        .collection(COLLECTIONS.USERS)
        .doc(userId)
        .collection('services')
        .doc(serviceId)
        .update(updateData);

      functions.logger.info(`Service updated: ${serviceId}`, {
        serviceId,
        providerId: userId,
        updatedFields: Object.keys(updateData),
        timestamp: new Date().toISOString(),
      });

      return {
        success: true,
        message: 'Service updated successfully',
      };
    } catch (error) {
      functions.logger.error('Error updating service:', error);
      throw handleError(error);
    }
  },
);

/**
 * Deleta um serviço
 * Apenas o dono do serviço pode deletar
 */
export const deleteService = functions.https.onCall(
  async (data, context) => {
    try {
      validateAppCheck(context);
      assertAuthenticated(context);

      const userId = context.auth!.uid;
      const db = getFirestore();
      const {serviceId} = data;

      if (!serviceId || typeof serviceId !== 'string') {
        throw new AppError('invalid-argument', 'serviceId is required', 400);
      }

      // Buscar serviço
      const serviceDoc = await db.collection(COLLECTIONS.SERVICES).doc(serviceId).get();
      if (!serviceDoc.exists) {
        throw new AppError('not-found', 'Service not found', 404);
      }

      const serviceData = serviceDoc.data();

      // Verificar propriedade
      if (serviceData?.providerId !== userId) {
        throw new AppError('permission-denied', 'Only service owner can delete service', 403);
      }

      // Deletar da coleção pública
      await db.collection(COLLECTIONS.SERVICES).doc(serviceId).delete();

      // Deletar da subcoleção do usuário
      await db
        .collection(COLLECTIONS.USERS)
        .doc(userId)
        .collection('services')
        .doc(serviceId)
        .delete();

      functions.logger.info(`Service deleted: ${serviceId}`, {
        serviceId,
        providerId: userId,
        timestamp: new Date().toISOString(),
      });

      return {
        success: true,
        message: 'Service deleted successfully',
      };
    } catch (error) {
      functions.logger.error('Error deleting service:', error);
      throw handleError(error);
    }
  },
);
