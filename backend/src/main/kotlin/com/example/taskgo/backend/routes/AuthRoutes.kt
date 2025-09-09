package com.example.taskgo.backend.routes

import com.example.taskgo.backend.auth.JwtConfig
import com.example.taskgo.backend.domain.AuthRequest
import com.example.taskgo.backend.domain.AuthResponse
import com.example.taskgo.backend.domain.UserRepository
import com.example.taskgo.backend.repository.InMemoryUserRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.security.MessageDigest

fun Route.authRoutes(userRepository: UserRepository) {
    route("/auth") {
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
                val user = userRepository.createUser(request.email, passwordHash)
                
                // Generate JWT token
                val token = JwtConfig.generateToken(user.email)
                
                call.respond(AuthResponse(token))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
            }
        }
        
        post("/login") {
            try {
                val request = call.receive<AuthRequest>()
                
                // Find user
                val user = userRepository.findUserByEmail(request.email)
                if (user == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "invalid credentials"))
                    return@post
                }
                
                // Validate password
                val userRepo = userRepository as? InMemoryUserRepository
                if (userRepo == null || !userRepo.validatePassword(request.email, request.password)) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "invalid credentials"))
                    return@post
                }
                
                // Generate JWT token
                val token = JwtConfig.generateToken(user.email)
                
                call.respond(AuthResponse(token))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
            }
        }
    }
}

private fun hashPassword(password: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(password.toByteArray())
    return hash.joinToString("") { "%02x".format(it) }
}

