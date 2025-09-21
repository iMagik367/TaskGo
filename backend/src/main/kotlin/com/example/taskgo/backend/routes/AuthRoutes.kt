package com.example.taskgo.backend.routes

import com.example.taskgo.backend.auth.JwtConfig
import com.example.taskgo.backend.domain.AuthRequest
import com.example.taskgo.backend.domain.LoginRequest
import com.example.taskgo.backend.domain.AuthResponse
import com.example.taskgo.backend.domain.UserRepository
import com.example.taskgo.backend.domain.UserRole
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.security.MessageDigest
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun Route.authRoutes(userRepository: UserRepository) {
    route("/auth") {
        /**
         * Endpoint para iniciar recuperação de senha (envio de token por e-mail)
         * POST /auth/forgot-password { email }
         */
        post("/forgot-password") {
            val params = call.receiveParameters()
            val email = params["email"] ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "email required"))
            val user = userRepository.findUserByEmail(email)
            if (user == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "user not found"))
                return@post
            }
            // Geração de token simples (em produção, use JWT ou UUID + expiração)
            val token = java.util.UUID.randomUUID().toString()
            // TODO: Salvar token temporário no banco/cache e enviar por e-mail
            // Exemplo: EmailService.sendResetPassword(email, token)
            call.respond(mapOf("message" to "Token enviado para o e-mail (mock)", "token" to token))
        }

        /**
         * Endpoint para resetar a senha usando token
         * POST /auth/reset-password { email, token, newPassword }
         */
        post("/reset-password") {
            val params = call.receiveParameters()
            val email = params["email"] ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "email required"))
            val token = params["token"] ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "token required"))
            val newPassword = params["newPassword"] ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "newPassword required"))
            // TODO: Validar token (mock: aceitar qualquer token)
            val user = userRepository.findUserByEmail(email)
            if (user == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "user not found"))
                return@post
            }
            val passwordHash = hashPassword(newPassword)
            val updated = userRepository.updatePasswordByEmail(email, passwordHash)
            if (updated) {
                call.respond(mapOf("message" to "Senha redefinida com sucesso"))
            } else {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Falha ao atualizar senha"))
            }
        }
        post("/signup") {
            try {
                val request = call.receive<AuthRequest>()
                
                // Check if user already exists
                val existingUser = userRepository.findUserByEmail(request.email)
                if (existingUser != null) {
                    call.respond(HttpStatusCode.Conflict, mapOf("error" to "email already registered"))
                    return@post
                }
                
                // Create new user
                val passwordHash = hashPassword(request.password)
                val userRole = request.role ?: UserRole.CUSTOMER
                val user = userRepository.createUser(request.email, passwordHash, userRole)

                // Generate JWT token with role
                val token = JwtConfig.generateToken(user.email, userRole)
                
                call.respond(AuthResponse(token))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
            }
        }
        
        post("/login") {
            try {
                val request = call.receive<LoginRequest>()
                
                // Find user
                val user = userRepository.findUserByEmail(request.email)
                if (user == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "invalid credentials"))
                    return@post
                }
                
                // Validate password
                if (!userRepository.validatePassword(request.email, request.password)) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "invalid credentials"))
                    return@post
                }
                
                // Generate JWT token with role from DB
                val dbUser = userRepository.findUserByEmail(request.email)!!
                val token = JwtConfig.generateToken(dbUser.email, dbUser.role)
                
                call.respond(AuthResponse(token))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
            }
        }

        authenticate("auth-jwt") {
            get("/me") {
                val principal = call.principal<JWTPrincipal>()!!
                val email = principal.payload.getClaim("email").asString()
                val user = userRepository.findUserByEmail(email)
                if (user == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "user not found"))
                } else {
                    call.respond(user)
                }
            }

            get("/users") {
                val principal = call.principal<JWTPrincipal>()!!
                val role = principal.payload.getClaim("role").asString()
                if (role != "ADMIN") {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "forbidden"))
                    return@get
                }
                val users = userRepository.listAll()
                call.respond(users)
            }
        }
    }
}

private fun hashPassword(password: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(password.toByteArray())
    return hash.joinToString("") { "%02x".format(it) }
}

