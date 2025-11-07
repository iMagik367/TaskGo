package com.taskgoapp.taskgo.utils

import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.FirebaseException
import android.util.Log

object FirebaseErrorHandler {
    
    fun getErrorMessage(error: Throwable): String {
        return when (error) {
            is FirebaseFunctionsException -> getFunctionErrorMessage(error)
            is FirebaseException -> error.message ?: "Erro no Firebase"
            else -> error.message ?: "Erro desconhecido"
        }
    }
    
    private fun getFunctionErrorMessage(error: FirebaseFunctionsException): String {
        return when (error.code) {
            FirebaseFunctionsException.Code.NOT_FOUND -> "Recurso não encontrado"
            FirebaseFunctionsException.Code.PERMISSION_DENIED -> "Permissão negada"
            FirebaseFunctionsException.Code.ABORTED -> "Operação abortada"
            FirebaseFunctionsException.Code.UNAVAILABLE -> "Serviço temporariamente indisponível"
            FirebaseFunctionsException.Code.DEADLINE_EXCEEDED -> "Timeout na requisição"
            FirebaseFunctionsException.Code.INTERNAL -> "Erro interno do servidor"
            FirebaseFunctionsException.Code.INVALID_ARGUMENT -> "Argumentos inválidos"
            FirebaseFunctionsException.Code.UNAUTHENTICATED -> "Não autenticado"
            else -> error.message ?: "Erro ao processar requisição"
        }
    }
    
    fun logError(tag: String, error: Throwable) {
        Log.e(tag, "Error: ${error.message}", error)
    }
}

