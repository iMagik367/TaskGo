package com.taskgoapp.taskgo.data.repository

import com.taskgoapp.taskgo.data.local.dao.MessageDao
import com.taskgoapp.taskgo.data.mapper.MessageMapper.toEntity
import com.taskgoapp.taskgo.data.mapper.MessageMapper.toModel
import com.taskgoapp.taskgo.domain.repository.MessageRepository
import com.taskgoapp.taskgo.core.model.MessageThread
import com.taskgoapp.taskgo.core.model.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao
) : MessageRepository {

    override fun observeThreads(): Flow<List<MessageThread>> {
        return messageDao.observeThreads().map { entities ->
            entities.map { it.toModel() }
        }
    }

    override suspend fun getThread(id: String): MessageThread? {
        return messageDao.getThreadById(id)?.toModel()
    }

    override fun observeMessages(threadId: String): Flow<List<ChatMessage>> {
        return messageDao.observeMessages(threadId).map { entities ->
            entities.map { it.toModel() }
        }
    }

    override suspend fun sendMessage(threadId: String, text: String) {
        val message = ChatMessage(
            id = generateMessageId(),
            threadId = threadId,
            senderMe = true,
            text = text,
            time = System.currentTimeMillis()
        )
        messageDao.upsertMessage(message.toEntity())
        
        // Update thread with last message
        val thread = messageDao.getThreadById(threadId)
        if (thread != null) {
            val updatedThread = thread.copy(
                lastMessage = text,
                lastTime = System.currentTimeMillis()
            )
            messageDao.upsertThread(updatedThread)
        }
    }

    override suspend fun createThread(title: String): String {
        val threadId = generateMessageId()
        val thread = MessageThread(
            id = threadId,
            title = title,
            lastMessage = "",
            lastTime = System.currentTimeMillis()
        )
        messageDao.upsertThread(thread.toEntity())
        return threadId
    }

    private fun generateMessageId(): String {
        return "msg_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
}