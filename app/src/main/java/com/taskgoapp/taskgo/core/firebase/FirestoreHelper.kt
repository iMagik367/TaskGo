package com.taskgoapp.taskgo.core.firebase

import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log

/**
 * Helper para obter instância do Firestore configurada para o database padrão '(default)'
 * O Firestore usa o database '(default)' por padrão quando nenhum nome é especificado
 */
object FirestoreHelper {
    private const val TAG = "FirestoreHelper"
    
    /**
     * Obtém instância do Firestore para o database padrão '(default)'
     */
    @JvmStatic
    fun getInstance(): FirebaseFirestore {
        return try {
            Log.d(TAG, "Acessando Firestore database padrao (default)...")
            // Usar database padrão '(default)' - não especificar nome usa o default automaticamente
            val firestore = FirebaseFirestore.getInstance()
            Log.d(TAG, "Database Firestore acessado com sucesso")
            firestore
        } catch (e: Exception) {
            Log.e(TAG, "ERRO CRITICO: Nao foi possivel acessar o database Firestore", e)
            Log.e(TAG, "   Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "   Exception message: ${e.message}")
            e.printStackTrace()
            throw IllegalStateException(
                "FALHA CRITICA: Database Firestore nao esta disponivel. " +
                "Verifique se o Firebase esta configurado corretamente.",
                e
            )
        }
    }
}
