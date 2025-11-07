package com.taskgoapp.taskgo.core.utils

import android.util.Log
import com.taskgoapp.taskgo.utils.FirebaseErrorHandler
import com.google.firebase.FirebaseException
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException

/**
 * Centralized error handler for the application
 */
object ErrorHandler {
    
    private const val TAG = "ErrorHandler"
    
    /**
     * Get user-friendly error message from exception
     */
    fun getErrorMessage(error: Throwable): String {
        return when (error) {
            is FirebaseException -> FirebaseErrorHandler.getErrorMessage(error)
            is UnknownHostException -> "Erro de conexão. Verifique sua internet."
            is TimeoutException -> "Tempo de espera excedido. Tente novamente."
            is NetworkException -> "Erro de conexão. Verifique sua internet."
            else -> {
                val message = error.message
                if (message.isNullOrBlank()) {
                    "Erro desconhecido. Tente novamente."
                } else {
                    message
                }
            }
        }
    }
    
    /**
     * Handle error and log it
     */
    fun handleError(
        error: Throwable,
        tag: String = TAG,
        onError: ((String) -> Unit)? = null
    ) {
        val message = getErrorMessage(error)
        Log.e(tag, "Error: $message", error)
        onError?.invoke(message)
    }
    
    /**
     * Log error without showing to user
     */
    fun logError(error: Throwable, tag: String = TAG) {
        Log.e(tag, "Error: ${error.message}", error)
    }
}

/**
 * Custom exception for network errors
 */
class NetworkException(message: String? = null, cause: Throwable? = null) : 
    Exception(message ?: "Network error", cause)


