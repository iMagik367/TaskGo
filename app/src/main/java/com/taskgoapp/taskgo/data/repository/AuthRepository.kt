package com.taskgoapp.taskgo.data.repository

import android.content.Context
import com.taskgoapp.taskgo.core.auth.TokenManager
import com.taskgoapp.taskgo.core.model.Result
import com.taskgoapp.taskgo.data.api.AuthApiService
import com.taskgoapp.taskgo.data.api.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApiService: AuthApiService,
    private val context: Context
) {
    private val tokenManager = TokenManager(context)

    /**
     * Registrar novo usuário
     */
    suspend fun register(
        email: String,
        password: String,
        displayName: String? = null,
        phone: String? = null,
        role: String = "client"
    ): Result<RegisterResponse> {
        return try {
            val request = RegisterRequest(email, password, displayName, phone, role)
            val response = authApiService.register(request)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.status == "success" && body.data != null) {
                    Result.Success(body.data)
                } else {
                    Result.Error(Exception(body.message ?: "Erro ao registrar"))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Erro desconhecido"
                Result.Error(Exception("Erro ao registrar: $errorBody"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Login com email e senha
     */
    suspend fun login(
        email: String,
        password: String,
        twoFactorCode: String? = null
    ): Result<AuthResponse> {
        return try {
            val request = LoginRequest(email, password, twoFactorCode)
            val response = authApiService.login(request)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                
                // Verificar se requer 2FA
                if (body.requires2FA == true) {
                    return Result.Error(Exception("2FA_REQUIRED"))
                }

                if (body.status == "success" && body.data != null) {
                    val authData = body.data
                    
                    // Salvar tokens
                    tokenManager.saveTokens(
                        authData.accessToken,
                        authData.refreshToken,
                        authData.expiresIn
                    )
                    
                    // Salvar dados do usuário
                    tokenManager.saveCurrentUser(authData.user)
                    
                    Result.Success(authData)
                } else {
                    Result.Error(Exception(body.message ?: "Erro ao fazer login"))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Erro desconhecido"
                Result.Error(Exception("Erro ao fazer login: $errorBody"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Login com Google
     */
    suspend fun loginWithGoogle(idToken: String): Result<AuthResponse> {
        return try {
            val request = GoogleLoginRequest(idToken)
            val response = authApiService.loginWithGoogle(request)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                
                if (body.status == "success" && body.data != null) {
                    val authData = body.data
                    
                    // Salvar tokens
                    tokenManager.saveTokens(
                        authData.accessToken,
                        authData.refreshToken,
                        authData.expiresIn
                    )
                    
                    // Salvar dados do usuário
                    tokenManager.saveCurrentUser(authData.user)
                    
                    Result.Success(authData)
                } else {
                    Result.Error(Exception(body.message ?: "Erro ao fazer login com Google"))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Erro desconhecido"
                Result.Error(Exception("Erro ao fazer login com Google: $errorBody"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Renovar access token
     */
    suspend fun refreshToken(): Result<String> {
        return try {
            val refreshToken = tokenManager.getRefreshToken()
            if (refreshToken.isEmpty()) {
                return Result.Error(Exception("Refresh token não encontrado"))
            }

            val request = RefreshTokenRequest(refreshToken)
            val response = authApiService.refreshToken(request)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                
                if (body.status == "success" && body.data != null) {
                    val refreshData = body.data
                    
                    // Atualizar access token
                    val currentRefreshToken = tokenManager.getRefreshToken()
                    tokenManager.saveTokens(
                        refreshData.accessToken,
                        currentRefreshToken,
                        refreshData.expiresIn
                    )
                    
                    Result.Success(refreshData.accessToken)
                } else {
                    Result.Error(Exception(body.message ?: "Erro ao renovar token"))
                }
            } else {
                // Se refresh token inválido, fazer logout
                tokenManager.clearAuth()
                Result.Error(Exception("Sessão expirada. Faça login novamente."))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Logout
     */
    suspend fun logout(): Result<Unit> {
        return try {
            val refreshToken = tokenManager.getRefreshToken()
            val request = LogoutRequest(refreshToken)
            val response = authApiService.logout(request)

            // Limpar tokens localmente independente da resposta
            tokenManager.clearAuth()

            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                // Mesmo com erro no servidor, logout local foi feito
                Result.Success(Unit)
            }
        } catch (e: Exception) {
            // Mesmo com erro, limpar tokens localmente
            tokenManager.clearAuth()
            Result.Success(Unit)
        }
    }

    /**
     * Verificar email
     */
    suspend fun verifyEmail(token: String): Result<Unit> {
        return try {
            val request = VerifyEmailRequest(token)
            val response = authApiService.verifyEmail(request)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.status == "success") {
                    Result.Success(Unit)
                } else {
                    Result.Error(Exception(body.message ?: "Erro ao verificar email"))
                }
            } else {
                Result.Error(Exception("Erro ao verificar email"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Reenviar email de verificação
     */
    suspend fun resendVerification(email: String): Result<Unit> {
        return try {
            val request = ResendVerificationRequest(email)
            val response = authApiService.resendVerification(request)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.status == "success") {
                    Result.Success(Unit)
                } else {
                    Result.Error(Exception(body.message ?: "Erro ao reenviar verificação"))
                }
            } else {
                Result.Error(Exception("Erro ao reenviar verificação"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Solicitar reset de senha
     */
    suspend fun forgotPassword(email: String): Result<Unit> {
        return try {
            val request = ForgotPasswordRequest(email)
            val response = authApiService.forgotPassword(request)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.status == "success") {
                    Result.Success(Unit)
                } else {
                    Result.Error(Exception(body.message ?: "Erro ao solicitar reset de senha"))
                }
            } else {
                Result.Error(Exception("Erro ao solicitar reset de senha"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Redefinir senha com token
     */
    suspend fun resetPassword(token: String, newPassword: String): Result<Unit> {
        return try {
            val request = ResetPasswordRequest(token, newPassword)
            val response = authApiService.resetPassword(request)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.status == "success") {
                    Result.Success(Unit)
                } else {
                    Result.Error(Exception(body.message ?: "Erro ao redefinir senha"))
                }
            } else {
                Result.Error(Exception("Erro ao redefinir senha"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Alterar senha (autenticado)
     */
    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> {
        return try {
            val request = ChangePasswordRequest(currentPassword, newPassword)
            val response = authApiService.changePassword(request)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.status == "success") {
                    Result.Success(Unit)
                } else {
                    Result.Error(Exception(body.message ?: "Erro ao alterar senha"))
                }
            } else {
                Result.Error(Exception("Erro ao alterar senha"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Habilitar 2FA
     */
    suspend fun enable2FA(method: String, phone: String? = null): Result<TwoFactorSetupResponse> {
        return try {
            val request = Enable2FARequest(method, phone)
            val response = authApiService.enable2FA(request)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.status == "success" && body.data != null) {
                    Result.Success(body.data)
                } else {
                    Result.Error(Exception(body.message ?: "Erro ao habilitar 2FA"))
                }
            } else {
                Result.Error(Exception("Erro ao habilitar 2FA"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Verificar código 2FA
     */
    suspend fun verify2FA(code: String): Result<Unit> {
        return try {
            val request = Verify2FARequest(code)
            val response = authApiService.verify2FA(request)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.status == "success") {
                    Result.Success(Unit)
                } else {
                    Result.Error(Exception(body.message ?: "Erro ao verificar código 2FA"))
                }
            } else {
                Result.Error(Exception("Erro ao verificar código 2FA"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Desabilitar 2FA
     */
    suspend fun disable2FA(): Result<Unit> {
        return try {
            val response = authApiService.disable2FA()

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.status == "success") {
                    Result.Success(Unit)
                } else {
                    Result.Error(Exception(body.message ?: "Erro ao desabilitar 2FA"))
                }
            } else {
                Result.Error(Exception("Erro ao desabilitar 2FA"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    /**
     * Obter usuário atual
     */
    fun getCurrentUser() = tokenManager.getCurrentUser()

    /**
     * Verificar se está autenticado
     */
    fun isAuthenticated() = tokenManager.isAuthenticated()

    /**
     * Obter access token
     */
    fun getAccessToken() = tokenManager.getAccessToken()
}
