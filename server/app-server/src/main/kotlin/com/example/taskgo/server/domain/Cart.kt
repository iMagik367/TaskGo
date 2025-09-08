package com.example.taskgo.server.domain

import kotlinx.serialization.Serializable

@Serializable
data class CartItem(
    val productId: Long,
    val quantity: Int
)

@Serializable
data class Cart(
    val userEmail: String,
    val items: List<CartItem>
)

@Serializable
data class Order(
    val id: Long,
    val userEmail: String,
    val items: List<CartItem>,
    val total: Double
)

interface CartRepository {
    suspend fun getCart(userEmail: String): Cart
    suspend fun addItem(userEmail: String, productId: Long, quantity: Int): Cart
    suspend fun removeItem(userEmail: String, productId: Long): Cart
    suspend fun clear(userEmail: String)
}

class InMemoryCartRepository : CartRepository {
    private val data = mutableMapOf<String, MutableList<CartItem>>()

    override suspend fun getCart(userEmail: String): Cart = Cart(userEmail, data[userEmail]?.toList() ?: emptyList())

    override suspend fun addItem(userEmail: String, productId: Long, quantity: Int): Cart {
        val list = data.getOrPut(userEmail) { mutableListOf() }
        val idx = list.indexOfFirst { it.productId == productId }
        if (idx >= 0) list[idx] = list[idx].copy(quantity = list[idx].quantity + quantity)
        else list.add(CartItem(productId, quantity))
        return Cart(userEmail, list.toList())
    }

    override suspend fun removeItem(userEmail: String, productId: Long): Cart {
        val list = data.getOrPut(userEmail) { mutableListOf() }
        list.removeAll { it.productId == productId }
        return Cart(userEmail, list.toList())
    }

    override suspend fun clear(userEmail: String) { data.remove(userEmail) }
}

interface OrderRepository {
    suspend fun list(userEmail: String): List<Order>
    suspend fun create(userEmail: String, items: List<CartItem>, total: Double): Order
}

class InMemoryOrderRepository : OrderRepository {
    private val data = mutableMapOf<String, MutableList<Order>>()
    private var seq: Long = 1

    override suspend fun list(userEmail: String): List<Order> = data[userEmail]?.toList() ?: emptyList()

    override suspend fun create(userEmail: String, items: List<CartItem>, total: Double): Order {
        val order = Order(seq++, userEmail, items, total)
        data.getOrPut(userEmail) { mutableListOf() }.add(order)
        return order
    }
}
