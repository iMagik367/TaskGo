import * as admin from 'firebase-admin';
import {getFirestore} from '../utils/firestore';
import * as functions from 'firebase-functions';
import {AppError, handleError, assertAuthenticated} from '../utils/errors';
import {validateAppCheck} from '../security/appCheck';
import {normalizeLocationId} from '../utils/location';

/**
 * Define o role inicial do usuário após cadastro
 * Esta função é chamada quando o usuário seleciona o tipo de conta (client/partner)
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

      // Validar role - apenas "partner" ou "client" são aceitos
      const validRoles = ['partner', 'client'];
      
      if (!validRoles.includes(role)) {
        throw new AppError(
          'invalid-argument',
          `Invalid role: ${role}. Must be one of: ${validRoles.join(', ')}`,
          400,
        );
      }

      const finalRole = role;

      // Verificar se o usuário já tem role definido
      const userDoc = await db.collection('users').doc(userId).get();
      if (!userDoc.exists) {
        throw new AppError('not-found', 'User document not found', 404);
      }

      const userData = userDoc.data();
      const existingRole = userData?.role;

      // CRÍTICO: Permitir mudança de 'user'/'client' para 'partner'
      // Bloquear apenas se já tiver um role definitivo diferente de 'user'/'client'
      const defaultRoles = ['user', 'client'];
      const isDefaultRole = existingRole && defaultRoles.includes(existingRole);
      
      if (existingRole && !isDefaultRole) {
        // Se já tem role definitivo (partner, admin, etc), não permitir mudança
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

      // CRÍTICO: Permitir mudança de 'user'/'client' para 'partner'
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
      const updateData = {
        role: finalRole,
        pendingAccountType: false, // Remover flag de pendência
        roleSetAt: admin.firestore.FieldValue.serverTimestamp(),
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      };
      
      await db.collection('users').doc(userId).update(updateData);
      
      // CRÍTICO: Atualizar também em locations/{locationId}/users/{userId} e
      // users/{locationId}/users/{userId} se tiver city/state
      const userDataForLocation = userDoc.data();
      const userCity = (userDataForLocation as Record<string, unknown>)?.city;
      const userState = (userDataForLocation as Record<string, unknown>)?.state;
      
      if (userCity && userState && typeof userCity === 'string' && 
          typeof userState === 'string' && 
          userCity.trim() !== '' && userState.trim() !== '') {
        try {
          // CRÍTICO: Usar normalizeLocationId do utils/location para garantir normalização correta
          const locationId = normalizeLocationId(userCity, userState);
          
          // Atualizar em locations/{locationId}/users/{userId} (pública)
          const locationUsersRef = db.collection('locations').doc(locationId).collection('users').doc(userId);
          const locationUserDoc = await locationUsersRef.get();
          if (locationUserDoc.exists) {
            await locationUsersRef.update(updateData);
            functions.logger.info(
              `✅ Role updated in locations/${locationId}/users/${userId}`
            );
          } else {
            // Se não existe, criar com todos os dados do usuário + updateData
            const fullUserData = {...userData, ...updateData};
            await locationUsersRef.set(fullUserData, { merge: true });
            functions.logger.info(
              `✅ User document created in locations/${locationId}/users/${userId} with role update`
            );
          }
          
          // Atualizar também em users/{locationId}/users/{userId} (privada)
          const privateUsersRef = db.collection('users').doc(locationId).collection('users').doc(userId);
          const privateUserDoc = await privateUsersRef.get();
          if (privateUserDoc.exists) {
            await privateUsersRef.update(updateData);
            functions.logger.info(
              `✅ Role updated in users/${locationId}/users/${userId}`
            );
          } else {
            // Se não existe, criar com todos os dados do usuário + updateData
            const fullUserData = {...userData, ...updateData};
            await privateUsersRef.set(fullUserData, { merge: true });
            functions.logger.info(
              `✅ User document created in users/${locationId}/users/${userId} with role update`
            );
          }
        } catch (e) {
          functions.logger.error(
            `❌ ERRO CRÍTICO: Failed to update role in locations/users collections: ${e}`,
            {userId, city: userCity, state: userState, error: e}
          );
          // Continuar mesmo se falhar - pelo menos atualizou na coleção global
        }
      } else {
        functions.logger.warn(
          `User ${userId} does not have city/state - cannot save in locations/users collections`
        );
      }

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
