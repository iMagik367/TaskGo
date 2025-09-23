package com.example.taskgo.backend.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.taskgo.backend.domain.UserRole
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import java.util.*

object JwtConfig {
    private val secret: String = "x7piVUwXsW5wcNek6A3zrQhFjmKvg9bR"  // Chave fixa para desenvolvimento
    private const val issuer = "taskgo-app"
    private const val audience = "taskgo-users"
    private const val realm = "TaskGo App"

    fun configure(application: Application) {
        application.install(Authentication) {
            jwt("auth-jwt") {
                realm = this@JwtConfig.realm
                verifier(
                    JWT
                        .require(Algorithm.HMAC256(secret))
                        .withAudience(audience)
                        .withIssuer(issuer)
                        .build()
                )
                validate { credential ->
                    val email = credential.payload.getClaim("email").asString()
                    val role = credential.payload.getClaim("role").asString()
                    if (!email.isNullOrBlank() && !role.isNullOrBlank()) JWTPrincipal(credential.payload) else null
                }
            }
        }
    }

    fun generateToken(email: String, role: UserRole): String {
        return JWT.create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withClaim("email", email)
            .withClaim("role", role.name)
            .withExpiresAt(Date(System.currentTimeMillis() + 60000 * 60 * 24)) // 24 hours
            .sign(Algorithm.HMAC256(secret))
    }
}

