package com.taskgoapp.taskgo.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.taskgoapp.taskgo.core.data.PreferencesManager
import com.taskgoapp.taskgo.core.model.AccountType
import com.taskgoapp.taskgo.core.model.UserProfile
import com.taskgoapp.taskgo.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
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
    val error: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val preferencesManager = PreferencesManager(context)

    private val _uiState = MutableStateFlow(ProfileState())
    val uiState: StateFlow<ProfileState> = _uiState.asStateFlow()

    init {
        // Carregar dados do PreferencesManager primeiro
        loadFromPreferences()
        
        viewModelScope.launch {
            userRepository.observeCurrentUser()
                .flowOn(Dispatchers.IO) // Mover operação de I/O para background
                .collectLatest { user ->
                    user?.let { setFromUser(it) }
                }
        }
    }
    
    private fun loadFromPreferences() {
        val profileImages = preferencesManager.getUserProfileImages()
        val avatarUri = preferencesManager.getUserAvatarUri()
        println("DEBUG: Carregando do PreferencesManager - avatarUri: $avatarUri")
        _uiState.value = _uiState.value.copy(
            profileImages = profileImages,
            avatarUri = avatarUri
        )
    }

    private fun setFromUser(user: UserProfile) {
        // Priorizar avatarUri do PreferencesManager se existir
        val savedAvatarUri = preferencesManager.getUserAvatarUri()
        println("DEBUG: setFromUser - savedAvatarUri: $savedAvatarUri, user.avatarUri: ${user.avatarUri}")
        
        _uiState.value = _uiState.value.copy(
            id = user.id,
            name = user.name,
            email = user.email,
            phone = user.phone.orEmpty(),
            city = user.city.orEmpty(),
            profession = user.profession.orEmpty(),
            accountType = user.accountType,
            rating = user.rating,
            avatarUri = savedAvatarUri ?: user.avatarUri, // Priorizar o salvo
            profileImages = user.profileImages ?: emptyList()
        )
    }

    fun onNameChange(v: String) { _uiState.value = _uiState.value.copy(name = v) }
    fun onEmailChange(v: String) { _uiState.value = _uiState.value.copy(email = v) }
    fun onPhoneChange(v: String) { _uiState.value = _uiState.value.copy(phone = v) }
    fun onCityChange(v: String) { _uiState.value = _uiState.value.copy(city = v) }
    fun onProfessionChange(v: String) { _uiState.value = _uiState.value.copy(profession = v) }
    fun onAccountTypeChange(v: AccountType) { _uiState.value = _uiState.value.copy(accountType = v) }

    fun onAvatarSelected(uri: String) {
        println("DEBUG: onAvatarSelected chamado com URI: $uri")
        _uiState.value = _uiState.value.copy(avatarUri = uri)
        viewModelScope.launch(Dispatchers.IO) {
            preferencesManager.saveUserAvatarUri(uri)
            println("DEBUG: Avatar salvo no PreferencesManager")
        }
    }

    fun onProfileImagesChanged(images: List<String>) {
        _uiState.value = _uiState.value.copy(profileImages = images)
        viewModelScope.launch(Dispatchers.IO) {
            preferencesManager.saveUserProfileImages(images)
        }
    }

    fun save() {
        val s = _uiState.value
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.value = s.copy(isSaving = true, saved = false, error = null)
                
                // Salvar imagens e avatar no PreferencesManager
                preferencesManager.saveUserProfileImages(s.profileImages)
                s.avatarUri?.let { preferencesManager.saveUserAvatarUri(it) }
                
                val user = UserProfile(
                    id = if (s.id.isBlank()) "user_1" else s.id,
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
                userRepository.updateUser(user)
                _uiState.value = _uiState.value.copy(isSaving = false, saved = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false, error = e.message)
            }
        }
    }
}


