package com.example.taskgo.backend.auth

data class OAuthUserInfo(
    val email: String,
    val name: String?,
    val picture: String?,
    val provider: String,
    val providerId: String
)
