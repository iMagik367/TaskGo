import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';

/**
 * Cloud Function para verificar identidade do usuário
 * Analisa documentos enviados e atualiza status de verificação
 */
export const verifyIdentity = functions.https.onCall(async (data, context) => {
  // Verificar autenticação
  if (!context.auth) {
    throw new functions.https.HttpsError(
      'unauthenticated',
      'Usuário não autenticado'
    );
  }

  const userId = context.auth.uid;
  const { documentFront, documentBack, selfie, addressProof } = data;

  try {
    // Validar que todos os documentos obrigatórios foram enviados
    if (!documentFront || !documentBack || !selfie) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'Documentos obrigatórios não fornecidos'
      );
    }

    // Atualizar status de verificação no Firestore
    const userRef = admin.firestore().collection('users').doc(userId);
    
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

    // TODO: Integrar com serviço de verificação de documentos (ex: Serpro, Serasa)
    // Por enquanto, apenas salva os documentos

    return {
      success: true,
      message: 'Documentos enviados com sucesso. Aguardando verificação.'
    };
  } catch (error) {
    console.error('Erro ao verificar identidade:', error);
    const message = error instanceof Error ? error.message : 'Erro desconhecido';
    throw new functions.https.HttpsError(
      'internal',
      'Erro ao processar verificação de identidade',
      message
    );
  }
});

/**
 * Cloud Function para aprovar/rejeitar verificação de identidade (apenas admin)
 */
export const approveIdentityVerification = functions.https.onCall(async (data, context) => {
  // Verificar autenticação
  if (!context.auth) {
    throw new functions.https.HttpsError(
      'unauthenticated',
      'Usuário não autenticado'
    );
  }

  // Verificar se é admin
  const adminUser = await admin.firestore()
    .collection('users')
    .doc(context.auth.uid)
    .get();

  if (adminUser.data()?.role !== 'admin') {
    throw new functions.https.HttpsError(
      'permission-denied',
      'Apenas administradores podem aprovar verificações'
    );
  }

  const { userId, approved, reason } = data;

  try {
    const userRef = admin.firestore().collection('users').doc(userId);
    
    if (approved) {
      await userRef.update({
        verified: true,
        verifiedAt: admin.firestore.FieldValue.serverTimestamp(),
        verifiedBy: context.auth.uid,
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

    return {
      success: true,
      message: approved ? 'Verificação aprovada' : 'Verificação rejeitada'
    };
  } catch (error) {
    console.error('Erro ao aprovar verificação:', error);
    const message = error instanceof Error ? error.message : 'Erro desconhecido';
    throw new functions.https.HttpsError(
      'internal',
      'Erro ao processar aprovação',
      message
    );
  }
});


