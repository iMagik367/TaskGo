package com.taskgoapp.taskgo.core.error

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CoroutineExceptionHandler

object FirestoreExceptionHandler {
    private const val TAG = "FirestoreExceptionHandler"
    const val DEFAULT_TAG = "FirestoreExceptionHandler"
    
    /**
     * Handler global para exceções do Firestore em corrotinas
     * Previne crashes quando há erros de permissão ou outros erros do Firestore
     */
    val coroutineExceptionHandler = CoroutineExceptionHandler { context, exception ->
        when (exception) {
            is FirebaseFirestoreException -> {
                when (exception.code) {
                    FirebaseFirestoreException.Code.PERMISSION_DENIED -> {
                        Log.w(TAG, "Permissão negada no Firestore: ${exception.message}")
                        // Não crashar o app, apenas logar
                    }
                    FirebaseFirestoreException.Code.UNAVAILABLE -> {
                        Log.w(TAG, "Firestore temporariamente indisponível: ${exception.message}")
                    }
                    FirebaseFirestoreException.Code.UNAUTHENTICATED -> {
                        Log.w(TAG, "Usuário não autenticado: ${exception.message}")
                    }
                    else -> {
                        Log.e(TAG, "Erro do Firestore: ${exception.message}", exception)
                    }
                }
            }
            else -> {
                Log.e(TAG, "Exceção não tratada: ${exception.message}", exception)
            }
        }
    }
    
    /**
     * Trata exceções do Firestore de forma segura, retornando um valor padrão
     */
    inline fun <T> handleFirestoreException(
        defaultValue: T,
        tag: String = DEFAULT_TAG,
        action: () -> T
    ): T {
        return try {
            action()
        } catch (e: FirebaseFirestoreException) {
            when (e.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED -> {
                    Log.w(tag, "Permissão negada: ${e.message}")
                }
                else -> {
                    Log.e(tag, "Erro do Firestore: ${e.message}", e)
                }
            }
            defaultValue
        } catch (e: Exception) {
            Log.e(tag, "Erro inesperado: ${e.message}", e)
            defaultValue
        }
    }
}

