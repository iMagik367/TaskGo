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

data class ChatRequest(
    val contents: List<ContentRequest>
)

data class ContentRequest(
    val role: String, // "user" or "model"
    val parts: List<PartRequest>
)

data class PartRequest(
    val text: String? = null,
    val inlineData: InlineData? = null
)

data class InlineData(
    val mimeType: String,
    val data: String // Base64 encoded
)

// ChatMessage para uso interno do serviço
data class ChatMessage(
    val role: String, // "user" or "assistant"
    val content: String,
    val imageData: List<ImageData> = emptyList()
)

data class ImageData(
    val mimeType: String,
    val base64Data: String
)

data class ChatResponse(
    val candidates: List<Candidate>?
)

data class Candidate(
    val content: Content?
)

data class Content(
    val parts: List<Part>?,
    val role: String?
)

data class Part(
    val text: String?
)

@Singleton
class GoogleCloudAIService @Inject constructor(
    private val apiKey: String
) {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val baseUrl = "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-flash:generateContent"
    
    suspend fun sendMessage(messages: List<ChatMessage>, systemInstruction: String? = null): kotlin.Result<String> = withContext(Dispatchers.IO) {
        try {
            // Converter mensagens para o formato da API Gemini
            val contents = messages.map { msg ->
                val parts = mutableListOf<Map<String, Any>>()
                
                // Adicionar texto se houver
                if (msg.content.isNotBlank()) {
                    parts.add(mapOf("text" to msg.content))
                }
                
                // Adicionar imagens se houver
                msg.imageData.forEach { imageData ->
                    parts.add(mapOf(
                        "inline_data" to mapOf(
                            "mime_type" to imageData.mimeType,
                            "data" to imageData.base64Data
                        )
                    ))
                }
                
                mapOf(
                    "role" to (if (msg.role == "assistant") "model" else "user"),
                    "parts" to parts
                )
            }
            
            // Construir request body - system instruction deve ser adicionado como primeira mensagem do sistema
            val requestBody = mutableMapOf<String, Any>("contents" to contents)
            
            // Se houver system instruction, adicionar como primeira mensagem do sistema
            if (systemInstruction != null && systemInstruction.isNotBlank()) {
                // Adicionar system instruction como primeira mensagem do sistema
                val systemMessage = mapOf(
                    "role" to "user",
                    "parts" to listOf(mapOf("text" to systemInstruction))
                )
                val systemResponse = mapOf(
                    "role" to "model",
                    "parts" to listOf(mapOf("text" to "Entendido. Estou pronto para ajudar."))
                )
                // Inserir no início do array de contents
                val updatedContents = mutableListOf<Map<String, Any>>()
                updatedContents.add(systemMessage)
                updatedContents.add(systemResponse)
                updatedContents.addAll(contents)
                requestBody["contents"] = updatedContents
            }
            
            val jsonBody = gson.toJson(requestBody)
            
            val request = Request.Builder()
                .url("$baseUrl?key=$apiKey")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@withContext kotlin.Result.failure(Exception("Empty response"))
            
            if (!response.isSuccessful) {
                return@withContext kotlin.Result.failure(Exception("API Error: ${response.code} - $responseBody"))
            }
            
            val chatResponse = gson.fromJson(responseBody, ChatResponse::class.java)
            val text = chatResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return@withContext kotlin.Result.failure(Exception("No response text"))
            
            kotlin.Result.success(text)
        } catch (e: Exception) {
            kotlin.Result.failure(e)
        }
    }
}

