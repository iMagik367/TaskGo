package com.example.taskgo.backend

import com.example.taskgo.backend.auth.JwtConfig
import com.example.taskgo.backend.domain.InMemoryCartRepository
import com.example.taskgo.backend.domain.InMemoryProductRepository
import com.example.taskgo.backend.domain.UserRepository
import com.example.taskgo.backend.repository.InMemoryUserRepository
import com.example.taskgo.backend.repository.UserRepositoryJdbc
import com.example.taskgo.backend.repository.ProductRepositoryJdbc
import com.example.taskgo.backend.db.Database
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

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    
    embeddedServer(Netty, port = port, host = "0.0.0.0") {
        install(CallLogging)
        install(ContentNegotiation) { json() }
        
        // Initialize repositories (toggle DB via env DB_ENABLE=true)
        val useDb = System.getenv("DB_ENABLE")?.equals("true", ignoreCase = true) == true

        var dataSourceOrNull: javax.sql.DataSource? = null
        if (useDb) {
            try {
                dataSourceOrNull = Database.init()
            } catch (t: Throwable) {
                // Fallback gracioso para repositórios em memória quando DB não está configurado
                println("Failed to initialize database. Falling back to in-memory repositories: ${t.message}")
                dataSourceOrNull = null
            }
        }

        val userRepository: UserRepository = when (val ds = dataSourceOrNull) {
            null -> InMemoryUserRepository()
            else -> UserRepositoryJdbc(ds)
        }

        val productRepository = when (val ds = dataSourceOrNull) {
            null -> InMemoryProductRepository()
            else -> ProductRepositoryJdbc(ds)
        }
        
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

                get("/debug/db") {
                    try {
                        val ds = Database.init()
                        call.respond(mapOf("success" to true, "message" to "Database initialized successfully", "ds_class" to ds.javaClass.simpleName))
                    } catch (e: Exception) {
                        call.respond(mapOf("success" to false, "error" to e.message, "type" to e.javaClass.simpleName))
                    }
                }

                get("/debug/env") {
                    val env = mapOf(
                        "DB_ENABLE" to System.getenv("DB_ENABLE"),
                        "DB_URL" to System.getenv("DB_URL")?.take(50) + "...",
                        "DB_USER" to System.getenv("DB_USER"),
                        "DB_PASS" to System.getenv("DB_PASS")?.take(5) + "...",
                        "PORT" to System.getenv("PORT")
                    )
                    call.respond(mapOf("env" to env))
                }

                get("/debug/test-connection") {
                    try {
                        val ds = Database.init()
                        val connection = ds.connection
                        val isValid = connection.isValid(5)
                        connection.close()
                        call.respond(mapOf("success" to true, "connection_valid" to isValid))
                    } catch (e: Exception) {
                        call.respond(mapOf("success" to false, "error" to e.message, "type" to e.javaClass.simpleName))
                    }
                }

                get("/debug/test-query") {
                    try {
                        val ds = Database.init()
                        val connection = ds.connection
                        val statement = connection.createStatement()
                        val resultSet = statement.executeQuery("SELECT COUNT(*) as count FROM products")
                        val count = if (resultSet.next()) resultSet.getInt("count") else 0
                        resultSet.close()
                        statement.close()
                        connection.close()
                        call.respond(mapOf("success" to true, "product_count" to count))
                    } catch (e: Exception) {
                        call.respond(mapOf("success" to false, "error" to e.message, "type" to e.javaClass.simpleName))
                    }
                }

                // Rotas principais
                productRoutes(productRepository)
                authRoutes(userRepository)
                // cartRoutes(cartRepository) // habilitar quando JWT estiver ativo
            }
        }
    }.start(wait = true)
}


