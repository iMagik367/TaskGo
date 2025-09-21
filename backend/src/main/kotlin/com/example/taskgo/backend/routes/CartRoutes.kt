package com.example.taskgo.backend.routes

import com.example.taskgo.backend.domain.AddCartItemRequest
import com.example.taskgo.backend.domain.CartRepository
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

import com.example.taskgo.backend.domain.OrderRepository
import com.example.taskgo.backend.domain.ProductRepository

fun Route.cartRoutes(cartRepository: CartRepository, orderRepository: OrderRepository? = null, productRepository: ProductRepository? = null) {
    authenticate("auth-jwt") {
        route("/cart") {
            get {
                try {
                    val principal = call.principal<JWTPrincipal>()!!
                    val email = principal.payload.getClaim("email").asString()
                    val cart = cartRepository.getCart(email)
                    call.respond(cart)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                }
            }

            post {
                try {
                    val principal = call.principal<JWTPrincipal>()!!
                    val email = principal.payload.getClaim("email").asString()
                    val request = call.receive<AddCartItemRequest>()
                    val cart = cartRepository.addItem(email, request.productId, request.quantity)
                    call.respond(cart)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                }
            }

            delete("/{productId}") {
                try {
                    val principal = call.principal<JWTPrincipal>()!!
                    val email = principal.payload.getClaim("email").asString()
                    val productId = call.parameters["productId"]?.toLongOrNull()
                        ?: throw Exception("Invalid product ID")

                    val cart = cartRepository.removeItem(email, productId)
                    call.respond(cart)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                }
            }

            post("/checkout") {
                try {
                    val principal = call.principal<JWTPrincipal>()!!
                    val email = principal.payload.getClaim("email").asString()
                    if (orderRepository == null || productRepository == null) {
                        call.respond(HttpStatusCode.NotImplemented, mapOf("error" to "Orders not enabled")); return@post
                    }
                    val cartBefore = cartRepository.getCart(email)
                    if (cartBefore.items.isEmpty()) { call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Cart is empty")); return@post }
                    val order = orderRepository.createFromCart(email, cartBefore.items) { pid ->
                        val p = productRepository.getById(pid) ?: error("Product not found")
                        p.price
                    }
                    cartRepository.clearCart(email)
                    call.respond(mapOf("message" to "Checkout completed", "order" to order))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                }
            }
        }
    }
}

