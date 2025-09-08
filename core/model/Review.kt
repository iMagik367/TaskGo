package com.example.taskgoapp.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Review(
    val id: String = "",
    val orderId: String = "",
    val serviceId: String = "",
    val reviewerId: String = "",
    val reviewerName: String = "",
    val reviewerImage: String? = null,
    val reviewedId: String = "", // ID do prestador ou cliente
    val rating: Float = 0f,
    val comment: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
