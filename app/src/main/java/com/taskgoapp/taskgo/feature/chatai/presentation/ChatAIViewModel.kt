package com.taskgoapp.taskgo.feature.chatai.presentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskgoapp.taskgo.core.ai.AppSystemPrompt
import com.taskgoapp.taskgo.core.ai.AudioRecorderManager
import com.taskgoapp.taskgo.core.ai.GoogleCloudAIService
import com.taskgoapp.taskgo.core.ai.GoogleSpeechToTextService
import com.taskgoapp.taskgo.core.ai.GoogleTranslationService
import com.taskgoapp.taskgo.core.ai.ImageData
import com.taskgoapp.taskgo.core.ai.TextToSpeechManager
import com.taskgoapp.taskgo.feature.chatai.data.ChatAttachment
import com.taskgoapp.taskgo.feature.chatai.data.ChatStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import android.util.Base64 as AndroidBase64
import javax.inject.Inject

/**
 * Máquina de estados para o fluxo de voz
 * Baseado no padrão de chat_voice_widget.py (VoiceState enum)
 */
enum class VoiceState {
    IDLE,           // Estado inicial, pronto para gravar
    RECORDING,      // Gravando áudio do usuário
    PROCESSING,     // Processando transcrição ou resposta da AI
    SPEAKING        // Reproduzindo resposta da AI em voz
}

data class ChatUiState(
    val chatId: String? = null,
    val messages: List<AiMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRecording: Boolean = false,
    val isSpeaking: Boolean = false,
    val voiceState: VoiceState = VoiceState.IDLE, // Máquina de estados para voz
    val sourceLanguage: String = "pt",
    val targetLanguage: String = "pt"
)

