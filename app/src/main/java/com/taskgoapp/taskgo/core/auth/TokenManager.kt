package com.taskgoapp.taskgo.core.auth

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.taskgoapp.taskgo.data.api.model.UserResponse
import java.util.Date

class TokenManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("taskgo_auth", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_TOKEN_EXPIRES_AT = "token_expires_at"
        private const val KEY_CURRENT_USER = "current_user"
    }

    /**
     * Salvar tokens de autenticação
     */
    fun saveTokens(accessToken: String, refreshToken: String, expiresIn: Int) {
        prefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            // Calcular data de expiração (expiresIn está em segundos)
            val expiresAt = System.currentTimeMillis() + (expiresIn * 1000L)
            putLong(KEY_TOKEN_EXPIRES_AT, expiresAt)
            apply()
        }
    }

    /**
     * Obter access token
     */
    fun getAccessToken(): String? {
        return prefs.getString(KEY_ACCESS_TOKEN, null)
    }

    /**
     * Obter refresh token
     */
    fun getRefreshToken(): String? {
        return prefs.getString(KEY_REFRESH_TOKEN, null) ?: ""
    }

    /**
     * Verificar se token está expirado
     */
    fun isTokenExpired(): Boolean {
        val expiresAt = prefs.getLong(KEY_TOKEN_EXPIRES_AT, 0)
        if (expiresAt == 0L) return true
        return System.currentTimeMillis() >= expiresAt
    }

    /**
     * Verificar se token expira em breve (menos de 5 minutos)
     */
    fun isTokenExpiringSoon(): Boolean {
        val expiresAt = prefs.getLong(KEY_TOKEN_EXPIRES_AT, 0)
        if (expiresAt == 0L) return true
        val fiveMinutes = 5 * 60 * 1000L
        return System.currentTimeMillis() >= (expiresAt - fiveMinutes)
    }

    /**
     * Salvar dados do usuário atual
     */
    fun saveCurrentUser(user: UserResponse) {
        val userJson = gson.toJson(user)
        prefs.edit().putString(KEY_CURRENT_USER, userJson).apply()
    }

    /**
     * Obter usuário atual
     */
    fun getCurrentUser(): UserResponse? {
        val userJson = prefs.getString(KEY_CURRENT_USER, null) ?: return null
        return try {
            gson.fromJson(userJson, UserResponse::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Obter ID do usuário atual
     */
    fun getCurrentUserId(): String? {
        return getCurrentUser()?.id
    }

    /**
     * Limpar todos os dados de autenticação
     */
    fun clearAuth() {
        prefs.edit().apply {
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            remove(KEY_TOKEN_EXPIRES_AT)
            remove(KEY_CURRENT_USER)
            apply()
        }
    }

    /**
     * Verificar se usuário está autenticado
     */
    fun isAuthenticated(): Boolean {
        val token = getAccessToken()
        return token != null && !isTokenExpired()
    }
}
