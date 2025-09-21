package com.example.taskgo.backend.domain

import kotlinx.serialization.Serializable

@Serializable
data class OrderItem(
    val productId: Long,
    val quantity: Int,
    val price: Double
)

@Serializable
data class Order(
    val id: Long,
    val userEmail: String,
    val items: List<OrderItem>,
    val total: Double,
    val status: String,
    val createdAt: String
)

interface OrderRepository {
    suspend fun createFromCart(userEmail: String, items: List<CartItem>, productsPricer: suspend (Long) -> Double): Order
    suspend fun listByUser(userEmail: String): List<Order>
}






