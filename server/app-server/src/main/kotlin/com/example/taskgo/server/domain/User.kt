package com.example.taskgo.server.domain

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Long,
    val name: String,
    val email: String,
    val passwordHash: String
)

@Serializable
data class AuthRequest(val email: String, val password: String)

@Serializable
data class AuthResponse(val token: String)

interface UserRepository {
    suspend fun create(name: String, email: String, passwordHash: String): User
    suspend fun findByEmail(email: String): User?
    suspend fun updateNameAndPassword(email: String, name: String, passwordHash: String): User?
}

class InMemoryUserRepository : UserRepository {
    private val users = mutableListOf<User>()
    private var seq: Long = 1

    override suspend fun create(name: String, email: String, passwordHash: String): User {
        val user = User(seq++, name, email.lowercase(), passwordHash)
        users.add(user)
        return user
    }

    override suspend fun findByEmail(email: String): User? = users.firstOrNull { it.email == email.lowercase() }

    override suspend fun updateNameAndPassword(email: String, name: String, passwordHash: String): User? {
        val idx = users.indexOfFirst { it.email == email.lowercase() }
        if (idx < 0) return null
        val updated = users[idx].copy(name = name, passwordHash = passwordHash)
        users[idx] = updated
        return updated
    }
}
