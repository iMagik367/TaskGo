import * as admin from 'firebase-admin';
import * as functions from 'firebase-functions';
import {validateAppCheck} from './security/appCheck';
import {getFirestore} from './utils/firestore';

/**
 * Triggered when a new user is created in Firebase Auth
 * Creates corresponding user document in Firestore
 * IMPORTANTE: Usa merge para não sobrescrever campos já definidos pelo app (como role)
 */
export const onUserCreate = functions.auth.user().onCreate(async (user) => {
  const db = getFirestore();
  
  try {
    const userRef = db.collection('users').doc(user.uid);
    
    // Verificar se o documento já existe (pode ter sido criado pelo app antes da função executar)
    const userDoc = await userRef.get();
    
    if (userDoc.exists) {
      // Documento já existe - fazer merge apenas dos campos básicos que podem estar faltando
      // CRÍTICO: NÃO sobrescrever role, pendingAccountType ou outros campos já definidos pelo app
      const existingData = userDoc.data();
      const updateData: { [key: string]: unknown } = {
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      };
      
      // Só atualizar campos que não existem ou são null
      if (!existingData?.email && user.email) {
        updateData.email = user.email;
      }
      if (!existingData?.displayName && user.displayName) {
        updateData.displayName = user.displayName;
      }
      if (!existingData?.photoURL && user.photoURL) {
        updateData.photoURL = user.photoURL;
      }
      if (!existingData?.createdAt) {
        updateData.createdAt = admin.firestore.FieldValue.serverTimestamp();
      }
      if (existingData?.profileComplete === undefined) {
        updateData.profileComplete = false;
      }
      if (existingData?.verified === undefined) {
        updateData.verified = false;
      }
      
      // CRÍTICO: NÃO atualizar role ou pendingAccountType - deixar o que já foi definido pelo app
      // Se o usuário já tem role definido (não é "client" padrão) ou pendingAccountType == false,
      // significa que já passou pelo primeiro login e selecionou o tipo de conta
      const hasDefinedRole = existingData?.role && existingData.role !== 'client';
      const isAccountTypeDefined = existingData?.pendingAccountType === false;
      
      if (hasDefinedRole || isAccountTypeDefined) {
        const roleInfo = existingData?.role || 'undefined';
        const pendingInfo = existingData?.pendingAccountType;
        functions.logger.info(
          'User ' + user.uid + ' already has account type defined ' +
          '(role: ' + roleInfo + ', pendingAccountType: ' + pendingInfo + '). ' +
          'Skipping update to preserve user choice.'
        );
        return null;
      }
      
      if (Object.keys(updateData).length > 1) { // Mais que apenas updatedAt
        await userRef.update(updateData);
        const rolePreserved = existingData?.role || 'undefined';
        const pendingPreserved = existingData?.pendingAccountType;
        functions.logger.info(
          'User document merged for ' + user.uid + ', ' +
          'role preserved: ' + rolePreserved + ', ' +
          'pendingAccountType preserved: ' + pendingPreserved
        );
      }
    } else {
      // Documento não existe - criar com flag pendingAccountType para indicar que o app precisa mostrar dialog
      // O app vai chamar setInitialUserRole para definir o role correto (incluindo Custom Claims)
      const userData = {
        uid: user.uid,
        email: user.email,
        displayName: user.displayName,
        photoURL: user.photoURL,
        role: 'user', // Default role - será atualizado por setInitialUserRole quando o usuário selecionar tipo de conta
        pendingAccountType: true, // Flag para indicar que o app precisa mostrar dialog de seleção
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        profileComplete: false,
        verified: false,
      };

      await userRef.set(userData, { merge: true });
      
      // Definir Custom Claim padrão como "user" (será atualizado por setInitialUserRole se necessário)
      await admin.auth().setCustomUserClaims(user.uid, {
        role: 'user',
      });
      
      functions.logger.info(
        `User document created for ${user.uid} with pendingAccountType flag and default Custom Claim role=user`
      );
    }
    
    return null;
  } catch (error) {
    functions.logger.error('Error creating user document:', error);
    throw error;
  }
});

/**
 * Triggered when a user is deleted from Firebase Auth
 * Removes user data from Firestore
 */
export const onUserDelete = functions.auth.user().onDelete(async (user) => {
  const db = getFirestore();
  const batch = db.batch();
  
  try {
    // Delete user document
    const userRef = db.collection('users').doc(user.uid);
    batch.delete(userRef);

    // Delete user's orders (soft delete by updating status)
    const ordersSnapshot = await db.collection('orders')
      .where('clientId', '==', user.uid)
      .get();
    
    ordersSnapshot.forEach((doc) => {
      batch.update(doc.ref, {
        status: 'cancelled',
        deleted: true,
        deletedAt: admin.firestore.FieldValue.serverTimestamp(),
      });
    });

    await batch.commit();
    functions.logger.info(`User data deleted for ${user.uid}`);
    return null;
  } catch (error) {
    functions.logger.error('Error deleting user data:', error);
    throw error;
  }
});

/**
 * Function to promote user to provider
 */
