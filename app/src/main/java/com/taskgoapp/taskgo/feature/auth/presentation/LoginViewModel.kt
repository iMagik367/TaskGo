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
    private val initialDataSyncManager: com.taskgoapp.taskgo.core.sync.InitialDataSyncManager,
    private val preferencesManager: com.taskgoapp.taskgo.data.local.datastore.PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    /**
     * Login com CPF/CNPJ - busca o email associado ao documento e faz login
     * Apenas para prestadores (role = "provider")
     */
    fun loginWithDocument(document: String, password: String) {
        if (_uiState.value.isLoading) {
            Log.d("LoginViewModel", "Login já em progresso, ignorando requisição")
            return
        }
        
        Log.d("LoginViewModel", "Iniciando login com documento: $document")
        _uiState.value = LoginUiState(isLoading = true, errorMessage = null, isSuccess = false)
        
        viewModelScope.launch {
            try {
                // Buscar usuário por CPF/CNPJ
                val user = firestoreUserRepository.getUserByDocument(document)
                
                if (user == null || user.email.isBlank()) {
                    _uiState.value = LoginUiState(
                        isLoading = false,
                        errorMessage = "CPF/CNPJ não encontrado. Verifique se você já possui cadastro.",
                        isSuccess = false
                    )
                    return@launch
                }
                
                // CRÍTICO: Verificar se o usuário é prestador
                val userRole = user.role?.lowercase() ?: ""
                if (userRole != "provider") {
                    Log.w("LoginViewModel", "Tentativa de login com CPF para usuário que não é prestador. Role: $userRole")
                    _uiState.value = LoginUiState(
                        isLoading = false,
                        errorMessage = "Este CPF/CNPJ não está cadastrado como prestador. Use email e senha para fazer login.",
                        isSuccess = false
                    )
                    return@launch
                }
                
                // Fazer login com o email encontrado
                Log.d("LoginViewModel", "Email encontrado para documento: ${user.email}, Role: ${user.role}")
                login(user.email, password)
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Erro ao buscar usuário por documento: ${e.message}", e)
                _uiState.value = LoginUiState(
                    isLoading = false,
                    errorMessage = "Erro ao buscar usuário. Tente novamente.",
                    isSuccess = false
                )
            }
        }
    }
    
    fun login(email: String, password: String) {
        if (_uiState.value.isLoading) {
            Log.d("LoginViewModel", "Login já em progresso, ignorando requisição")
            return
        }
        
        Log.d("LoginViewModel", "Iniciando login para: $email")
        _uiState.value = LoginUiState(isLoading = true, errorMessage = null, isSuccess = false)
        
        viewModelScope.launch {
            try {
                // Garantir que Firebase Auth está inicializado
                val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                if (auth.app == null) {
                    Log.e("LoginViewModel", "Firebase Auth não inicializado")
                    _uiState.value = LoginUiState(
                        isLoading = false,
                        errorMessage = "Erro de autenticação. Reinicie o app e tente novamente.",
                        isSuccess = false
                    )
                    return@launch
                }
                
                val result = authRepository.signInWithEmail(email.trim(), password)
                result.fold(
                    onSuccess = { firebaseUser ->
                        Log.d("LoginViewModel", "Login bem-sucedido: ${firebaseUser.uid}")
                        
                        // Salvar email para biometria
                        preferencesManager.saveEmailForBiometric(email.trim())
                        
                        // Verificar e criar usuário no Firestore se necessário
                        try {
                            val existingUser = firestoreUserRepository.getUser(firebaseUser.uid)
                            if (existingUser == null) {
                                // Criar usuário no Firestore se não existir
                                Log.d("LoginViewModel", "Criando perfil no Firestore...")
                                val userFirestore = UserFirestore(
                                    uid = firebaseUser.uid,
                                    email = firebaseUser.email ?: email.trim(),
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
                                        Log.d("LoginViewModel", "Perfil criado com sucesso no Firestore")
                                    },
                                    onFailure = { exception ->
                                        Log.e("LoginViewModel", "Erro ao criar perfil no Firestore: ${exception.message}", exception)
                                    }
                                )
                            } else {
                                Log.d("LoginViewModel", "Usuário já existe no Firestore: ${existingUser.displayName}")
                            }
                        } catch (e: Exception) {
                            Log.e("LoginViewModel", "Erro ao verificar/criar usuário no Firestore: ${e.message}", e)
                        }
                        
                        _uiState.value = LoginUiState(isLoading = false, isSuccess = true, errorMessage = null)
                        
                        // Sincronizar dados em background (não bloqueia login)
                        viewModelScope.launch {
                            try {
                                initialDataSyncManager.syncAllUserData()
                                preferencesManager.setInitialSyncCompleted(firebaseUser.uid)
                            } catch (e: Exception) {
                                Log.e("LoginViewModel", "Erro ao sincronizar dados iniciais: ${e.message}", e)
                            }
                        }
                    },
                    onFailure = { exception ->
                        Log.e("LoginViewModel", "Erro ao fazer login: ${exception.message}", exception)
                        Log.e("LoginViewModel", "Tipo de exceção: ${exception.javaClass.name}")
                        Log.e("LoginViewModel", "Stack trace completo:", exception)
                        
                        val errorMsg = when {
                            // Erros específicos do Firebase Auth primeiro
                            exception is com.google.firebase.auth.FirebaseAuthException -> {
                                Log.e("LoginViewModel", "Código de erro Firebase: ${exception.errorCode}")
                                when (exception.errorCode) {
                                    "ERROR_WRONG_PASSWORD" -> "Senha incorreta"
                                    "ERROR_USER_NOT_FOUND" -> "Usuário não encontrado"
                                    "ERROR_INVALID_EMAIL" -> "Email inválido"
                                    "ERROR_USER_DISABLED" -> "Esta conta foi desabilitada"
                                    "ERROR_TOO_MANY_REQUESTS" -> "Muitas tentativas. Tente novamente mais tarde"
                                    "ERROR_OPERATION_NOT_ALLOWED" -> "Operação não permitida"
                                    "ERROR_NETWORK_REQUEST_FAILED" -> "Erro de conexão. Verifique sua internet"
                                    else -> {
                                        // Mostrar mensagem real do Firebase quando possível
                                        val firebaseMsg = exception.message
                                        if (firebaseMsg != null && firebaseMsg.isNotBlank()) {
                                            firebaseMsg
                                        } else {
                                            "Erro ao fazer login (${exception.errorCode})"
                                        }
                                    }
                                }
                            }
                            // Erros de rede específicos
                            exception is com.google.firebase.FirebaseNetworkException -> "Erro de conexão com o Firebase. Verifique sua internet"
                            exception is java.net.UnknownHostException -> "Erro de conexão. Verifique sua internet"
                            exception is java.net.ConnectException -> "Erro de conexão. Verifique sua internet"
                            exception is java.net.SocketTimeoutException -> "Tempo de conexão esgotado. Verifique sua internet"
                            // Verificar mensagem de erro apenas se não for exceção específica
                            exception.message?.contains("wrong-password", ignoreCase = true) == true -> "Senha incorreta"
                            exception.message?.contains("user-not-found", ignoreCase = true) == true -> "Usuário não encontrado"
                            exception.message?.contains("invalid-email", ignoreCase = true) == true -> "Email inválido"
                            exception.message?.contains("network", ignoreCase = true) == true -> "Erro de conexão. Verifique sua internet"
                            exception.message?.contains("timeout", ignoreCase = true) == true -> "Tempo de conexão esgotado. Verifique sua internet"
                            // Último recurso: mostrar mensagem real do erro
                            else -> {
                                val message = exception.message ?: "Falha ao fazer login"
                                Log.e("LoginViewModel", "Mensagem de erro não tratada: $message")
                                message
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
                
                // Garantir que Firebase Auth está inicializado
                val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                if (auth.app == null) {
                    Log.e("LoginViewModel", "Firebase Auth não inicializado")
                    _uiState.value = LoginUiState(
                        isLoading = false,
                        errorMessage = "Erro de autenticação. Reinicie o app e tente novamente.",
                        isSuccess = false
                    )
                    return@launch
                }
                
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
                        
                        if (existingUser != null) {
                            // Usuário já existe
                            Log.d("LoginViewModel", "Usuário já existe no Firestore")
                            // Sincronizar dados em background (não bloqueia login)
                            viewModelScope.launch {
                                try {
                                    val syncCompleted = preferencesManager.isInitialSyncCompleted(firebaseUser.uid)
                                    if (!syncCompleted) {
                                        initialDataSyncManager.syncAllUserData()
                                        preferencesManager.setInitialSyncCompleted(firebaseUser.uid)
                                    }
                                } catch (e: Exception) {
                                    Log.e("LoginViewModel", "Erro ao sincronizar dados: ${e.message}", e)
                                }
                            }
                            // Salvar email para biometria
                            preferencesManager.saveEmailForBiometric(firebaseUser.email ?: "")
                            _uiState.value = LoginUiState(isLoading = false, isSuccess = true)
                        } else {
                            // Criar usuário no Firestore
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
                                    Log.d("LoginViewModel", "Perfil criado com sucesso no Firestore")
                                    preferencesManager.saveEmailForBiometric(firebaseUser.email ?: "")
                                    // Sincronizar dados em background
                                    viewModelScope.launch {
                                        try {
                                            initialDataSyncManager.syncAllUserData()
                                            preferencesManager.setInitialSyncCompleted(firebaseUser.uid)
                                        } catch (e: Exception) {
                                            Log.e("LoginViewModel", "Erro ao sincronizar dados: ${e.message}", e)
                                        }
                                    }
                                    _uiState.value = LoginUiState(isLoading = false, isSuccess = true)
                                },
                                onFailure = { exception ->
                                    Log.e("LoginViewModel", "Erro ao criar perfil: ${exception.message}", exception)
                                    // Mesmo com erro, permitir login (usuário pode ser criado pela Cloud Function)
                                    preferencesManager.saveEmailForBiometric(firebaseUser.email ?: "")
                                    _uiState.value = LoginUiState(isLoading = false, isSuccess = true)
                                }
                            )
                        }
                    },
                    onFailure = { exception ->
                        Log.e("LoginViewModel", "Erro ao fazer login com Google: ${exception.message}", exception)
                        val errorMsg = when {
                            exception is com.google.firebase.auth.FirebaseAuthException -> {
                                when (exception.errorCode) {
                                    "ERROR_NETWORK_REQUEST_FAILED" -> "Erro de conexão. Verifique sua internet"
                                    "ERROR_INVALID_CREDENTIAL" -> "Credenciais inválidas"
                                    else -> exception.message ?: "Falha ao fazer login com Google"
                                }
                            }
                            exception.message?.contains("network", ignoreCase = true) == true -> "Erro de conexão. Verifique sua internet"
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


