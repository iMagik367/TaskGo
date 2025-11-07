package com.taskgoapp.taskgo.feature.messages.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskgoapp.taskgo.core.model.ChatMessage
import com.taskgoapp.taskgo.core.model.MessageThread
import com.taskgoapp.taskgo.domain.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MessagesUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val threads: List<MessageThread> = emptyList()
)

data class ChatUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val thread: MessageThread? = null,
    val messages: List<ChatMessage> = emptyList()
)

@HiltViewModel
class MessagesViewModel @Inject constructor(
    private val messageRepository: MessageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessagesUiState())
    val uiState: StateFlow<MessagesUiState> = _uiState.asStateFlow()

    val threads: StateFlow<List<MessageThread>> = messageRepository
        .observeThreads()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    fun getChatState(threadId: String): StateFlow<ChatUiState> {
        val stateFlow = MutableStateFlow(ChatUiState())

        viewModelScope.launch {
            // Observar thread
            messageRepository.getThread(threadId)?.let { thread ->
                stateFlow.value = stateFlow.value.copy(thread = thread)
            }

            // Observar mensagens
            messageRepository.observeMessages(threadId).collect { messages ->
                stateFlow.value = stateFlow.value.copy(messages = messages)
            }
        }

        return stateFlow.asStateFlow()
    }

    init {
        loadThreads()
    }

    private fun loadThreads() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                // Os dados vêm automaticamente via Flow do repositório
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Erro ao carregar mensagens"
                )
            }
        }
    }

    fun sendMessage(threadId: String, text: String) {
        viewModelScope.launch {
            try {
                messageRepository.sendMessage(threadId, text)
            } catch (e: Exception) {
                // Tratar erro se necessário
            }
        }
    }

    fun createThread(title: String): String? {
        var threadId: String? = null
        viewModelScope.launch {
            try {
                threadId = messageRepository.createThread(title)
            } catch (e: Exception) {
                // Tratar erro se necessário
            }
        }
        return threadId
    }

    fun refresh() {
        loadThreads()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

