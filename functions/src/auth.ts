import * as admin from 'firebase-admin';
import * as functions from 'firebase-functions';
import {validateAppCheck} from './security/appCheck';
import {getFirestore} from './utils/firestore';
import {ordersPath, getUserLocationId} from './utils/firestorePaths';

/**
 * Triggered when a new user is created in Firebase Auth
 * Creates corresponding user document in Firestore
 * IMPORTANTE: Usa merge para não sobrescrever campos já definidos pelo app (como role)
 */
export const onUserCreate = functions.auth.user().onCreate(async (user) => {
  try {
    // CRÍTICO: Usuários devem ser salvos APENAS em locations/{locationId}/users/{userId}
    // A Cloud Function é chamada quando o usuário é criado no Firebase Auth
    // Nesse momento, o usuário ainda não tem city/state (será preenchido no cadastro)
    // O app criará o documento em locations quando o usuário completar o cadastro com city/state
    // Portanto, esta função não precisa criar nada - apenas retornar
    
    // Verificar se o app já criou o documento (pode ter acontecido se o cadastro foi muito rápido)
    // Buscar em todas as locations conhecidas seria ineficiente, então apenas retornamos
    // O app garantirá que o documento seja criado em locations quando tiver city/state
    
    const message = 'User ' + user.uid + ' created in Auth. ' +
      'Document will be created in locations/{locationId}/users when user completes signup with city/state.';
    functions.logger.info(message);
    
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
    // CRÍTICO: Buscar orders na coleção por localização
    const locationId = await getUserLocationId(db, user.uid);
    const locationOrdersCollection = ordersPath(db, locationId);
    const ordersSnapshot = await locationOrdersCollection
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
 * Function to promote user to partner
 */
export const promoteToPartner = functions.https.onCall(async (data, context) => {
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
      role: 'partner',
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    functions.logger.info(`User ${context.auth.uid} promoted to partner`);
    return {success: true};
  } catch (error) {
    functions.logger.error('Error promoting user to partner:', error);
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
    
    // CRÍTICO: Buscar primeiro na coleção global 'users' (legacy)
    // Depois buscar em locations/{locationId}/users se necessário
    // Por enquanto, manter busca apenas em 'users' global para compatibilidade
    
    // Buscar por CPF
    if (cleanDocument.length === 11) {
      // Tentar buscar na coleção global primeiro
      const cpfQuery = await db.collection('users')
        .where('cpf', '==', cleanDocument)
        .limit(1)
        .get();
      
      if (!cpfQuery.empty) {
            const userData = cpfQuery.docs[0].data();
            const role = userData?.role?.toLowerCase() || '';
            
            // Verificar se é parceiro
            if (role !== 'partner') {
              throw new functions.https.HttpsError(
                'failed-precondition',
                'Este CPF/CNPJ não está cadastrado como parceiro'
              );
            }
        
        functions.logger.info(`Email found for CPF in users collection: ${cleanDocument}`);
        return {
          email: userData.email,
          role: userData.role,
          uid: cpfQuery.docs[0].id
        };
      }
      
      // Se não encontrou na coleção global, buscar em locations/{locationId}/users
      // LEI MÁXIMA DO TASKGO: Buscar em todas as locations quando necessário
      functions.logger.info('CPF não encontrado em users global, buscando em locations/{locationId}/users...');
      const locationsSnapshot = await db.collection('locations').limit(100).get();
      
      for (const locationDoc of locationsSnapshot.docs) {
        try {
          const locationUsersCollection = locationDoc.ref.collection('users');
          const cpfLocationQuery = await locationUsersCollection
            .where('cpf', '==', cleanDocument)
            .limit(1)
            .get();
          
          if (!cpfLocationQuery.empty) {
            const userData = cpfLocationQuery.docs[0].data();
            const role = userData?.role?.toLowerCase() || '';
            
            // Verificar se é parceiro
            if (role !== 'partner') {
              throw new functions.https.HttpsError(
                'failed-precondition',
                'Este CPF/CNPJ não está cadastrado como parceiro'
              );
            }
            
            functions.logger.info(`Email found for CPF in locations/${locationDoc.id}/users: ${cleanDocument}`);
            return {
              email: userData.email,
              role: userData.role,
              uid: cpfLocationQuery.docs[0].id
            };
          }
        } catch (e) {
          functions.logger.warn(`Erro ao buscar em locations/${locationDoc.id}/users: ${e}`);
          // Continuar buscando em outras locations
        }
      }
    }
    
    // Buscar por CNPJ
    if (cleanDocument.length === 14) {
      // Tentar buscar na coleção global primeiro
      const cnpjQuery = await db.collection('users')
        .where('cnpj', '==', cleanDocument)
        .limit(1)
        .get();
      
      if (!cnpjQuery.empty) {
        const userData = cnpjQuery.docs[0].data();
        const role = userData?.role?.toLowerCase() || '';
        
        // Verificar se é parceiro (aceitar partner e provider para compatibilidade)
        if (role !== 'partner') {
          throw new functions.https.HttpsError(
            'failed-precondition',
            'Este CPF/CNPJ não está cadastrado como parceiro'
          );
        }
        
        functions.logger.info(`Email found for CNPJ in users collection: ${cleanDocument}`);
        return {
          email: userData.email,
          role: userData.role,
          uid: cnpjQuery.docs[0].id
        };
      }
      
      // Se não encontrou na coleção global, buscar em locations/{locationId}/users
      // LEI MÁXIMA DO TASKGO: Buscar em todas as locations quando necessário
      functions.logger.info('CNPJ não encontrado em users global, buscando em locations/{locationId}/users...');
      const locationsSnapshot = await db.collection('locations').limit(100).get();
      
      for (const locationDoc of locationsSnapshot.docs) {
        try {
          const locationUsersCollection = locationDoc.ref.collection('users');
          const cnpjLocationQuery = await locationUsersCollection
            .where('cnpj', '==', cleanDocument)
            .limit(1)
            .get();
          
          if (!cnpjLocationQuery.empty) {
            const userData = cnpjLocationQuery.docs[0].data();
            const role = userData?.role?.toLowerCase() || '';
            
            // Verificar se é parceiro
            if (role !== 'partner') {
              throw new functions.https.HttpsError(
                'failed-precondition',
                'Este CPF/CNPJ não está cadastrado como parceiro'
              );
            }
            
            functions.logger.info(`Email found for CNPJ in locations/${locationDoc.id}/users: ${cleanDocument}`);
            return {
              email: userData.email,
              role: userData.role,
              uid: cnpjLocationQuery.docs[0].id
            };
          }
        } catch (e) {
          functions.logger.warn(`Erro ao buscar em locations/${locationDoc.id}/users: ${e}`);
          // Continuar buscando em outras locations
        }
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
