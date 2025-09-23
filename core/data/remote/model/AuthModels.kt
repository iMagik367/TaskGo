package br.com.taskgo.core.data.remote.model

data class LoginRequest(
    val email: String,
    val password: String,
    val provider: String? = null,
    val token: String? = null
)

data class LoginResponse(
    val token: String,
    val refreshToken: String,
    val user: UserResponse
)

data class UserResponse(
    val id: String,
    val name: String,
    val email: String,
    val role: String,
    val profilePicture: String?
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String
)

data class RefreshTokenRequest(
    val refreshToken: String
)