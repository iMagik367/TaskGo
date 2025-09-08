package com.example.taskgoapp.core.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val profileImage: String? = null,
    val userType: UserType = UserType.CLIENT,
    val isVerified: Boolean = false,
    val rating: Float = 0f,
    val totalReviews: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class UserType {
    CLIENT,     // Cliente que contrata serviços
    PROVIDER    // Prestador de serviços
}
