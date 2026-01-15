package com.taskgoapp.taskgo

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import javax.inject.Inject
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.taskgoapp.taskgo.BuildConfig
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class TaskGoApp : Application(), Configuration.Provider {
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "WorkManager configurado com HiltWorkerFactory")
        
        Log.d(TAG, "Inicializando TaskGoApp...")
        Log.d(TAG, "BuildConfig.USE_EMULATOR: ${BuildConfig.USE_EMULATOR}")
        Log.d(TAG, "BuildConfig.DEBUG: ${BuildConfig.DEBUG}")
        
        // Diagnosticar conectividade de rede
        Log.d(TAG, "=== DIAGNÓSTICO DE REDE ===")
        applicationScope.launch {
            try {
                val diagnostic = com.taskgoapp.taskgo.core.network.NetworkDiagnostic.diagnose(this@TaskGoApp)
                Log.d(TAG, "Resultado do diagnóstico: ${diagnostic}")
                if (!diagnostic.isHealthy) {
                    Log.e(TAG, "⚠️ PROBLEMA DE CONECTIVIDADE DETECTADO:")
                    Log.e(TAG, "   - Internet: ${diagnostic.hasInternet}")
                    Log.e(TAG, "   - Firebase: ${diagnostic.canReachFirebase}")
                    Log.e(TAG, "   - Google: ${diagnostic.canReachGoogle}")
                    Log.e(TAG, "   - reCAPTCHA: ${diagnostic.canReachRecaptcha}")
                    Log.e(TAG, "   - Erro: ${diagnostic.error ?: "N/A"}")
                    Log.e(TAG, "📋 Consulte DIAGNOSTICO_CONECTIVIDADE.md para resolver")
                } else {
                    Log.d(TAG, "✅ Conectividade OK")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao diagnosticar rede: ${e.message}", e)
            }
        }
        
        // Initialize Firebase - CRÍTICO: Deve ser a primeira coisa
        try {
            // Firebase é inicializado automaticamente pelo google-services.json
            // Apenas verificar se está inicializado
            val firebaseApp = FirebaseApp.getInstance()
            
            Log.d(TAG, "✅ Firebase inicializado com sucesso")
            Log.d(TAG, "Firebase Project ID: ${firebaseApp.options.projectId}")
            Log.d(TAG, "Firebase Application ID: ${firebaseApp.options.applicationId}")
            
            // Initialize Firebase Crashlytics
            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Crashlytics desabilitado em modo DEBUG")
            } else {
                Log.d(TAG, "Crashlytics habilitado para produção")
            }
            
            // Garantir que Firebase Auth está pronto
            try {
                val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                Log.d(TAG, "✅ Firebase Auth inicializado")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erro ao inicializar Firebase Auth: ${e.message}", e)
            }
            
            // Validar configurações do Firebase em background (não bloqueia)
            applicationScope.launch {
                try {
                    val validation = com.taskgoapp.taskgo.core.firebase.FirebaseConfigValidator.validate(this@TaskGoApp)
                    if (!validation.isValid) {
                        Log.e(TAG, "⚠️ CONFIGURAÇÕES DO FIREBASE INVÁLIDAS!")
                        Log.e(TAG, "Consulte VERIFICACAO_FIREBASE_CONFIG.md para resolver")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao validar configurações do Firebase: ${e.message}", e)
                }
            }
        } catch (e: IllegalStateException) {
            // Firebase não inicializado - tentar inicializar manualmente
            Log.e(TAG, "Firebase não inicializado, tentando inicializar manualmente...")
            try {
                FirebaseApp.initializeApp(this)
                Log.d(TAG, "✅ Firebase inicializado manualmente")
            } catch (initError: Exception) {
                Log.e(TAG, "❌ ERRO CRÍTICO: Não foi possível inicializar Firebase: ${initError.message}", initError)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao inicializar Firebase: ${e.message}", e)
        }
        
        // Initialize Firebase App Check
        if (BuildConfig.FIREBASE_APP_CHECK_ENABLED) {
            try {
                val appCheck = FirebaseAppCheck.getInstance()
                
                if (BuildConfig.DEBUG) {
                    // Para builds de debug, usar DebugAppCheckProviderFactory
                    val debugToken = BuildConfig.FIREBASE_DEBUG_APP_CHECK_TOKEN
                    val debugTokenName = BuildConfig.FIREBASE_DEBUG_APP_CHECK_TOKEN_NAME
                    if (debugToken.isNotBlank()) {
                        val persistenceKey = FirebaseApp.getInstance().persistenceKey
                        val sharedPrefsName = "com.google.firebase.appcheck.debug.store.$persistenceKey"
                        val debugSecretKey = "com.google.firebase.appcheck.debug.DEBUG_SECRET"

                        getSharedPreferences(sharedPrefsName, MODE_PRIVATE)
                            .edit()
                            .putString(debugSecretKey, debugToken)
                            .apply()

                        Log.d(TAG, "✅ App Check DEBUG configurado com token: ${debugTokenName}")
                    }

                    appCheck.installAppCheckProviderFactory(
                        DebugAppCheckProviderFactory.getInstance()
                    )
                } else {
                    // Para builds de release, usar Play Integrity
                    Log.d(TAG, "=== CONFIGURANDO APP CHECK RELEASE ===")
                    Log.d(TAG, "Provider: PlayIntegrityAppCheckProviderFactory")
                    
                    try {
                        appCheck.installAppCheckProviderFactory(
                            PlayIntegrityAppCheckProviderFactory.getInstance()
                        )
                        Log.d(TAG, "✅ Play Integrity Provider instalado")
                        
                        // Tentar obter token para verificar se está funcionando
                        applicationScope.launch {
                            kotlinx.coroutines.delay(5000) // Aguardar 5 segundos para Play Integrity inicializar
                            try {
                                appCheck.getAppCheckToken(false).addOnSuccessListener { token ->
                                    Log.d(TAG, "✅ App Check token obtido com sucesso (Play Integrity)")
                                    Log.d(TAG, "Token (primeiros 20 chars): ${token.token.take(20)}...")
                                    Log.d(TAG, "Token expira em: ${token.expireTimeMillis - System.currentTimeMillis()}ms")
                                }.addOnFailureListener { e ->
                                    Log.e(TAG, "❌ FALHA AO OBTER APP CHECK TOKEN")
                                    Log.e(TAG, "Erro: ${e.message}")
                                    Log.e(TAG, "Tipo: ${e.javaClass.simpleName}")
                                    
                                    // Análise detalhada do erro
                                    val errorMessage = e.message ?: ""
                                    when {
                                        errorMessage.contains("403", ignoreCase = true) ||
                                        errorMessage.contains("App attestation failed", ignoreCase = true) ||
                                        errorMessage.contains("attestation", ignoreCase = true) -> {
                                            Log.e(TAG, "═══════════════════════════════════════════════════")
                                            Log.e(TAG, "ERRO CRÍTICO: App Attestation Failed (403)")
                                            Log.e(TAG, "═══════════════════════════════════════════════════")
                                            Log.e(TAG, "CAUSA PRINCIPAL: SHA-256 do App Signing Key não cadastrado")
                                            Log.e(TAG, "")
                                            Log.e(TAG, "SOLUÇÃO:")
                                            Log.e(TAG, "1. Acesse Google Play Console → App Signing")
                                            Log.e(TAG, "2. Copie o SHA-256 do 'App signing certificate'")
                                            Log.e(TAG, "3. Firebase Console → Configurações do Projeto → Android App")
                                            Log.e(TAG, "4. Adicione o SHA-256 do App Signing Key")
                                            Log.e(TAG, "5. Aguarde 5-10 minutos para propagação")
                                            Log.e(TAG, "")
                                            Log.e(TAG, "IMPORTANTE: Use o SHA-256 do App Signing Key,")
                                            Log.e(TAG, "NÃO o SHA-256 do Upload Key!")
                                            Log.e(TAG, "═══════════════════════════════════════════════════")
                                        }
                                        errorMessage.contains("API", ignoreCase = true) &&
                                        errorMessage.contains("not", ignoreCase = true) -> {
                                            Log.e(TAG, "CAUSA: Play Integrity API não habilitada")
                                            Log.e(TAG, "SOLUÇÃO: Habilitar Play Integrity API no Google Cloud Console")
                                        }
                                        errorMessage.contains("network", ignoreCase = true) ||
                                        errorMessage.contains("connection", ignoreCase = true) -> {
                                            Log.e(TAG, "CAUSA: Problema de rede")
                                            Log.e(TAG, "SOLUÇÃO: Verificar conectividade")
                                        }
                                        else -> {
                                            Log.e(TAG, "CAUSA DESCONHECIDA - Verificar logs completos")
                                            Log.e(TAG, "Stack trace:", e)
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Erro ao verificar token: ${e.message}", e)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ ERRO ao instalar Play Integrity Provider: ${e.message}", e)
                        throw e
                    }
                    
                    Log.d(TAG, "✅ App Check RELEASE configurado com Play Integrity")
                }
                
                Log.d(TAG, "✅ Firebase App Check inicializado")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erro ao inicializar Firebase App Check: ${e.message}", e)
                throw e // Lançar exceção para identificar problema
            }
        } else {
            Log.d(TAG, "ℹ️ Firebase App Check desativado via configuração")
        }
    }
    
    companion object {
        private const val TAG = "TaskGoApp"
    }
    
    override val workManagerConfiguration: Configuration
        get() {
            return if (::workerFactory.isInitialized) {
                Configuration.Builder()
                    .setWorkerFactory(workerFactory)
                    .setMinimumLoggingLevel(Log.DEBUG)
                    .build()
            } else {
                Log.w(TAG, "HiltWorkerFactory não inicializado, usando configuração padrão")
                Configuration.Builder()
                    .setMinimumLoggingLevel(Log.DEBUG)
                    .build()
            }
        }
}




