package com.taskgoapp.taskgo.feature.notifications.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskgoapp.taskgo.data.firestore.models.NotificationFirestore
import com.taskgoapp.taskgo.data.repository.FirestoreNotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class NotificationsUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: FirestoreNotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    val notifications: StateFlow<List<NotificationFirestore>> = notificationRepository
        .observeNotifications()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.markAsRead(notificationId)
        }
    }

    fun formatTimestamp(timestamp: Date?): String {
        if (timestamp == null) return ""
        
        val now = Date()
        val diff = now.time - timestamp.time
        
        return when {
            diff < 60 * 1000 -> "Agora"
            diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}m atrás"
            diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}h atrás"
            diff < 7 * 24 * 60 * 60 * 1000 -> {
                val days = diff / (24 * 60 * 60 * 1000)
                if (days == 1L) "Ontem" else "$days dias atrás"
            }
            else -> {
                val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                formatter.format(timestamp)
            }
        }
    }

    fun getNotificationIcon(type: String): String {
        return when (type) {
            "order_created" -> "📦"
            "order_accepted" -> "✅"
            "order_completed" -> "🎉"
            "payment_received" -> "💰"
            "review_received" -> "⭐"
            "system_alert" -> "🔔"
            "document_verification" -> "📄"
            else -> "🔔"
        }
    }
}

