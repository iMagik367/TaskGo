package com.example.taskgo.server.routes

import com.example.taskgo.server.domain.ChatRepository
import com.example.taskgo.server.domain.InMemoryChatRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

@kotlinx.serialization.Serializable
data class SendMessageRequest(val peerEmail: String, val content: String)

fun Route.messageRoutes(repo: ChatRepository = InMemoryChatRepository()) {
    authenticate("auth-jwt") {
        route("/chats") {
            get {
                val principal = call.principal<JWTPrincipal>()!!
                val email = principal.payload.getClaim("email").asString()
                call.respond(repo.listChats(email))
            }
        }
        route("/messages") {
            get("/{chatId}") {
                val principal = call.principal<JWTPrincipal>()!!
                val email = principal.payload.getClaim("email").asString()
                val chatId = call.parameters["chatId"]?.toLongOrNull()
                if (chatId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid chatId"))
                    return@get
                }
                call.respond(repo.listMessages(email, chatId))
            }
            post {
                val principal = call.principal<JWTPrincipal>()!!
                val email = principal.payload.getClaim("email").asString()
                val body = call.receive<SendMessageRequest>()
                if (body.content.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "empty content"))
                    return@post
                }
                val msg = repo.sendMessage(email, body.peerEmail, body.content)
                call.respond(msg)
            }
        }
    }
}
