package com.taskgoapp.taskgo.feature.auth.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskgoapp.taskgo.core.model.UserType
import com.taskgoapp.taskgo.core.model.AccountType
import com.taskgoapp.taskgo.core.data.models.ServiceCategory
import com.taskgoapp.taskgo.data.firestore.models.UserFirestore
import com.taskgoapp.taskgo.data.repository.FirebaseAuthRepository
import com.taskgoapp.taskgo.data.repository.FirestoreUserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

data class SignupUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class SignupViewModel @Inject constructor(
    private val authRepository: FirebaseAuthRepository,
    private val firestoreUserRepository: FirestoreUserRepository,
    private val preferencesManager: com.taskgoapp.taskgo.data.local.datastore.PreferencesManager,
    private val categoriesRepository: com.taskgoapp.taskgo.domain.repository.CategoriesRepository,
    private val firebaseFunctionsService: com.taskgoapp.taskgo.data.firebase.FirebaseFunctionsService,
    private val initialDataSyncManager: com.taskgoapp.taskgo.core.sync.InitialDataSyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignupUiState())
    val uiState: StateFlow<SignupUiState> = _uiState
    
    // Observar categorias de serviço para exibir checkboxes no cadastro de Parceiro
    val serviceCategories: StateFlow<List<ServiceCategory>> = 
        categoriesRepository.observeServiceCategories()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun signup(
        name: String,
        email: String,
        phone: String,
        password: String,
        userType: UserType,
        accountType: AccountType? = null,
        cpf: String? = null,
        rg: String? = null,
        cnpj: String? = null,
        birthDate: Date? = null,
        address: com.taskgoapp.taskgo.core.model.Address? = null,
        biometricEnabled: Boolean = false,
        twoFactorEnabled: Boolean = false,
        twoFactorMethod: String? = null,
        preferredCategories: List<String>? = null // Categorias de serviço selecionadas pelo Parceiro
    ) {
        if (_uiState.value.isLoading) return

        // Validações básicas
        if (name.isBlank()) {
            _uiState.value = SignupUiState(errorMessage = "Nome é obrigatório")
            return
        }

        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.value = SignupUiState(errorMessage = "Email inválido")
            return
        }

        // Validar senha usando PasswordValidator
        val passwordValidator = com.taskgoapp.taskgo.core.validation.PasswordValidator()
        val passwordValidation = passwordValidator.validate(password)
        if (passwordValidation is com.taskgoapp.taskgo.core.validation.ValidationResult.Invalid) {
            _uiState.value = SignupUiState(errorMessage = passwordValidation.message)
            return
        }

        _uiState.value = SignupUiState(isLoading = true, errorMessage = null, isSuccess = false)

        viewModelScope.launch {
            try {
                Log.d("SignupViewModel", "Iniciando cadastro para: $email")
                Log.d("SignupViewModel", "Dados: nome=$name, email=$email, telefone=$phone, userType=$userType")
                
                // 1. Criar usuário no Firebase Auth
                val authResult = authRepository.signUpWithEmail(email.trim(), password)

                authResult.fold(
                    onSuccess = { firebaseUser ->
                        Log.d("SignupViewModel", "Usuário criado no Firebase Auth: ${firebaseUser.uid}")
                        
                        // 2. Atualizar perfil do Firebase Auth com o nome
                        authRepository.updateProfile(name, null).fold(
                            onSuccess = {
                                Log.d("SignupViewModel", "Perfil do Firebase Auth atualizado")
                                
                                // 3. Criar/atualizar perfil no Firestore
                                // Priorizar accountType se fornecido, senão usar userType
                                // Mapear AccountType para role string
                                val role = when (accountType) {
                                    AccountType.PARCEIRO -> "partner"
                                    AccountType.PRESTADOR -> "partner" // Legacy - migrar para partner
                                    AccountType.VENDEDOR -> "partner" // Legacy - migrar para partner
                                    AccountType.CLIENTE -> "client"
                                    null -> when (userType) {
                                        UserType.CLIENT -> "client"
                                        UserType.PROVIDER -> "partner" // Provider agora é partner
                                    }
                                }

                                val userFirestore = UserFirestore(
                                    uid = firebaseUser.uid,
                                    email = firebaseUser.email ?: email,
                                    displayName = name,
                                    photoURL = firebaseUser.photoUrl?.toString(),
                                    role = role,
                                    profileComplete = false,
                                    verified = false,
                                    createdAt = Date(),
                                    updatedAt = Date(),
                                    phone = phone,
                                    cpf = cpf,
                                    rg = rg,
                                    cnpj = cnpj,
                                    birthDate = birthDate,
                                    address = address,
                                    biometricEnabled = biometricEnabled,
                                    twoFactorEnabled = twoFactorEnabled,
                                    twoFactorMethod = twoFactorMethod,
                                    preferredCategories = if (accountType == AccountType.PARCEIRO && preferredCategories != null && preferredCategories.isNotEmpty()) {
                                        preferredCategories
                                    } else null
                                )
                                
                                // Salvar preferências de biometria e 2FA
                                viewModelScope.launch {
                                    preferencesManager.updateBiometricEnabled(biometricEnabled)
                                    preferencesManager.updateTwoFactorEnabled(twoFactorEnabled)
                                    if (twoFactorMethod != null) {
                                        preferencesManager.updateTwoFactorMethod(twoFactorMethod)
                                    }
                                    if (biometricEnabled) {
                                        preferencesManager.saveEmailForBiometric(email.trim())
                                    }
                                }

                                Log.d("SignupViewModel", "Verificando se usuário existe no Firestore...")
                                
                                // CRÍTICO: Aguardar um pouco para garantir que a Cloud Function tenha executado
                                // Se a função executar depois, ela não vai sobrescrever o role (função atualizada)
                                kotlinx.coroutines.delay(500)
                                
                                // Verificar se o documento já existe (criado pela Cloud Function)
                                val existingUser = try {
                                    firestoreUserRepository.getUser(firebaseUser.uid)
                                } catch (e: Exception) {
                                    Log.e("SignupViewModel", "Erro ao buscar usuário: ${e.message}", e)
                                    null
                                }

                                // CRÍTICO: Primeiro chamar setInitialUserRole Cloud Function para definir Custom Claims
                                Log.d("SignupViewModel", "Chamando setInitialUserRole Cloud Function com role: $role")
                                val setRoleResult = firebaseFunctionsService.setInitialUserRole(
                                    role, 
                                    accountType?.name
                                )
                                
                                setRoleResult.fold(
                                    onSuccess = { result ->
                                        Log.d("SignupViewModel", "setInitialUserRole bem-sucedido: $result")
                                        
                                        // CRÍTICO: Recarregar token para obter novos Custom Claims
                                        Log.d("SignupViewModel", "Recarregando token para obter novos Custom Claims...")
                                        try {
                                            firebaseUser.getIdToken(true).await()
                                            Log.d("SignupViewModel", "Token recarregado com sucesso")
                                        } catch (e: Exception) {
                                            Log.e("SignupViewModel", "Erro ao recarregar token: ${e.message}", e)
                                        }
                                        
                                        // Agora salvar/atualizar no Firestore
                                        Log.d("SignupViewModel", "Salvando usuário no Firestore com role: $role (accountType: $accountType)")
                                        
                                        if (existingUser != null) {
                                            Log.d("SignupViewModel", "Usuário existente encontrado, atualizando com role: $role")
                                            Log.d("SignupViewModel", "Role anterior: '${existingUser.role}', novo role: '$role'")
                                            
                                            // Atualizar documento existente - CRÍTICO: sempre atualizar o role com o accountType selecionado
                                            val updatedUser = existingUser.copy(
                                                displayName = name,
                                                phone = phone,
                                                role = role, // CRÍTICO: Sempre usar o role baseado no accountType selecionado
                                                cpf = cpf,
                                                rg = rg,
                                                cnpj = cnpj,
                                                birthDate = birthDate,
                                                address = address,
                                                biometricEnabled = biometricEnabled,
                                                twoFactorEnabled = twoFactorEnabled,
                                                twoFactorMethod = twoFactorMethod,
                                                preferredCategories = if (accountType == AccountType.PARCEIRO && preferredCategories != null && preferredCategories.isNotEmpty()) {
                                                    preferredCategories
                                                } else existingUser.preferredCategories, // Preservar existente se não fornecido
                                                pendingAccountType = false, // Remover flag de pendência
                                                updatedAt = Date()
                                            )
                                            firestoreUserRepository.updateUser(updatedUser).fold(
                                                onSuccess = {
                                                    Log.d("SignupViewModel", "✅ Perfil atualizado com sucesso com role: $role")
                                                    
                                                    // CRÍTICO: Forçar sincronização dos dados do usuário após atualizar role
                                                    Log.d("SignupViewModel", "🔄 Forçando sincronização dos dados do usuário após atualização de role...")
                                                    viewModelScope.launch {
                                                        try {
                                                            initialDataSyncManager.syncAllUserData()
                                                            Log.d("SignupViewModel", "✅ Sincronização de dados concluída")
                                                        } catch (e: Exception) {
                                                            Log.e("SignupViewModel", "Erro ao sincronizar dados após atualização de role: ${e.message}", e)
                                                        }
                                                    }
                                                    
                                                    _uiState.value = SignupUiState(isLoading = false, isSuccess = true)
                                                },
                                                onFailure = { exception ->
                                                    Log.e("SignupViewModel", "Erro ao atualizar perfil: ${exception.message}", exception)
                                                    // Mesmo com erro no Firestore, o Custom Claim já foi definido, então permitir cadastro
                                                    _uiState.value = SignupUiState(isLoading = false, isSuccess = true)
                                                }
                                            )
                                        } else {
                                            Log.d("SignupViewModel", "Usuário não existe, criando novo documento com role: $role")
                                            // Criar novo documento com o role correto desde o início
                                            val newUser = userFirestore.copy(
                                                pendingAccountType = false // Remover flag de pendência
                                            )
                                            firestoreUserRepository.updateUser(newUser).fold(
                                                onSuccess = {
                                                    Log.d("SignupViewModel", "✅ Perfil criado com sucesso com role: $role")
                                                    
                                                    // CRÍTICO: Forçar sincronização dos dados do usuário após criar perfil
                                                    Log.d("SignupViewModel", "🔄 Forçando sincronização dos dados do usuário após criação de perfil...")
                                                    viewModelScope.launch {
                                                        try {
                                                            initialDataSyncManager.syncAllUserData()
                                                            Log.d("SignupViewModel", "✅ Sincronização de dados concluída")
                                                        } catch (e: Exception) {
                                                            Log.e("SignupViewModel", "Erro ao sincronizar dados após criação de perfil: ${e.message}", e)
                                                        }
                                                    }
                                                    
                                                    _uiState.value = SignupUiState(isLoading = false, isSuccess = true)
                                                },
                                                onFailure = { exception ->
                                                    Log.e("SignupViewModel", "Erro ao criar perfil: ${exception.message}", exception)
                                                    // Mesmo com erro no Firestore, o Custom Claim já foi definido, então permitir cadastro
                                                    _uiState.value = SignupUiState(isLoading = false, isSuccess = true)
                                                }
                                            )
                                        }
                                    },
                                    onFailure = { exception ->
                                        Log.e("SignupViewModel", "Erro ao chamar setInitialUserRole: ${exception.message}", exception)
                                        // Se falhar, tentar salvar diretamente no Firestore (fallback)
                                        Log.d("SignupViewModel", "Tentando salvar diretamente no Firestore (fallback)...")
                                        
                                        if (existingUser != null) {
                                            val updatedUser = existingUser.copy(
                                                displayName = name,
                                                phone = phone,
                                                role = role,
                                                cpf = cpf,
                                                rg = rg,
                                                cnpj = cnpj,
                                                birthDate = birthDate,
                                                address = address,
                                                biometricEnabled = biometricEnabled,
                                                twoFactorEnabled = twoFactorEnabled,
                                                twoFactorMethod = twoFactorMethod,
                                                preferredCategories = if (accountType == AccountType.PARCEIRO && preferredCategories != null && preferredCategories.isNotEmpty()) {
                                                    preferredCategories
                                                } else existingUser.preferredCategories,
                                                pendingAccountType = false,
                                                updatedAt = Date()
                                            )
                                            firestoreUserRepository.updateUser(updatedUser).fold(
                                                onSuccess = {
                                                    Log.d("SignupViewModel", "✅ Perfil atualizado (fallback)")
                                                    _uiState.value = SignupUiState(isLoading = false, isSuccess = true)
                                                },
                                                onFailure = { e ->
                                                    Log.e("SignupViewModel", "Erro ao atualizar perfil (fallback): ${e.message}", e)
                                                    _uiState.value = SignupUiState(
                                                        isLoading = false,
                                                        errorMessage = "Erro ao criar perfil: ${e.message}"
                                                    )
                                                }
                                            )
                                        } else {
                                            val newUser = userFirestore.copy(pendingAccountType = false)
                                            firestoreUserRepository.updateUser(newUser).fold(
                                                onSuccess = {
                                                    Log.d("SignupViewModel", "✅ Perfil criado (fallback)")
                                                    _uiState.value = SignupUiState(isLoading = false, isSuccess = true)
                                                },
                                                onFailure = { e ->
                                                    Log.e("SignupViewModel", "Erro ao criar perfil (fallback): ${e.message}", e)
                                                    _uiState.value = SignupUiState(
                                                        isLoading = false,
                                                        errorMessage = "Erro ao criar perfil: ${e.message}"
                                                    )
                                                }
                                            )
                                        }
                                    }
                                )
                            },
                            onFailure = { exception ->
                                Log.e("SignupViewModel", "Erro ao atualizar perfil Firebase Auth: ${exception.message}", exception)
                                _uiState.value = SignupUiState(
                                    isLoading = false,
                                    errorMessage = "Erro ao atualizar perfil: ${exception.message}"
                                )
                            }
                        )
                    },
                    onFailure = { exception ->
                        Log.e("SignupViewModel", "Erro ao criar usuário: ${exception.message}", exception)
                        val errorMsg = when {
                            exception.message?.contains("email-already-in-use") == true -> "Este email já está cadastrado"
                            exception.message?.contains("invalid-email") == true -> "Email inválido"
                            exception.message?.contains("weak-password") == true -> "Senha muito fraca. Use pelo menos 6 caracteres"
                            exception.message?.contains("network") == true -> "Erro de conexão. Verifique sua internet"
                            else -> exception.message ?: "Falha ao criar conta"
                        }
                        _uiState.value = SignupUiState(isLoading = false, errorMessage = errorMsg)
                    }
                )
            } catch (e: Exception) {
                Log.e("SignupViewModel", "Erro inesperado: ${e.message}", e)
                _uiState.value = SignupUiState(
                    isLoading = false,
                    errorMessage = "Erro inesperado: ${e.message}"
                )
            }
        }
    }
}


