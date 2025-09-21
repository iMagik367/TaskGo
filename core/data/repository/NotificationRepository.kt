package com.example.taskgoapp.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class NotificationRepository {
    fun getNotifications(): Flow<List<Any>> {
        return flowOf(emptyList())
    }
    
    fun markAsRead(notificationId: Long) {
        // TODO: Marcar a notificação específica como lida no banco de dados
        // notificationId: identificador único da notificação a ser marcada
    }
    
    fun deleteNotification(notificationId: Long) {
        // TODO: Remover a notificação específica do banco de dados
        // notificationId: identificador único da notificação a ser removida
    }
}

