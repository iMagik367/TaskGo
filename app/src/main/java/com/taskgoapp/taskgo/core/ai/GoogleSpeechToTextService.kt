package com.taskgoapp.taskgo.core.ai

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
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

data class SpeechRecognitionRequest(
    val config: RecognitionConfig,
    val audio: RecognitionAudio
)

data class RecognitionConfig(
    val encoding: String = "LINEAR16",
    val sampleRateHertz: Int = 16000,
    val languageCode: String = "pt-BR",
    val enableAutomaticPunctuation: Boolean = true
)

data class RecognitionAudio(
    val content: String? = null,
    val uri: String? = null
)

data class SpeechRecognitionResponse(
    val results: List<SpeechResult>?
)

data class SpeechResult(
    val alternatives: List<Alternative>?
)

data class Alternative(
    val transcript: String?,
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
        try {
            if (!hasMicrophonePermission()) {
                return@withContext kotlin.Result.failure(Exception("Microphone permission not granted"))
            }
            
            // Ler arquivo de áudio e converter para base64
            val audioBytes = audioFile.readBytes()
            val audioBase64 = android.util.Base64.encodeToString(audioBytes, android.util.Base64.NO_WRAP)
            
            val requestBody = SpeechRecognitionRequest(
                config = RecognitionConfig(
                    encoding = "LINEAR16",
                    sampleRateHertz = 16000,
                    languageCode = "pt-BR"
                ),
                audio = RecognitionAudio(content = audioBase64)
            )
            
            val jsonBody = gson.toJson(requestBody)
            
            val request = Request.Builder()
                .url("$baseUrl?key=$apiKey")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@withContext kotlin.Result.failure(Exception("Empty response"))
            
            if (!response.isSuccessful) {
                return@withContext kotlin.Result.failure(Exception("Speech API Error: ${response.code} - $responseBody"))
            }
            
            val recognitionResponse = gson.fromJson(responseBody, SpeechRecognitionResponse::class.java)
            val transcript = recognitionResponse.results?.firstOrNull()?.alternatives?.firstOrNull()?.transcript
                ?: return@withContext kotlin.Result.failure(Exception("No transcript found"))
            
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

