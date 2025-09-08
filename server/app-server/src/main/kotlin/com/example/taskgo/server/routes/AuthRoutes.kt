package com.example.taskgo.server.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.taskgo.server.auth.JwtConfig
import com.example.taskgo.server.domain.AuthRequest
import com.example.taskgo.server.domain.AuthResponse
import com.example.taskgo.server.domain.UserRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.security.MessageDigest
import java.util.*

private fun hashPassword(password: String): String {
    val md = MessageDigest.getInstance("SHA-256")
    return Base64.getEncoder().encodeToString(md.digest(password.toByteArray()))
}

private fun generateToken(email: String): String {
    val algorithm = Algorithm.HMAC256(JwtConfig.secret)
    return JWT.create()
        .withIssuer(JwtConfig.issuer)
        .withAudience(JwtConfig.audience)
        .withClaim("email", email)
        .withIssuedAt(Date())
        .withExpiresAt(Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24))
        .sign(algorithm)
}

fun Route.authRoutes(repo: UserRepository) {
    route("/auth") {
        post("/signup") {
            val body = call.receive<AuthRequest>()
            val exists = repo.findByEmail(body.email)
            if (exists != null) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "email already registered"))
                return@post
            }
            val user = repo.create(name = body.email.substringBefore('@'), email = body.email, passwordHash = hashPassword(body.password))
            val token = generateToken(user.email)
            call.respond(AuthResponse(token))
        }
        post("/login") {
            val body = call.receive<AuthRequest>()
            val user = repo.findByEmail(body.email)
            if (user == null || user.passwordHash != hashPassword(body.password)) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "invalid credentials"))
                return@post
            }
            val token = generateToken(user.email)
            call.respond(AuthResponse(token))
        }
    }
}