export const promoteToProvider = functions.https.onCall(async (data, context) => {
  const db = getFirestore();
  
  try {
    validateAppCheck(context);
    
    if (!context.auth) {
      throw new functions.https.HttpsError(
        'unauthenticated',
        'User must be authenticated'
      );
    }

    await db.collection('users').doc(context.auth.uid).update({
      role: 'provider',
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    functions.logger.info(`User ${context.auth.uid} promoted to provider`);
    return {success: true};
  } catch (error) {
    functions.logger.error('Error promoting user to provider:', error);
    if (error instanceof functions.https.HttpsError) {
      throw error;
    }
    throw new functions.https.HttpsError(
      'internal',
      'Failed to promote user'
    );
  }
});

/**
 * Busca email do usuário por CPF ou CNPJ para login de parceiros
 * Permite busca sem autenticação (para login), mas valida App Check
 */
export const getUserEmailByDocument = functions.https.onCall(async (data, context) => {
  try {
    // Validar App Check (sem autenticação - necessário para login)
    validateAppCheck(context);
    
    const {document} = data;
    
    if (!document || typeof document !== 'string') {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'Document (CPF/CNPJ) is required'
      );
    }
    
    // Remover formatação do documento
    const cleanDocument = document.replace(/[^0-9]/g, '');
    
    if (cleanDocument.length !== 11 && cleanDocument.length !== 14) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'Invalid document format. Must be CPF (11 digits) or CNPJ (14 digits)'
      );
    }
    
    const db = getFirestore();
    
    // Buscar por CPF
    if (cleanDocument.length === 11) {
      const cpfQuery = await db.collection('users')
        .where('cpf', '==', cleanDocument)
        .limit(1)
        .get();
      
      if (!cpfQuery.empty) {
        const userData = cpfQuery.docs[0].data();
        const role = userData?.role?.toLowerCase() || '';
        
        // Verificar se é parceiro
        if (role !== 'partner' && role !== 'provider') {
          throw new functions.https.HttpsError(
            'failed-precondition',
            'Este CPF/CNPJ não está cadastrado como parceiro'
          );
        }
        
        functions.logger.info(`Email found for CPF: ${cleanDocument}`);
        return {
          email: userData.email,
          role: userData.role,
          uid: cpfQuery.docs[0].id
        };
      }
    }
    
    // Buscar por CNPJ
    if (cleanDocument.length === 14) {
      const cnpjQuery = await db.collection('users')
        .where('cnpj', '==', cleanDocument)
        .limit(1)
        .get();
      
      if (!cnpjQuery.empty) {
        const userData = cnpjQuery.docs[0].data();
        const role = userData?.role?.toLowerCase() || '';
        
        // Verificar se é parceiro
        if (role !== 'partner' && role !== 'provider') {
          throw new functions.https.HttpsError(
            'failed-precondition',
            'Este CPF/CNPJ não está cadastrado como parceiro'
          );
        }
        
        functions.logger.info(`Email found for CNPJ: ${cleanDocument}`);
        return {
          email: userData.email,
          role: userData.role,
          uid: cnpjQuery.docs[0].id
        };
      }
    }
    
    // Não encontrado
    throw new functions.https.HttpsError(
      'not-found',
      'CPF/CNPJ não encontrado. Verifique se você já possui cadastro.'
    );
  } catch (error) {
    if (error instanceof functions.https.HttpsError) {
      throw error;
    }
    functions.logger.error('Error getting user email by document:', error);
    throw new functions.https.HttpsError(
      'internal',
      'Erro ao buscar usuário. Tente novamente.'
    );
  }
});

/**
 * Function to approve provider documents
 */
export const approveProviderDocuments = functions.https.onCall(async (data, context) => {
  const db = getFirestore();
  
  try {
    validateAppCheck(context);
    
    if (!context.auth) {
      throw new functions.https.HttpsError('unauthenticated', 'User must be authenticated');
    }

    // Check if user is admin
    const adminDoc = await db.collection('users').doc(context.auth.uid).get();
    if (!adminDoc.exists || adminDoc.data()?.role !== 'admin') {
      throw new functions.https.HttpsError('permission-denied', 'Admin access required');
    }

    const {providerId, documents} = data;

    if (!providerId || !documents) {
      throw new functions.https.HttpsError(
        'invalid-argument',
        'Provider ID and documents are required'
      );
    }
    await db.collection('users').doc(providerId).update({
      documents: documents,
      documentsApproved: true,
      documentsApprovedAt: admin.firestore.FieldValue.serverTimestamp(),
      documentsApprovedBy: context.auth.uid,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    // Send notification to provider
    await db.collection('notifications').add({
      userId: providerId,
      type: 'system_alert',
      title: 'Documents Approved',
      message: 'Your documents have been approved. You can now accept orders!',
      read: false,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    functions.logger.info(`Documents approved for provider ${providerId}`);
    return {success: true};
  } catch (error) {
    functions.logger.error('Error approving documents:', error);
    if (error instanceof functions.https.HttpsError) {
      throw error;
    }
    throw new functions.https.HttpsError('internal', 'Failed to approve documents');
  }
});
