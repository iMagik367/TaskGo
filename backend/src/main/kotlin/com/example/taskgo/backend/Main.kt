package com.example.taskgo.backend

import com.example.taskgo.backend.domain.InMemoryCartRepository
import com.example.taskgo.backend.domain.InMemoryProductRepository
import com.example.taskgo.backend.repository.InMemoryUserRepository
import com.example.taskgo.backend.repository.ProductRepositoryJdbc
import com.example.taskgo.backend.repository.UserRepositoryJdbc
import com.example.taskgo.backend.repository.ServiceRepositoryJdbc
import com.example.taskgo.backend.db.Database
import com.example.taskgo.backend.routes.authRoutes
import com.example.taskgo.backend.routes.cartRoutes
import com.example.taskgo.backend.routes.productRoutes
import com.example.taskgo.backend.routes.syncRoutes
import com.example.taskgo.backend.routes.adminRoutes
import com.example.taskgo.backend.routes.serviceRoutes
import com.example.taskgo.backend.routes.realtimeRoutes
import com.example.taskgo.backend.routes.orderRoutes
import com.example.taskgo.backend.auth.JwtConfig
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
            import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.compression.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.http.content.*
import kotlinx.serialization.Serializable
import java.security.MessageDigest
import kotlinx.coroutines.runBlocking
import io.ktor.http.*@Serializable
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
    val dbEnabled = System.getenv("DB_ENABLE")?.equals("true", ignoreCase = true) == true
    println(if (dbEnabled) "💾 Using persistent database (JDBC)" else "💾 Using in-memory repositories (no database)")
    
    embeddedServer(Netty, port = port, host = "0.0.0.0") {
        install(CallLogging)
        install(ContentNegotiation) { json() }
        install(CORS) {
            anyHost()
            allowHeader("Authorization")
            allowHeader("Content-Type")
            allowMethod(io.ktor.http.HttpMethod.Get)
            allowMethod(io.ktor.http.HttpMethod.Post)
            allowMethod(io.ktor.http.HttpMethod.Put)
            allowMethod(io.ktor.http.HttpMethod.Delete)
            allowMethod(io.ktor.http.HttpMethod.Patch)
        }
        install(Compression) {
            gzip { priority = 1.0 }
            deflate { priority = 10.0 }
        }
        JwtConfig.configure(this)

        routing {
            get("/health") {
                call.respond(HttpStatusCode.OK, HealthResponse("OK"))
            }
        }
        
        // Initialize repositories
        val userRepository: com.example.taskgo.backend.domain.UserRepository
        val productRepository: com.example.taskgo.backend.domain.ProductRepository
        val serviceRepository: com.example.taskgo.backend.domain.ServiceRepository
        val cartRepository: com.example.taskgo.backend.domain.CartRepository
        val orderRepository: com.example.taskgo.backend.domain.OrderRepository?

        if (dbEnabled) {
            val ds = Database.init()
            userRepository = UserRepositoryJdbc(ds)
            productRepository = ProductRepositoryJdbc(ds)
            serviceRepository = ServiceRepositoryJdbc(ds)
            cartRepository = com.example.taskgo.backend.repository.CartRepositoryJdbc(ds)
            orderRepository = com.example.taskgo.backend.repository.OrderRepositoryJdbc(ds)
        } else {
            userRepository = InMemoryUserRepository()
            productRepository = InMemoryProductRepository()
            serviceRepository = com.example.taskgo.backend.domain.InMemoryServiceRepository()
            cartRepository = InMemoryCartRepository()
            orderRepository = null
        }

        // Bootstrap ADMIN user (dev convenience)
        try {
            val bootstrapEnabled = (System.getenv("ADMIN_BOOTSTRAP") ?: "true").equals("true", ignoreCase = true)
            if (bootstrapEnabled) {
                runBlocking {
                    val existingAdmins = false // Simplified for now
                    if (!existingAdmins) {
                        val adminEmail = System.getenv("ADMIN_EMAIL") ?: "admin@example.com"
                        val adminPassword = System.getenv("ADMIN_PASSWORD") ?: "aka.300896"
                        val passwordHash = com.example.taskgo.backend.util.PasswordUtil.hashPassword(adminPassword)
                        println("DEBUG: Creating admin user with:")
                        println("DEBUG: Email: $adminEmail")
                        println("DEBUG: Password hash: $passwordHash")
                        userRepository.createUser(adminEmail, passwordHash, com.example.taskgo.backend.domain.UserRole.ADMIN)
                        println("👑 Bootstrap ADMIN created: $adminEmail")
                    }
                }
            }
        } catch (e: Exception) {
            println("Failed to bootstrap admin user: ${e.message}")
        }

    routing {
            // Serve admin static panel (fora da rota /v1)
            staticResources("/admin", "/public/admin")
            
            route("/v1") {
                get("/health") { call.respond(HealthResponse("ok")) }
                get("/ready") { call.respond(ReadyResponse(true)) }
                get("/test") { call.respond(TestResponse("API is working")) }

                // Debug endpoints
                get("/debug/simple") {
                    call.respond(mapOf("status" to "ok", "message" to "Simple debug working"))
                }

                get("/debug/repositories") {
                    call.respond(
                        DebugRepositoriesResponse(
                            database_enabled = dbEnabled,
                            user_repository_type = userRepository.javaClass.simpleName,
                            product_repository_type = productRepository.javaClass.simpleName,
                            cart_repository_type = cartRepository.javaClass.simpleName,
                            message = if (dbEnabled) "Using JDBC repositories" else "Using in-memory repositories only"
                        )
                    )
                }

                // Rotas principais
                productRoutes(productRepository)
                serviceRoutes(serviceRepository)
                authRoutes(userRepository)
                cartRoutes(cartRepository, orderRepository, productRepository)
                if (orderRepository != null) {
                    orderRoutes(orderRepository)
                }
                syncRoutes(userRepository, productRepository)
                adminRoutes(userRepository, productRepository, serviceRepository)
                realtimeRoutes()
            }
        }
    }.start(wait = true)
}


