package com.taskgoapp.taskgo.core.ai

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Request para Google Speech-to-Text API v1
 * Usar @SerializedName para garantir serialização correta do JSON
 */
data class SpeechRecognitionRequest(
    @SerializedName("config")
    val config: RecognitionConfig,
    @SerializedName("audio")
    val audio: RecognitionAudio
)

/**
 * Configuração de reconhecimento de fala
 */
data class RecognitionConfig(
    @SerializedName("encoding")
    val encoding: String = "LINEAR16",
    @SerializedName("sampleRateHertz")
    val sampleRateHertz: Int = 16000,
    @SerializedName("languageCode")
    val languageCode: String = "pt-BR",
    @SerializedName("enableAutomaticPunctuation")
    val enableAutomaticPunctuation: Boolean = true
)

/**
 * Áudio para reconhecimento (content em base64 ou URI)
 */
data class RecognitionAudio(
    @SerializedName("content")
    val content: String? = null,
    @SerializedName("uri")
    val uri: String? = null
)

/**
 * Response do Google Speech-to-Text API v1
 */
data class SpeechRecognitionResponse(
    @SerializedName("results")
    val results: List<SpeechResult>?
)

data class SpeechResult(
    @SerializedName("alternatives")
    val alternatives: List<Alternative>?
)

data class Alternative(
    @SerializedName("transcript")
    val transcript: String?,
    @SerializedName("confidence")
    val confidence: Double?
)

