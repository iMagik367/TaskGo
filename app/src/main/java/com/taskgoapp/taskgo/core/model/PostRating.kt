package com.taskgoapp.taskgo.core.model

import java.util.Date

/**
 * Modelo de avaliação de um post
 */
data class PostRating(
    val id: String,
    val postId: String,
    val userId: String,
    val userName: String?,
    val userAvatarUrl: String?,
    val rating: Int, // 1-5 estrelas
    val comment: String?,
    val createdAt: Date
)
