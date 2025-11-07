package com.taskgoapp.taskgo.feature.auth.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskgoapp.taskgo.data.repository.FirebaseAuthRepository
import com.taskgoapp.taskgo.data.repository.FirestoreUserRepository
import com.taskgoapp.taskgo.data.firestore.models.UserFirestore
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Date

data class LoginUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: FirebaseAuthRepository,
    private val firestoreUserRepository: FirestoreUserRepository,
    private val preferencesManager: com.taskgoapp.taskgo.data.local.datastore.PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    fun login(email: String, password: String) {
        if (_uiState.value.isLoading) {
            Log.d("LoginViewModel", "Login já em progresso, ignorando requisição")
            return
        }
        
        Log.d("LoginViewModel", "Iniciando login para: $email")
        _uiState.value = LoginUiState(isLoading = true, errorMessage = null, isSuccess = false)
        
        viewModelScope.launch {
            try {
                val result = authRepository.signInWithEmail(email.trim(), password)
                result.fold(
                    onSuccess = { user ->
                        Log.d("LoginViewModel", "Login bem-sucedido: ${user.uid}")
                        // Salvar email para biometria se necessário
                        viewModelScope.launch {
                            preferencesManager.saveEmailForBiometric(email.trim())
                        }
                        _uiState.value = LoginUiState(isLoading = false, isSuccess = true, errorMessage = null)
                    },
                    onFailure = { exception ->
                        Log.e("LoginViewModel", "Erro ao fazer login: ${exception.message}", exception)
                        Log.e("LoginViewModel", "Tipo de exceção: ${exception.javaClass.name}")
                        Log.e("LoginViewModel", "Stack trace completo:", exception)
                        
                        val errorMsg = when {
                            exception.message?.contains("wrong-password", ignoreCase = true) == true -> "Senha incorreta"
                            exception.message?.contains("user-not-found", ignoreCase = true) == true -> "Usuário não encontrado"
                            exception.message?.contains("invalid-email", ignoreCase = true) == true -> "Email inválido"
                            exception.message?.contains("network", ignoreCase = true) == true -> "Erro de conexão. Verifique sua internet"
                            exception.message?.contains("timeout", ignoreCase = true) == true -> "Tempo de conexão esgotado. Verifique sua internet"
                            exception.message?.contains("unable to resolve host", ignoreCase = true) == true -> "Erro de conexão. Verifique sua internet"
                            exception.message?.contains("socket", ignoreCase = true) == true -> "Erro de conexão. Verifique sua internet"
                            exception.message?.contains("connect", ignoreCase = true) == true -> "Erro de conexão. Verifique sua internet"
                            exception is java.net.UnknownHostException -> "Erro de conexão. Verifique sua internet"
                            exception is java.net.ConnectException -> "Erro de conexão. Verifique sua internet"
                            exception is java.net.SocketTimeoutException -> "Tempo de conexão esgotado. Verifique sua internet"
                            exception is com.google.firebase.FirebaseNetworkException -> "Erro de conexão com o Firebase. Verifique sua internet"
                            exception.message?.contains("app-check", ignoreCase = true) == true -> "Erro de autenticação. Tente novamente"
                            else -> {
                                val message = exception.message ?: "Falha ao fazer login"
                                Log.e("LoginViewModel", "Mensagem de erro não tratada: $message")
                                "Erro de conexão. Verifique sua internet e tente novamente"
                            }
                        }
                        _uiState.value = LoginUiState(isLoading = false, errorMessage = errorMsg, isSuccess = false)
                    }
                )
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Erro inesperado no login: ${e.message}", e)
                _uiState.value = LoginUiState(
                    isLoading = false,
                    errorMessage = "Erro inesperado: ${e.message}",
                    isSuccess = false
                )
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        if (_uiState.value.isLoading) return
        _uiState.value = LoginUiState(isLoading = true)
        
        viewModelScope.launch {
            try {
                Log.d("LoginViewModel", "Iniciando login com Google")
                
                val result = authRepository.signInWithGoogle(idToken)
                result.fold(
                    onSuccess = { firebaseUser ->
                        Log.d("LoginViewModel", "Login com Google bem-sucedido: ${firebaseUser.uid}")
                        
                        // Verificar se o usuário existe no Firestore, se não, criar
                        val existingUser = try {
                            firestoreUserRepository.getUser(firebaseUser.uid)
                        } catch (e: Exception) {
                            Log.e("LoginViewModel", "Erro ao buscar usuário: ${e.message}", e)
                            null
                        }
                        
                        if (existingUser == null) {
                            // Criar usuário no Firestore se não existir
                            Log.d("LoginViewModel", "Criando perfil no Firestore...")
                            val userFirestore = UserFirestore(
                                uid = firebaseUser.uid,
                                email = firebaseUser.email ?: "",
                                displayName = firebaseUser.displayName,
                                photoURL = firebaseUser.photoUrl?.toString(),
                                role = "client",
                                profileComplete = false,
                                verified = firebaseUser.isEmailVerified,
                                createdAt = Date(),
                                updatedAt = Date()
                            )
                            
                            firestoreUserRepository.updateUser(userFirestore).fold(
                                onSuccess = {
                                    Log.d("LoginViewModel", "Perfil criado com sucesso")
                                    _uiState.value = LoginUiState(isLoading = false, isSuccess = true)
                                },
                                onFailure = { exception ->
                                    Log.e("LoginViewModel", "Erro ao criar perfil: ${exception.message}", exception)
                                    // Mesmo com erro ao criar perfil, permitir login
                                    _uiState.value = LoginUiState(isLoading = false, isSuccess = true)
                                }
                            )
                        } else {
                            Log.d("LoginViewModel", "Usuário já existe no Firestore")
                            _uiState.value = LoginUiState(isLoading = false, isSuccess = true)
                        }
                    },
                    onFailure = { exception ->
                        Log.e("LoginViewModel", "Erro ao fazer login com Google: ${exception.message}", exception)
                        val errorMsg = when {
                            exception.message?.contains("network") == true -> "Erro de conexão. Verifique sua internet"
                            else -> exception.message ?: "Falha ao fazer login com Google"
                        }
                        _uiState.value = LoginUiState(isLoading = false, errorMessage = errorMsg)
                    }
                )
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Erro inesperado: ${e.message}", e)
                _uiState.value = LoginUiState(
                    isLoading = false,
                    errorMessage = "Erro inesperado: ${e.message}"
                )
            }
        }
    }

    fun loginWithBiometric(
        biometricManager: com.taskgoapp.taskgo.core.biometric.BiometricManager,
        activity: android.app.Activity,
        onBiometricNotAvailable: () -> Unit
    ) {
        if (_uiState.value.isLoading) return
        
        viewModelScope.launch {
            val savedEmail = preferencesManager.getEmailForBiometric()
            if (savedEmail.isNullOrBlank()) {
                onBiometricNotAvailable()
                return@launch
            }

            _uiState.value = LoginUiState(isLoading = true, errorMessage = null, isSuccess = false)
            
            biometricManager.authenticate(
                activity = activity,
                title = "Login Biométrico",
                subtitle = "Use sua biometria para fazer login",
                onSuccess = {
                    // Biometria autenticada, buscar senha salva ou usar email apenas
                    // Por segurança, ainda precisamos validar com Firebase
                    // Por enquanto, vamos apenas mostrar que precisa inserir senha
                    _uiState.value = LoginUiState(
                        isLoading = false,
                        errorMessage = "Por favor, insira sua senha para completar o login",
                        isSuccess = false
                    )
                },
                onError = { error ->
                    _uiState.value = LoginUiState(
                        isLoading = false,
                        errorMessage = error,
                        isSuccess = false
                    )
                },
                onCancel = {
                    _uiState.value = LoginUiState(isLoading = false)
                }
            )
        }
    }
}


