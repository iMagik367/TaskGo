package com.example.taskgo.server.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

object JwtConfig {
    lateinit var issuer: String
    lateinit var audience: String
    lateinit var secret: String

    fun initialize(environment: ApplicationEnvironment) {
        issuer = environment.config.propertyOrNull("jwt.issuer")?.getString() ?: "taskgo-server"
        audience = environment.config.propertyOrNull("jwt.audience")?.getString() ?: "taskgo-client"
        secret = System.getenv("JWT_SECRET") ?: environment.config.propertyOrNull("jwt.secret")?.getString() ?: "dev-secret-change-me"
    }

    fun configure(application: Application) {
        application.install(Authentication) {
            jwt("auth-jwt") {
                val algorithm = Algorithm.HMAC256(secret)
                verifier(
                    JWT
                        .require(algorithm)
                        .withIssuer(issuer)
                        .withAudience(audience)
                        .build()
                )
                validate { credential ->
                    if (credential.payload.getClaim("email").asString().isNullOrBlank()) null else JWTPrincipal(credential.payload)
                }
            }
        }
    }
}
