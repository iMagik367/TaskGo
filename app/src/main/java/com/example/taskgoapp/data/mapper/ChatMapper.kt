package com.example.taskgoapp.data.mapper

import com.example.taskgoapp.data.local.entity.ChatMessageEntity
import com.example.taskgoapp.core.model.ChatMessage

object ChatMapper {
    
    fun ChatMessageEntity.toModel(): ChatMessage {
        return ChatMessage(
            id = this.id,
            threadId = this.threadId,
            text = this.text,
            senderMe = this.senderMe,
            time = this.time
        )
    }
    
    fun ChatMessage.toEntity(): ChatMessageEntity {
        return ChatMessageEntity(
            id = this.id,
            threadId = this.threadId,
            text = this.text,
            senderMe = this.senderMe,
            time = this.time
        )
    }
}
