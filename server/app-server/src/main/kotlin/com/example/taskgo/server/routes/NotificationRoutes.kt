package com.example.taskgo.server.routes

import com.example.taskgo.server.domain.InMemoryNotificationRepository
import com.example.taskgo.server.domain.NotificationRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.notificationRoutes(repo: NotificationRepository = InMemoryNotificationRepository()) {
    authenticate("auth-jwt") {
        route("/notifications") {
            get {
                val principal = call.principal<JWTPrincipal>()!!
                val email = principal.payload.getClaim("email").asString()
                call.respond(repo.list(email))
            }
            post("/{id}/read") {
                val principal = call.principal<JWTPrincipal>()!!
                val email = principal.payload.getClaim("email").asString()
                val id = call.parameters["id"]?.toLongOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid id"))
                    return@post
                }
                val updated = repo.markRead(email, id)
                if (updated == null) call.respond(HttpStatusCode.NotFound, mapOf("error" to "not found"))
                else call.respond(updated)
            }
        }
    }
}
