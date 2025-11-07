package com.taskgoapp.taskgo.data.mapper

import com.taskgoapp.taskgo.data.local.entity.ChatMessageEntity
import com.taskgoapp.taskgo.core.model.ChatMessage

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
