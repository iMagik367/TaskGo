import * as admin from 'firebase-admin';
import {getFirestore} from '../utils/firestore';
import * as functions from 'firebase-functions';
import {AppError, handleError, assertAuthenticated} from '../utils/errors';
import {validateAppCheck} from '../security/appCheck';

/**
 * Define o role inicial do usuário após cadastro
 * Esta função é chamada quando o usuário seleciona o tipo de conta (client/provider/seller)
 * 
 * IMPORTANTE:
 * - Define Custom Claims no Firebase Auth (autoridade única)
 * - Sincroniza role no documento do Firestore (apenas para referência)
 * - Firestore Rules devem usar request.auth.token.role (Custom Claims)
 */
export const setInitialUserRole = functions.https.onCall(
  async (data, context) => {
    try {
      validateAppCheck(context);
      assertAuthenticated(context);

      const userId = context.auth!.uid;
      const db = getFirestore();
      const {role, accountType} = data;

      // Validar parâmetros
      if (!role || typeof role !== 'string') {
        throw new AppError('invalid-argument', 'role is required and must be a string', 400);
      }

      // Mapear accountType legado para roles novos
      // accountType: "client" | "provider" | "seller"
      // role: "user" | "admin" | "moderator"
      // Mas também suportamos roles legados: "provider", "seller", "partner"
      const validRoles = ['user', 'admin', 'moderator', 'provider', 'seller', 'partner', 'client'];
      
      // Role "client" é mapeado para "user"
      // Role "provider" e "seller" são mantidos para compatibilidade
      let finalRole = role;

      if (role === 'client') {
        finalRole = 'user';
      }

      // Validar role final
      if (!validRoles.includes(finalRole)) {
        throw new AppError(
          'invalid-argument',
          `Invalid role: ${role}. Must be one of: ${validRoles.join(', ')}`,
          400,
        );
      }

      // Verificar se o usuário já tem role definido
      const userDoc = await db.collection('users').doc(userId).get();
      if (!userDoc.exists) {
        throw new AppError('not-found', 'User document not found', 404);
      }

      const userData = userDoc.data();
      const existingRole = userData?.role;

      // CRÍTICO: Permitir mudança de 'user'/'client' para 'partner'/'provider'/'seller'
      // Bloquear apenas se já tiver um role definitivo diferente de 'user'/'client'
      const defaultRoles = ['user', 'client'];
      const isDefaultRole = existingRole && defaultRoles.includes(existingRole);
      
      if (existingRole && !isDefaultRole) {
        // Se já tem role definitivo (partner, provider, seller, admin, etc), não permitir mudança
        // Apenas admins podem mudar roles após definição inicial
        functions.logger.warn(
          `User ${userId} already has definitive role: ${existingRole}. ` +
          `Attempted to set: ${finalRole}. Only admins can change roles.`
        );
        throw new AppError(
          'failed-precondition',
          `User already has role: ${existingRole}. Only admins can change roles.`,
          400,
        );
      }

      // Verificar se já tem Custom Claims
      const userRecord = await admin.auth().getUser(userId);
      const existingCustomClaims = userRecord.customClaims || {};
      const existingCustomClaimsRole = existingCustomClaims.role;

      // CRÍTICO: Permitir mudança de 'user'/'client' para outros roles
      // Bloquear apenas se já tiver um role definitivo diferente de 'user'/'client'
      const isDefaultCustomClaim = existingCustomClaimsRole && 
                                   defaultRoles.includes(existingCustomClaimsRole);
      
      if (existingCustomClaimsRole && !isDefaultCustomClaim) {
        // Se já tem Custom Claim definitivo, não permitir mudança
        functions.logger.warn(
          `User ${userId} already has definitive Custom Claim role: ${existingCustomClaimsRole}. ` +
          `Attempted to set: ${finalRole}. Only admins can change roles.`
        );
        throw new AppError(
          'failed-precondition',
          `User already has Custom Claim role: ${existingCustomClaimsRole}. Only admins can change roles.`,
          400,
        );
      }
      
      functions.logger.info(
        `Setting role for user ${userId}: existingRole=${existingRole}, ` +
        `existingCustomClaim=${existingCustomClaimsRole}, finalRole=${finalRole}`
      );

      // Definir Custom Claims no Firebase Auth (autoridade única)
      await admin.auth().setCustomUserClaims(userId, {
        ...existingCustomClaims,
        role: finalRole,
      });

      // Sincronizar role no documento do Firestore (apenas para referência/compatibilidade)
      // IMPORTANTE: Firestore Rules devem usar request.auth.token.role (Custom Claims)
      await db.collection('users').doc(userId).update({
        role: finalRole,
        pendingAccountType: false, // Remover flag de pendência
        roleSetAt: admin.firestore.FieldValue.serverTimestamp(),
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });

      functions.logger.info(`Initial role ${finalRole} set for user ${userId}`, {
        userId,
        role: finalRole,
        accountType: accountType || null,
        timestamp: new Date().toISOString(),
      });

      return {
        success: true,
        role: finalRole,
        message: `Role ${finalRole} set successfully`,
      };
    } catch (error) {
      functions.logger.error('Error setting initial user role:', error);
      throw handleError(error);
    }
  },
);
