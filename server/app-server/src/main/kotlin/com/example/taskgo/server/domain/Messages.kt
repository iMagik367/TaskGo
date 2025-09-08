package com.example.taskgo.server.domain

import kotlinx.serialization.Serializable

@Serializable
data class Chat(
    val id: Long,
    val userEmail: String,
    val peerEmail: String,
    val lastMessage: String,
    val updatedAt: Long
)

@Serializable
data class Message(
    val id: Long,
    val chatId: Long,
    val from: String,
    val to: String,
    val content: String,
    val createdAt: Long
)

@Serializable
data class Notification(
    val id: Long,
    val userEmail: String,
    val title: String,
    val body: String,
    val read: Boolean,
    val createdAt: Long
)

interface ChatRepository {
    suspend fun listChats(userEmail: String): List<Chat>
    suspend fun sendMessage(userEmail: String, peerEmail: String, content: String): Message
    suspend fun listMessages(userEmail: String, chatId: Long): List<Message>
}

class InMemoryChatRepository : ChatRepository {
    private var chatSeq = 1L
    private var msgSeq = 1L
    private val chats = mutableListOf<Chat>()
    private val messages = mutableListOf<Message>()

    override suspend fun listChats(userEmail: String): List<Chat> = chats.filter { it.userEmail == userEmail || it.peerEmail == userEmail }

    override suspend fun sendMessage(userEmail: String, peerEmail: String, content: String): Message {
        val now = System.currentTimeMillis()
        val chat = chats.firstOrNull {
            (it.userEmail == userEmail && it.peerEmail == peerEmail) || (it.userEmail == peerEmail && it.peerEmail == userEmail)
        } ?: Chat(chatSeq++, userEmail, peerEmail, lastMessage = content, updatedAt = now).also { chats.add(it) }
        val msg = Message(msgSeq++, chat.id, from = userEmail, to = peerEmail, content = content, createdAt = now)
        messages.add(msg)
        val idx = chats.indexOfFirst { it.id == chat.id }
        chats[idx] = chats[idx].copy(lastMessage = content, updatedAt = now)
        return msg
    }

    override suspend fun listMessages(userEmail: String, chatId: Long): List<Message> = messages.filter { it.chatId == chatId && (it.from == userEmail || it.to == userEmail) }
}

interface NotificationRepository {
    suspend fun list(userEmail: String): List<Notification>
    suspend fun markRead(userEmail: String, id: Long): Notification?
    suspend fun push(userEmail: String, title: String, body: String): Notification
}

class InMemoryNotificationRepository : NotificationRepository {
    private var seq = 1L
    private val notifs = mutableListOf<Notification>()

    override suspend fun list(userEmail: String): List<Notification> = notifs.filter { it.userEmail == userEmail }

    override suspend fun markRead(userEmail: String, id: Long): Notification? {
        val idx = notifs.indexOfFirst { it.id == id && it.userEmail == userEmail }
        if (idx < 0) return null
        notifs[idx] = notifs[idx].copy(read = true)
        return notifs[idx]
    }

    override suspend fun push(userEmail: String, title: String, body: String): Notification {
        val n = Notification(seq++, userEmail, title, body, read = false, createdAt = System.currentTimeMillis())
        notifs.add(n)
        return n
    }
}
