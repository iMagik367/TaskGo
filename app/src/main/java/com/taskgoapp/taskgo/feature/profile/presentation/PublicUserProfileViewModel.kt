package com.taskgoapp.taskgo.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskgoapp.taskgo.core.model.AccountType
import com.taskgoapp.taskgo.data.firestore.models.ServiceFirestore
import com.taskgoapp.taskgo.data.firestore.models.UserFirestore
import com.taskgoapp.taskgo.data.repository.FirestoreUserRepository
import com.taskgoapp.taskgo.data.repository.FirestoreServicesRepository
import com.taskgoapp.taskgo.data.repository.FirestoreProductsRepositoryImpl
import com.taskgoapp.taskgo.data.repository.FirestoreReviewsRepository
import com.taskgoapp.taskgo.data.firestore.models.ProductFirestore
import com.taskgoapp.taskgo.data.mapper.ProductMapper.toFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PublicUserProfileUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val userProfile: UserFirestore? = null,
    val accountType: AccountType? = null
)

@HiltViewModel
class PublicUserProfileViewModel @Inject constructor(
    private val userRepository: FirestoreUserRepository,
    private val servicesRepository: FirestoreServicesRepository,
    private val firestoreProductsRepository: FirestoreProductsRepositoryImpl,
    private val reviewsRepository: FirestoreReviewsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PublicUserProfileUiState())
    val uiState: StateFlow<PublicUserProfileUiState> = _uiState.asStateFlow()
    
    // Serviços (para PARCEIRO)
    private val _services = MutableStateFlow<List<ServiceFirestore>>(emptyList())
    val services: StateFlow<List<ServiceFirestore>> = _services.asStateFlow()
    
    // Produtos (para PARCEIRO)
    private val _products = MutableStateFlow<List<ProductFirestore>>(emptyList())
    val products: StateFlow<List<ProductFirestore>> = _products.asStateFlow()
    
    // Avaliações
    private val _reviews = MutableStateFlow<List<com.taskgoapp.taskgo.data.firestore.models.ReviewFirestore>>(emptyList())
    val reviews: StateFlow<List<com.taskgoapp.taskgo.data.firestore.models.ReviewFirestore>> = _reviews.asStateFlow()
    
    fun loadProfile(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Carregar perfil do usuário - aguardar resultado
                val profile = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    userRepository.getUser(userId)
                }
                
                if (profile == null) {
                    android.util.Log.w("PublicUserProfileVM", "Usuário não encontrado no Firestore: $userId")
                    // Tentar novamente após um delay (pode ser que a Cloud Function ainda não tenha criado)
                    kotlinx.coroutines.delay(1000)
                    val retryProfile = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        userRepository.getUser(userId)
                    }
                    
                    if (retryProfile == null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Usuário não encontrado"
                        )
                        return@launch
                    }
                    
                    // Usar o perfil encontrado na segunda tentativa
                    val accountType = if (retryProfile.role.lowercase() == "partner") {
                        AccountType.PARCEIRO
                    } else {
                        AccountType.CLIENTE
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        userProfile = retryProfile,
                        accountType = accountType,
                        isLoading = false
                    )
                    
                    // Carregar dados específicos
                    loadUserSpecificData(userId, accountType)
                    return@launch
                }
                
                // Determinar tipo de conta
                val accountType = if (profile.role.lowercase() == "partner") {
                    AccountType.PARCEIRO
                } else {
                    AccountType.CLIENTE
                }
                
                _uiState.value = _uiState.value.copy(
                    userProfile = profile,
                    accountType = accountType,
                    isLoading = false
                )
                
                // Carregar dados específicos
                loadUserSpecificData(userId, accountType)
                    
            } catch (e: Exception) {
                android.util.Log.e("PublicUserProfileVM", "Erro ao carregar perfil: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Erro ao carregar perfil"
                )
            }
        }
    }
    
    private fun loadUserSpecificData(userId: String, accountType: AccountType) {
        // Carregar dados específicos baseado no tipo de conta em corrotinas separadas
        when (accountType) {
            AccountType.PARCEIRO -> {
                // PARCEIRO: Observar tanto serviços quanto produtos (unificado)
                // Observar serviços do parceiro
                viewModelScope.launch {
                    servicesRepository.observeProviderServices(userId)
                        .catch { e ->
                            android.util.Log.e("PublicUserProfileVM", "Erro ao observar serviços: ${e.message}", e)
                            emit(emptyList())
                        }
                        .collect { servicesList ->
                            _services.value = servicesList
                        }
                }
                // Observar produtos do parceiro
                viewModelScope.launch {
                    firestoreProductsRepository.observeProductsBySeller(userId)
                        .catch { e ->
                            android.util.Log.e("PublicUserProfileVM", "Erro ao observar produtos: ${e.message}", e)
                            emit(emptyList())
                        }
                        .collect { productsList ->
                            _products.value = productsList.map { it.toFirestore() }
                        }
                }
            }
            else -> {
                // CLIENTE - não precisa carregar serviços ou produtos
            }
        }
        
        // Observar avaliações (para todos os tipos)
        viewModelScope.launch {
            reviewsRepository.observeProviderReviews(userId)
                .catch { e ->
                    android.util.Log.e("PublicUserProfileVM", "Erro ao observar avaliações: ${e.message}", e)
                    emit(emptyList())
                }
                .collect { reviewsList ->
                    _reviews.value = reviewsList
                }
        }
    }
}





