package com.taskgoapp.taskgo.data.api.model

import com.google.gson.annotations.SerializedName

// Request Models
data class RegisterRequest(
    val email: String,
    val password: String,
    @SerializedName("display_name") val displayName: String? = null,
    val phone: String? = null,
    val role: String = "client" // "client" or "partner"
)

data class LoginRequest(
    val email: String,
    val password: String,
    @SerializedName("two_factor_code") val twoFactorCode: String? = null
)

data class GoogleLoginRequest(
    @SerializedName("id_token") val idToken: String
)

data class RefreshTokenRequest(
    @SerializedName("refresh_token") val refreshToken: String
)

data class LogoutRequest(
    @SerializedName("refresh_token") val refreshToken: String? = null
)

data class VerifyEmailRequest(
    val token: String
)

data class ResendVerificationRequest(
    val email: String
)

data class ForgotPasswordRequest(
    val email: String
)

data class ResetPasswordRequest(
    val token: String,
    @SerializedName("new_password") val newPassword: String
)

data class ChangePasswordRequest(
    @SerializedName("current_password") val currentPassword: String,
    @SerializedName("new_password") val newPassword: String
)

data class Enable2FARequest(
    val method: String, // "sms", "email", "authenticator"
    val phone: String? = null
)

data class Verify2FARequest(
    val code: String
)

// Response Models
data class ApiResponse<T>(
    val status: String,
    val message: String? = null,
    val data: T? = null,
    @SerializedName("requires_2fa") val requires2FA: Boolean? = null
)

data class RegisterResponse(
    @SerializedName("user_id") val userId: String,
    val email: String
)

data class AuthResponse(
    val user: UserResponse,
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("expires_in") val expiresIn: Int
)

data class RefreshTokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("expires_in") val expiresIn: Int
)

data class UserResponse(
    val id: String,
    val email: String,
    val role: String,
    @SerializedName("display_name") val displayName: String? = null,
    val phone: String? = null,
    @SerializedName("photo_url") val photoUrl: String? = null,
    @SerializedName("email_verified") val emailVerified: Boolean,
    @SerializedName("two_factor_enabled") val twoFactorEnabled: Boolean
)

data class TwoFactorSetupResponse(
    @SerializedName("qr_code_url") val qrCodeUrl: String? = null,
    @SerializedName("backup_codes") val backupCodes: List<String>,
    val secret: String? = null
)
