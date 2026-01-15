package com.taskgoapp.taskgo.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.taskgoapp.taskgo.data.repository.FirestoreUserRepository
import com.taskgoapp.taskgo.data.firebase.FirebaseFunctionsService
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
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val userRepository: FirestoreUserRepository,
    private val functionsService: FirebaseFunctionsService
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
                val currentUser = auth.currentUser ?: return@launch
                val user = userRepository.getUser(currentUser.uid)
                
                val method = when {
                    user?.phone != null -> "Telefone: ${maskPhone(user.phone)}"
                    user?.email != null -> "Email: ${maskEmail(user.email)}"
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
                
                val currentUser = auth.currentUser ?: run {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Usuário não autenticado"
                    )
                    return@launch
                }
                
                // Chamar Cloud Function para enviar código
                val result = functionsService.sendTwoFactorCode()
                
                result.fold(
                    onSuccess = { data ->
                        val method = data["method"] as? String ?: "email"
                        val message = data["message"] as? String ?: "Código enviado"
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            codeSent = true,
                            verificationMethod = message
                        )
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Erro ao enviar código: ${exception.message}"
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
    
    fun resendCode() {
        sendVerificationCode()
    }
    
    fun verifyCode(code: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                val currentUser = auth.currentUser ?: run {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Usuário não autenticado"
                    )
                    return@launch
                }
                
                // Chamar Cloud Function para verificar código
                val result = functionsService.verifyTwoFactorCode(code)
                
                result.fold(
                    onSuccess = { data ->
                        val verified = data["verified"] as? Boolean ?: false
                        if (verified) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isVerified = true
                            )
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Código inválido"
                            )
                        }
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

