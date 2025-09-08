package com.example.taskgo.server

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import com.example.taskgo.server.routes.productRoutes
import com.example.taskgo.server.routes.authRoutes
import com.example.taskgo.server.routes.cartRoutes
import com.example.taskgo.server.routes.profileRoutes
import com.example.taskgo.server.routes.messageRoutes
import com.example.taskgo.server.routes.notificationRoutes
import com.example.taskgo.server.routes.serviceRoutes
import com.example.taskgo.server.auth.JwtConfig
import com.example.taskgo.server.db.Database
import com.example.taskgo.server.db.UserRepositoryJdbc
import com.example.taskgo.server.domain.InMemoryUserRepository
import com.example.taskgo.server.domain.UserRepository
import org.flywaydb.core.Flyway

fun main() {
    val port = (System.getenv("PORT") ?: "8080").toInt()
    embeddedServer(Netty, port = port, host = "0.0.0.0") {
        install(CallLogging)
        install(ContentNegotiation) { json() }
        JwtConfig.initialize(environment)
        JwtConfig.configure(this)
        val useDb = (System.getenv("DB_ENABLE") ?: "false").equals("true", ignoreCase = true)
        val userRepo: UserRepository = if (useDb) {
            try {
                Database.init(System.getenv("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASS"))
                val dsField = Database::class.java.getDeclaredField("dataSource").apply { isAccessible = true }
                val ds = dsField.get(Database) as javax.sql.DataSource
                // Flyway migrate
                try { Flyway.configure().dataSource(ds).load().migrate() } catch (_: Throwable) {}
                UserRepositoryJdbc { ds.connection }
            } catch (_: Throwable) {
                InMemoryUserRepository()
            }
        } else InMemoryUserRepository()
        routing {
            get("/health") { call.respond(mapOf("status" to "ok")) }
            get("/ready") { call.respond(mapOf("ready" to true)) }
            get("/v1/spec") {
                val spec = this::class.java.classLoader.getResource("openapi.yaml")
                if (spec == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "spec not found"))
                } else {
                    call.respondText(spec.readText(), ContentType.parse("application/yaml"))
                }
            }
            route("/v1") {
                authRoutes(userRepo)
                profileRoutes(userRepo)
                productRoutes()
                cartRoutes()
                messageRoutes()
                notificationRoutes()
                serviceRoutes()
            }
        }
    }.start(wait = true)
}


