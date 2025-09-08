package com.example.taskgo.server.routes

import com.example.taskgo.server.domain.UserRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.security.MessageDigest
import java.util.*

@kotlinx.serialization.Serializable
data class UpdateProfileRequest(val name: String? = null, val password: String? = null)

private fun hash(password: String): String {
    val md = MessageDigest.getInstance("SHA-256")
    return Base64.getEncoder().encodeToString(md.digest(password.toByteArray()))
}

fun Route.profileRoutes(users: UserRepository) {
    authenticate("auth-jwt") {
        get("/me") {
            val principal = call.principal<JWTPrincipal>()!!
            val email = principal.payload.getClaim("email").asString()
            val user = users.findByEmail(email)
            if (user == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "user not found"))
            } else {
                call.respond(mapOf("id" to user.id, "name" to user.name, "email" to user.email))
            }
        }
        put("/me") {
            val principal = call.principal<JWTPrincipal>()!!
            val email = principal.payload.getClaim("email").asString()
            val user = users.findByEmail(email)
            if (user == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "user not found"))
                return@put
            }
            val req = call.receive<UpdateProfileRequest>()
            val newName = req.name?.takeIf { it.isNotBlank() } ?: user.name
            val newHash = req.password?.takeIf { it.isNotBlank() }?.let { hash(it) } ?: user.passwordHash
            val updated = users.updateNameAndPassword(email, newName, newHash) ?: user
            call.respond(mapOf("id" to updated.id, "name" to updated.name, "email" to updated.email))
        }
    }
}
