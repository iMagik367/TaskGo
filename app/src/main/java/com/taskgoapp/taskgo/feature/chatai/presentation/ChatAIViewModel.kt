package com.taskgoapp.taskgo.feature.chatai.presentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskgoapp.taskgo.core.ai.AppSystemPrompt
import com.taskgoapp.taskgo.core.ai.GoogleCloudAIService
import com.taskgoapp.taskgo.core.ai.GoogleTranslationService
import com.taskgoapp.taskgo.core.ai.ImageData
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

data class ChatUiState(
    val chatId: String? = null,
    val messages: List<AiMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRecording: Boolean = false,
    val sourceLanguage: String = "pt",
    val targetLanguage: String = "pt"
)

@HiltViewModel
class ChatAIViewModel @Inject constructor(
    private val aiService: GoogleCloudAIService,
    private val translationService: GoogleTranslationService,
    private val chatStorage: ChatStorage,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    fun initializeChat(chatId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(chatId = chatId)
            // Carregar mensagens salvas do storage
            val savedMessages = chatStorage.loadMessages(chatId)
            _uiState.value = _uiState.value.copy(messages = savedMessages)
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
                // Converter anexos de imagem para base64
                val imageDataList = attachments
                    .filter { it.type == com.taskgoapp.taskgo.feature.chatai.data.AttachmentType.IMAGE }
                    .mapNotNull { attachment ->
                        convertImageToBase64(attachment.uri, attachment.mimeType)
                    }
                
                // Converter histórico de mensagens para formato da API
                val chatMessages = _uiState.value.messages.map { msg ->
                    val msgImageData = msg.attachments
                        .filter { it.type == com.taskgoapp.taskgo.feature.chatai.data.AttachmentType.IMAGE }
                        .mapNotNull { attachment ->
                            convertImageToBase64(attachment.uri, attachment.mimeType)
                        }
                    
                    com.taskgoapp.taskgo.core.ai.ChatMessage(
                        role = if (msg.isFromAi) "assistant" else "user",
                        content = msg.text,
                        imageData = msgImageData
                    )
                }
                
                // Adicionar system prompt
                val result = aiService.sendMessage(chatMessages, AppSystemPrompt.SYSTEM_MESSAGE)
                result.fold(
                    onSuccess = { responseText ->
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
                        
                        // Salvar mensagens no storage
                        _uiState.value.chatId?.let { chatId ->
                            chatStorage.saveMessages(chatId, updatedMessages)
                        }
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message ?: "Erro ao processar mensagem"
                        )
                    }
                )
            } catch (e: Exception) {
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
    
    fun setRecording(isRecording: Boolean) {
        _uiState.value = _uiState.value.copy(isRecording = isRecording)
    }
    
    fun setTargetLanguage(language: String) {
        _uiState.value = _uiState.value.copy(targetLanguage = language)
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
