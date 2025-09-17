package com.example.taskgo.backend.routes

import com.example.taskgo.backend.domain.AddCartItemRequest
import com.example.taskgo.backend.domain.CartRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.cartRoutes(cartRepository: CartRepository) {
    route("/cart") {
        // Simplified cart routes without JWT authentication for in-memory testing
        get {
            try {
                // Use a default user email for testing
                val userEmail = "test@example.com"
                val cart = cartRepository.getCart(userEmail)
                call.respond(cart)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
            }
        }
        
        post {
            try {
                // Use a default user email for testing
                val userEmail = "test@example.com"
                val request = call.receive<AddCartItemRequest>()
                val cart = cartRepository.addItem(userEmail, request.productId, request.quantity)
                call.respond(cart)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
            }
        }
        
        delete("/{productId}") {
            try {
                // Use a default user email for testing
                val userEmail = "test@example.com"
                val productId = call.parameters["productId"]?.toLongOrNull()
                    ?: throw Exception("Invalid product ID")
                
                val cart = cartRepository.removeItem(userEmail, productId)
                call.respond(cart)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
            }
        }
        
        post("/checkout") {
            try {
                // Use a default user email for testing
                val userEmail = "test@example.com"
                // For now, just clear the cart
                val cart = cartRepository.clearCart(userEmail)
                call.respond(mapOf("message" to "Checkout completed", "cart" to cart))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
            }
        }
    }
}

