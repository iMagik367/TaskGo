package com.taskgoapp.taskgo.core.model

import java.util.Date

/**
 * Model de domínio para Story (similar ao Instagram)
 * Stories expiram após 24 horas
 */
data class Story(
    val id: String,
    val userId: String,
    val userName: String,
    val userAvatarUrl: String?,
    val mediaUrl: String,
    val mediaType: String, // "image" ou "video"
    val thumbnailUrl: String? = null, // Para vídeos
    val caption: String? = null, // Texto opcional sobre a story
    val createdAt: Date,
    val expiresAt: Date, // Data de expiração (criado + 24 horas)
    val viewsCount: Int = 0,
    val isViewed: Boolean = false, // Se o usuário atual já viu esta story
    val location: StoryLocation? = null // Localização opcional
)

/**
 * Localização da Story
 */
data class StoryLocation(
    val city: String,
    val state: String,
    val latitude: Double,
    val longitude: Double
)

