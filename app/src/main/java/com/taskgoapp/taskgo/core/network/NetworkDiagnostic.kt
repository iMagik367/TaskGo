package com.taskgoapp.taskgo.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object NetworkDiagnostic {
    private const val TAG = "NetworkDiagnostic"
    
    /**
     * Verifica se há conexão com a internet
     */
    fun hasInternetConnection(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.isConnected == true
        }
    }
    
    /**
     * Verifica se é possível conectar ao Firebase
     */
    suspend fun canReachFirebase(): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://firebase.googleapis.com")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.requestMethod = "HEAD"
            val responseCode = connection.responseCode
            connection.disconnect()
            
            val canReach = responseCode in 200..399
            Log.d(TAG, "Firebase reachable: $canReach (response code: $responseCode)")
            canReach
        } catch (e: Exception) {
            Log.e(TAG, "Cannot reach Firebase: ${e.message}", e)
            false
        }
    }
    
    /**
     * Verifica se é possível conectar ao Google
     */
    suspend fun canReachGoogle(): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://www.google.com")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.requestMethod = "HEAD"
            val responseCode = connection.responseCode
            connection.disconnect()
            
            val canReach = responseCode in 200..399
            Log.d(TAG, "Google reachable: $canReach (response code: $responseCode)")
            canReach
        } catch (e: Exception) {
            Log.e(TAG, "Cannot reach Google: ${e.message}", e)
            false
        }
    }
    
    /**
     * Verifica se é possível conectar ao reCAPTCHA
     */
    suspend fun canReachRecaptcha(): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://www.google.com/recaptcha/api.js")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.requestMethod = "HEAD"
            val responseCode = connection.responseCode
            connection.disconnect()
            
            val canReach = responseCode in 200..399
            Log.d(TAG, "reCAPTCHA reachable: $canReach (response code: $responseCode)")
            canReach
        } catch (e: Exception) {
            Log.e(TAG, "Cannot reach reCAPTCHA: ${e.message}", e)
            false
        }
    }
    
    /**
     * Executa diagnóstico completo de rede
     */
    suspend fun diagnose(context: Context): NetworkDiagnosticResult {
        Log.d(TAG, "=== INICIANDO DIAGNÓSTICO DE REDE ===")
        
        val hasConnection = hasInternetConnection(context)
        Log.d(TAG, "Conexão de internet disponível: $hasConnection")
        
        if (!hasConnection) {
            return NetworkDiagnosticResult(
                hasInternet = false,
                canReachFirebase = false,
                canReachGoogle = false,
                canReachRecaptcha = false,
                error = "Sem conexão de internet"
            )
        }
        
        val canReachGoogle = canReachGoogle()
        val canReachFirebase = canReachFirebase()
        val canReachRecaptcha = canReachRecaptcha()
        
        val result = NetworkDiagnosticResult(
            hasInternet = true,
            canReachFirebase = canReachFirebase,
            canReachGoogle = canReachGoogle,
            canReachRecaptcha = canReachRecaptcha,
            error = if (!canReachFirebase) "Não é possível conectar ao Firebase" else null
        )
        
        Log.d(TAG, "=== DIAGNÓSTICO CONCLUÍDO ===")
        Log.d(TAG, "Resultado: $result")
        
        return result
    }
}

data class NetworkDiagnosticResult(
    val hasInternet: Boolean,
    val canReachFirebase: Boolean,
    val canReachGoogle: Boolean,
    val canReachRecaptcha: Boolean,
    val error: String? = null
) {
    val isHealthy: Boolean
        get() = hasInternet && canReachFirebase && canReachGoogle && canReachRecaptcha
}

