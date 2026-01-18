import * as admin from 'firebase-admin';

/**
 * Get Firestore instance for the 'taskgo' database
 * CRÍTICO: Database 'taskgo' está em modo MongoDB compatibility
 * NÃO FAZ FALLBACK PARA DEFAULT - FALHA SE NÃO CONSEGUIR ACESSAR TASKGO
 * 
 * Firebase Admin SDK v12+ suporta múltiplos databases Firestore no mesmo projeto.
 * Para databases em modo MongoDB, o acesso é feito da mesma forma.
 */
export function getFirestore(): admin.firestore.Firestore {
  const app = admin.app();
  
  try {
    // Acessar database 'taskgo' (Firestore em modo MongoDB)
    // O Firebase Admin SDK gerencia as credenciais automaticamente via Application Default Credentials
    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    // @ts-expect-error - firestore() aceita database ID em projetos Enterprise/MongoDB
    const db = app.firestore('taskgo');
    
    if (!db) {
      throw new Error(
        'FALHA CRÍTICA: Não foi possível acessar o database "taskgo". ' +
        'Verifique se o database está configurado no Firebase Console.'
      );
    }
    
    return db;
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : 'Erro desconhecido';
    throw new Error(
      `FALHA CRÍTICA ao acessar database "taskgo": ${errorMessage}. ` +
      'Verifique: 1) Database "taskgo" existe no Firebase Console, ' +
      '2) Credenciais do projeto estão configuradas, ' +
      '3) Permissões estão corretas.'
    );
  }
}
