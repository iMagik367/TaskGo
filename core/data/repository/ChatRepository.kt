package com.example.taskgoapp.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class ChatRepository {
    fun getConversations(): Flow<List<Any>> {
        return flowOf(emptyList())
    }
    
    fun getMessagesForThread(threadId: Long): Flow<List<Any>> {
        return flowOf(emptyList())
    }
    
    fun sendMessage(message: Any) {
        // Em uma implementação real, isso seria enviado para um servidor
        // Por enquanto, apenas simulamos o envio
    }
}

