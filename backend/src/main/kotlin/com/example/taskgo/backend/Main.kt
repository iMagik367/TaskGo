package com.example.taskgo.backend

import com.example.taskgo.backend.domain.InMemoryCartRepository
import com.example.taskgo.backend.domain.InMemoryProductRepository
import com.example.taskgo.backend.repository.InMemoryUserRepository
import com.example.taskgo.backend.routes.authRoutes
import com.example.taskgo.backend.routes.cartRoutes
import com.example.taskgo.backend.routes.productRoutes
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(val status: String)

@Serializable
data class ReadyResponse(val ready: Boolean)

@Serializable
data class TestResponse(val message: String)

@Serializable
data class ProductResponse(val id: Int, val name: String, val price: Double)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class UserResponse(val id: Int, val email: String)

@Serializable
data class LoginResponse(val token: String, val user: UserResponse)

@Serializable
data class ProductListResponse(val items: List<com.example.taskgo.backend.domain.Product>, val page: Int, val size: Int)

@Serializable
data class ErrorResponse(val error: String)

@Serializable
data class DebugRepositoriesResponse(
    val database_enabled: Boolean,
    val user_repository_type: String,
    val product_repository_type: String,
    val cart_repository_type: String,
    val message: String
)

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    println("🚀 Starting TaskGo server on port $port")
    println("🌍 Environment: ${System.getenv("RAILWAY_ENVIRONMENT") ?: "local"}")
    println("💾 Using in-memory repositories (no database)")
    
    embeddedServer(Netty, port = port, host = "0.0.0.0") {
        install(CallLogging)
        install(ContentNegotiation) { json() }
        
        // Initialize in-memory repositories only
        val userRepository = InMemoryUserRepository()
        val productRepository = InMemoryProductRepository()
        val cartRepository = InMemoryCartRepository()

        routing {
            route("/v1") {
                get("/health") { call.respond(HealthResponse("ok")) }
                get("/ready") { call.respond(ReadyResponse(true)) }
                get("/test") { call.respond(TestResponse("API is working")) }

                // Debug endpoints
                get("/debug/simple") {
                    call.respond(mapOf("status" to "ok", "message" to "Simple debug working"))
                }

                       get("/debug/repositories") {
                           call.respond(DebugRepositoriesResponse(
                               database_enabled = false,
                               user_repository_type = userRepository.javaClass.simpleName,
                               product_repository_type = productRepository.javaClass.simpleName,
                               cart_repository_type = cartRepository.javaClass.simpleName,
                               message = "Using in-memory repositories only"
                           ))
                       }

                // Rotas principais
                productRoutes(productRepository)
                authRoutes(userRepository)
                cartRoutes(cartRepository)
            }
        }
    }.start(wait = true)
}


