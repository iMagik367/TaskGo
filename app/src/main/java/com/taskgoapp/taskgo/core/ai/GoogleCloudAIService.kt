package com.taskgoapp.taskgo.core.ai

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
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

/**
 * Cache de resposta para melhorar performance e reduzir chamadas à API
 * Baseado no padrão de cache dos arquivos Python de referência
 */
private data class CachedResponse(
    val response: String,
    val timestamp: Long
)

@Singleton
class GoogleCloudAIService @Inject constructor(
    private val apiKey: String
) {
    // Cliente com timeout otimizado e retry automático
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS) // Aumentado para respostas mais complexas
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true) // Retry automático em falhas de conexão
        .build()
    private val gson = Gson()
    
    private val baseUrl = "https://generativelanguage.googleapis.com/v1/models"
    private val primaryModel = "gemini-2.0-flash-exp"
    // Fallback para modelos alternativos caso o principal falhe
    private val fallbackModels = listOf(
        "gemini-1.5-flash-latest",
        "gemini-1.5-pro-latest"
    )
    
    // Cache de health check (baseado em ollama_api.py _check_ollama_running)
    @Volatile
    private var lastHealthCheck: Long = 0
    @Volatile
    private var isHealthy: Boolean = true
    private val healthCheckInterval = 300_000L // 5 minutos
    
    // Cache de respostas para perguntas frequentes (baseado em chat_manager.py)
    private val responseCache = mutableMapOf<String, CachedResponse>()
    private val cacheExpiry = 3600_000L // 1 hora
    
    /**
     * Verifica se a API está disponível e funcionando
     * Baseado no padrão de ollama_api.py (_check_ollama_running)
     */
    suspend fun checkConnection(): Boolean = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (now - lastHealthCheck < healthCheckInterval && isHealthy) {
            return@withContext true
        }
        
        try {
            // Fazer uma chamada simples para verificar conexão
            val testUrl = "$baseUrl/$primaryModel:generateContent?key=$apiKey"
            val testBody = mapOf(
                "contents" to listOf(
                    mapOf(
                        "role" to "user",
                        "parts" to listOf(mapOf("text" to "test"))
                    )
                ),
                "generationConfig" to mapOf(
                    "maxOutputTokens" to 1
                )
            )
            
            val request = Request.Builder()
                .url(testUrl)
                .post(gson.toJson(testBody).toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            val isOk = response.isSuccessful
            
            lastHealthCheck = now
            isHealthy = isOk
            
            if (!isOk) {
                android.util.Log.w("GoogleCloudAIService", "Health check failed: ${response.code}")
            }
            
            isOk
        } catch (e: Exception) {
            android.util.Log.e("GoogleCloudAIService", "Health check error: ${e.message}", e)
            lastHealthCheck = now
            isHealthy = false
            false
        }
    }
    
    /**
     * Envia mensagem com sistema robusto de retry e fallback
     * Baseado no padrão de chat_manager.py e ollama_api.py
     */
    suspend fun sendMessage(
        messages: List<ChatMessage>, 
        systemInstruction: String? = null
    ): kotlin.Result<String> = withContext(Dispatchers.IO) {
        // Verificar conexão primeiro
        if (!checkConnection()) {
            android.util.Log.w("GoogleCloudAIService", "API não está disponível, usando fallback")
            return@withContext getFallbackResponse(messages.lastOrNull()?.content ?: "")
        }
        
        // Verificar cache
        val cacheKey = generateCacheKey(messages, systemInstruction)
        val cached = responseCache[cacheKey]
        if (cached != null && System.currentTimeMillis() - cached.timestamp < cacheExpiry) {
            android.util.Log.d("GoogleCloudAIService", "Resposta retornada do cache")
            return@withContext kotlin.Result.success(cached.response)
        }
        
        // Tentar com retry e backoff exponencial (baseado em engine.py)
        val maxRetries = 3
        var lastError: Exception? = null
        
        for (attempt in 0 until maxRetries) {
            try {
                val result = trySendWithRetry(messages, systemInstruction, attempt)
                if (result.isSuccess) {
                    val response = result.getOrNull() ?: ""
                    // Salvar no cache
                    responseCache[cacheKey] = CachedResponse(response, System.currentTimeMillis())
                    // Limpar cache antigo
                    cleanupCache()
                    return@withContext result
                } else {
                    lastError = result.exceptionOrNull() as? Exception
                }
            } catch (e: Exception) {
                lastError = e
            }
            
            // Backoff exponencial: 1s, 2s, 4s (baseado em adaptive_fuzzer.py)
            if (attempt < maxRetries - 1) {
                val delayMs = (1 shl attempt) * 1000L
                android.util.Log.w("GoogleCloudAIService", "Tentativa ${attempt + 1} falhou, aguardando ${delayMs}ms...")
                delay(delayMs)
            }
        }
        
        // Se todas as tentativas falharam, usar fallback
        android.util.Log.w("GoogleCloudAIService", "Todas as tentativas falharam, usando fallback")
        val fallback = getFallbackResponse(messages.lastOrNull()?.content ?: "")
        return@withContext fallback
    }
    
    private suspend fun trySendWithRetry(
        messages: List<ChatMessage>,
        systemInstruction: String?,
        attempt: Int
    ): kotlin.Result<String> {
        val modelsToTry = listOf(primaryModel) + fallbackModels
        
        for (modelName in modelsToTry) {
            try {
                val result = trySendMessageWithModel(messages, systemInstruction, modelName)
                if (result.isSuccess) {
                    return result
                }
            } catch (e: Exception) {
                android.util.Log.w("GoogleCloudAIService", "Modelo $modelName falhou na tentativa ${attempt + 1}: ${e.message}")
            }
        }
        
        return kotlin.Result.failure(Exception("Todos os modelos falharam"))
    }
    
    private fun generateCacheKey(
        messages: List<ChatMessage>,
        systemInstruction: String?
    ): String {
        val lastMessage = messages.lastOrNull()?.content ?: ""
        val hash = (lastMessage + (systemInstruction ?: "")).hashCode()
        return "msg_$hash"
    }
    
    private fun cleanupCache() {
        val now = System.currentTimeMillis()
        val toRemove = responseCache.entries.filter { 
            now - it.value.timestamp > cacheExpiry 
        }.map { it.key }
        toRemove.forEach { responseCache.remove(it) }
    }
    
    /**
     * Resposta de fallback quando a API não está disponível
     * Baseado no padrão de ollama_api.py (_fallback_response)
     */
    private fun getFallbackResponse(message: String): kotlin.Result<String> {
        val lowerMessage = message.lowercase()
        
        // Palavras-chave para diferentes categorias (baseado em ollama_api.py)
        val helpKeywords = listOf("ajuda", "help", "como", "guia", "tutorial", "instrução", "manual")
        val serviceKeywords = listOf("serviço", "service", "ordem", "order", "prestador", "provider", "contratar")
        val productKeywords = listOf("produto", "product", "compra", "buy", "carrinho", "cart", "loja", "store")
        val paymentKeywords = listOf("pagamento", "payment", "pix", "cartão", "card", "pagar", "pay")
        val greetingKeywords = listOf("olá", "oi", "bom dia", "boa tarde", "boa noite", "tudo bem", "e aí", "eai")
        val profileKeywords = listOf("perfil", "profile", "conta", "account", "dados", "editar", "edit")
        val searchKeywords = listOf("buscar", "search", "encontrar", "find", "procurar", "look")
        
        return when {
            helpKeywords.any { lowerMessage.contains(it) } -> {
                kotlin.Result.success(
                    "Olá! Sou o assistente do TaskGo e estou aqui para ajudar. " +
                    "Posso te ajudar com:\n\n" +
                    "• **Serviços**: criar ordens, encontrar prestadores, acompanhar serviços\n" +
                    "• **Produtos**: buscar, comprar, gerenciar carrinho\n" +
                    "• **Pagamentos**: PIX, cartão, histórico de transações\n" +
                    "• **Perfil**: editar dados, ver histórico, avaliações\n" +
                    "• **Busca**: encontrar serviços e produtos rapidamente\n\n" +
                    "O que você gostaria de saber? 😊"
                )
            }
            serviceKeywords.any { lowerMessage.contains(it) } -> {
                kotlin.Result.success(
                    "No TaskGo você pode:\n\n" +
                    "1. **Criar ordem de serviço**: Descreva o que precisa e receba propostas de prestadores qualificados\n" +
                    "2. **Encontrar prestadores**: Veja perfis, avaliações e trabalhos anteriores\n" +
                    "3. **Acompanhar serviços**: Rastreie o status em tempo real\n" +
                    "4. **Avaliar**: Deixe sua opinião após o serviço ser concluído\n\n" +
                    "Quer ajuda com algo específico sobre serviços?"
                )
            }
            productKeywords.any { lowerMessage.contains(it) } -> {
                kotlin.Result.success(
                    "No marketplace de produtos do TaskGo você pode:\n\n" +
                    "• **Buscar produtos** por categoria ou nome\n" +
                    "• **Ver detalhes** completos com fotos e avaliações\n" +
                    "• **Adicionar ao carrinho** e finalizar compra\n" +
                    "• **Acompanhar pedidos** em tempo real\n\n" +
                    "Precisa de ajuda com alguma compra específica?"
                )
            }
            paymentKeywords.any { lowerMessage.contains(it) } -> {
                kotlin.Result.success(
                    "O TaskGo aceita múltiplas formas de pagamento:\n\n" +
                    "• **PIX**: Pagamento instantâneo e seguro\n" +
                    "• **Cartão de Crédito**: Parcelamento disponível\n" +
                    "• **Cartão de Débito**: Débito direto na conta\n" +
                    "• **Google Pay**: Pagamento rápido e integrado\n\n" +
                    "Todas as transações são seguras e protegidas. Precisa de ajuda com algum pagamento?"
                )
            }
            profileKeywords.any { lowerMessage.contains(it) } -> {
                kotlin.Result.success(
                    "No seu perfil do TaskGo você pode:\n\n" +
                    "• **Editar dados pessoais**: nome, telefone, endereço\n" +
                    "• **Alterar foto de perfil**: personalize sua conta\n" +
                    "• **Ver histórico**: serviços contratados e produtos comprados\n" +
                    "• **Avaliações recebidas**: veja o que outros usuários disseram\n" +
                    "• **Configurações**: ajuste preferências e notificações\n\n" +
                    "Quer ajuda com alguma configuração específica?"
                )
            }
            searchKeywords.any { lowerMessage.contains(it) } -> {
                kotlin.Result.success(
                    "A busca do TaskGo permite encontrar:\n\n" +
                    "• **Serviços**: por categoria, localização ou palavra-chave\n" +
                    "• **Produtos**: por nome, categoria ou descrição\n" +
                    "• **Prestadores**: por especialidade ou avaliação\n\n" +
                    "Use filtros avançados para refinar sua busca. O que você está procurando?"
                )
            }
            greetingKeywords.any { lowerMessage.contains(it) } -> {
                kotlin.Result.success(
                    "Olá! Que bom te ver por aqui! 😊\n\n" +
                    "Sou o assistente do TaskGo e estou aqui para ajudar você a aproveitar " +
                    "ao máximo nossa plataforma de serviços e produtos.\n\n" +
                    "Posso te ajudar com serviços, produtos, pagamentos, perfil e muito mais. " +
                    "Como posso te ajudar hoje?"
                )
            }
            else -> {
                kotlin.Result.success(
                    "Olá! Sou o assistente do TaskGo. " +
                    "No momento, estou com limitações de conexão, mas posso te ajudar com informações básicas.\n\n" +
                    "O TaskGo é uma plataforma completa para contratar serviços e comprar produtos. " +
                    "Você pode criar ordens de serviço, buscar prestadores, comprar produtos e muito mais.\n\n" +
                    "Tente novamente em alguns instantes para uma resposta mais completa, ou me pergunte sobre " +
                    "serviços, produtos, pagamentos ou qualquer funcionalidade do app! 😊"
                )
            }
        }
    }
    
    private suspend fun trySendMessageWithModel(
        messages: List<ChatMessage>,
        systemInstruction: String?,
        modelName: String
    ): kotlin.Result<String> {
        try {
            val modelUrl = "https://generativelanguage.googleapis.com/v1/models/$modelName:generateContent"
            
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
            
            // Construir request body - IMPORTANTE: systemInstruction não é suportado na API REST v1
            // Solução robusta: incorporar system instruction de forma contextual e natural
            val finalContents = if (systemInstruction != null && systemInstruction.isNotBlank() && contents.isNotEmpty()) {
                // Estratégia avançada: incorporar system instruction na primeira mensagem do usuário
                // de forma que o modelo entenda o contexto sem poluir a conversa
                val firstMessage = contents[0]
                val firstMessageRole = firstMessage["role"] as? String ?: "user"
                
                // Se a primeira mensagem for do usuário, incorporar system instruction de forma contextual
                if (firstMessageRole == "user") {
                    val originalParts = firstMessage["parts"] as? List<*> ?: emptyList<Any>()
                    val firstParts = mutableListOf<Map<String, Any>>()
                    
                    // Converter partes originais para o tipo correto
                    originalParts.forEach { part ->
                        when (val partMap = part as? Map<*, *>) {
                            null -> { /* Ignorar partes inválidas */ }
                            else -> {
                                val typedPartMap = mutableMapOf<String, Any>()
                                partMap.forEach { (key, value) ->
                                    val stringKey = key?.toString() ?: ""
                                    if (stringKey.isNotEmpty() && value != null) {
                                        typedPartMap[stringKey] = value
                                    }
                                }
                                
                                if (typedPartMap.isNotEmpty()) {
                                    firstParts.add(typedPartMap)
                                }
                            }
                        }
                    }
                    
                    // Encontrar a primeira parte de texto
                    val firstTextPartIndex = firstParts.indexOfFirst { 
                        it.containsKey("text")
                    }
                    
                    if (firstTextPartIndex >= 0) {
                        // Modificar a primeira parte de texto para incluir system instruction
                        val originalTextPart = firstParts[firstTextPartIndex]
                        val originalText = originalTextPart["text"] as? String ?: ""
                        
                        // Incorporar system instruction de forma elegante e contextual
                        val enhancedText = buildString {
                            append(systemInstruction)
                            append("\n\n---\n\n")
                            append(originalText)
                        }
                        
                        val enhancedTextPart = originalTextPart.toMutableMap()
                        enhancedTextPart["text"] = enhancedText
                        firstParts[firstTextPartIndex] = enhancedTextPart
                    } else {
                        // Se não houver parte de texto, adicionar system instruction como nova parte
                        firstParts.add(0, mapOf("text" to "$systemInstruction\n\n---\n\n"))
                    }
                    
                    val modifiedFirstMessage = firstMessage.toMutableMap()
                    modifiedFirstMessage["parts"] = firstParts
                    
                    listOf(modifiedFirstMessage) + contents.drop(1)
                } else {
                    // Se a primeira mensagem não for do usuário, adicionar system instruction como mensagem inicial
                val systemMessage = mapOf(
                    "role" to "user",
                    "parts" to listOf(mapOf("text" to systemInstruction))
                )
                    listOf(systemMessage) + contents
                }
            } else if (systemInstruction != null && systemInstruction.isNotBlank() && contents.isEmpty()) {
                // Se não houver mensagens, criar uma mensagem com o system instruction
                listOf(
                    mapOf(
                        "role" to "user",
                        "parts" to listOf(mapOf("text" to systemInstruction))
                    )
                )
            } else {
                contents
            }
            
            val requestBody = mutableMapOf<String, Any>("contents" to finalContents)
            
            // Otimizações de performance: aumentar tokens para respostas mais completas
            // Baseado em chat_manager.py que usa max_tokens: 4000
            requestBody["generationConfig"] = mapOf(
                "maxOutputTokens" to 2048, // Aumentado para respostas mais completas
                "temperature" to 0.7,
                "topP" to 0.95,
                "topK" to 40
            )
            
            val jsonBody = gson.toJson(requestBody)
            
            val request = Request.Builder()
                .url("$modelUrl?key=$apiKey")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return kotlin.Result.failure(Exception("Empty response"))
            
            if (!response.isSuccessful) {
                // Se for erro 404 (modelo não encontrado), tentar próximo modelo
                if (response.code == 404) {
                    android.util.Log.w("GoogleCloudAIService", "Modelo $modelName não encontrado (404). Tentando próximo modelo...")
                    return kotlin.Result.failure(Exception("Model not found: $modelName"))
                }
                return kotlin.Result.failure(Exception("API Error: ${response.code} - $responseBody"))
            }
            
            val chatResponse = gson.fromJson(responseBody, ChatResponse::class.java)
            val text = chatResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return kotlin.Result.failure(Exception("No response text"))
            
            android.util.Log.d("GoogleCloudAIService", "Resposta recebida com sucesso do modelo $modelName")
            return kotlin.Result.success(text)
        } catch (e: Exception) {
            android.util.Log.e("GoogleCloudAIService", "Erro ao enviar mensagem para modelo $modelName: ${e.message}", e)
            return kotlin.Result.failure(e)
        }
    }
}

