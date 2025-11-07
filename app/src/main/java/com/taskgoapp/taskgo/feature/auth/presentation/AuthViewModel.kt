package com.taskgoapp.taskgo.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskgoapp.taskgo.data.repository.FirebaseAuthRepository
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val user: FirebaseUser? = null,
    val error: String? = null
)

data class ChangePasswordUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

data class ForgotPasswordUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: FirebaseAuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _changePasswordState = MutableStateFlow(ChangePasswordUiState())
    val changePasswordState: StateFlow<ChangePasswordUiState> = _changePasswordState.asStateFlow()

    private val _forgotPasswordState = MutableStateFlow(ForgotPasswordUiState())
    val forgotPasswordState: StateFlow<ForgotPasswordUiState> = _forgotPasswordState.asStateFlow()

    init {
        // Verificar se usuário já está logado
        checkAuthState()
    }

    private fun checkAuthState() {
        authRepository.getCurrentUser()?.let { user ->
            _uiState.value = AuthUiState(
                isLoggedIn = true,
                user = user
            )
        }
    }

    // Social login pode ser implementado depois com Firebase Auth
    fun socialLogin(provider: String, idToken: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            // TODO: Implementar social login com Firebase
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Social login ainda não implementado"
            )
        }
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            _changePasswordState.value = ChangePasswordUiState(isLoading = true)
            // Primeiro reautenticar, depois atualizar senha
            authRepository.reauthenticate(currentPassword)
                .onSuccess {
                    authRepository.updatePassword(newPassword)
                        .onSuccess {
                            _changePasswordState.value = ChangePasswordUiState(success = true)
                        }
                        .onFailure { error ->
                            _changePasswordState.value = ChangePasswordUiState(error = error.message ?: "Erro ao alterar senha")
                        }
                }
                .onFailure { error ->
                    _changePasswordState.value = ChangePasswordUiState(error = error.message ?: "Senha atual incorreta")
                }
        }
    }

    fun clearChangePasswordState() {
        _changePasswordState.value = ChangePasswordUiState()
    }

    fun forgotPassword(email: String) {
        viewModelScope.launch {
            _forgotPasswordState.value = ForgotPasswordUiState(isLoading = true)
            authRepository.resetPassword(email)
                .onSuccess {
                    _forgotPasswordState.value = ForgotPasswordUiState(success = true)
                }
                .onFailure { error ->
                    _forgotPasswordState.value = ForgotPasswordUiState(error = error.message ?: "Erro ao enviar e-mail")
                }
        }
    }

    fun clearForgotPasswordState() {
        _forgotPasswordState.value = ForgotPasswordUiState()
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            authRepository.signInWithEmail(email, password)
                .onSuccess { user ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        user = user
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Erro no login"
                    )
                }
        }
    }

    fun signup(email: String, password: String, role: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            authRepository.signUpWithEmail(email, password)
                .onSuccess { user ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        user = user
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Erro no cadastro"
                    )
                }
        }
    }

    fun logout() {
        authRepository.signOut()
        _uiState.value = AuthUiState()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
