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

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    
    embeddedServer(Netty, port = port, host = "0.0.0.0") {
        install(CallLogging)
        install(ContentNegotiation) { json() }
        
        // Configure JWT
        JwtConfig.configure(this)
        
        // Initialize repositories
        val userRepository: UserRepository = InMemoryUserRepository()
        val productRepository = InMemoryProductRepository()
        val cartRepository = InMemoryCartRepository()

        routing {
            get("/health") { call.respond(mapOf("status" to "ok")) }
            get("/ready") { call.respond(mapOf("ready" to true)) }
            route("/v1") {
                get("/spec") { 
                    call.respond(mapOf(
                        "openapi" to "3.1.0", 
                        "info" to mapOf(
                            "title" to "TaskGo API", 
                            "version" to "0.1.0",
                            "description" to "API for TaskGo marketplace app"
                        )
                    )) 
                }
                
                // Simple test route
                get("/test") {
                    call.respond(mapOf("message" to "API is working"))
                }
                
                authRoutes(userRepository)
                productRoutes(productRepository)
                cartRoutes(cartRepository)
            }
        }
    }.start(wait = true)
}


