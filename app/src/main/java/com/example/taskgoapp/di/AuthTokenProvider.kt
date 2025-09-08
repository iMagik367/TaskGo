package com.example.taskgoapp.di

interface AuthTokenProvider {
    fun getToken(): String?
}

class DefaultAuthTokenProvider : AuthTokenProvider {
    override fun getToken(): String? = null
}


