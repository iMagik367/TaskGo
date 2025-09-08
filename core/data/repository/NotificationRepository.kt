package com.example.taskgoapp.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class NotificationRepository {
    fun getNotifications(): Flow<List<Any>> {
        return flowOf(emptyList())
    }
    
    fun markAsRead(notificationId: Long) {
        // Em uma implementação real, isso seria salvo no banco de dados
        // Por enquanto, apenas simulamos a marcação como lida
    }
    
    fun deleteNotification(notificationId: Long) {
        // Em uma implementação real, isso seria removido do banco de dados
        // Por enquanto, apenas simulamos a remoção
    }
}