@Singleton
class GoogleSpeechToTextService @Inject constructor(
    private val apiKey: String,
    private val context: Context
) {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val baseUrl = "https://speech.googleapis.com/v1/speech:recognize"
    
    fun hasMicrophonePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    suspend fun recognizeSpeech(audioFile: File): kotlin.Result<String> = withContext(Dispatchers.IO) {
        // Sistema de retry com backoff exponencial (baseado em engine.py e adaptive_fuzzer.py)
        val maxRetries = 3
        var lastError: Exception? = null
        
        for (attempt in 0 until maxRetries) {
            try {
                if (!hasMicrophonePermission()) {
                    return@withContext kotlin.Result.failure(Exception("Microphone permission not granted"))
                }
                
                val result = tryRecognizeSpeech(audioFile, attempt)
                if (result.isSuccess) {
                    return@withContext result
                } else {
                    lastError = result.exceptionOrNull() as? Exception
                }
            } catch (e: Exception) {
                lastError = e
            }
            
            // Backoff exponencial: 1s, 2s, 4s
            if (attempt < maxRetries - 1) {
                val delayMs = (1 shl attempt) * 1000L
                android.util.Log.w("GoogleSpeechToTextService", "Tentativa ${attempt + 1} falhou, aguardando ${delayMs}ms...")
                kotlinx.coroutines.delay(delayMs)
            }
        }
        
        // Se todas as tentativas falharam, retornar erro com mensagem amigável
        android.util.Log.e("GoogleSpeechToTextService", "Todas as tentativas de reconhecimento falharam")
        return@withContext kotlin.Result.failure(
            lastError ?: Exception("Não foi possível reconhecer o áudio. Verifique sua conexão e tente novamente.")
        )
    }
    
    private suspend fun tryRecognizeSpeech(
        audioFile: File,
        attempt: Int
    ): kotlin.Result<String> = withContext(Dispatchers.IO) {
        try {
            
            // Detectar formato do arquivo e configurar encoding apropriado
            // IMPORTANTE: Google Speech-to-Text API requer encoding específico baseado no formato real do arquivo
            val fileName = audioFile.name.lowercase()
            val (encoding, sampleRate) = when {
                fileName.endsWith(".wav") -> Pair("LINEAR16", 16000)
                fileName.endsWith(".flac") -> Pair("FLAC", 16000)
                fileName.endsWith(".m4a") || fileName.endsWith(".aac") -> Pair("MP3", 16000) // M4A/AAC são tratados como MP3
                fileName.endsWith(".mp4") -> Pair("MP3", 16000)
                fileName.endsWith(".ogg") || fileName.endsWith(".opus") -> Pair("OGG_OPUS", 16000)
                fileName.endsWith(".amr") || fileName.endsWith(".3gp") -> Pair("AMR", 8000)
                fileName.endsWith(".amr_wb") -> Pair("AMR_WB", 16000)
                else -> {
                    // Para arquivos desconhecidos, tentar detectar pelo conteúdo ou usar LINEAR16 como padrão
                    android.util.Log.w("GoogleSpeechToTextService", "Formato de arquivo desconhecido: $fileName. Usando LINEAR16 como padrão.")
                    Pair("LINEAR16", 16000)
                }
            }
            
            // Ler arquivo de áudio e converter para base64
            val audioBytes = audioFile.readBytes()
            if (audioBytes.isEmpty()) {
                return@withContext kotlin.Result.failure(Exception("Arquivo de áudio vazio"))
            }
            
            val audioBase64 = android.util.Base64.encodeToString(audioBytes, android.util.Base64.NO_WRAP)
            
            // Construir request body com serialização explícita para evitar campos inválidos
            val requestBody = SpeechRecognitionRequest(
                config = RecognitionConfig(
                    encoding = encoding,
                    sampleRateHertz = sampleRate,
                    languageCode = "pt-BR",
                    enableAutomaticPunctuation = true
                ),
                audio = RecognitionAudio(content = audioBase64)
            )
            
            // Serializar JSON com configuração explícita para evitar problemas de serialização
            val jsonBody = try {
                gson.toJson(requestBody)
            } catch (e: Exception) {
                android.util.Log.e("GoogleSpeechToTextService", "Erro ao serializar request body: ${e.message}", e)
                return@withContext kotlin.Result.failure(Exception("Erro ao preparar requisição: ${e.message}"))
            }
            
            // Log do JSON para debug (apenas em desenvolvimento)
            android.util.Log.d("GoogleSpeechToTextService", "JSON Request: $jsonBody")
            
            val request = Request.Builder()
                .url("$baseUrl?key=$apiKey")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@withContext kotlin.Result.failure(Exception("Resposta vazia da API"))
            
            if (!response.isSuccessful) {
                // Log detalhado do erro para debug
                android.util.Log.e("GoogleSpeechToTextService", "Speech API Error ${response.code}: $responseBody")
                
                // Tentar extrair mensagem de erro mais clara do JSON de resposta
                val errorMessage = try {
                    val errorJson = gson.fromJson(responseBody, Map::class.java) as? Map<*, *>
                    val error = errorJson?.get("error") as? Map<*, *>
                    error?.get("message") as? String ?: responseBody
                } catch (e: Exception) {
                    responseBody
                }
                
                return@withContext kotlin.Result.failure(
                    Exception("Erro ao converter áudio para texto: Speech API Error: ${response.code} - $errorMessage")
                )
            }
            
            // Parse da resposta usando deserialização manual segura para evitar problemas com classes internas do Google
            val transcript = try {
                // Usar JsonObject para deserialização manual e segura
                val jsonObject = gson.fromJson(responseBody, JsonObject::class.java)
                
                // Extrair transcript de forma segura, ignorando campos desconhecidos
                val resultsArray = jsonObject.getAsJsonArray("results")
                if (resultsArray == null || resultsArray.size() == 0) {
                    android.util.Log.w("GoogleSpeechToTextService", "Nenhum resultado encontrado na resposta. Response: $responseBody")
                    return@withContext kotlin.Result.failure(Exception("Nenhum texto foi reconhecido no áudio. Tente falar mais claramente."))
                }
                
                val firstResult = resultsArray[0].asJsonObject
                val alternativesArray = firstResult.getAsJsonArray("alternatives")
                
                if (alternativesArray == null || alternativesArray.size() == 0) {
                    android.util.Log.w("GoogleSpeechToTextService", "Nenhuma alternativa encontrada na resposta. Response: $responseBody")
                    return@withContext kotlin.Result.failure(Exception("Nenhum texto foi reconhecido no áudio. Tente falar mais claramente."))
                }
                
                val firstAlternative = alternativesArray[0].asJsonObject
                val transcriptElement = firstAlternative.get("transcript")
                
                if (transcriptElement == null || transcriptElement.isJsonNull) {
                    android.util.Log.w("GoogleSpeechToTextService", "Transcript vazio na resposta. Response: $responseBody")
                    return@withContext kotlin.Result.failure(Exception("Nenhum texto foi reconhecido no áudio. Tente falar mais claramente."))
                }
                
                transcriptElement.asString
            } catch (e: JsonSyntaxException) {
                android.util.Log.e("GoogleSpeechToTextService", "Erro de sintaxe JSON ao fazer parse da resposta: ${e.message}", e)
                android.util.Log.e("GoogleSpeechToTextService", "Response body: $responseBody")
                return@withContext kotlin.Result.failure(Exception("Erro ao processar resposta da API: Formato de resposta inválido"))
            } catch (e: IllegalStateException) {
                android.util.Log.e("GoogleSpeechToTextService", "Erro de estado ao fazer parse da resposta: ${e.message}", e)
                android.util.Log.e("GoogleSpeechToTextService", "Response body: $responseBody")
                return@withContext kotlin.Result.failure(Exception("Erro ao processar resposta da API: Estrutura de resposta inesperada"))
            } catch (e: Exception) {
                android.util.Log.e("GoogleSpeechToTextService", "Erro ao fazer parse da resposta: ${e.message}", e)
                android.util.Log.e("GoogleSpeechToTextService", "Response body: $responseBody")
                return@withContext kotlin.Result.failure(Exception("Erro ao processar resposta da API: ${e.message}"))
            }
            
            if (transcript.isBlank()) {
                android.util.Log.w("GoogleSpeechToTextService", "Transcript vazio após parse. Response: $responseBody")
                return@withContext kotlin.Result.failure(Exception("Nenhum texto foi reconhecido no áudio. Tente falar mais claramente."))
            }
            
            android.util.Log.d("GoogleSpeechToTextService", "Transcrição bem-sucedida: $transcript")
            kotlin.Result.success(transcript)
        } catch (e: Exception) {
            kotlin.Result.failure(e)
        }
    }
    
    fun createAudioRecord(): AudioRecord? {
        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        
        return AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )
    }
}

