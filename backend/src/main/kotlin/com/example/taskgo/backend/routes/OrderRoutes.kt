package com.example.taskgo.backend.routes

import com.example.taskgo.backend.domain.OrderRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.orderRoutes(orderRepository: OrderRepository) {
    authenticate("auth-jwt") {
        route("/orders") {
            get {
                try {
                    val principal = call.principal<JWTPrincipal>()!!
                    val email = principal.payload.getClaim("email").asString()
                    val orders = orderRepository.listByUser(email)
                    call.respond(mapOf("items" to orders))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                }
            }
        }
    }
}






