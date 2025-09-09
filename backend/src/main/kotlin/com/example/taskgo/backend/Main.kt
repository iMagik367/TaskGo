package com.example.taskgo.backend

import com.example.taskgo.backend.auth.JwtConfig
import com.example.taskgo.backend.domain.InMemoryCartRepository
import com.example.taskgo.backend.domain.InMemoryProductRepository
import com.example.taskgo.backend.domain.UserRepository
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
data class ApiInfo(val title: String, val version: String, val description: String)

@Serializable
data class SpecResponse(val openapi: String, val info: ApiInfo)

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

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    
    embeddedServer(Netty, port = port, host = "0.0.0.0") {
        install(CallLogging)
        install(ContentNegotiation) { json() }
        
        // Initialize repositories
        val userRepository: UserRepository = InMemoryUserRepository()
        val productRepository = InMemoryProductRepository()
        val cartRepository = InMemoryCartRepository()

        routing {
            get("/health") { call.respond(HealthResponse("ok")) }
            get("/ready") { call.respond(ReadyResponse(true)) }
            route("/v1") {
                get("/spec") { 
                    call.respond(SpecResponse(
                        openapi = "3.1.0",
                        info = ApiInfo(
                            title = "TaskGo API",
                            version = "0.1.0",
                            description = "API for TaskGo marketplace app"
                        )
                    ))
                }
                
                // Simple test route
                get("/test") {
                    call.respond(TestResponse("API is working"))
                }
                
                // Simple products route without JWT
                get("/products") {
                    val products = listOf(
                        ProductResponse(1, "Produto 1", 29.99),
                        ProductResponse(2, "Produto 2", 49.99)
                    )
                    call.respond(products)
                }
                
                // Simple login route without JWT
                post("/login") {
                    val body = call.receive<LoginRequest>()
                    call.respond(LoginResponse(
                        token = "mock-token",
                        user = UserResponse(1, body.email)
                    ))
                }
            }
        }
    }.start(wait = true)
}


