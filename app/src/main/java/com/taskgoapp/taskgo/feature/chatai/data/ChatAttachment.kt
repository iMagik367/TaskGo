package com.taskgoapp.taskgo.feature.chatai.data

import android.net.Uri

data class ChatAttachment(
    val id: String,
    val uri: Uri,
    val type: AttachmentType,
    val fileName: String? = null,
    val mimeType: String? = null
) {
    // Para serialização, converter Uri para String
    fun toSerializable(): SerializableAttachment {
        return SerializableAttachment(
            id = id,
            uriString = uri.toString(),
            type = type,
            fileName = fileName,
            mimeType = mimeType
        )
    }
    
    companion object {
        fun fromSerializable(serializable: SerializableAttachment): ChatAttachment? {
            return try {
                ChatAttachment(
                    id = serializable.id,
                    uri = Uri.parse(serializable.uriString),
                    type = serializable.type,
                    fileName = serializable.fileName,
                    mimeType = serializable.mimeType
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

data class SerializableAttachment(
    val id: String,
    val uriString: String,
    val type: AttachmentType,
    val fileName: String? = null,
    val mimeType: String? = null
)

enum class AttachmentType {
    IMAGE,
    DOCUMENT
}

