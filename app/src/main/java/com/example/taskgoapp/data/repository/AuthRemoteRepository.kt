package com.example.taskgoapp.data.repository

import com.example.taskgoapp.core.data.PreferencesManager
import com.example.taskgoapp.core.data.remote.AuthApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRemoteRepository @Inject constructor(
    private val authApi: AuthApi,
    private val prefs: PreferencesManager
) {
    suspend fun signup(email: String, password: String): String {
        val res = authApi.signup(com.example.taskgoapp.core.data.remote.SignupRequest(email, password))
        prefs.saveAuthToken(res.token)
        return res.token
    }
    suspend fun login(email: String, password: String): String {
        val res = authApi.login(com.example.taskgoapp.core.data.remote.LoginRequest(email, password))
        prefs.saveAuthToken(res.token)
        return res.token
    }
    fun logout() { prefs.saveAuthToken(null) }
}



