package com.example.taskgo.backend.routes

import com.example.taskgo.backend.domain.ProductRepository
import com.example.taskgo.backend.domain.UserRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.syncRoutes(userRepository: UserRepository, productRepository: ProductRepository) {
    authenticate("auth-jwt") {
        route("/sync") {
            get {
                val principal = call.principal<JWTPrincipal>()!!
                val email = principal.payload.getClaim("email").asString()
                val role = principal.payload.getClaim("role").asString()

                val products = productRepository.list(null, null, 1, 100)
                val payload = mapOf(
                    "user" to mapOf("email" to email, "role" to role),
                    "products" to products
                )
                call.respond(payload)
            }
        }
    }
}




