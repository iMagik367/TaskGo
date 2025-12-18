package com.taskgoapp.taskgo.feature.chatai.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.taskgoapp.taskgo.feature.chatai.presentation.AiMessage
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("taskgo_ai_chats", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    fun saveMessages(chatId: String, messages: List<AiMessage>) {
        val messagesJson = gson.toJson(messages)
        prefs.edit().putString("chat_messages_$chatId", messagesJson).apply()
    }
    
    fun loadMessages(chatId: String): List<AiMessage> {
        val messagesJson = prefs.getString("chat_messages_$chatId", null)
        return if (messagesJson != null) {
            try {
                val type = object : TypeToken<List<AiMessage>>() {}.type
                gson.fromJson<List<AiMessage>>(messagesJson, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }
    
    fun deleteMessages(chatId: String) {
        prefs.edit().remove("chat_messages_$chatId").apply()
    }
    
    fun generateTitleFromMessage(message: String): String {
        // Limitar a 50 caracteres e remover quebras de linha
        val cleaned = message
            .replace("\n", " ")
            .replace("\r", " ")
            .trim()
        
        return if (cleaned.length > 50) {
            cleaned.take(47) + "..."
        } else {
            cleaned.ifEmpty { "Nova conversa" }
        }
    }
}

