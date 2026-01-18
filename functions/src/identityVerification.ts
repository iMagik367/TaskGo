import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
import {getFirestore} from './utils/firestore';
import {validateAppCheck} from './security/appCheck';
import {assertAuthenticated, handleError} from './utils/errors';

/**
 * Cloud Function para verificar identidade do usuário
 * Analisa documentos enviados e atualiza status de verificação
 */
export const verifyIdentity = functions.https.onCall(async (data, context) => {
  try {
    // Validar App Check
    validateAppCheck(context);
    
    // Verificar autenticação
    assertAuthenticated(context);

    const userId = context.auth!.uid;
    const { documentFront, documentBack, selfie, addressProof } = data;

    // Validar que todos os documentos obrigatórios foram enviados
    if (!documentFront || !documentBack || !selfie) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'Documentos obrigatórios não fornecidos'
      );
    }

    // Atualizar status de verificação no Firestore
    const userRef = getFirestore().collection('users').doc(userId);
    
    await userRef.update({
      documentFront,
      documentBack,
      selfie,
      addressProof: addressProof || null,
      verified: false, // Será aprovado manualmente por admin
      verifiedAt: null,
      verifiedBy: null,
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    functions.logger.info(`Identity verification submitted for user ${userId}`, {
      userId,
      timestamp: new Date().toISOString(),
    });

    return {
      success: true,
      message: 'Documentos enviados com sucesso. Aguardando verificação.'
    };
  } catch (error) {
    functions.logger.error('Error verifying identity:', error);
    throw handleError(error);
  }
});

/**
 * Cloud Function para aprovar/rejeitar verificação de identidade (apenas admin)
 */
export const approveIdentityVerification = functions.https.onCall(async (data, context) => {
  try {
    // Validar App Check
    validateAppCheck(context);
    
    // Verificar autenticação e permissão de admin
    assertAuthenticated(context);
    
    // Verificar se é admin (usar Custom Claims ou documento como fallback)
    const isAdmin = context.auth!.token.role === 'admin';
    if (!isAdmin) {
      // Fallback para documento do Firestore
      const adminUser = await getFirestore()
        .collection('users')
        .doc(context.auth!.uid)
        .get();

      if (adminUser.data()?.role !== 'admin') {
        throw new functions.https.HttpsError(
          'permission-denied',
          'Apenas administradores podem aprovar verificações'
        );
      }
    }

    const { userId, approved, reason } = data;

    const userRef = getFirestore().collection('users').doc(userId);
    
    if (approved) {
      await userRef.update({
        verified: true,
        verifiedAt: admin.firestore.FieldValue.serverTimestamp(),
        verifiedBy: context.auth!.uid,
        updatedAt: admin.firestore.FieldValue.serverTimestamp()
      });
    } else {
      await userRef.update({
        verified: false,
        verifiedAt: null,
        verifiedBy: null,
        verificationRejectedReason: reason || 'Documentos não aprovados',
        updatedAt: admin.firestore.FieldValue.serverTimestamp()
      });
    }

    functions.logger.info(`Identity verification ${approved ? 'approved' : 'rejected'} for user ${userId}`, {
      adminId: context.auth!.uid,
      targetUserId: userId,
      approved,
      timestamp: new Date().toISOString(),
    });

    return {
      success: true,
      message: approved ? 'Verificação aprovada' : 'Verificação rejeitada'
    };
  } catch (error) {
    functions.logger.error('Error approving identity verification:', error);
    throw handleError(error);
  }
});


