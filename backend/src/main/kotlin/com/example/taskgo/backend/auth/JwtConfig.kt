package com.example.taskgo.backend.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import java.util.*

object JwtConfig {
    private const val secret = "your-secret-key-change-in-production"
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
                    if (credential.payload.getClaim("email").asString() != "") {
                        JWTPrincipal(credential.payload)
                    } else {
                        null
                    }
                }
            }
        }
    }

    fun generateToken(email: String): String {
        return JWT.create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withClaim("email", email)
            .withExpiresAt(Date(System.currentTimeMillis() + 60000 * 60 * 24)) // 24 hours
            .sign(Algorithm.HMAC256(secret))
    }
}

