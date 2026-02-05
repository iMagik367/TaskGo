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
import com.taskgoapp.taskgo.core.model.fold
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
    private val initialDataSyncManager: com.taskgoapp.taskgo.core.sync.InitialDataSyncManager,
    private val locationManager: com.taskgoapp.taskgo.core.location.LocationManager
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

        if (accountType == null) {
            _uiState.value = SignupUiState(errorMessage = "Tipo de conta é obrigatório. Selecione PARCEIRO ou CLIENTE.")
            return
        }

        if (address?.city.isNullOrBlank() || address?.state.isNullOrBlank()) {
            _uiState.value = SignupUiState(errorMessage = "Cidade e estado são obrigatórios")
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
                
                val userCity = address?.city?.takeIf { it.isNotBlank() } ?: ""
                val userState = address?.state?.takeIf { it.isNotBlank() } ?: ""
                
                if (userCity.isBlank() || userState.isBlank()) {
                    _uiState.value = SignupUiState(
                        isLoading = false,
                        errorMessage = "Cidade e estado são obrigatórios",
                        isSuccess = false
                    )
                    return@launch
                }
                
                val validatedCity = com.taskgoapp.taskgo.core.location.LocationValidator.validateAndNormalizeCity(userCity) ?: userCity
                val validatedState = com.taskgoapp.taskgo.core.location.LocationValidator.validateAndNormalizeState(userState) ?: userState
                
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
                                    AccountType.CLIENTE -> "client"
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
                                    address = address, // Address já é do tipo correto (com.taskgoapp.taskgo.core.model.Address)
                                    biometricEnabled = biometricEnabled,
                                    twoFactorEnabled = twoFactorEnabled,
                                    twoFactorMethod = twoFactorMethod,
                                    preferredCategories = if (accountType == AccountType.PARCEIRO && preferredCategories != null && preferredCategories.isNotEmpty()) {
                                        preferredCategories
                                    } else null,
                                    city = validatedCity,
                                    state = validatedState
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
                                Log.d("SignupViewModel", "📍 City/state que serão salvos: $validatedCity/$validatedState")
                                
                                val setRoleResult = firebaseFunctionsService.setInitialUserRole(
                                    role, 
                                    accountType.name
                                )
                                
                                setRoleResult.fold(
                                    onSuccess = { result: Map<String, Any> ->
                                        Log.d("SignupViewModel", "setInitialUserRole bem-sucedido: $result")
                                        
                                        // Executar operações suspend em coroutine
                                        viewModelScope.launch {
                                            // CRÍTICO: Recarregar token para obter novos Custom Claims
                                            Log.d("SignupViewModel", "Recarregando token para obter novos Custom Claims...")
                                            try {
                                                firebaseUser.getIdToken(true).await()
                                                Log.d("SignupViewModel", "Token recarregado com sucesso")
                                            } catch (e: Exception) {
                                                Log.e("SignupViewModel", "Erro ao recarregar token: ${e.message}", e)
                                            }
                                            
                                            // Agora salvar no Firestore
                                            Log.d("SignupViewModel", "Salvando usuário no Firestore com role: $role (accountType: $accountType)")
                                            
                                            // CRÍTICO: O documento sempre deve ser criado com city/state
                                            // Sempre criar novo documento em locations/city_state/users/user_id
                                            val newUser = userFirestore.copy(
                                                pendingAccountType = false
                                            )
                                            
                                            firestoreUserRepository.updateUser(newUser).fold(
                                                onSuccess = { _: Unit ->
                                                    val locationId = com.taskgoapp.taskgo.core.firebase.LocationHelper.normalizeLocationId(validatedCity, validatedState)
                                                    Log.d("SignupViewModel", "✅ Perfil criado com sucesso com role: $role em locations/$locationId/users/${firebaseUser.uid}")
                                                    
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
                                                    _uiState.value = SignupUiState(
                                                        isLoading = false,
                                                        errorMessage = "Erro ao criar perfil: ${exception.message}"
                                                    )
                                                }
                                            )
                                    }
                                    },
                                    onFailure = { exception: Throwable ->
                                        Log.e("SignupViewModel", "Erro ao chamar setInitialUserRole: ${exception.message}", exception)
                                        // Se falhar, tentar salvar diretamente no Firestore (fallback)
                                        Log.d("SignupViewModel", "Tentando salvar diretamente no Firestore (fallback)...")
                                        
                                        viewModelScope.launch {
                                            // CRÍTICO: Sempre criar novo documento com city/state
                                            val newUser = userFirestore.copy(pendingAccountType = false)
                                            firestoreUserRepository.updateUser(newUser).fold(
                                                onSuccess = { _: Unit ->
                                                    Log.d("SignupViewModel", "✅ Perfil criado (fallback)")
                                                    _uiState.value = SignupUiState(isLoading = false, isSuccess = true)
                                                },
                                                onFailure = { e: Throwable ->
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


