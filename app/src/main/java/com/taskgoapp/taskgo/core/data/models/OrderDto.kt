package com.taskgoapp.taskgo.core.data.models

import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class OrderDto(
    val id: Long,
    val items: List<OrderItemDto>,
    val total: Double,
    val status: String,
    val createdAt: String // ISO string
)

@Serializable
data class OrderItemDto(
    val productId: Long,
    val quantity: Int,
    val price: Double
)

@Serializable
data class OrdersResponse(
    val items: List<OrderDto>
)

