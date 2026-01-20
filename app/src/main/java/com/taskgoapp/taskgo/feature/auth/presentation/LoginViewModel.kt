package com.taskgoapp.taskgo.feature.auth.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskgoapp.taskgo.data.repository.FirebaseAuthRepository
import com.taskgoapp.taskgo.data.repository.FirestoreUserRepository
import com.taskgoapp.taskgo.data.firestore.models.UserFirestore
import com.taskgoapp.taskgo.core.model.AccountType
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

data class LoginUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val requiresTwoFactor: Boolean = false,
    val showAccountTypeDialog: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: FirebaseAuthRepository,
    private val firestoreUserRepository: FirestoreUserRepository,
    private val initialDataSyncManager: com.taskgoapp.taskgo.core.sync.InitialDataSyncManager,
    private val preferencesManager: com.taskgoapp.taskgo.data.local.datastore.PreferencesManager,
    private val firebaseFunctionsService: com.taskgoapp.taskgo.data.firebase.FirebaseFunctionsService
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState
    
    // Armazenar temporariamente o firebaseUser quando mostrar dialog de AccountType
    private var pendingFirebaseUser: com.google.firebase.auth.FirebaseUser? = null

    /**
     * Login com CPF/CNPJ - busca o email associado ao documento e faz login
     * Apenas para parceiros (role = "partner" ou "provider" - legacy)
     */
    fun loginWithDocument(document: String, password: String) {
        if (_uiState.value.isLoading) {
            Log.d("LoginViewModel", "Login já em progresso, ignorando requisição")
            return
        }
        
        Log.d("LoginViewModel", "Iniciando login com documento: $document")
        _uiState.value = LoginUiState(isLoading = true, errorMessage = null, isSuccess = false, requiresTwoFactor = false)
        
        viewModelScope.launch {
            try {
                // CRÍTICO: Usar Cloud Function para buscar email por CPF/CNPJ
                // Isso permite busca sem autenticação (necessário para login)
                Log.d("LoginViewModel", "Buscando email por CPF/CNPJ via Cloud Function: $document")
                val result = firebaseFunctionsService.getUserEmailByDocument(document)
                
                result.fold(
                    onSuccess = { data ->
                        val email = data["email"] as? String
                        val role = data["role"] as? String
                        
                        if (email.isNullOrBlank()) {
                            _uiState.value = LoginUiState(
                                isLoading = false,
                                errorMessage = "CPF/CNPJ não encontrado. Verifique se você já possui cadastro.",
                                isSuccess = false,
                                requiresTwoFactor = false
                            )
                            return@fold
                        }
                        
                        // Verificar se é parceiro (já validado na Cloud Function, mas verificar novamente)
                        val userRole = role?.lowercase() ?: ""
                        if (userRole != "partner" && userRole != "provider") {
                            Log.w("LoginViewModel", "Tentativa de login com CPF para usuário que não é parceiro. Role: $userRole")
                            _uiState.value = LoginUiState(
                                isLoading = false,
                                errorMessage = "Este CPF/CNPJ não está cadastrado como parceiro. Use email e senha para fazer login.",
                                isSuccess = false,
                                requiresTwoFactor = false
                            )
                            return@fold
                        }
                        
                        // Fazer login com o email encontrado - o método login() já verifica 2FA
                        Log.d("LoginViewModel", "Email encontrado para documento: $email, Role: $role")
                        login(email, password)
                    },
                    onFailure = { exception ->
                        Log.e("LoginViewModel", "Erro ao buscar email por documento: ${exception.message}", exception)
                        val errorMessage = when (exception) {
                            is com.google.firebase.functions.FirebaseFunctionsException -> {
                                when (exception.code) {
                                    com.google.firebase.functions.FirebaseFunctionsException.Code.NOT_FOUND -> {
                                        "CPF/CNPJ não encontrado. Verifique se você já possui cadastro."
                                    }
                                    com.google.firebase.functions.FirebaseFunctionsException.Code.FAILED_PRECONDITION -> {
                                        exception.message ?: "Este CPF/CNPJ não está cadastrado como parceiro."
                                    }
                                    com.google.firebase.functions.FirebaseFunctionsException.Code.INVALID_ARGUMENT -> {
                                        exception.message ?: "CPF/CNPJ inválido. Verifique o formato."
                                    }
                                    else -> {
                                        "Erro ao buscar usuário. Tente novamente."
                                    }
                                }
                            }
                            else -> {
                                "Erro ao buscar usuário. Tente novamente."
                            }
                        }
                        _uiState.value = LoginUiState(
                            isLoading = false,
                            errorMessage = errorMessage,
                            isSuccess = false,
                            requiresTwoFactor = false
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Erro inesperado ao buscar usuário por documento: ${e.message}", e)
                _uiState.value = LoginUiState(
                    isLoading = false,
                    errorMessage = "Erro ao buscar usuário. Tente novamente.",
                    isSuccess = false,
                    requiresTwoFactor = false
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
                        // Já estamos dentro de viewModelScope.launch, então fazer diretamente
                        try {
                            var userFirestore: com.taskgoapp.taskgo.data.firestore.models.UserFirestore? = null
                            
                            val existingUser = firestoreUserRepository.getUser(firebaseUser.uid)
                            if (existingUser == null) {
                                // Criar usuário no Firestore se não existir
                                Log.d("LoginViewModel", "Criando perfil no Firestore...")
                                val newUser = UserFirestore(
                                    uid = firebaseUser.uid,
                                    email = firebaseUser.email ?: email.trim(),
                                    displayName = firebaseUser.displayName,
                                    photoURL = firebaseUser.photoUrl?.toString(),
                                    role = "client",
                                    profileComplete = false,
                                    verified = firebaseUser.isEmailVerified,
                                    createdAt = Date(),
                                    updatedAt = Date(),
                                    twoFactorEnabled = false
                                )
                                
                                firestoreUserRepository.updateUser(newUser).fold(
                                    onSuccess = {
                                        Log.d("LoginViewModel", "Perfil criado com sucesso no Firestore")
                                        userFirestore = newUser
                                        checkTwoFactorAndNavigate(userFirestore, firebaseUser)
                                    },
                                    onFailure = { exception ->
                                        Log.e("LoginViewModel", "Erro ao criar perfil no Firestore: ${exception.message}", exception)
                                        // Mesmo com erro, permitir login mas sem 2FA
                                        checkTwoFactorAndNavigate(null, firebaseUser)
                                    }
                                )
                            } else {
                                Log.d("LoginViewModel", "Usuário já existe no Firestore: ${existingUser.displayName}, Role: ${existingUser.role}, 2FA: ${existingUser.twoFactorEnabled}")
                                
                                // Verificar se é parceiro tentando fazer login via email
                                val existingUserRole = existingUser.role?.lowercase() ?: ""
                                if (existingUserRole == "partner" || existingUserRole == "provider") {
                                    // Parceiros devem usar CPF/CNPJ para login
                                    authRepository.signOut()
                                    _uiState.value = LoginUiState(
                                        isLoading = false,
                                        errorMessage = "Parceiros devem fazer login com CPF/CNPJ na tela de login de parceiro.",
                                        isSuccess = false,
                                        requiresTwoFactor = false
                                    )
                                    return@fold
                                }
                                
                                userFirestore = existingUser
                                checkTwoFactorAndNavigate(userFirestore, firebaseUser)
                            }
                        } catch (e: Exception) {
                            Log.e("LoginViewModel", "Erro ao verificar/criar usuário no Firestore: ${e.message}", e)
                            // Em caso de erro, permitir login mas sem 2FA
                            checkTwoFactorAndNavigate(null, firebaseUser)
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
                        _uiState.value = LoginUiState(isLoading = false, errorMessage = errorMsg, isSuccess = false, requiresTwoFactor = false)
                    }
                )
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Erro inesperado no login: ${e.message}", e)
                _uiState.value = LoginUiState(
                    isLoading = false,
                    errorMessage = "Erro inesperado: ${e.message}",
                    isSuccess = false,
                    requiresTwoFactor = false
                )
            }
        }
    }
    
    /**
     * Verifica se 2FA está ativado e navega adequadamente
     */
    private fun checkTwoFactorAndNavigate(userFirestore: com.taskgoapp.taskgo.data.firestore.models.UserFirestore?, firebaseUser: com.google.firebase.auth.FirebaseUser) {
        // Verificar se 2FA está ativado
        val twoFactorEnabled = userFirestore?.twoFactorEnabled == true
        Log.d("LoginViewModel", "Verificando 2FA: enabled=$twoFactorEnabled, userFirestore=${userFirestore?.uid}")
        
        if (twoFactorEnabled) {
            Log.d("LoginViewModel", "2FA ativado, requer verificação - navegando para tela de 2FA")
            _uiState.value = LoginUiState(
                isLoading = false,
                isSuccess = false,
                requiresTwoFactor = true,
                errorMessage = null
            )
        } else {
            Log.d("LoginViewModel", "2FA não ativado, navegando para home")
            _uiState.value = LoginUiState(isLoading = false, isSuccess = true, errorMessage = null, requiresTwoFactor = false)
            
            // Sincronizar dados em background (não bloqueia login)
            viewModelScope.launch {
                try {
                    initialDataSyncManager.syncAllUserData()
                    preferencesManager.setInitialSyncCompleted(firebaseUser.uid)
                } catch (e: Exception) {
                    Log.e("LoginViewModel", "Erro ao sincronizar dados iniciais: ${e.message}", e)
                }
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        if (_uiState.value.isLoading) return
        _uiState.value = LoginUiState(isLoading = true, requiresTwoFactor = false)
        
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
                            // CRÍTICO: Verificar se o usuário JÁ TEM role definido (partner ou client)
                            // Se já tem role definido, NUNCA mostrar dialog, mesmo que pendingAccountType seja true
                            val hasDefinedRole = existingUser.role != null && 
                                               existingUser.role.isNotBlank() && 
                                               (existingUser.role.lowercase() == "partner" || existingUser.role.lowercase() == "client")
                            
                            // Verificar se é um usuário "pendente" - APENAS no primeiro login após cadastro
                            // Critério: pendingAccountType == true E (role == null OU role == "client" padrão) E profileComplete == false
                            // E NÃO tem role definido (hasDefinedRole == false)
                            val isPendingUser = !hasDefinedRole && 
                                             existingUser.pendingAccountType == true && 
                                             (existingUser.role == null || existingUser.role.lowercase() == "client") && 
                                             existingUser.profileComplete == false
                            
                            if (isPendingUser) {
                                // PRIMEIRO LOGIN APÓS CADASTRO - Usuário foi criado pela Cloud Function mas ainda não selecionou tipo de conta
                                Log.d("LoginViewModel", "PRIMEIRO LOGIN: Usuário pendente detectado, mostrando dialog de seleção de tipo de conta. Role atual: ${existingUser.role}, pendingAccountType: ${existingUser.pendingAccountType}")
                                pendingFirebaseUser = firebaseUser
                                _uiState.value = LoginUiState(
                                    isLoading = false,
                                    showAccountTypeDialog = true,
                                    isSuccess = false,
                                    requiresTwoFactor = false
                                )
                            } else {
                                // USUÁRIO JÁ TEM TIPO DE CONTA DEFINIDO - NÃO mostrar dialog NUNCA
                                val existingRole = existingUser.role?.lowercase() ?: "client"
                                Log.d("LoginViewModel", "LOGIN SUBSEQUENTE: Usuário já tem tipo de conta definido. Role: $existingRole, pendingAccountType: ${existingUser.pendingAccountType}, profileComplete: ${existingUser.profileComplete}, hasDefinedRole: $hasDefinedRole")
                            
                                // Salvar email para biometria
                                preferencesManager.saveEmailForBiometric(firebaseUser.email ?: "")
                                
                                // Verificar 2FA e navegar
                                checkTwoFactorAndNavigate(existingUser, firebaseUser)
                            }
                        } else {
                            // NOVO USUÁRIO - Primeiro login, mostrar dialog de seleção de tipo de conta
                            Log.d("LoginViewModel", "PRIMEIRO LOGIN: Novo usuário Google (documento não existe no Firestore), mostrando dialog de seleção de tipo de conta")
                            pendingFirebaseUser = firebaseUser
                            _uiState.value = LoginUiState(
                                isLoading = false,
                                showAccountTypeDialog = true,
                                isSuccess = false,
                                requiresTwoFactor = false
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
                        _uiState.value = LoginUiState(isLoading = false, errorMessage = errorMsg, requiresTwoFactor = false)
                    }
                )
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Erro inesperado: ${e.message}", e)
                _uiState.value = LoginUiState(
                    isLoading = false,
                    errorMessage = "Erro inesperado: ${e.message}",
                    requiresTwoFactor = false
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

            _uiState.value = LoginUiState(isLoading = true, errorMessage = null, isSuccess = false, requiresTwoFactor = false)
            
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
                        isSuccess = false,
                        requiresTwoFactor = false
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
                    _uiState.value = LoginUiState(isLoading = false, requiresTwoFactor = false)
                }
            )
        }
    }
    
    /**
     * Cria o usuário no Firestore com o AccountType selecionado após login com Google
     */
    fun createUserWithAccountType(accountType: com.taskgoapp.taskgo.core.model.AccountType) {
        val firebaseUser = pendingFirebaseUser ?: return
        pendingFirebaseUser = null
        
        _uiState.value = LoginUiState(isLoading = true, showAccountTypeDialog = false, requiresTwoFactor = false)
        
        viewModelScope.launch {
            try {
                val role = when (accountType) {
                    com.taskgoapp.taskgo.core.model.AccountType.PARCEIRO -> "partner"
                    com.taskgoapp.taskgo.core.model.AccountType.PRESTADOR -> "partner" // Legacy - migrar para partner
                    com.taskgoapp.taskgo.core.model.AccountType.VENDEDOR -> "partner" // Legacy - migrar para partner
                    com.taskgoapp.taskgo.core.model.AccountType.CLIENTE -> "client"
                }
                
                Log.d("LoginViewModel", "Criando/atualizando perfil no Firestore com AccountType: $accountType, role: $role")
                
                // CRÍTICO: Primeiro chamar setInitialUserRole Cloud Function para definir Custom Claims
                Log.d("LoginViewModel", "🔵 Chamando setInitialUserRole Cloud Function...")
                Log.d("LoginViewModel", "   Parâmetros: role=$role, accountType=${accountType.name}, userId=${firebaseUser.uid}")
                val setRoleResult = firebaseFunctionsService.setInitialUserRole(role, accountType.name)
                
                setRoleResult.fold(
                    onSuccess = { result ->
                        Log.d("LoginViewModel", "✅ setInitialUserRole bem-sucedido: $result")
                        val resultRole = result["role"] as? String ?: "não encontrado"
                        Log.d("LoginViewModel", "   Role retornado pela CF: $resultRole")
                        
                        // CRÍTICO: Recarregar token para obter novos Custom Claims
                        Log.d("LoginViewModel", "Recarregando token para obter novos Custom Claims...")
                        try {
                            firebaseUser.getIdToken(true).await()
                            Log.d("LoginViewModel", "Token recarregado com sucesso")
                        } catch (e: Exception) {
                            Log.e("LoginViewModel", "Erro ao recarregar token: ${e.message}", e)
                        }
                        
                        // Verificar se o documento já existe antes de atualizar
                        val existingUser = try {
                            firestoreUserRepository.getUser(firebaseUser.uid)
                        } catch (e: Exception) {
                            Log.w("LoginViewModel", "Erro ao verificar usuário existente: ${e.message}")
                            null
                        }
                        
                        // Preservar createdAt se o usuário já existir
                        val createdAt = existingUser?.createdAt ?: Date()
                        
                        val userFirestore = UserFirestore(
                            uid = firebaseUser.uid,
                            email = firebaseUser.email ?: "",
                            displayName = firebaseUser.displayName,
                            photoURL = firebaseUser.photoUrl?.toString(),
                            role = role, // CRÍTICO: Definir role corretamente
                            pendingAccountType = false, // CRÍTICO: Remover flag para que dialog não apareça mais
                            profileComplete = existingUser?.profileComplete ?: false,
                            verified = firebaseUser.isEmailVerified,
                            createdAt = createdAt, // Preservar data de criação original
                            updatedAt = Date()
                        )
                        
                        Log.d("LoginViewModel", "Usuário antes de atualizar: ${existingUser?.uid}, role: ${existingUser?.role}, pendingAccountType: ${existingUser?.pendingAccountType}")
                        Log.d("LoginViewModel", "Dados a serem salvos: role=$role, pendingAccountType=false")
                        
                        firestoreUserRepository.updateUser(userFirestore).fold(
                            onSuccess = {
                                Log.d("LoginViewModel", "✅ Perfil atualizado com sucesso no Firestore. AccountType: $accountType, role: $role, pendingAccountType: false")
                                preferencesManager.saveEmailForBiometric(firebaseUser.email ?: "")
                                
                                // CRÍTICO: Forçar sincronização dos dados do usuário após atualizar role
                                Log.d("LoginViewModel", "🔄 Forçando sincronização dos dados do usuário após atualização de role...")
                                viewModelScope.launch {
                                    try {
                                        initialDataSyncManager.syncAllUserData()
                                        Log.d("LoginViewModel", "✅ Sincronização de dados concluída")
                                    } catch (e: Exception) {
                                        Log.e("LoginViewModel", "Erro ao sincronizar dados após atualização de role: ${e.message}", e)
                                    }
                                }
                                
                                // Verificar se o role foi salvo corretamente após atualização
                                kotlinx.coroutines.delay(500) // Aguardar para garantir que a atualização foi processada
                                val verifyUser = try {
                                    firestoreUserRepository.getUser(firebaseUser.uid)
                                } catch (e: Exception) {
                                    Log.w("LoginViewModel", "Erro ao verificar usuário após atualização: ${e.message}")
                                    null
                                }
                                val savedRole = verifyUser?.role?.lowercase() ?: "não encontrado"
                                val savedPending = verifyUser?.pendingAccountType ?: false
                                Log.d("LoginViewModel", "🔍 VERIFICAÇÃO CRÍTICA - role salvo: $savedRole (esperado: $role), pendingAccountType: $savedPending (esperado: false)")
                                
                                // Verificar 2FA e navegar
                                checkTwoFactorAndNavigate(verifyUser ?: userFirestore, firebaseUser)
                            },
                            onFailure = { exception ->
                                Log.e("LoginViewModel", "Erro ao atualizar perfil no Firestore: ${exception.message}", exception)
                                // Mesmo com erro no Firestore, o Custom Claim já foi definido, então permitir login
                                preferencesManager.saveEmailForBiometric(firebaseUser.email ?: "")
                                checkTwoFactorAndNavigate(existingUser ?: userFirestore, firebaseUser)
                            }
                        )
                    },
                    onFailure = { exception ->
                        Log.e("LoginViewModel", "❌ ERRO ao chamar setInitialUserRole: ${exception.message}", exception)
                        Log.e("LoginViewModel", "   Exception type: ${exception.javaClass.simpleName}")
                        Log.e("LoginViewModel", "   Stack trace:", exception)
                        // Se falhar, tentar salvar diretamente no Firestore (fallback)
                        val existingUser = try {
                            firestoreUserRepository.getUser(firebaseUser.uid)
                        } catch (e: Exception) {
                            Log.w("LoginViewModel", "Erro ao verificar usuário existente: ${e.message}")
                            null
                        }
                        
                        val createdAt = existingUser?.createdAt ?: Date()
                        val userFirestore = UserFirestore(
                            uid = firebaseUser.uid,
                            email = firebaseUser.email ?: "",
                            displayName = firebaseUser.displayName,
                            photoURL = firebaseUser.photoUrl?.toString(),
                            role = role,
                            pendingAccountType = false,
                            profileComplete = existingUser?.profileComplete ?: false,
                            verified = firebaseUser.isEmailVerified,
                            createdAt = createdAt,
                            updatedAt = Date()
                        )
                        
                        firestoreUserRepository.updateUser(userFirestore).fold(
                            onSuccess = {
                                Log.d("LoginViewModel", "Perfil atualizado no Firestore (fallback)")
                                preferencesManager.saveEmailForBiometric(firebaseUser.email ?: "")
                                checkTwoFactorAndNavigate(userFirestore, firebaseUser)
                            },
                            onFailure = { e ->
                                Log.e("LoginViewModel", "Erro ao criar perfil (fallback): ${e.message}", e)
                                preferencesManager.saveEmailForBiometric(firebaseUser.email ?: "")
                                _uiState.value = LoginUiState(
                                    isLoading = false,
                                    errorMessage = "Erro ao criar perfil: ${e.message}",
                                    showAccountTypeDialog = false,
                                    requiresTwoFactor = false
                                )
                            }
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Erro ao criar usuário com AccountType: ${e.message}", e)
                _uiState.value = LoginUiState(
                    isLoading = false,
                    errorMessage = "Erro ao criar perfil: ${e.message}",
                    showAccountTypeDialog = false,
                    requiresTwoFactor = false
                )
            }
        }
    }
    
    fun cancelAccountTypeSelection() {
        pendingFirebaseUser = null
        _uiState.value = LoginUiState(showAccountTypeDialog = false)
        // Fazer logout já que cancelou a seleção
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
}


