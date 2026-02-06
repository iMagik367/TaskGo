package com.taskgoapp.taskgo.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskgoapp.taskgo.data.repository.AuthRepository
import com.taskgoapp.taskgo.data.api.model.UserResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val user: UserResponse? = null,
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
    private val authRepository: AuthRepository,
    private val addressRepository: com.taskgoapp.taskgo.domain.repository.AddressRepository,
    private val cardRepository: com.taskgoapp.taskgo.domain.repository.CardRepository,
    private val productsRepository: com.taskgoapp.taskgo.domain.repository.ProductsRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
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
        if (authRepository.isAuthenticated()) {
            authRepository.getCurrentUser()?.let { user ->
                _uiState.value = AuthUiState(
                    isLoggedIn = true,
                    user = user
                )
            }
        }
    }

    // Login com Google
    fun socialLogin(provider: String, idToken: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            if (provider == "google") {
                val result = authRepository.loginWithGoogle(idToken)
                result.fold(
                    onSuccess = { authResponse ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            user = authResponse.user
                        )
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Erro ao fazer login com Google"
                        )
                    }
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Provedor não suportado"
                )
            }
        }
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            _changePasswordState.value = ChangePasswordUiState(isLoading = true)
            
            val result = authRepository.changePassword(currentPassword, newPassword)
            result.fold(
                onSuccess = {
                    _changePasswordState.value = ChangePasswordUiState(success = true)
                },
                onFailure = { error ->
                    _changePasswordState.value = ChangePasswordUiState(error = error.message ?: "Erro ao alterar senha")
                }
            )
        }
    }

    fun clearChangePasswordState() {
        _changePasswordState.value = ChangePasswordUiState()
    }

    fun forgotPassword(email: String) {
        viewModelScope.launch {
            _forgotPasswordState.value = ForgotPasswordUiState(isLoading = true)
            
            val result = authRepository.forgotPassword(email)
            result.fold(
                onSuccess = {
                    _forgotPasswordState.value = ForgotPasswordUiState(success = true)
                },
                onFailure = { error ->
                    _forgotPasswordState.value = ForgotPasswordUiState(error = error.message ?: "Erro ao enviar e-mail")
                }
            )
        }
    }

    fun clearForgotPasswordState() {
        _forgotPasswordState.value = ForgotPasswordUiState()
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val result = authRepository.login(email, password)
            result.fold(
                onSuccess = { authResponse ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        user = authResponse.user
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Erro no login"
                    )
                }
            )
        }
    }

    fun signup(email: String, password: String, role: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val result = authRepository.register(email, password, null, null, role ?: "client")
            result.fold(
                onSuccess = { registerResponse ->
                    // Após registro, fazer login automaticamente
                    val loginResult = authRepository.login(email, password)
                    loginResult.fold(
                        onSuccess = { authResponse ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isLoggedIn = true,
                                user = authResponse.user
                            )
                        },
                        onFailure = { error ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Conta criada, mas erro ao fazer login: ${error.message}"
                            )
                        }
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Erro no cadastro"
                    )
                }
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            // CRÍTICO: Limpar todos os dados locais antes de fazer logout para evitar mistura de dados entre usuários
            try {
                // Limpar carrinho
                productsRepository.clearCart()
                
                // Limpar endereços locais (serão recarregados quando novo usuário fizer login)
                addressRepository.observeAddresses().first().forEach { address ->
                    addressRepository.deleteAddress(address.id)
                }
                
                // Limpar cartões locais (serão recarregados quando novo usuário fizer login)
                cardRepository.observeCards().first().forEach { card ->
                    cardRepository.deleteCard(card.id)
                }
                
                // CRÍTICO: Limpar foto de perfil e dados do usuário do PreferencesManager
                val preferencesManager = com.taskgoapp.taskgo.core.data.PreferencesManager(context)
                preferencesManager.saveUserAvatarUri("")
                preferencesManager.saveUserProfileImages(emptyList())
                preferencesManager.saveUserName("")
                preferencesManager.saveUserEmail("")
                preferencesManager.saveUserPhone("")
                preferencesManager.saveUserCpf("")
                preferencesManager.saveUserProfession("")
                android.util.Log.d("AuthViewModel", "Dados do usuário limpos do PreferencesManager")
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Erro ao limpar dados locais no logout: ${e.message}", e)
            }
            
            // Fazer logout
            authRepository.logout()
            _uiState.value = AuthUiState()
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
