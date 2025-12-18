package com.taskgoapp.taskgo.feature.auth.presentation

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.taskgoapp.taskgo.core.biometric.BiometricManager
import com.taskgoapp.taskgo.data.local.datastore.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BiometricAuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val hasSavedCredentials: Boolean = false,
    val savedEmail: String? = null,
    val showPasswordDialog: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class BiometricAuthViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(BiometricAuthUiState())
    val uiState: StateFlow<BiometricAuthUiState> = _uiState.asStateFlow()
    
    init {
        Log.d("BiometricAuthViewModel", "=== Inicializando BiometricAuthViewModel ===")
        checkSavedCredentials()
    }
    
    private fun checkSavedCredentials() {
        viewModelScope.launch {
            try {
                Log.d("BiometricAuthViewModel", "Verificando credenciais salvas")
                val savedEmail = preferencesManager.getEmailForBiometric()
                val biometricEnabled = preferencesManager.biometricEnabled.first()
                val currentUser = firebaseAuth.currentUser
                
                Log.d("BiometricAuthViewModel", "savedEmail: $savedEmail, biometricEnabled: $biometricEnabled, currentUser: ${currentUser?.email}")
                
                val hasCredentials = savedEmail != null && savedEmail.isNotBlank() && currentUser != null
                
                _uiState.value = _uiState.value.copy(
                    hasSavedCredentials = hasCredentials,
                    savedEmail = savedEmail,
                    isAuthenticated = currentUser != null && !hasCredentials // Se não precisa autenticar, já está autenticado
                )
                
                Log.d("BiometricAuthViewModel", "Estado atualizado - hasSavedCredentials: $hasCredentials")
            } catch (e: Exception) {
                Log.e("BiometricAuthViewModel", "Erro ao verificar credenciais", e)
                _uiState.value = _uiState.value.copy(
                    error = "Erro ao verificar credenciais: ${e.message}"
                )
            }
        }
    }
    
    fun authenticateWithBiometric(
        activity: Activity,
        biometricManager: BiometricManager,
        onSuccess: () -> Unit
    ) {
        Log.d("BiometricAuthViewModel", "Iniciando autenticação biométrica")
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        
        biometricManager.authenticate(
            activity = activity,
            title = "Autenticação Biométrica",
            subtitle = "Use sua biometria para acessar o app",
            negativeButtonText = "Cancelar",
            onSuccess = {
                Log.d("BiometricAuthViewModel", "Autenticação biométrica bem-sucedida")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isAuthenticated = true
                )
                onSuccess()
            },
            onError = { error ->
                Log.e("BiometricAuthViewModel", "Erro na autenticação biométrica: $error")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = error
                )
            },
            onCancel = {
                Log.d("BiometricAuthViewModel", "Autenticação biométrica cancelada")
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        )
    }
    
    fun authenticateWithPassword(
        password: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                Log.d("BiometricAuthViewModel", "Iniciando autenticação com senha")
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                val email = _uiState.value.savedEmail
                if (email == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Email não encontrado"
                    )
                    return@launch
                }
                
                // Reautenticar com Firebase
                val currentUser = firebaseAuth.currentUser
                if (currentUser == null) {
                    // Se não estiver logado, fazer login
                    firebaseAuth.signInWithEmailAndPassword(email, password)
                        .addOnSuccessListener {
                            Log.d("BiometricAuthViewModel", "Login com senha bem-sucedido")
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isAuthenticated = true
                            )
                            onSuccess()
                        }
                        .addOnFailureListener { e ->
                            Log.e("BiometricAuthViewModel", "Erro no login com senha", e)
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Senha incorreta"
                            )
                        }
                } else {
                    // Se já estiver logado, apenas verificar senha reautenticando
                    val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, password)
                    currentUser.reauthenticate(credential)
                        .addOnSuccessListener {
                            Log.d("BiometricAuthViewModel", "Reautenticação com senha bem-sucedida")
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isAuthenticated = true
                            )
                            onSuccess()
                        }
                        .addOnFailureListener { e ->
                            Log.e("BiometricAuthViewModel", "Erro na reautenticação", e)
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Senha incorreta"
                            )
                        }
                }
            } catch (e: Exception) {
                Log.e("BiometricAuthViewModel", "Erro ao autenticar com senha", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erro: ${e.message}"
                )
            }
        }
    }
    
    fun showPasswordDialog() {
        _uiState.value = _uiState.value.copy(showPasswordDialog = true)
    }
    
    fun hidePasswordDialog() {
        _uiState.value = _uiState.value.copy(showPasswordDialog = false, error = null)
    }
}

