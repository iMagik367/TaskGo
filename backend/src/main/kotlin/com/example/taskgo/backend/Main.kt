package com.example.taskgo.backend

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.example.taskgo.backend.routes.productRoutes

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        install(CallLogging)
        install(ContentNegotiation) { json() }

        routing {
            get("/health") { call.respond(mapOf("status" to "ok")) }
            get("/ready") { call.respond(mapOf("ready" to true)) }
            route("/v1") {
                get("/spec") { call.respond(mapOf("openapi" to "3.1.0", "info" to mapOf("title" to "TaskGo API", "version" to "0.1.0"))) }
                productRoutes()
            }
        }
    }.start(wait = true)
}


