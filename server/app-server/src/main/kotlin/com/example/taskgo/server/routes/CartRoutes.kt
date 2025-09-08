package com.example.taskgo.server.routes

import com.example.taskgo.server.domain.InMemoryCartRepository
import com.example.taskgo.server.domain.InMemoryOrderRepository
import com.example.taskgo.server.domain.ProductRepository
import com.example.taskgo.server.domain.InMemoryProductRepository
import com.example.taskgo.server.domain.CartItem
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

@kotlinx.serialization.Serializable
data class AddCartItemRequest(val productId: Long, val quantity: Int)

fun Route.cartRoutes(
    carts: InMemoryCartRepository = InMemoryCartRepository(),
    orders: InMemoryOrderRepository = InMemoryOrderRepository(),
    products: ProductRepository = InMemoryProductRepository()
) {
    authenticate("auth-jwt") {
        route("/cart") {
            get {
                val principal = call.principal<JWTPrincipal>()!!
                val email = principal.payload.getClaim("email").asString()
                call.respond(carts.getCart(email))
            }
            post {
                val req = call.receive<AddCartItemRequest>()
                if (req.quantity <= 0) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid quantity"))
                    return@post
                }
                val principal = call.principal<JWTPrincipal>()!!
                val email = principal.payload.getClaim("email").asString()
                val item = products.getById(req.productId)
                if (item == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "product not found"))
                    return@post
                }
                val cart = carts.addItem(email, req.productId, req.quantity)
                call.respond(cart)
            }
            delete("/{productId}") {
                val principal = call.principal<JWTPrincipal>()!!
                val email = principal.payload.getClaim("email").asString()
                val productId = call.parameters["productId"]?.toLongOrNull()
                if (productId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid productId"))
                    return@delete
                }
                val cart = carts.removeItem(email, productId)
                call.respond(cart)
            }
            post("/checkout") {
                val principal = call.principal<JWTPrincipal>()!!
                val email = principal.payload.getClaim("email").asString()
                val cart = carts.getCart(email)
                val total = cart.items.sumOf { (products.getById(it.productId)?.price ?: 0.0) * it.quantity }
                val order = orders.create(email, cart.items, total)
                carts.clear(email)
                call.respond(order)
            }
        }
        route("/orders") {
            get {
                val principal = call.principal<JWTPrincipal>()!!
                val email = principal.payload.getClaim("email").asString()
                val ordersRepo = orders
                call.respond(ordersRepo.list(email))
            }
        }
    }
}
