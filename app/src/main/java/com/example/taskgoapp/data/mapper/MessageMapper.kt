package com.example.taskgoapp.data.mapper

import com.example.taskgoapp.data.local.entity.MessageThreadEntity
import com.example.taskgoapp.data.local.entity.ChatMessageEntity
import com.example.taskgoapp.core.model.MessageThread
import com.example.taskgoapp.core.model.ChatMessage

object MessageMapper {
    
    fun MessageThreadEntity.toModel(): MessageThread {
        return MessageThread(
            id = this.id,
            title = this.title,
            lastMessage = this.lastMessage,
            lastTime = this.lastTime
        )
    }
    
    fun MessageThread.toEntity(): MessageThreadEntity {
        return MessageThreadEntity(
            id = this.id,
            title = this.title,
            lastMessage = this.lastMessage,
            lastTime = this.lastTime
        )
    }
    
    fun ChatMessageEntity.toModel(): ChatMessage {
        return ChatMessage(
            id = this.id,
            threadId = this.threadId,
            senderMe = this.senderMe,
            text = this.text,
            time = this.time
        )
    }
    
    fun ChatMessage.toEntity(): ChatMessageEntity {
        return ChatMessageEntity(
            id = this.id,
            threadId = this.threadId,
            senderMe = this.senderMe,
            text = this.text,
            time = this.time
        )
    }
}