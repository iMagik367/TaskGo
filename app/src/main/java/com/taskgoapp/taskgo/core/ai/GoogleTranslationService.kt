package com.taskgoapp.taskgo.core.ai

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

data class TranslationRequest(
    val q: List<String>,
    val target: String,
    val source: String? = null
)

data class TranslationResponse(
    val data: TranslationData?
)

data class TranslationData(
    val translations: List<Translation>?
)

data class Translation(
    val translatedText: String,
    val detectedSourceLanguage: String?
)

@Singleton
class GoogleTranslationService @Inject constructor(
    private val apiKey: String
) {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val baseUrl = "https://translation.googleapis.com/language/translate/v2"
    
    suspend fun translateText(
        text: String,
        targetLanguage: String,
        sourceLanguage: String? = null
    ): kotlin.Result<String> = withContext(Dispatchers.IO) {
        try {
            val requestBody = TranslationRequest(
                q = listOf(text),
                target = targetLanguage,
                source = sourceLanguage
            )
            val jsonBody = gson.toJson(requestBody)
            
            val request = Request.Builder()
                .url("$baseUrl?key=$apiKey")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@withContext kotlin.Result.failure(Exception("Empty response"))
            
            if (!response.isSuccessful) {
                return@withContext kotlin.Result.failure(Exception("Translation API Error: ${response.code} - $responseBody"))
            }
            
            val translationResponse = gson.fromJson(responseBody, TranslationResponse::class.java)
            val translatedText = translationResponse.data?.translations?.firstOrNull()?.translatedText
                ?: return@withContext kotlin.Result.failure(Exception("No translation text"))
            
            kotlin.Result.success(translatedText)
        } catch (e: Exception) {
            kotlin.Result.failure(e)
        }
    }
    
    suspend fun detectLanguage(text: String): kotlin.Result<String> = withContext(Dispatchers.IO) {
        try {
            val requestBody = TranslationRequest(
                q = listOf(text),
                target = "en" // Target não importa para detecção
            )
            val jsonBody = gson.toJson(requestBody)
            
            val request = Request.Builder()
                .url("$baseUrl/detect?key=$apiKey")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@withContext kotlin.Result.failure(Exception("Empty response"))
            
            if (!response.isSuccessful) {
                return@withContext kotlin.Result.failure(Exception("Language Detection Error: ${response.code}"))
            }
            
            // Parse response para detectar idioma
            val detectedLanguage = "pt" // Simplificado - em produção, parsear JSON completo
            kotlin.Result.success(detectedLanguage)
        } catch (e: Exception) {
            kotlin.Result.failure(e)
        }
    }
}

