package br.com.taskgo.taskgo.data.repository

import com.example.taskgoapp.core.data.PreferencesManager
import com.example.taskgoapp.core.data.remote.AuthApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRemoteRepository @Inject constructor(
    private val authApi: AuthApi,
    private val prefs: PreferencesManager
) {
    suspend fun socialLogin(provider: String, idToken: String): String {
        val res = authApi.socialLogin(com.example.taskgoapp.core.data.remote.SocialLoginRequest(provider, idToken))
        prefs.saveAuthToken(res.token)
        return res.token
    }
    
    suspend fun changePassword(currentPassword: String, newPassword: String): String {
        val res = authApi.changePassword(com.example.taskgoapp.core.data.remote.ChangePasswordRequest(currentPassword, newPassword))
        return res.message
    }
    
    suspend fun signup(email: String, password: String, role: String? = null): String {
        val res = authApi.signup(com.example.taskgoapp.core.data.remote.SignupRequest(email, password, role))
        prefs.saveAuthToken(res.token)
        return res.token
    }

    suspend fun login(email: String, password: String): String {
        val res = authApi.login(com.example.taskgoapp.core.data.remote.LoginRequest(email, password))
        prefs.saveAuthToken(res.token)
        return res.token
    }

    fun logout() { 
        prefs.saveAuthToken(null) 
    }

    suspend fun forgotPassword(email: String): String {
        val res = authApi.forgotPassword(com.example.taskgoapp.core.data.remote.ForgotPasswordRequest(email))
        return res.message
    }
}



