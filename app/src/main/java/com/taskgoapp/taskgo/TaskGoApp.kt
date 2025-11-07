package com.taskgoapp.taskgo

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.taskgoapp.taskgo.BuildConfig
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TaskGoApp : Application() {

    override fun onCreate() {
        super.onCreate()
        
        Log.d(TAG, "Inicializando TaskGoApp...")
        Log.d(TAG, "BuildConfig.USE_EMULATOR: ${BuildConfig.USE_EMULATOR}")
        Log.d(TAG, "BuildConfig.DEBUG: ${BuildConfig.DEBUG}")
        
        // Initialize Firebase
        try {
            FirebaseApp.initializeApp(this)
            Log.d(TAG, "Firebase inicializado com sucesso")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao inicializar Firebase: ${e.message}", e)
        }
        
        // Initialize Firebase App Check
        try {
            val appCheck = FirebaseAppCheck.getInstance()
            
            if (BuildConfig.DEBUG) {
                // Para builds de debug, usar DebugAppCheckProviderFactory
                // Isso permite testar sem precisar configurar Play Integrity
                Log.d(TAG, "Configurando App Check para modo DEBUG")
                appCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
                
                // Obter e logar o token de debug (adicione este token no Firebase Console)
                // Aguardar um pouco antes de tentar obter o token para dar tempo das APIs serem verificadas
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    appCheck.getAppCheckToken(false).addOnSuccessListener { token ->
                        Log.d(TAG, "✅ App Check Debug Token obtido: ${token.token}")
                        Log.d(TAG, "📋 IMPORTANTE: Adicione este token no Firebase Console:")
                        Log.d(TAG, "   1. Acesse: https://console.firebase.google.com/project/task-go-ee85f/appcheck")
                        Log.d(TAG, "   2. Vá em 'Manage debug tokens'")
                        Log.d(TAG, "   3. Adicione o token: ${token.token}")
                    }.addOnFailureListener { e ->
                        Log.e(TAG, "❌ Erro ao obter token de debug do App Check", e)
                        val errorMsg = e.message ?: "Erro desconhecido"
                        if (errorMsg.contains("403") || errorMsg.contains("API has not been used")) {
                            Log.e(TAG, "⚠️ PROBLEMA IDENTIFICADO: As APIs do Firebase não estão habilitadas no Google Cloud Console!")
                            Log.e(TAG, "📋 SOLUÇÃO:")
                            Log.e(TAG, "   1. Habilite Firebase App Check API:")
                            Log.e(TAG, "      https://console.developers.google.com/apis/api/firebaseappcheck.googleapis.com/overview?project=605187481719")
                            Log.e(TAG, "   2. Habilite Firebase Installations API:")
                            Log.e(TAG, "      https://console.developers.google.com/apis/api/firebaseinstallations.googleapis.com/overview?project=605187481719")
                            Log.e(TAG, "   3. Aguarde 5-10 minutos e reinicie o app")
                        } else {
                            Log.e(TAG, "Erro detalhado: $errorMsg")
                        }
                    }
                }, 2000) // Aguardar 2 segundos antes de tentar obter o token
            } else {
                // Para builds de release, usar Play Integrity
                Log.d(TAG, "Configurando App Check para modo RELEASE (Play Integrity)")
                appCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                )
            }
            
            Log.d(TAG, "Firebase App Check configurado com sucesso")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao configurar Firebase App Check: ${e.message}", e)
            Log.e(TAG, "⚠️ O app continuará funcionando, mas o login pode falhar se as APIs não estiverem habilitadas")
            // Continuar mesmo se App Check falhar - pode não ser crítico para login básico
        }
    }
    
    companion object {
        private const val TAG = "TaskGoApp"
    }
}




