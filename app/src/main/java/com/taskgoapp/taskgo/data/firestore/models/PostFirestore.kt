package com.taskgoapp.taskgo.data.firestore.models

import java.util.Date

/**
 * Modelo de Post no Firestore
 */
data class PostFirestore(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userAvatarUrl: String? = null,
    val userRole: String? = null, // Role do autor do post (partner, client, etc)
    val text: String = "",
    val mediaUrls: List<String> = emptyList(),
    val mediaTypes: List<String> = emptyList(), // "image" ou "video"
    val location: PostLocation? = null,
    val createdAt: Date? = null,
    val updatedAt: Date? = null,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val likedBy: List<String> = emptyList(), // Lista de userIds que curtiram
    val tags: List<String>? = null // Hashtags opcionais
)

/**
 * Localização do post (cidade, estado e coordenadas)
 */
data class PostLocation(
    val city: String = "",
    val state: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)
