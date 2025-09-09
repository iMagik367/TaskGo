package com.example.taskgo.backend.domain

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Long,
    val email: String,
    val name: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class AuthRequest(
    val email: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val token: String
)

interface UserRepository {
    suspend fun createUser(email: String, passwordHash: String): User
    suspend fun findUserByEmail(email: String): User?
    suspend fun updateUser(user: User): User
}

