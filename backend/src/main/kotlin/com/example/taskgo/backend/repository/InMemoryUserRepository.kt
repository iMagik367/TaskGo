package com.example.taskgo.backend.repository

import com.example.taskgo.backend.domain.User
import com.example.taskgo.backend.domain.UserRepository
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

class InMemoryUserRepository : UserRepository {
    private val users = ConcurrentHashMap<String, User>()
    private val passwordHashes = ConcurrentHashMap<String, String>()

    override suspend fun createUser(email: String, passwordHash: String): User {
        val user = User(
            id = System.currentTimeMillis(),
            email = email,
            name = null
        )
        users[email] = user
        passwordHashes[email] = passwordHash
        return user
    }

    override suspend fun findUserByEmail(email: String): User? {
        return users[email]
    }

    override suspend fun updateUser(user: User): User {
        users[user.email] = user
        return user
    }

    suspend fun validatePassword(email: String, password: String): Boolean {
        val storedHash = passwordHashes[email] ?: return false
        return hashPassword(password) == storedHash
    }

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(password.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
}

