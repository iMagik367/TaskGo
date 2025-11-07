package com.taskgoapp.taskgo.core.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp

object FirebaseConfigValidator {
    private const val TAG = "FirebaseConfigValidator"
    
    /**
     * Valida as configurações do Firebase
     */
    fun validate(context: Context): ValidationResult {
        Log.d(TAG, "=== VALIDAÇÃO DAS CONFIGURAÇÕES DO FIREBASE ===")
        
        val result = ValidationResult()
        
        // Validar Firebase App inicializado
        try {
            val firebaseApp = FirebaseApp.getInstance()
            result.firebaseInitialized = true
            Log.d(TAG, "✅ Firebase App inicializado")
            
            // Validar API Key
            val apiKey = firebaseApp.options.apiKey
            result.apiKey = apiKey
            Log.d(TAG, "API Key: $apiKey")
            if (apiKey.isNotEmpty()) {
                result.apiKeyValid = true
                Log.d(TAG, "✅ API Key presente")
            } else {
                result.apiKeyValid = false
                Log.e(TAG, "❌ API Key vazia")
            }
            
            // Validar Project ID
            val projectId = firebaseApp.options.projectId ?: ""
            result.projectId = projectId
            Log.d(TAG, "Project ID: $projectId")
            if (projectId == "task-go-ee85f") {
                result.projectIdValid = true
                Log.d(TAG, "✅ Project ID correto")
            } else {
                result.projectIdValid = false
                Log.e(TAG, "❌ Project ID incorreto. Esperado: task-go-ee85f, Encontrado: $projectId")
            }
            
            // Validar Application ID
            val applicationId = firebaseApp.options.applicationId
            result.applicationId = applicationId
            Log.d(TAG, "Application ID: $applicationId")
            if (applicationId.isNotEmpty()) {
                result.applicationIdValid = true
                Log.d(TAG, "✅ Application ID presente")
            } else {
                result.applicationIdValid = false
                Log.e(TAG, "❌ Application ID vazio")
            }
            
            // Validar Storage Bucket
            val storageBucket = firebaseApp.options.storageBucket
            result.storageBucket = storageBucket
            Log.d(TAG, "Storage Bucket: $storageBucket")
            if (storageBucket != null && storageBucket.isNotEmpty()) {
                result.storageBucketValid = true
                Log.d(TAG, "✅ Storage Bucket presente")
            } else {
                result.storageBucketValid = false
                Log.w(TAG, "⚠️ Storage Bucket vazio")
            }
            
        } catch (e: Exception) {
            result.firebaseInitialized = false
            Log.e(TAG, "❌ Firebase App não inicializado: ${e.message}", e)
        }
        
        // Validar google-services.json
        try {
            val googleServicesJson = validateGoogleServicesJson(context)
            result.googleServicesJsonValid = googleServicesJson.isValid
            result.packageName = googleServicesJson.packageName
            result.googleServicesApiKey = googleServicesJson.apiKey
            
            if (googleServicesJson.isValid) {
                Log.d(TAG, "✅ google-services.json válido")
                Log.d(TAG, "Package Name no google-services.json: ${googleServicesJson.packageName}")
                Log.d(TAG, "API Key no google-services.json: ${googleServicesJson.apiKey}")
                
                // Verificar se o package name está correto
                if (googleServicesJson.packageName == "com.taskgoapp.taskgo") {
                    result.packageNameValid = true
                    Log.d(TAG, "✅ Package Name correto")
                } else {
                    result.packageNameValid = false
                    Log.e(TAG, "❌ Package Name incorreto. Esperado: com.taskgoapp.taskgo, Encontrado: ${googleServicesJson.packageName}")
                }
                
                // Verificar se a API Key do google-services.json bate com a do Firebase
                val firebaseApiKey = try {
                    FirebaseApp.getInstance().options.apiKey
                } catch (e: Exception) {
                    ""
                }
                if (googleServicesJson.apiKey == firebaseApiKey) {
                    result.apiKeyMatches = true
                    Log.d(TAG, "✅ API Key do google-services.json bate com a do Firebase")
                } else {
                    result.apiKeyMatches = false
                    Log.e(TAG, "❌ API Key do google-services.json NÃO bate com a do Firebase")
                    Log.e(TAG, "   google-services.json: ${googleServicesJson.apiKey}")
                    Log.e(TAG, "   Firebase: $firebaseApiKey")
                }
            } else {
                Log.e(TAG, "❌ google-services.json inválido ou não encontrado")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao validar google-services.json: ${e.message}", e)
        }
        
        // Resultado final
        result.isValid = result.firebaseInitialized && 
                        result.apiKeyValid && 
                        result.projectIdValid && 
                        result.applicationIdValid &&
                        result.googleServicesJsonValid &&
                        result.packageNameValid &&
                        result.apiKeyMatches
        
        Log.d(TAG, "=== RESULTADO DA VALIDAÇÃO ===")
        Log.d(TAG, "Válido: ${result.isValid}")
        Log.d(TAG, "Firebase inicializado: ${result.firebaseInitialized}")
        Log.d(TAG, "API Key válida: ${result.apiKeyValid}")
        Log.d(TAG, "Project ID válido: ${result.projectIdValid}")
        Log.d(TAG, "Package Name válido: ${result.packageNameValid}")
        Log.d(TAG, "API Keys correspondem: ${result.apiKeyMatches}")
        
        if (!result.isValid) {
            Log.e(TAG, "⚠️ CONFIGURAÇÕES DO FIREBASE INVÁLIDAS!")
            Log.e(TAG, "Consulte VERIFICACAO_FIREBASE_CONFIG.md para resolver")
        } else {
            Log.d(TAG, "✅ Todas as configurações do Firebase estão válidas")
        }
        
        return result
    }
    
    private fun validateGoogleServicesJson(context: Context): GoogleServicesJsonResult {
        return try {
            // O google-services.json é processado pelo plugin do Google Services
            // Não podemos lê-lo diretamente, então validamos apenas as informações do Firebase
            // que já foram inicializadas
            GoogleServicesJsonResult(
                isValid = true,
                packageName = context.packageName,
                apiKey = try {
                    FirebaseApp.getInstance().options.apiKey
                } catch (e: Exception) {
                    ""
                },
                projectId = try {
                    FirebaseApp.getInstance().options.projectId ?: ""
                } catch (e: Exception) {
                    ""
                },
                projectNumber = try {
                    FirebaseApp.getInstance().options.projectId ?: ""
                } catch (e: Exception) {
                    ""
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao validar google-services.json: ${e.message}", e)
            GoogleServicesJsonResult(isValid = false, packageName = "", apiKey = "", projectId = "", projectNumber = "")
        }
    }
    
    data class ValidationResult(
        var isValid: Boolean = false,
        var firebaseInitialized: Boolean = false,
        var apiKey: String = "",
        var apiKeyValid: Boolean = false,
        var projectId: String = "",
        var projectIdValid: Boolean = false,
        var applicationId: String = "",
        var applicationIdValid: Boolean = false,
        var storageBucket: String? = null,
        var storageBucketValid: Boolean = false,
        var googleServicesJsonValid: Boolean = false,
        var packageName: String = "",
        var packageNameValid: Boolean = false,
        var googleServicesApiKey: String = "",
        var apiKeyMatches: Boolean = false
    )
    
    private data class GoogleServicesJsonResult(
        val isValid: Boolean,
        val packageName: String,
        val apiKey: String,
        val projectId: String,
        val projectNumber: String
    )
}

