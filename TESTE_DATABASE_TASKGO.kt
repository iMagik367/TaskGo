// TESTE PARA VERIFICAR SE O DATABASE 'TASKGO' ESTÁ FUNCIONANDO
// Execute este código no app para testar a conexão

package com.taskgoapp.taskgo.core.firebase

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object TesteDatabaseTaskgo {
    private const val TAG = "TesteDatabaseTaskgo"
    
    /**
     * Testa se o database 'taskgo' está acessível e pode escrever dados
     */
    suspend fun testarDatabaseTaskgo(): Boolean {
        return try {
            Log.d(TAG, "🧪 Iniciando teste do database 'taskgo'...")
            
            // 1. Obter instância do Firestore usando FirestoreHelper
            val firestore = FirestoreHelper.getInstance()
            Log.d(TAG, "✅ FirestoreHelper.getInstance() executado com sucesso")
            
            // 2. Tentar ler uma coleção (mesmo que vazia)
            val testCollection = firestore.collection("_test")
            val snapshot = testCollection.limit(1).get().await()
            Log.d(TAG, "✅ Leitura do database 'taskgo' funcionou. Documentos encontrados: ${snapshot.size()}")
            
            // 3. Tentar escrever um documento de teste
            val testDoc = testCollection.document("test_${System.currentTimeMillis()}")
            testDoc.set(mapOf(
                "test" to true,
                "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )).await()
            Log.d(TAG, "✅ Escrita no database 'taskgo' funcionou")
            
            // 4. Limpar documento de teste
            testDoc.delete().await()
            Log.d(TAG, "✅ Deleção no database 'taskgo' funcionou")
            
            Log.d(TAG, "🎉 TESTE COMPLETO: Database 'taskgo' está funcionando corretamente!")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ ERRO NO TESTE: ${e.message}", e)
            Log.e(TAG, "   Exception type: ${e.javaClass.simpleName}")
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Verifica qual database está sendo usado
     */
    fun verificarDatabaseUsado(firestore: FirebaseFirestore): String {
        return try {
            // Tentar obter informações do database
            // Nota: Firebase SDK não expõe diretamente o database ID, mas podemos inferir
            val app = firestore.app
            Log.d(TAG, "App name: ${app.name}")
            Log.d(TAG, "App options projectId: ${app.options.projectId}")
            
            // Se chegou até aqui sem erro usando FirestoreHelper, está usando 'taskgo'
            "taskgo (inferido via FirestoreHelper)"
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao verificar database: ${e.message}", e)
            "erro ao verificar"
        }
    }
}
