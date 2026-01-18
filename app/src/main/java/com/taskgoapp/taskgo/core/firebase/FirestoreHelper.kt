package com.taskgoapp.taskgo.core.firebase

import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log

/**
 * Helper para obter instância do Firestore configurada para o database 'taskgo'
 * CRÍTICO: Todos os dados devem ser gravados no banco 'taskgo', não em 'default'
 */
object FirestoreHelper {
    private const val TAG = "FirestoreHelper"
    private const val DATABASE_ID = "taskgo"
    
    /**
     * Obtém instância do Firestore para o database 'taskgo'
     * Se o database 'taskgo' não estiver disponível, retorna o default com log de erro
     */
    @JvmStatic
    fun getInstance(): FirebaseFirestore {
        // CRÍTICO: NÃO FAZ FALLBACK PARA DEFAULT - FALHA SE NÃO CONSEGUIR ACESSAR TASKGO
        return try {
            Log.d(TAG, "🔍 Acessando database '$DATABASE_ID'...")
            // Usar database 'taskgo' ao invés de 'default'
            val firestore = FirebaseFirestore.getInstance(FirebaseApp.getInstance(), DATABASE_ID)
            Log.d(TAG, "✅ Database '$DATABASE_ID' acessado com sucesso")
            firestore
        } catch (e: Exception) {
            // FALHAR se não conseguir acessar taskgo - NÃO usar default
            Log.e(TAG, "❌ ERRO CRÍTICO: Não foi possível acessar o database '$DATABASE_ID'", e)
            Log.e(TAG, "   Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "   Exception message: ${e.message}")
            e.printStackTrace()
            throw IllegalStateException(
                "FALHA CRÍTICA: Database '$DATABASE_ID' não está disponível. " +
                "Verifique se o database está configurado no Firebase Console.",
                e
            )
        }
    }
}
