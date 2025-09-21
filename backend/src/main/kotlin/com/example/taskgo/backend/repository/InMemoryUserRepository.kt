package com.example.taskgo.backend.repository

import com.example.taskgo.backend.domain.User
import com.example.taskgo.backend.domain.UserRepository
import com.example.taskgo.backend.domain.UserRole
import com.example.taskgo.backend.domain.UserDetailsUpdate
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

class InMemoryUserRepository : UserRepository {
    private val users = ConcurrentHashMap<String, User>()
    private val passwordHashes = ConcurrentHashMap<String, String>()

    override suspend fun createUser(email: String, passwordHash: String, role: UserRole): User {
        val user = User(
            id = System.currentTimeMillis(),
            email = email,
            name = null,
            role = role
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

    override suspend fun validatePassword(email: String, password: String): Boolean {
        val storedHash = passwordHashes[email] ?: return false
        return hashPassword(password) == storedHash
    }

    override suspend fun listAll(): List<User> {
        return users.values.sortedByDescending { it.id }
    }

    override suspend fun updateUserRole(userId: Long, newRole: UserRole): User? {
        val user = users.values.firstOrNull { it.id == userId } ?: return null
        val updated = user.copy(role = newRole)
        users[user.email] = updated
        return updated
    }

    override suspend fun updateUserDetails(userId: Long, details: UserDetailsUpdate): User? {
        val user = users.values.firstOrNull { it.id == userId } ?: return null
        val updated = user.copy(
            name = details.name ?: user.name,
            phone = details.phone ?: user.phone,
            address = details.address ?: user.address,
            documents = details.documents ?: user.documents,
            profilePhoto = details.profilePhoto ?: user.profilePhoto
        )
        users[user.email] = updated
        return updated
    }

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(password.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
}