@HiltViewModel
class ChatAIViewModel @Inject constructor(
    private val aiService: GoogleCloudAIService,
    private val translationService: GoogleTranslationService,
    private val speechToTextService: GoogleSpeechToTextService,
    private val audioRecorderManager: AudioRecorderManager,
    private val textToSpeechManager: TextToSpeechManager,
    private val chatStorage: ChatStorage,
    private val functionsService: com.taskgoapp.taskgo.data.firebase.FirebaseFunctionsService,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    fun initializeChat(chatId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(chatId = chatId)
            // Carregar mensagens salvas do storage local
            val savedMessages = chatStorage.loadMessages(chatId)
            _uiState.value = _uiState.value.copy(messages = savedMessages)
            
            // Tentar carregar do Firestore também (se existir)
            try {
                val historyResult = functionsService.getConversationHistory(chatId)
                historyResult.fold(
                    onSuccess = { data ->
                        // Se houver mensagens no Firestore, usar elas (mais atualizadas)
                        val firestoreMessages = (data["messages"] as? List<Map<String, Any>>)?.mapNotNull { msgData ->
                            try {
                                val role = msgData["role"] as? String ?: return@mapNotNull null
                                val content = msgData["content"] as? String ?: return@mapNotNull null
                                val timestamp = (msgData["timestamp"] as? Map<String, Any>)?.let {
                                    // Converter Firestore Timestamp para Long
                                    val seconds = (it["_seconds"] as? Number)?.toLong() ?: 0L
                                    val nanos = (it["_nanoseconds"] as? Number)?.toLong() ?: 0L
                                    seconds * 1000 + nanos / 1_000_000
                                } ?: System.currentTimeMillis()
                                
                                AiMessage(
                                    id = timestamp,
                                    text = content,
                                    isFromAi = role == "assistant",
                                    timestamp = timestamp
                                )
                            } catch (e: Exception) {
                                null
                            }
                        } ?: emptyList()
                        
                        if (firestoreMessages.isNotEmpty()) {
                            _uiState.value = _uiState.value.copy(messages = firestoreMessages)
                            // Sincronizar com storage local
                            chatStorage.saveMessages(chatId, firestoreMessages)
                        }
                    },
                    onFailure = {
                        // Se não encontrar no Firestore, usar mensagens locais
                        android.util.Log.d("ChatAIViewModel", "Conversa não encontrada no Firestore, usando mensagens locais")
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("ChatAIViewModel", "Erro ao carregar histórico do Firestore: ${e.message}", e)
            }
        }
    }
    
    fun sendMessage(text: String, attachments: List<ChatAttachment> = emptyList(), onFirstMessageSent: ((String) -> Unit)? = null) {
        if (text.isBlank() && attachments.isEmpty()) return
        
        val isFirstMessage = _uiState.value.messages.isEmpty()
        
        val userMessage = AiMessage(
            id = System.currentTimeMillis(),
            text = text,
            isFromAi = false,
            timestamp = System.currentTimeMillis(),
            attachments = attachments
        )
        
        val updatedMessages = _uiState.value.messages + userMessage
        _uiState.value = _uiState.value.copy(
            messages = updatedMessages,
            isLoading = true,
            error = null
        )
        
        // Se for a primeira mensagem, gerar título e notificar
        if (isFirstMessage && text.isNotBlank()) {
            val title = chatStorage.generateTitleFromMessage(text)
            onFirstMessageSent?.invoke(title)
        }
        
        // Salvar mensagem do usuário imediatamente
        _uiState.value.chatId?.let { chatId ->
            chatStorage.saveMessages(chatId, updatedMessages)
        }
        
        viewModelScope.launch {
            try {
                // Criar ou obter conversationId
                var conversationId = _uiState.value.chatId
                if (conversationId == null) {
                    // Criar nova conversa no Firestore
                    val createResult = functionsService.createConversation()
                    createResult.fold(
                        onSuccess = { data ->
                            conversationId = data["conversationId"] as? String
                            if (conversationId != null) {
                                _uiState.value = _uiState.value.copy(chatId = conversationId)
                            }
                        },
                        onFailure = {
                            android.util.Log.w("ChatAIViewModel", "Erro ao criar conversa no Firestore, continuando sem conversationId")
                        }
                    )
                }
                
                // Usar Cloud Function aiChatProxy que salva automaticamente no Firestore
                val chatResult = functionsService.aiChatProxy(text, conversationId)
                chatResult.fold(
                    onSuccess = { data ->
                        val responseText = data["response"] as? String ?: ""
                        val returnedConversationId = data["conversationId"] as? String
                        
                        // Atualizar conversationId se foi criado
                        if (returnedConversationId != null && _uiState.value.chatId == null) {
                            _uiState.value = _uiState.value.copy(chatId = returnedConversationId)
                        }
                        
                        val aiMessage = AiMessage(
                            id = System.currentTimeMillis(),
                            text = responseText,
                            isFromAi = true,
                            timestamp = System.currentTimeMillis()
                        )
                        val updatedMessages = _uiState.value.messages + aiMessage
                        _uiState.value = _uiState.value.copy(
                            messages = updatedMessages,
                            isLoading = false
                        )
                        
                        // Salvar mensagens no storage local também (para cache offline)
                        _uiState.value.chatId?.let { chatId ->
                            chatStorage.saveMessages(chatId, updatedMessages)
                        }
                        
                        android.util.Log.d("ChatAIViewModel", "Resposta da AI recebida com sucesso e salva no Firestore")
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message ?: "Erro ao processar mensagem"
                        )
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("ChatAIViewModel", "Erro ao enviar mensagem: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Erro desconhecido"
                )
            }
        }
    }
    
    fun translateMessage(message: AiMessage, targetLanguage: String) {
        viewModelScope.launch {
            try {
                val result = translationService.translateText(message.text, targetLanguage)
                result.fold(
                    onSuccess = { translatedText ->
                        val translatedMessage = message.copy(text = translatedText)
                        val updatedMessages = _uiState.value.messages.map {
                            if (it.id == message.id) translatedMessage else it
                        }
                        _uiState.value = _uiState.value.copy(messages = updatedMessages)
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            error = error.message ?: "Erro ao traduzir"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Erro ao traduzir"
                )
            }
        }
    }
    
    /**
     * Atualiza o estado da voz (máquina de estados)
     * Baseado no padrão de chat_voice_widget.py (_set_state)
     */
    private fun setVoiceState(newState: VoiceState) {
        val currentState = _uiState.value.voiceState
        if (currentState == newState) return
        
        android.util.Log.d("ChatAIViewModel", "[STATE_MACHINE] Mudando de ${currentState.name} para ${newState.name}")
        _uiState.value = _uiState.value.copy(
            voiceState = newState,
            isRecording = newState == VoiceState.RECORDING,
            isLoading = newState == VoiceState.PROCESSING
        )
    }
    
    fun setRecording(isRecording: Boolean) {
        val newState = if (isRecording) VoiceState.RECORDING else VoiceState.IDLE
        setVoiceState(newState)
    }
    
    /**
     * Inicia a gravação de áudio
     * Baseado no padrão de chat_voice_widget.py (_on_main_button_action)
     */
    fun startAudioRecording() {
        if (_uiState.value.voiceState != VoiceState.IDLE) {
            android.util.Log.w("ChatAIViewModel", "Tentativa de iniciar gravação em estado inválido: ${_uiState.value.voiceState}")
            return
        }
        
        viewModelScope.launch {
            try {
                setVoiceState(VoiceState.RECORDING)
                val result = audioRecorderManager.startRecording()
                result.fold(
                    onSuccess = { file ->
                        // Gravação iniciada com sucesso
                        android.util.Log.d("ChatAIViewModel", "Gravação iniciada: ${file.absolutePath}")
                    },
                    onFailure = { error ->
                        android.util.Log.e("ChatAIViewModel", "Erro ao iniciar gravação: ${error.message}", error)
                        setVoiceState(VoiceState.IDLE)
                        _uiState.value = _uiState.value.copy(
                            error = "Erro ao iniciar gravação: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("ChatAIViewModel", "Erro ao iniciar gravação: ${e.message}", e)
                setVoiceState(VoiceState.IDLE)
                _uiState.value = _uiState.value.copy(
                    error = "Erro ao iniciar gravação: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Para a gravação de áudio e converte para texto
     * Baseado no padrão de chat_voice_widget.py (_on_transcribed_text)
     */
    fun stopAudioRecordingAndSend() {
        if (_uiState.value.voiceState != VoiceState.RECORDING) {
            android.util.Log.w("ChatAIViewModel", "Tentativa de parar gravação em estado inválido: ${_uiState.value.voiceState}")
            return
        }
        
        viewModelScope.launch {
            try {
                setVoiceState(VoiceState.PROCESSING)
                
                val result = audioRecorderManager.stopRecording()
                result.fold(
                    onSuccess = { audioFile ->
                        if (audioFile != null && audioFile.exists()) {
                            // Converter áudio para texto
                            android.util.Log.d("ChatAIViewModel", "Convertendo áudio para texto: ${audioFile.absolutePath}")
                            val speechResult = speechToTextService.recognizeSpeech(audioFile)
                            
                            speechResult.fold(
                                onSuccess = { transcript ->
                                    android.util.Log.d("ChatAIViewModel", "Transcrição: $transcript")
                                    // Enviar mensagem como texto
                                    if (transcript.isNotBlank()) {
                                        // Enviar mensagem (que já gerencia o estado de loading)
                                        sendMessage(transcript.trim())
                                        // Voltar para IDLE após enviar
                                        setVoiceState(VoiceState.IDLE)
                                    } else {
                                        setVoiceState(VoiceState.IDLE)
                                        _uiState.value = _uiState.value.copy(
                                            error = "Não foi possível reconhecer a fala. Tente novamente."
                                        )
                                    }
                                    // Deletar arquivo temporário após processar
                                    audioFile.delete()
                                },
                                onFailure = { error ->
                                    android.util.Log.e("ChatAIViewModel", "Erro ao converter áudio: ${error.message}", error)
                                    setVoiceState(VoiceState.IDLE)
                                    _uiState.value = _uiState.value.copy(
                                        error = "Erro ao converter áudio para texto: ${error.message}"
                                    )
                                    // Deletar arquivo temporário em caso de erro
                                    audioFile.delete()
                                }
                            )
                        } else {
                            setVoiceState(VoiceState.IDLE)
                            _uiState.value = _uiState.value.copy(
                                error = "Nenhum áudio foi gravado"
                            )
                        }
                    },
                    onFailure = { error ->
                        android.util.Log.e("ChatAIViewModel", "Erro ao parar gravação: ${error.message}", error)
                        setVoiceState(VoiceState.IDLE)
                        _uiState.value = _uiState.value.copy(
                            error = "Erro ao parar gravação: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("ChatAIViewModel", "Erro ao parar gravação: ${e.message}", e)
                setVoiceState(VoiceState.IDLE)
                _uiState.value = _uiState.value.copy(
                    error = "Erro ao parar gravação: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Cancela a gravação atual
     * Baseado no padrão de chat_voice_widget.py (_exit_voice_mode)
     */
    fun cancelAudioRecording() {
        viewModelScope.launch {
            try {
                audioRecorderManager.cancelRecording()
                setVoiceState(VoiceState.IDLE)
                _uiState.value = _uiState.value.copy(error = null)
            } catch (e: Exception) {
                android.util.Log.e("ChatAIViewModel", "Erro ao cancelar gravação: ${e.message}", e)
                setVoiceState(VoiceState.IDLE)
                _uiState.value = _uiState.value.copy(
                    error = "Erro ao cancelar gravação: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Fala uma resposta em voz alta (Text-to-Speech)
     * Baseado no padrão de chat_voice_widget.py (_tts_speak)
     */
    fun speakResponse(text: String) {
        if (_uiState.value.voiceState == VoiceState.RECORDING) {
            android.util.Log.w("ChatAIViewModel", "Não é possível falar enquanto está gravando")
            return
        }
        
        viewModelScope.launch {
            try {
                setVoiceState(VoiceState.SPEAKING)
                val result = textToSpeechManager.speak(text)
                result.fold(
                    onSuccess = {
                        android.util.Log.d("ChatAIViewModel", "Texto sendo falado: $text")
                    },
                    onFailure = { error ->
                        android.util.Log.e("ChatAIViewModel", "Erro ao falar resposta: ${error.message}", error)
                        setVoiceState(VoiceState.IDLE)
                        _uiState.value = _uiState.value.copy(
                            error = "Erro ao falar resposta: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("ChatAIViewModel", "Erro ao falar resposta: ${e.message}", e)
                setVoiceState(VoiceState.IDLE)
                _uiState.value = _uiState.value.copy(
                    error = "Erro ao falar resposta: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Para a fala atual
     * Baseado no padrão de chat_voice_widget.py (_on_tts_finished)
     */
    fun stopSpeaking() {
        viewModelScope.launch {
            textToSpeechManager.stop()
            setVoiceState(VoiceState.IDLE)
        }
    }
    
    // Observar estado do TTS e atualizar máquina de estados
    // Baseado no padrão de chat_voice_widget.py (monitoramento de estado)
    init {
        viewModelScope.launch {
            while (true) {
                val isSpeaking = textToSpeechManager.isSpeaking()
                if (isSpeaking && _uiState.value.voiceState != VoiceState.SPEAKING) {
                    setVoiceState(VoiceState.SPEAKING)
                } else if (!isSpeaking && _uiState.value.voiceState == VoiceState.SPEAKING) {
                    setVoiceState(VoiceState.IDLE)
                }
                kotlinx.coroutines.delay(200) // Verificar a cada 200ms
            }
        }
    }
    
    /**
     * Obtém a amplitude do áudio para animação visual
     */
    fun getAudioAmplitude(): Double {
        return audioRecorderManager.getAmplitude()
    }
    
    fun setTargetLanguage(language: String) {
        _uiState.value = _uiState.value.copy(targetLanguage = language)
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    private suspend fun convertImageToBase64(uri: Uri, mimeType: String?): ImageData? = withContext(Dispatchers.IO) {
        try {
            // Usa BitmapUtils para carregar e redimensionar a imagem
            val bitmap = com.taskgoapp.taskgo.core.utils.BitmapUtils.loadAndResizeBitmap(
                context, 
                uri,
                maxDimension = 1024 // Limita a 1024px para envio via API
            ) ?: return@withContext null
            
            val outputStream = ByteArrayOutputStream()
            val format = when (mimeType) {
                "image/png" -> Bitmap.CompressFormat.PNG
                "image/jpeg", "image/jpg" -> Bitmap.CompressFormat.JPEG
                "image/webp" -> Bitmap.CompressFormat.WEBP
                else -> Bitmap.CompressFormat.JPEG
            }
            
            bitmap.compress(format, 85, outputStream)
            val byteArray = outputStream.toByteArray()
            val base64 = AndroidBase64.encodeToString(byteArray, AndroidBase64.NO_WRAP)
            
            // Libera memória do bitmap
            bitmap.recycle()
            
            ImageData(
                mimeType = mimeType ?: "image/jpeg",
                base64Data = base64
            )
        } catch (e: Exception) {
            null
        }
    }
}
