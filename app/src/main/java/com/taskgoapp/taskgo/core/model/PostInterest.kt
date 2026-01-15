package com.taskgoapp.taskgo.core.model

import java.util.Date

/**
 * Modelo de interesse do usuário em um post
 */
data class PostInterest(
    val id: String,
    val userId: String,
    val postId: String,
    val isInterested: Boolean, // true = tem interesse, false = não tem interesse
    val createdAt: Date
)
