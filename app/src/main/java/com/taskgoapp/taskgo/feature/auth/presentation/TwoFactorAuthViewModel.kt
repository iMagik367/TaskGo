package com.taskgoapp.taskgo.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskgoapp.taskgo.data.repository.AuthRepository
import com.taskgoapp.taskgo.core.model.fold
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TwoFactorAuthUiState(
    val verificationMethod: String = "",
    val isLoading: Boolean = false,
    val isVerified: Boolean = false,
    val error: String? = null,
    val codeSent: Boolean = false
)

@HiltViewModel
class TwoFactorAuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TwoFactorAuthUiState())
    val uiState: StateFlow<TwoFactorAuthUiState> = _uiState.asStateFlow()
    
    init {
        loadVerificationMethod()
        sendVerificationCode()
    }
    
    private fun loadVerificationMethod() {
        viewModelScope.launch {
            try {
                val currentUser = authRepository.getCurrentUser() ?: return@launch
                
                val method = when {
                    currentUser.phone != null -> "Telefone: ${maskPhone(currentUser.phone)}"
                    currentUser.email.isNotEmpty() -> "Email: ${maskEmail(currentUser.email)}"
                    else -> "Email ou Telefone"
                }
                
                _uiState.value = _uiState.value.copy(verificationMethod = method)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Erro ao carregar método de verificação")
            }
        }
    }
    
    fun sendVerificationCode() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                val currentUser = authRepository.getCurrentUser() ?: run {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Usuário não autenticado"
                    )
                    return@launch
                }
                
                // O código será enviado automaticamente pelo backend quando necessário
                // Por enquanto, apenas atualizar o estado
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    codeSent = true,
                    verificationMethod = "Código enviado para ${maskEmail(currentUser.email)}"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erro: ${e.message}"
                )
            }
        }
    }
    
    fun resendCode() {
        sendVerificationCode()
    }
    
    fun verifyCode(code: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                val currentUser = authRepository.getCurrentUser() ?: run {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Usuário não autenticado"
                    )
                    return@launch
                }
                
                // Verificar código 2FA via API
                val result = authRepository.verify2FA(code)
                
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isVerified = true
                        )
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = when {
                                exception.message?.contains("expirado") == true -> "Código expirado. Solicite um novo código."
                                exception.message?.contains("inválido") == true -> "Código inválido. Tente novamente."
                                else -> "Erro ao verificar código: ${exception.message}"
                            }
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erro: ${e.message}"
                )
            }
        }
    }
    
    
    private fun maskEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) return email
        val username = parts[0]
        val domain = parts[1]
        val maskedUsername = if (username.length > 2) {
            "${username.take(2)}***"
        } else {
            "***"
        }
        return "$maskedUsername@$domain"
    }
    
    private fun maskPhone(phone: String): String {
        return if (phone.length > 4) {
            "***${phone.takeLast(4)}"
        } else {
            "***"
        }
    }
}

