package com.example.taskgo.backend.email

interface EmailService {
    suspend fun sendPasswordReset(email: String, token: String)
}
