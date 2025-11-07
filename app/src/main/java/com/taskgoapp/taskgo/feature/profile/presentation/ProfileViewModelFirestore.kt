package com.taskgoapp.taskgo.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskgoapp.taskgo.data.repository.FirebaseAuthRepository
import com.taskgoapp.taskgo.data.repository.FirestoreUserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = false,
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val rating: Double? = null,
    val servicesCount: Int = 0,
    val role: String = "client",
    val error: String? = null
)

@HiltViewModel
class ProfileViewModelFirestore @Inject constructor(
    private val authRepository: FirebaseAuthRepository,
    private val firestoreUserRepository: FirestoreUserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            _uiState.value = ProfileUiState(error = "Usuário não autenticado")
            return
        }

        _uiState.value = ProfileUiState(isLoading = true)
        
        viewModelScope.launch {
            try {
                val userFirestore = firestoreUserRepository.getUser(currentUser.uid)
                
                if (userFirestore != null) {
                    _uiState.value = ProfileUiState(
                        isLoading = false,
                        name = userFirestore.displayName ?: currentUser.displayName ?: "",
                        email = userFirestore.email,
                        phone = "", // Pode ser adicionado ao modelo UserFirestore se necessário
                        rating = null, // Pode ser calculado a partir de avaliações se necessário
                        servicesCount = 0, // Pode ser contado a partir de serviços do usuário
                        role = userFirestore.role
                    )
                } else {
                    // Se não existe no Firestore, usar dados do Firebase Auth
                    _uiState.value = ProfileUiState(
                        isLoading = false,
                        name = currentUser.displayName ?: "",
                        email = currentUser.email ?: "",
                        phone = currentUser.phoneNumber ?: "",
                        role = "client"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = ProfileUiState(
                    isLoading = false,
                    error = "Erro ao carregar perfil: ${e.message}"
                )
            }
        }
    }

    fun refresh() {
        loadUserProfile()
    }
}

