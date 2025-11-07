package com.taskgoapp.taskgo.core.model

import java.time.Instant

data class Order(
    val id: Long,
    val items: List<OrderItem>,
    val total: Double,
    val status: String,
    val createdAt: Instant
)

data class OrderItem(
    val productId: Long,
    val quantity: Int,
    val price: Double
)

