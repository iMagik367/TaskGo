package com.example.taskgo.backend.domain

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
data class AddCartItemRequest(
    val productId: Long,
    val quantity: Int
)

interface CartRepository {
    suspend fun getCart(userEmail: String): Cart
    suspend fun addItem(userEmail: String, productId: Long, quantity: Int): Cart
    suspend fun removeItem(userEmail: String, productId: Long): Cart
    suspend fun clearCart(userEmail: String): Cart
}

class InMemoryCartRepository : CartRepository {
    private val carts = mutableMapOf<String, MutableList<CartItem>>()

    override suspend fun getCart(userEmail: String): Cart {
        val items = carts[userEmail] ?: emptyList()
        return Cart(userEmail, items)
    }

    override suspend fun addItem(userEmail: String, productId: Long, quantity: Int): Cart {
        val items = carts.getOrPut(userEmail) { mutableListOf() }
        val existingIndex = items.indexOfFirst { it.productId == productId }
        
        if (existingIndex >= 0) {
            items[existingIndex] = items[existingIndex].copy(quantity = items[existingIndex].quantity + quantity)
        } else {
            items.add(CartItem(productId, quantity))
        }
        
        return Cart(userEmail, items.toList())
    }

    override suspend fun removeItem(userEmail: String, productId: Long): Cart {
        val items = carts[userEmail] ?: return Cart(userEmail, emptyList())
        items.removeAll { it.productId == productId }
        return Cart(userEmail, items.toList())
    }

    override suspend fun clearCart(userEmail: String): Cart {
        carts[userEmail] = mutableListOf()
        return Cart(userEmail, emptyList())
    }
}

