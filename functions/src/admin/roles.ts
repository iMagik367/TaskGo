import * as admin from 'firebase-admin';
import * as functions from 'firebase-functions';
import {AppError, handleError} from '../utils/errors';
import {assertAdmin, isValidRole, UserRole} from '../security/roles';
import {validateAppCheck} from '../security/appCheck';

/**
 * Define o role de um usuário usando Custom Claims
 * Somente admins podem atribuir roles
 * 
 * Custom Claims são a autoridade única para permissões
 * e são incluídas no token JWT do Firebase Auth
 */
export const setUserRole = functions.https.onCall(
  async (data, context) => {
    try {
      // Validar App Check
      validateAppCheck(context);

      // Verificar autenticação e permissão de admin
      assertAdmin(context);

      const {userId, role} = data;

      // Validar parâmetros
      if (!userId || typeof userId !== 'string') {
        throw new AppError(
          'invalid-argument',
          'userId is required and must be a string',
          400,
        );
      }

      if (!role || typeof role !== 'string') {
        throw new AppError(
          'invalid-argument',
          'role is required and must be a string',
          400,
        );
      }

      if (!isValidRole(role)) {
        throw new AppError(
          'invalid-argument',
          `Invalid role: ${role}. Must be one of: user, admin, moderator`,
          400,
        );
      }

      // Verificar se o usuário existe
      const userRecord = await admin.auth().getUser(userId);
      if (!userRecord) {
        throw new AppError('not-found', 'User not found', 404);
      }

      // Obter Custom Claims atuais
      const currentCustomClaims = userRecord.customClaims || {};

      // Atualizar Custom Claims com o novo role
      await admin.auth().setCustomUserClaims(userId, {
        ...currentCustomClaims,
        role: role,
      });

      // Sincronizar role no documento do Firestore (apenas para compatibilidade/referência)
      // IMPORTANTE: Firestore Rules devem usar request.auth.token.role (Custom Claims)
      // e NÃO confiar neste campo do documento
      const db = admin.firestore();
      await db.collection('users').doc(userId).update({
        role: role,
        roleUpdatedAt: admin.firestore.FieldValue.serverTimestamp(),
        roleUpdatedBy: context.auth!.uid,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });

      functions.logger.info(`Role ${role} set for user ${userId} by admin ${context.auth!.uid}`, {
        adminId: context.auth!.uid,
        targetUserId: userId,
        role: role,
        timestamp: new Date().toISOString(),
      });

      return {
        success: true,
        userId,
        role,
        message: `Role ${role} successfully set for user ${userId}`,
      };
    } catch (error) {
      functions.logger.error('Error setting user role:', error);
      throw handleError(error);
    }
  },
);

/**
 * Obtém o role atual de um usuário (através de Custom Claims)
 */
export const getUserRoleInfo = functions.https.onCall(
  async (data, context) => {
    try {
      validateAppCheck(context);

      // Apenas admins podem ver roles de outros usuários
      // Usuários podem ver seu próprio role
      if (!context.auth) {
        throw new AppError('unauthenticated', 'User must be authenticated', 401);
      }

      const {userId} = data;
      const targetUserId = userId || context.auth.uid;

      // Verificar permissão
      const isAdmin = context.auth.token.role === 'admin';
      const isOwnUser = targetUserId === context.auth.uid;

      if (!isAdmin && !isOwnUser) {
        throw new AppError(
          'permission-denied',
          'Cannot view role of other users',
          403,
        );
      }

      const userRecord = await admin.auth().getUser(targetUserId);
      const customClaims = userRecord.customClaims || {};
      const role = customClaims.role || 'user';

      return {
        userId: targetUserId,
        role: role,
        hasCustomClaims: !!customClaims.role,
      };
    } catch (error) {
      functions.logger.error('Error getting user role:', error);
      throw handleError(error);
    }
  },
);

/**
 * Lista todos os usuários com seus roles (apenas para admins)
 */
export const listUsersWithRoles = functions.https.onCall(
  async (data, context) => {
    try {
      validateAppCheck(context);
      assertAdmin(context);

      const {maxResults = 1000} = data;

      const listUsersResult = await admin.auth().listUsers(maxResults);
      const users = listUsersResult.users.map((user) => ({
        uid: user.uid,
        email: user.email,
        role: (user.customClaims?.role as UserRole) || 'user',
        createdAt: user.metadata.creationTime,
      }));

      functions.logger.info(`Listed ${users.length} users with roles`, {
        adminId: context.auth!.uid,
        count: users.length,
        timestamp: new Date().toISOString(),
      });

      return {
        users,
        total: users.length,
      };
    } catch (error) {
      functions.logger.error('Error listing users with roles:', error);
      throw handleError(error);
    }
  },
);
