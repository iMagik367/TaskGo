package com.example.taskgo.backend.domain

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Long,
    val email: String,
    val name: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val documents: List<String> = emptyList(),
    val profilePhoto: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val role: UserRole = UserRole.CUSTOMER
)

@Serializable
data class AuthRequest(
    val email: String,
    val password: String,
    val role: UserRole? = null
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val token: String
)

@Serializable
enum class UserRole {
    CUSTOMER,
    PROVIDER,
    ADMIN
}

interface UserRepository {
    suspend fun createUser(email: String, passwordHash: String, role: UserRole = UserRole.CUSTOMER): User
    suspend fun findUserByEmail(email: String): User?
    suspend fun updateUser(user: User): User
    suspend fun validatePassword(email: String, password: String): Boolean
    suspend fun listAll(): List<User>
    suspend fun updateUserRole(userId: Long, newRole: UserRole): User?
    suspend fun updateUserDetails(userId: Long, details: UserDetailsUpdate): User?
    suspend fun updatePasswordByEmail(email: String, newPasswordHash: String): Boolean
}

@Serializable
data class UserDetailsUpdate(
    val name: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val documents: List<String>? = null,
    val profilePhoto: String? = null
)

