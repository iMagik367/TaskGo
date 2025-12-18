package com.taskgoapp.taskgo.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskgoapp.taskgo.data.firestore.models.UserFirestore
import com.taskgoapp.taskgo.data.firestore.models.ServiceFirestore
import com.taskgoapp.taskgo.data.firestore.models.ReviewFirestore
import com.taskgoapp.taskgo.data.repository.FirestoreUserRepository
import com.taskgoapp.taskgo.data.repository.FirestoreServicesRepository
import com.taskgoapp.taskgo.data.repository.FirestoreReviewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProviderProfileUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val profile: UserFirestore? = null
)

@HiltViewModel
class ProviderProfileViewModel @Inject constructor(
    private val userRepository: FirestoreUserRepository,
    private val servicesRepository: FirestoreServicesRepository,
    private val reviewsRepository: FirestoreReviewsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ProviderProfileUiState())
    val uiState: StateFlow<ProviderProfileUiState> = _uiState.asStateFlow()
    
    private val _services = MutableStateFlow<List<ServiceFirestore>>(emptyList())
    val services: StateFlow<List<ServiceFirestore>> = _services.asStateFlow()
    
    private val _reviews = MutableStateFlow<List<ReviewFirestore>>(emptyList())
    val reviews: StateFlow<List<ReviewFirestore>> = _reviews.asStateFlow()
    
    fun loadProfile(providerId: String, isStore: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Carregar perfil do usuário
                val profile = userRepository.getUser(providerId)
                _uiState.value = _uiState.value.copy(profile = profile, isLoading = false)
                
                // Observar serviços do prestador
                if (!isStore) {
                    servicesRepository.observeProviderServices(providerId)
                        .collect { servicesList ->
                            _services.value = servicesList
                        }
                }
                
                // Observar avaliações
                reviewsRepository.observeProviderReviews(providerId)
                    .collect { reviewsList ->
                        _reviews.value = reviewsList
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Erro ao carregar perfil"
                )
            }
        }
    }
}

