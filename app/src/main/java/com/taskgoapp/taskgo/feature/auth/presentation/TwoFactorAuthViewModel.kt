package com.taskgoapp.taskgo.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.taskgoapp.taskgo.data.repository.FirestoreUserRepository
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
    private val userRepository: FirestoreUserRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TwoFactorAuthUiState())
    val uiState: StateFlow<TwoFactorAuthUiState> = _uiState.asStateFlow()
    
    private var verificationCode: String? = null
    private var codeExpirationTime: Long = 0
    
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
                
                val user = userRepository.getUser(currentUser.uid) ?: run {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Usuário não encontrado"
                    )
                    return@launch
                }
                
                // Gerar código de 6 dígitos
                verificationCode = generateVerificationCode()
                codeExpirationTime = System.currentTimeMillis() + (10 * 60 * 1000) // 10 minutos
                
                // Enviar código via email ou SMS
                val success = if (user.phone != null) {
                    sendSmsCode(user.phone, verificationCode!!)
                } else if (user.email != null) {
                    sendEmailCode(user.email, verificationCode!!)
                } else {
                    false
                }
                
                if (success) {
                    // Salvar código no Firestore para verificação
                    firestore.collection("twoFactorCodes")
                        .document(currentUser.uid)
                        .set(mapOf(
                            "code" to verificationCode,
                            "expiresAt" to codeExpirationTime,
                            "createdAt" to System.currentTimeMillis()
                        ))
                        .await()
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        codeSent = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Erro ao enviar código de verificação"
                    )
                }
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
                
                // Buscar código do Firestore
                val codeDoc = firestore.collection("twoFactorCodes")
                    .document(currentUser.uid)
                    .get()
                    .await()
                
                if (!codeDoc.exists()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Código não encontrado. Solicite um novo código."
                    )
                    return@launch
                }
                
                val storedCode = codeDoc.getString("code")
                val expiresAt = codeDoc.getLong("expiresAt") ?: 0L
                
                // Verificar se código expirou
                if (System.currentTimeMillis() > expiresAt) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Código expirado. Solicite um novo código."
                    )
                    return@launch
                }
                
                // Verificar código
                if (code == storedCode) {
                    // Código válido - remover do Firestore
                    firestore.collection("twoFactorCodes")
                        .document(currentUser.uid)
                        .delete()
                        .await()
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isVerified = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Código inválido. Tente novamente."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erro: ${e.message}"
                )
            }
        }
    }
    
    private fun generateVerificationCode(): String {
        return (100000..999999).random().toString()
    }
    
    private suspend fun sendEmailCode(email: String, code: String): Boolean {
        return try {
            // Usar Firebase Cloud Functions ou serviço de email
            // Por enquanto, simular envio (em produção, usar Cloud Functions)
            firestore.collection("emailQueue")
                .add(mapOf(
                    "to" to email,
                    "subject" to "Código de Verificação - TaskGo",
                    "body" to """
                        Olá,
                        
                        Seu código de verificação de duas etapas é: $code
                        
                        Este código expira em 10 minutos.
                        
                        Se você não solicitou este código, ignore este email.
                        
                        Atenciosamente,
                        Equipe TaskGo
                    """.trimIndent(),
                    "createdAt" to System.currentTimeMillis()
                ))
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private suspend fun sendSmsCode(phoneNumber: String, code: String): Boolean {
        return try {
            // Usar Firebase Cloud Functions ou serviço de SMS
            // Por enquanto, simular envio (em produção, usar Cloud Functions)
            firestore.collection("smsQueue")
                .add(mapOf(
                    "to" to phoneNumber,
                    "message" to "Seu código de verificação TaskGo é: $code. Válido por 10 minutos.",
                    "createdAt" to System.currentTimeMillis()
                ))
                .await()
            true
        } catch (e: Exception) {
            false
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

