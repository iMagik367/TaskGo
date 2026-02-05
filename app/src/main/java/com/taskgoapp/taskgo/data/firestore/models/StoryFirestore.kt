package com.taskgoapp.taskgo.data.firestore.models

import com.google.firebase.Timestamp
import java.util.Date

/**
 * Modelo Firestore para Story
 * Stories são armazenadas em: stories/{storyId}
 */
data class StoryFirestore(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userAvatarUrl: String? = null,
    val userRole: String? = null, // Role do autor da story (partner, client, etc)
    val mediaUrl: String = "",
    val mediaType: String = "", // "image" ou "video"
    val thumbnailUrl: String? = null,
    val caption: String? = null,
    val createdAt: Timestamp? = null,
    val expiresAt: Timestamp? = null, // Data de expiração (criado + 24 horas)
    val viewsCount: Int = 0,
    val location: StoryLocationFirestore? = null
)

/**
 * Localização da Story no Firestore
 */
data class StoryLocationFirestore(
    val city: String = "",
    val state: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

