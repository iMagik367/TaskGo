package com.taskgoapp.taskgo.feature.chatai.presentation

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.taskgoapp.taskgo.feature.chatai.data.ChatSession
import com.taskgoapp.taskgo.feature.chatai.data.ChatStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.UUID

data class ChatListUiState(
    val chats: List<ChatSession> = emptyList(),
    val filteredChats: List<ChatSession> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ChatListViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val chatStorage: ChatStorage
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChatListUiState())
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()
    
    private val prefs: SharedPreferences = context.getSharedPreferences("taskgo_ai_chats", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val CHATS_KEY = "ai_chats_list"
    
    init {
        loadChats()
    }
    
    private fun loadChats() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val chats = getChatsFromStorage()
                _uiState.value = _uiState.value.copy(
                    chats = chats,
                    filteredChats = filterChats(chats, _uiState.value.searchQuery),
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    fun createNewChat(): String {
        val newChatId = UUID.randomUUID().toString()
        val newChat = ChatSession(
            id = newChatId,
            title = "Nova conversa",
            timestamp = System.currentTimeMillis()
        )
        
        val updatedChats = listOf(newChat) + _uiState.value.chats
        _uiState.value = _uiState.value.copy(
            chats = updatedChats,
            filteredChats = filterChats(updatedChats, _uiState.value.searchQuery)
        )
        saveChatsToStorage(updatedChats)
        
        return newChatId
    }
    
    fun deleteChat(chatId: String) {
        val updatedChats = _uiState.value.chats.filter { it.id != chatId }
        _uiState.value = _uiState.value.copy(
            chats = updatedChats,
            filteredChats = filterChats(updatedChats, _uiState.value.searchQuery)
        )
        saveChatsToStorage(updatedChats)
        // Deletar mensagens do chat também
        chatStorage.deleteMessages(chatId)
    }
    
    fun updateChatLastMessage(chatId: String, lastMessage: String, isFirstMessage: Boolean = false) {
        val updatedChats = _uiState.value.chats.map { chat ->
            if (chat.id == chatId) {
                val newTitle = if (isFirstMessage && (chat.title == "Nova conversa" || chat.title.isBlank())) {
                    chatStorage.generateTitleFromMessage(lastMessage)
                } else {
                    chat.title
                }
                chat.copy(
                    title = newTitle,
                    lastMessage = lastMessage,
                    timestamp = System.currentTimeMillis(),
                    messageCount = chat.messageCount + 1
                )
            } else chat
        }.sortedByDescending { it.timestamp }
        
        _uiState.value = _uiState.value.copy(
            chats = updatedChats,
            filteredChats = filterChats(updatedChats, _uiState.value.searchQuery)
        )
        saveChatsToStorage(updatedChats)
    }
    
    fun updateChatTitle(chatId: String, title: String) {
        val updatedChats = _uiState.value.chats.map { chat ->
            if (chat.id == chatId) {
                chat.copy(title = title)
            } else chat
        }
        _uiState.value = _uiState.value.copy(
            chats = updatedChats,
            filteredChats = filterChats(updatedChats, _uiState.value.searchQuery)
        )
        saveChatsToStorage(updatedChats)
    }
    
    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredChats = filterChats(_uiState.value.chats, query)
        )
    }
    
    private fun filterChats(chats: List<ChatSession>, query: String): List<ChatSession> {
        if (query.isBlank()) return chats
        val lowerQuery = query.lowercase()
        return chats.filter { chat ->
            chat.title.lowercase().contains(lowerQuery) ||
            chat.lastMessage?.lowercase()?.contains(lowerQuery) == true
        }
    }
    
    private fun getChatsFromStorage(): List<ChatSession> {
        val chatsJson = prefs.getString(CHATS_KEY, null)
        return if (chatsJson != null) {
            try {
                val type = object : TypeToken<List<ChatSession>>() {}.type
                gson.fromJson<List<ChatSession>>(chatsJson, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }
    
    private fun saveChatsToStorage(chats: List<ChatSession>) {
        val chatsJson = gson.toJson(chats)
        prefs.edit().putString(CHATS_KEY, chatsJson).apply()
    }
}

