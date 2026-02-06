package com.taskgoapp.taskgo.feature.auth.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskgoapp.taskgo.data.repository.AuthRepository
import com.taskgoapp.taskgo.data.repository.FirestoreUserRepository
import com.taskgoapp.taskgo.data.firestore.models.UserFirestore
import com.taskgoapp.taskgo.core.model.AccountType
import com.taskgoapp.taskgo.core.model.fold
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

data class LoginUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val requiresTwoFactor: Boolean = false,
    val showAccountTypeDialog: Boolean = false,
    val requiresIdentityVerification: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val firestoreUserRepository: FirestoreUserRepository,
    private val initialDataSyncManager: com.taskgoapp.taskgo.core.sync.InitialDataSyncManager,
    private val preferencesManager: com.taskgoapp.taskgo.data.local.datastore.PreferencesManager,
    private val firebaseFunctionsService: com.taskgoapp.taskgo.data.firebase.FirebaseFunctionsService,
    private val locationUpdateService: com.taskgoapp.taskgo.core.location.LocationUpdateService,
    private val categoriesRepository: com.taskgoapp.taskgo.domain.repository.CategoriesRepository,
    private val locationManager: com.taskgoapp.taskgo.core.location.LocationManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState
    
    // Armazenar temporariamente o firebaseUser quando mostrar dialog de AccountType
    private var pendingFirebaseUser: com.google.firebase.auth.FirebaseUser? = null
    
    // Observar categorias de serviço para o dialog
    val serviceCategories = categoriesRepository.observeServiceCategories()
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Login com CPF/CNPJ - busca o email associado ao documento e faz login
     * Apenas para parceiros (role = "partner")
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
                    onSuccess = { data: Map<String, Any> ->
                        val email = data["email"] as? String
                        val role = data["role"] as? String
                        
                        if (email.isNullOrBlank()) {
                            _uiState.value = LoginUiState(
                                isLoading = false,
                                errorMessage = "CPF/CNPJ não encontrado. Verifique se você já possui cadastro.",
                                isSuccess = false,
                                requiresTwoFactor = false
                            )
                        } else {
                            // Verificar se é parceiro (já validado na Cloud Function, mas verificar novamente)
                            val userRole = role?.lowercase() ?: ""
                            if (userRole != "partner") {
                                Log.w("LoginViewModel", "Tentativa de login com CPF para usuário que não é parceiro. Role: $userRole")
                                _uiState.value = LoginUiState(
                                    isLoading = false,
                                    errorMessage = "Este CPF/CNPJ não está cadastrado como parceiro. Use email e senha para fazer login.",
                                    isSuccess = false,
                                    requiresTwoFactor = false
                                )
                            } else {
                                // Fazer login com o email encontrado - o método login() já verifica 2FA
                                Log.d("LoginViewModel", "Email encontrado para documento: $email, Role: $role")
                                login(email, password)
                            }
                        }
                    },
                    onFailure = { exception: Throwable ->
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
                
                val result = authRepository.login(email.trim(), password)
                result.fold(
                    onSuccess = { authResponse ->
                        Log.d("LoginViewModel", "Login bem-sucedido: ${authResponse.user.id}")
                        
                        // Salvar email para biometria
                        preferencesManager.saveEmailForBiometric(email.trim())
                        
                        // Verificar se requer 2FA
                        if (authResponse.user.twoFactorEnabled && !_uiState.value.requiresTwoFactor) {
                            _uiState.value = LoginUiState(
                                isLoading = false,
                                errorMessage = null,
                                isSuccess = false,
                                requiresTwoFactor = true
                            )
                            return@launch
                        }
                        
                        // Login bem-sucedido
                        _uiState.value = LoginUiState(
                            isLoading = false,
                            errorMessage = null,
                            isSuccess = true,
                            requiresTwoFactor = false
                        )
                    },
                    onFailure = { exception ->
                        Log.e("LoginViewModel", "Erro ao fazer login: ${exception.message}", exception)
                        
                        val errorMsg = when {
                            exception.message?.contains("2FA_REQUIRED", ignoreCase = true) == true -> {
                                _uiState.value = LoginUiState(
                                    isLoading = false,
                                    errorMessage = null,
                                    isSuccess = false,
                                    requiresTwoFactor = true
                                )
                                return@launch
                            }
                            exception.message?.contains("Credenciais inválidas", ignoreCase = true) == true -> "Email ou senha incorretos"
                            exception.message?.contains("Conta temporariamente bloqueada", ignoreCase = true) == true -> "Conta temporariamente bloqueada. Tente novamente mais tarde."
                            exception.message?.contains("network", ignoreCase = true) == true -> "Erro de conexão. Verifique sua internet"
                            exception.message?.contains("timeout", ignoreCase = true) == true -> "Tempo de conexão esgotado. Verifique sua internet"
                            else -> exception.message ?: "Erro ao fazer login. Tente novamente."
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
        // Verificar se precisa de verificação de identidade (parceiro não verificado)
        val isPartner = userFirestore?.role?.lowercase() == "partner"
        val needsIdentityVerification = isPartner && userFirestore?.verified != true && (userFirestore?.profileComplete != true)
        
        if (needsIdentityVerification) {
            Log.d("LoginViewModel", "Parceiro precisa de verificação de identidade, navegando...")
            _uiState.value = LoginUiState(
                isLoading = false,
                isSuccess = false,
                requiresTwoFactor = false,
                requiresIdentityVerification = true,
                errorMessage = null
            )
            return
        }
        
        // Verificar se 2FA está ativado
        val twoFactorEnabled = userFirestore?.twoFactorEnabled == true
        Log.d("LoginViewModel", "Verificando 2FA: enabled=$twoFactorEnabled, userFirestore=${userFirestore?.uid}")
        
        if (twoFactorEnabled) {
            Log.d("LoginViewModel", "2FA ativado, requer verificação - navegando para tela de 2FA")
            _uiState.value = LoginUiState(
                isLoading = false,
                isSuccess = false,
                requiresTwoFactor = true,
                requiresIdentityVerification = false,
                errorMessage = null
            )
        } else {
            Log.d("LoginViewModel", "2FA não ativado, navegando para home")
            _uiState.value = LoginUiState(isLoading = false, isSuccess = true, errorMessage = null, requiresTwoFactor = false, requiresIdentityVerification = false)
            
            // ✅ REMOVIDO: LocationUpdateService usa GPS para atualizar city/state
            // LEI MÁXIMA DO TASKGO: city/state deve vir APENAS do perfil do usuário (cadastro)
            // NUNCA usar GPS para city/state - GPS apenas para coordenadas (mapa) quando necessário
            Log.d("LoginViewModel", "✅ Login bem-sucedido - city/state vêm do perfil do Firestore")
            
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
                    onSuccess = { firebaseUser: com.google.firebase.auth.FirebaseUser ->
                        Log.d("LoginViewModel", "Login com Google bem-sucedido: ${firebaseUser.uid}")
                        
                        // Verificar se o usuário existe no Firestore, se não, criar
                        val existingUser = try {
                            firestoreUserRepository.getUser(firebaseUser.uid)
                        } catch (e: Exception) {
                            Log.e("LoginViewModel", "Erro ao buscar usuário: ${e.message}", e)
                            null
                        }
                        
                        if (existingUser != null) {
                            // CRÍTICO: Verificar se o usuário JÁ TEM role válido definido (partner ou client)
                            // Role válido = não é null, não é vazio, e é "partner" ou "client" (não "user" padrão)
                            val role = existingUser.role?.lowercase() ?: ""
                            val hasValidRole = role.isNotBlank() && (role == "partner" || role == "client")
                            
                            // CRÍTICO: Se pendingAccountType == true OU não tem role válido, SEMPRE mostrar dialog
                            // Isso garante que o dialog apareça mesmo se o usuário fechar e tentar de novo
                            val needsAccountTypeSelection = existingUser.pendingAccountType == true || !hasValidRole
                            
                            if (needsAccountTypeSelection) {
                                // USUÁRIO PRECISA SELECIONAR TIPO DE CONTA
                                Log.d("LoginViewModel", "PRIMEIRO LOGIN: Usuário precisa selecionar tipo de conta. Role atual: '${existingUser.role}', pendingAccountType: ${existingUser.pendingAccountType}, hasValidRole: $hasValidRole")
                                pendingFirebaseUser = firebaseUser
                                _uiState.value = LoginUiState(
                                    isLoading = false,
                                    showAccountTypeDialog = true,
                                    isSuccess = false,
                                    requiresTwoFactor = false
                                )
                            } else {
                                // USUÁRIO JÁ TEM TIPO DE CONTA VÁLIDO DEFINIDO - NÃO mostrar dialog
                                Log.d("LoginViewModel", "LOGIN SUBSEQUENTE: Usuário já tem tipo de conta válido definido. Role: $role, pendingAccountType: ${existingUser.pendingAccountType}")
                            
                                // Salvar email para biometria
                                preferencesManager.saveEmailForBiometric(firebaseUser.email ?: "")
                                
                                // Verificar 2FA e navegar
                                checkTwoFactorAndNavigate(existingUser, firebaseUser)
                            }
                        } else {
                            // NOVO USUÁRIO - Documento não existe no Firestore, mostrar dialog de seleção de tipo de conta
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
    fun createUserWithAccountType(data: com.taskgoapp.taskgo.core.design.AccountTypeSelectionData) {
        val firebaseUser = pendingFirebaseUser ?: return
        pendingFirebaseUser = null
        
        _uiState.value = LoginUiState(isLoading = true, showAccountTypeDialog = false, requiresTwoFactor = false)
        
        viewModelScope.launch {
            try {
                val role = when (data.accountType) {
                    com.taskgoapp.taskgo.core.model.AccountType.PARCEIRO -> "partner"
                    com.taskgoapp.taskgo.core.model.AccountType.CLIENTE -> "client"
                    else -> throw IllegalStateException("AccountType inválido: ${data.accountType}. Deve ser PARCEIRO ou CLIENTE.")
                }
                
                val userCity = data.city?.takeIf { it.isNotBlank() }
                    ?: run {
                        _uiState.value = LoginUiState(
                            isLoading = false,
                            errorMessage = "Cidade é obrigatória e não pode estar vazia",
                            isSuccess = false,
                            requiresTwoFactor = false
                        )
                        return@launch
                    }
                
                val userState = data.state?.takeIf { it.isNotBlank() }
                    ?: run {
                        _uiState.value = LoginUiState(
                            isLoading = false,
                            errorMessage = "Estado é obrigatório e não pode estar vazio",
                            isSuccess = false,
                            requiresTwoFactor = false
                        )
                        return@launch
                    }
                
                val validatedCity = com.taskgoapp.taskgo.core.location.LocationValidator.validateAndNormalizeCity(userCity) ?: userCity
                val validatedState = com.taskgoapp.taskgo.core.location.LocationValidator.validateAndNormalizeState(userState) ?: userState
                
                // CRÍTICO: Criar documento do usuário ANTES de chamar setInitialUserRole
                // A Cloud Function precisa que o documento exista para atualizar o role
                Log.d("LoginViewModel", "📝 Criando documento inicial do usuário no Firestore...")
                val initialUserDoc = UserFirestore(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = firebaseUser.displayName,
                    photoURL = firebaseUser.photoUrl?.toString(),
                    role = "client", // Temporário - será atualizado pela Cloud Function
                    pendingAccountType = true,
                    profileComplete = false,
                    verified = firebaseUser.isEmailVerified,
                    createdAt = Date(),
                    updatedAt = Date(),
                    city = validatedCity,
                    state = validatedState,
                    cpf = data.cpf,
                    cnpj = data.cnpj,
                    rg = data.rg
                )
                
                // Salvar documento inicial
                firestoreUserRepository.updateUser(initialUserDoc).fold(
                    onSuccess = {
                        Log.d("LoginViewModel", "✅ Documento inicial criado com sucesso")
                    },
                    onFailure = { e ->
                        Log.e("LoginViewModel", "❌ Erro ao criar documento inicial: ${e.message}", e)
                        _uiState.value = LoginUiState(
                            isLoading = false,
                            errorMessage = "Erro ao criar perfil: ${e.message}",
                            isSuccess = false,
                            requiresTwoFactor = false
                        )
                        return@launch
                    }
                )
                
                // Aguardar propagação do Firestore
                kotlinx.coroutines.delay(500)
                
                // Agora chamar setInitialUserRole para definir o role correto
                val setRoleResult = firebaseFunctionsService.setInitialUserRole(role, data.accountType.name)
                
                setRoleResult.fold(
                    onSuccess = { result: Map<String, Any> ->
                        Log.d("LoginViewModel", "✅ setInitialUserRole bem-sucedido: $result")
                        val resultRole = result["role"] as? String
                        Log.d("LoginViewModel", "   Role retornado pela CF: $resultRole")
                        
                        viewModelScope.launch {
                            // CRÍTICO: Recarregar token para obter novos Custom Claims
                            firebaseUser.getIdToken(true).await()
                            
                            // CRÍTICO: Aguardar propagação do Firestore após Cloud Function salvar
                            // A Cloud Function já salvou em users/{userId} e locations/{locationId}/users/{userId}
                            kotlinx.coroutines.delay(1000)
                            
                            // CRÍTICO: Ler o usuário do Firestore - o role já foi salvo pela Cloud Function
                            // O getUser busca primeiro em users/{userId} e depois em locations/{locationId}/users/{userId}
                            val existingUser = firestoreUserRepository.getUser(firebaseUser.uid)
                            
                            if (existingUser == null) {
                                Log.e("LoginViewModel", "❌ ERRO: existingUser é null após setInitialUserRole")
                                _uiState.value = LoginUiState(
                                    isLoading = false,
                                    errorMessage = "Erro ao ler dados do usuário após definir tipo de conta",
                                    isSuccess = false,
                                    requiresTwoFactor = false
                                )
                                return@launch
                            }
                            
                            // CRÍTICO: O existingUser sempre existirá após setInitialUserRole
                            // A Cloud Function já salvou em users/{userId} e o getUser sempre encontra lá
                            // O role já foi salvo pela Cloud Function e está no existingUser
                            val userFirestore = existingUser.copy(
                                pendingAccountType = false,
                                city = validatedCity,
                                state = validatedState,
                                cpf = data.cpf,
                                cnpj = data.cnpj,
                                rg = data.rg,
                                preferredCategories = if (data.accountType == com.taskgoapp.taskgo.core.model.AccountType.PARCEIRO && data.selectedCategories.isNotEmpty()) {
                                    data.selectedCategories.toList()
                                } else existingUser.preferredCategories,
                                updatedAt = Date()
                            )
                            
                            Log.d("LoginViewModel", "✅ Role lido do Firestore: ${userFirestore.role}")
                            
                            firestoreUserRepository.updateUser(userFirestore).fold(
                                onSuccess = { _: Unit ->
                                    preferencesManager.saveEmailForBiometric(firebaseUser.email ?: "")
                                    try {
                                        initialDataSyncManager.syncAllUserData()
                                    } catch (e: Exception) {
                                        // Ignorar erro de sincronização
                                    }
                                    
                                    if (data.accountType == com.taskgoapp.taskgo.core.model.AccountType.PARCEIRO) {
                                        _uiState.value = LoginUiState(
                                            isLoading = false,
                                            isSuccess = false,
                                            requiresTwoFactor = false,
                                            requiresIdentityVerification = true,
                                            errorMessage = null
                                        )
                                    } else {
                                        checkTwoFactorAndNavigate(userFirestore, firebaseUser)
                                    }
                                },
                                onFailure = { exception: Throwable ->
                                    preferencesManager.saveEmailForBiometric(firebaseUser.email ?: "")
                                    checkTwoFactorAndNavigate(existingUser ?: userFirestore, firebaseUser)
                                }
                            )
                        }
                    },
                    onFailure = { exception: Throwable ->
                        viewModelScope.launch {
                            val existingUser = try {
                                firestoreUserRepository.getUser(firebaseUser.uid)
                            } catch (e: Exception) {
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
                                updatedAt = Date(),
                                city = validatedCity,
                                state = validatedState
                            )
                            
                            firestoreUserRepository.updateUser(userFirestore).fold(
                                onSuccess = { _: Unit ->
                                    preferencesManager.saveEmailForBiometric(firebaseUser.email ?: "")
                                    checkTwoFactorAndNavigate(userFirestore, firebaseUser)
                                },
                                onFailure = { e: Throwable ->
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
                    }
                )
            } catch (e: Exception) {
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
        _uiState.value = LoginUiState(
            isLoading = false,
            showAccountTypeDialog = false,
            isSuccess = false,
            requiresTwoFactor = false
        )
        // Fazer logout já que cancelou a seleção
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
}


