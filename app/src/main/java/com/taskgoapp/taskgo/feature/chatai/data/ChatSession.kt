package com.taskgoapp.taskgo.feature.chatai.data

data class ChatSession(
    val id: String,
    val title: String,
    val lastMessage: String? = null,
    val timestamp: Long,
    val messageCount: Int = 0
)

