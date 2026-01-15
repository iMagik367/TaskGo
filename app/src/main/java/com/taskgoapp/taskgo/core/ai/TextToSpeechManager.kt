package com.taskgoapp.taskgo.core.ai

import android.content.Context
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

data class TTSState(
    val isInitialized: Boolean = false,
    val isSpeaking: Boolean = false,
    val currentText: String? = null
)

@Singleton
class TextToSpeechManager @Inject constructor(
    private val context: Context
) {
    private var tts: TextToSpeech? = null
    private val _state = MutableStateFlow(TTSState())
    val state: StateFlow<TTSState> = _state.asStateFlow()
    
    private val utteranceId = "tts_utterance_${System.currentTimeMillis()}"
    
    /**
     * Inicializa o Text-to-Speech
     */
    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = tts?.setLanguage(Locale("pt", "BR"))
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        // Tentar português geral se BR não estiver disponível
                        tts?.setLanguage(Locale("pt"))
                    }
                    
                    // Configurar velocidade e tom da voz (similar ao ChatGPT)
                    tts?.setSpeechRate(0.95f) // Velocidade ligeiramente mais lenta
                    tts?.setPitch(1.0f) // Tom natural
                    
                    // Configurar listener para acompanhar o progresso
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _state.value = _state.value.copy(isSpeaking = true)
                        }
                        
                        override fun onDone(utteranceId: String?) {
                            _state.value = _state.value.copy(isSpeaking = false, currentText = null)
                        }
                        
                        override fun onError(utteranceId: String?) {
                            _state.value = _state.value.copy(isSpeaking = false, currentText = null)
                        }
                    })
                    
                    _state.value = _state.value.copy(isInitialized = true)
                } else {
                    _state.value = _state.value.copy(isInitialized = false)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Fala um texto
     */
    suspend fun speak(text: String): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            if (!_state.value.isInitialized) {
                initialize()
                // Aguardar um pouco para inicializar
                kotlinx.coroutines.delay(500)
            }
            
            val ttsInstance = tts
            if (ttsInstance == null || !_state.value.isInitialized) {
                return@withContext Result.failure(Exception("TextToSpeech não inicializado"))
            }
            
            // Parar qualquer fala anterior
            stop()
            
            // Adicionar parâmetros para melhor qualidade (similar ao ChatGPT)
            val params = android.os.Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            }
            
            // Falar o texto
            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ttsInstance.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            } else {
                @Suppress("DEPRECATION")
                ttsInstance.speak(text, TextToSpeech.QUEUE_FLUSH, null)
                0
            }
            
            if (result == TextToSpeech.ERROR) {
                return@withContext Result.failure(Exception("Erro ao falar texto"))
            }
            
            _state.value = _state.value.copy(currentText = text)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Para a fala atual
     */
    suspend fun stop() = withContext(Dispatchers.Main) {
        try {
            tts?.stop()
            _state.value = _state.value.copy(isSpeaking = false, currentText = null)
        } catch (e: Exception) {
            // Ignorar erros
        }
    }
    
    /**
     * Libera recursos do TextToSpeech
     */
    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            _state.value = TTSState()
        } catch (e: Exception) {
            // Ignorar erros
        }
    }
    
    /**
     * Retorna se está falando no momento
     */
    fun isSpeaking(): Boolean = _state.value.isSpeaking
}
