package com.taskgoapp.taskgo.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.util.Log
import com.taskgoapp.taskgo.core.data.PreferencesManager
import com.taskgoapp.taskgo.core.model.AccountType
import com.taskgoapp.taskgo.core.model.Address
import com.taskgoapp.taskgo.core.model.UserProfile
import com.taskgoapp.taskgo.data.firestore.models.UserFirestore
import java.util.Date
import com.taskgoapp.taskgo.data.repository.FirebaseAuthRepository
import com.taskgoapp.taskgo.data.repository.FirestoreUserRepository
import com.taskgoapp.taskgo.data.repository.FirebaseStorageRepository
import com.taskgoapp.taskgo.domain.repository.UserRepository
import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

data class ProfileState(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val city: String = "",
    val profession: String = "",
    val accountType: AccountType = AccountType.CLIENTE,
    val rating: Double? = null,
    val avatarUri: String? = null,
    val profileImages: List<String> = emptyList(),
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
    val isLoading: Boolean = true,
    val createdAt: Date? = null,
    // Novos campos
    val cpf: String = "",
    val rg: String = "",
    val state: String = "",
    val country: String = "Brasil",
    val street: String = "",
    val number: String = "",
    val neighborhood: String = "",
    val zipCode: String = "",
    val complement: String = ""
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: FirebaseAuthRepository,
    private val firestoreUserRepository: FirestoreUserRepository,
    private val storageRepository: FirebaseStorageRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val preferencesManager = PreferencesManager(context)
    private val TAG = "ProfileViewModel"

    private val _uiState = MutableStateFlow(ProfileState())
    val uiState: StateFlow<ProfileState> = _uiState.asStateFlow()

    init {
        // Carregar dados do PreferencesManager primeiro
        loadFromPreferences()
        
        // Carregar dados reais do Firebase Auth/Firestore
        loadUserProfile()
        
        // Também observar mudanças do UserRepository (para sincronização)
        viewModelScope.launch {
            userRepository.observeCurrentUser()
                .flowOn(Dispatchers.IO)
                .collectLatest { user ->
                    user?.let { 
                        // Atualizar apenas se os dados do Firebase não estiverem disponíveis
                        if (_uiState.value.name.isBlank() && _uiState.value.email.isBlank()) {
                            setFromUser(it)
                        }
                    }
                }
        }
    }
    
    private fun loadUserProfile() {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Usuário não autenticado"
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "Carregando perfil do usuário: ${currentUser.uid}")
                
                // PRIMEIRO: Carregar do banco local (instantâneo)
                val localUser = userRepository.observeCurrentUser().first()
                if (localUser != null) {
                    Log.d(TAG, "Perfil carregado do banco local: ${localUser.name}, accountType: ${localUser.accountType}")
                    // CRÍTICO: Verificar se o avatarUri salvo pertence ao usuário atual
                    val savedAvatarUri = preferencesManager.getUserAvatarUri()
                    val profileImages = preferencesManager.getUserProfileImages()
                    
                    // Se o avatarUri salvo não pertence ao usuário atual, limpar
                    val finalLocalAvatarUri = if (savedAvatarUri != null && localUser.avatarUri != null && savedAvatarUri != localUser.avatarUri) {
                        Log.w(TAG, "AvatarUri salvo não corresponde ao usuário atual, usando do banco local")
                        localUser.avatarUri
                    } else {
                        savedAvatarUri ?: localUser.avatarUri
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        id = localUser.id,
                        name = localUser.name,
                        email = localUser.email,
                        phone = localUser.phone.orEmpty(),
                        city = localUser.city.orEmpty(),
                        profession = localUser.profession.orEmpty(),
                        accountType = localUser.accountType,
                        rating = localUser.rating,
                        avatarUri = finalLocalAvatarUri,
                        profileImages = profileImages.ifEmpty { localUser.profileImages ?: emptyList() }
                    )
                }
                
                // DEPOIS: Sincronizar com Firestore em background
                val userFirestore = firestoreUserRepository.getUser(currentUser.uid)
                val savedAvatarUri = preferencesManager.getUserAvatarUri()
                val profileImages = preferencesManager.getUserProfileImages()
                
                if (userFirestore != null) {
                    Log.d(TAG, "Usuário encontrado no Firestore: ${userFirestore.displayName}")
                    
                    // CRÍTICO: Priorizar foto do Firestore do usuário atual, não de usuários anteriores
                    // Se não houver foto no Firestore, usar a do Firebase Auth do usuário atual
                    val finalAvatarUri = userFirestore.photoURL ?: currentUser.photoUrl?.toString()
                    
                    // Se a foto do Firestore existe e é diferente da salva no PreferencesManager, atualizar
                    if (finalAvatarUri != null && finalAvatarUri != savedAvatarUri) {
                        preferencesManager.saveUserAvatarUri(finalAvatarUri)
                        Log.d(TAG, "Foto de perfil atualizada do Firestore: $finalAvatarUri")
                    } else if (finalAvatarUri == null && savedAvatarUri != null) {
                        // Se não há foto no Firestore mas há uma salva, pode ser de outro usuário - limpar
                        Log.w(TAG, "Foto salva não corresponde ao usuário atual, limpando")
                        preferencesManager.saveUserAvatarUri("")
                    }
                    
                    val accountType = when (userFirestore.role) {
                        "provider" -> AccountType.PRESTADOR
                        "seller" -> AccountType.VENDEDOR
                        else -> AccountType.CLIENTE
                    }
                    
                    // Atualizar estado com dados do Firestore (mais atualizados)
                    _uiState.value = _uiState.value.copy(
                        id = currentUser.uid,
                        name = userFirestore.displayName ?: currentUser.displayName ?: "",
                        email = userFirestore.email.ifBlank { currentUser.email ?: "" },
                        phone = userFirestore.phone ?: currentUser.phoneNumber ?: "",
                        avatarUri = finalAvatarUri,
                        profileImages = profileImages.ifEmpty { emptyList() },
                        cpf = userFirestore.cpf ?: "",
                        rg = userFirestore.rg ?: "",
                        city = userFirestore.address?.city ?: "",
                        state = userFirestore.address?.state ?: "",
                        country = userFirestore.address?.country ?: "Brasil",
                        street = userFirestore.address?.street ?: "",
                        number = userFirestore.address?.number ?: "",
                        neighborhood = userFirestore.address?.neighborhood ?: "",
                        zipCode = userFirestore.address?.zipCode ?: userFirestore.address?.cep ?: "",
                        complement = userFirestore.address?.complement ?: "",
                        createdAt = userFirestore.createdAt,
                        rating = userFirestore.rating,
                        accountType = accountType
                    )
                    
                    // Atualizar banco local com dados do Firestore
                    val user = UserProfile(
                        id = currentUser.uid,
                        name = userFirestore.displayName ?: currentUser.displayName ?: "",
                        email = userFirestore.email.ifBlank { currentUser.email ?: "" },
                        phone = userFirestore.phone ?: currentUser.phoneNumber,
                        city = userFirestore.address?.city,
                        profession = null,
                        accountType = accountType,
                        rating = userFirestore.rating,
                        avatarUri = finalAvatarUri,
                        profileImages = profileImages
                    )
                    userRepository.updateUser(user)
                } else if (localUser == null) {
                    // Se não existe no Firestore nem no banco local, usar dados do Firebase Auth
                    Log.d(TAG, "Usuário não encontrado no Firestore, usando Firebase Auth")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        id = currentUser.uid,
                        name = currentUser.displayName ?: "",
                        email = currentUser.email ?: "",
                        phone = currentUser.phoneNumber ?: "",
                        avatarUri = savedAvatarUri ?: currentUser.photoUrl?.toString(),
                        profileImages = profileImages,
                        accountType = AccountType.CLIENTE
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao carregar perfil: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erro ao carregar perfil: ${e.message}"
                )
            }
        }
    }
    
    private fun loadFromPreferences() {
        val profileImages = preferencesManager.getUserProfileImages()
        val avatarUri = preferencesManager.getUserAvatarUri()
        Log.d(TAG, "Carregando do PreferencesManager - avatarUri: $avatarUri")
        _uiState.value = _uiState.value.copy(
            profileImages = profileImages,
            avatarUri = avatarUri
        )
    }

    private fun setFromUser(user: UserProfile) {
        // Priorizar avatarUri do PreferencesManager se existir
        val savedAvatarUri = preferencesManager.getUserAvatarUri()
        Log.d(TAG, "setFromUser - savedAvatarUri: $savedAvatarUri, user.avatarUri: ${user.avatarUri}")
        
        _uiState.value = _uiState.value.copy(
            id = user.id,
            name = user.name,
            email = user.email,
            phone = user.phone.orEmpty(),
            city = user.city.orEmpty(),
            profession = user.profession.orEmpty(),
            accountType = user.accountType,
            rating = user.rating,
            avatarUri = savedAvatarUri ?: user.avatarUri,
            profileImages = user.profileImages ?: emptyList()
        )
    }

    fun onNameChange(v: String) { _uiState.value = _uiState.value.copy(name = v) }
    fun onEmailChange(v: String) { _uiState.value = _uiState.value.copy(email = v) }
    fun onPhoneChange(v: String) { _uiState.value = _uiState.value.copy(phone = v) }
    fun onCityChange(v: String) { _uiState.value = _uiState.value.copy(city = v) }
    fun onProfessionChange(v: String) { _uiState.value = _uiState.value.copy(profession = v) }
    fun onAccountTypeChange(v: AccountType) { _uiState.value = _uiState.value.copy(accountType = v) }
    fun onCpfChange(v: String) { _uiState.value = _uiState.value.copy(cpf = v) }
    fun onRgChange(v: String) { _uiState.value = _uiState.value.copy(rg = v) }
    fun onStateChange(v: String) { _uiState.value = _uiState.value.copy(state = v) }
    fun onCountryChange(v: String) { _uiState.value = _uiState.value.copy(country = v) }
    fun onStreetChange(v: String) { _uiState.value = _uiState.value.copy(street = v) }
    fun onNumberChange(v: String) { _uiState.value = _uiState.value.copy(number = v) }
    fun onNeighborhoodChange(v: String) { _uiState.value = _uiState.value.copy(neighborhood = v) }
    fun onZipCodeChange(v: String) { _uiState.value = _uiState.value.copy(zipCode = v) }
    fun onComplementChange(v: String) { _uiState.value = _uiState.value.copy(complement = v) }

    fun onAvatarSelected(uri: String) {
        Log.d(TAG, "onAvatarSelected chamado com URI: $uri")
        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            Log.e(TAG, "Usuário não autenticado ao selecionar avatar")
            return
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Se a URI é uma URL HTTP/HTTPS, já está no Storage, apenas salvar
                if (uri.startsWith("http://") || uri.startsWith("https://")) {
                    _uiState.value = _uiState.value.copy(avatarUri = uri)
                    preferencesManager.saveUserAvatarUri(uri)
                    Log.d(TAG, "Avatar já está no Storage, apenas atualizado localmente")
                } else {
                    // Fazer upload para Firebase Storage
                    val uriObj = Uri.parse(uri)
                    val uploadResult = storageRepository.uploadProfileImage(currentUser.uid, uriObj)
                    uploadResult.fold(
                        onSuccess = { downloadUrl ->
                            Log.d(TAG, "Avatar enviado para Firebase Storage: $downloadUrl")
                            _uiState.value = _uiState.value.copy(avatarUri = downloadUrl)
                            preferencesManager.saveUserAvatarUri(downloadUrl)
                            // Atualizar no Firestore imediatamente
                            val existingUser = firestoreUserRepository.getUser(currentUser.uid)
                            existingUser?.let {
                                firestoreUserRepository.updateUser(it.copy(photoURL = downloadUrl, updatedAt = Date()))
                            }
                        },
                        onFailure = { e ->
                            Log.e(TAG, "Erro ao fazer upload do avatar: ${e.message}", e)
                            // Mesmo assim, salvar localmente para uso offline
                            _uiState.value = _uiState.value.copy(avatarUri = uri)
                            preferencesManager.saveUserAvatarUri(uri)
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao processar avatar: ${e.message}", e)
                // Fallback: salvar localmente
                _uiState.value = _uiState.value.copy(avatarUri = uri)
                preferencesManager.saveUserAvatarUri(uri)
            }
        }
    }

    fun onProfileImagesChanged(images: List<String>) {
        _uiState.value = _uiState.value.copy(profileImages = images)
        viewModelScope.launch(Dispatchers.IO) {
            preferencesManager.saveUserProfileImages(images)
        }
    }

    fun refresh() {
        loadUserProfile()
    }

    fun save() {
        val s = _uiState.value
        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            _uiState.value = s.copy(error = "Usuário não autenticado")
            return
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.value = s.copy(isSaving = true, saved = false, error = null)
                
                // Salvar imagens e avatar no PreferencesManager
                preferencesManager.saveUserProfileImages(s.profileImages)
                s.avatarUri?.let { preferencesManager.saveUserAvatarUri(it) }
                
                // Criar objeto Address
                val address = if (s.street.isNotEmpty() || s.city.isNotEmpty()) {
                    Address(
                        street = s.street,
                        number = s.number,
                        complement = s.complement.ifBlank { null },
                        neighborhood = s.neighborhood,
                        city = s.city,
                        state = s.state,
                        country = s.country,
                        zipCode = s.zipCode,
                        cep = s.zipCode
                    )
                } else null
                
                // Buscar usuário atual do Firestore ou criar novo
                val existingUser = firestoreUserRepository.getUser(currentUser.uid)
                val role = when (s.accountType) {
                    AccountType.PRESTADOR -> "provider"
                    AccountType.VENDEDOR -> "seller"
                    AccountType.CLIENTE -> "client"
                }
                
                val userFirestore = existingUser?.copy(
                    displayName = s.name,
                    email = s.email,
                    phone = s.phone.ifBlank { null },
                    cpf = s.cpf.ifBlank { null },
                    rg = s.rg.ifBlank { null },
                    address = address,
                    photoURL = s.avatarUri,
                    role = role,
                    updatedAt = Date()
                ) ?: UserFirestore(
                    uid = currentUser.uid,
                    email = s.email,
                    displayName = s.name,
                    phone = s.phone.ifBlank { null },
                    cpf = s.cpf.ifBlank { null },
                    rg = s.rg.ifBlank { null },
                    address = address,
                    photoURL = s.avatarUri,
                    role = role,
                    profileComplete = true,
                    verified = currentUser.isEmailVerified,
                    createdAt = Date(),
                    updatedAt = Date()
                )
                
                // Criar UserProfile para salvar localmente PRIMEIRO (instantâneo)
                val user = UserProfile(
                    id = currentUser.uid,
                    name = s.name,
                    email = s.email,
                    phone = s.phone.ifBlank { null },
                    city = s.city.ifBlank { null },
                    profession = s.profession.ifBlank { null },
                    accountType = s.accountType,
                    rating = s.rating,
                    avatarUri = s.avatarUri,
                    profileImages = s.profileImages
                )
                
                // Salvar no banco local PRIMEIRO para atualização instantânea
                userRepository.updateUser(user)
                
                // Depois salvar no Firestore
                firestoreUserRepository.updateUser(userFirestore)
                
                _uiState.value = _uiState.value.copy(isSaving = false, saved = true)
                Log.d(TAG, "Perfil salvo com sucesso no Firestore e banco local")
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao salvar perfil: ${e.message}", e)
                _uiState.value = _uiState.value.copy(isSaving = false, error = e.message ?: "Erro ao salvar perfil")
            }
        }
    }
}


