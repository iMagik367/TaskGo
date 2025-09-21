package com.example.taskgoapp.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class ChatRepository {
    fun getConversations(): Flow<List<Any>> {
        return flowOf(emptyList())
    }
    
    fun getMessagesForThread(threadId: Long): Flow<List<Any>> {
        // TODO: Usar threadId para filtrar mensagens do thread específico
        return flowOf(emptyList())
    }
    
    fun sendMessage(message: Any) {
        // TODO: Enviar a mensagem para o servidor em uma implementação real
        // message: contém o conteúdo e metadados da mensagem a ser enviada
    }
}

