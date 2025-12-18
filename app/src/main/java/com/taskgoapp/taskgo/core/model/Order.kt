package com.taskgoapp.taskgo.core.model

import java.time.Instant

data class Order(
    val id: Long,
    val items: List<com.taskgoapp.taskgo.core.model.OrderItem>,
    val total: Double,
    val status: String,
    val createdAt: Instant
)

