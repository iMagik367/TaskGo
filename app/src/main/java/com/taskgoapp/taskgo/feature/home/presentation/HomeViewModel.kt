package com.taskgoapp.taskgo.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskgoapp.taskgo.core.model.Product
import com.taskgoapp.taskgo.core.data.models.ServiceCategory
import com.taskgoapp.taskgo.core.maps.ProviderLocation
import com.taskgoapp.taskgo.core.maps.StoreLocation
import com.taskgoapp.taskgo.domain.repository.ProductsRepository
import com.taskgoapp.taskgo.data.repository.FirestoreMapLocationsRepository
import com.taskgoapp.taskgo.data.repository.FirestoreServicesRepository
import com.taskgoapp.taskgo.data.firestore.models.ServiceFirestore
import com.taskgoapp.taskgo.domain.repository.UserRepository
import com.taskgoapp.taskgo.domain.repository.HomeBannersRepository
import com.taskgoapp.taskgo.core.model.HomeBanner
import com.taskgoapp.taskgo.core.model.AccountType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val products: List<Product> = emptyList(),
    val categories: List<ServiceCategory> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val productsRepository: ProductsRepository,
    private val mapLocationsRepository: FirestoreMapLocationsRepository,
    private val servicesRepository: FirestoreServicesRepository,
    private val userRepository: UserRepository,
    private val categoriesRepository: com.taskgoapp.taskgo.domain.repository.CategoriesRepository,
    private val homeBannersRepository: HomeBannersRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val products: StateFlow<List<Product>> = productsRepository
        .observeProducts()
        .onEach { products ->
            // ✅ CRÍTICO: Logar quando produtos são recebidos no ViewModel
            android.util.Log.d("HomeViewModel", "🔵 PRODUTOS RECEBIDOS no ViewModel: ${products.size} produtos")
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    // Carregar categorias de serviços dinamicamente do Firestore
    val categories: StateFlow<List<ServiceCategory>> = categoriesRepository
        .observeServiceCategories()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )
    
    // Carregar categorias de produtos dinamicamente do Firestore
    val productCategories: StateFlow<List<String>> = categoriesRepository
        .observeProductCategories()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )
    
    // Localizações de prestadores e lojas para o mapa
    val providers: StateFlow<List<ProviderLocation>> = mapLocationsRepository
        .observeProvidersWithLocation()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )
    
    val stores: StateFlow<List<StoreLocation>> = mapLocationsRepository
        .observeStoresWithLocation()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )
    
    // Serviços oferecidos (ativos)
    val services: StateFlow<List<ServiceFirestore>> = servicesRepository
        .observeAllActiveServices()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )
    
    // Account type do usuário atual
    val accountType: StateFlow<AccountType> = userRepository
        .observeCurrentUser()
        .map { user -> user?.accountType ?: AccountType.CLIENTE }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            AccountType.CLIENTE
        )

    val homeBanners: StateFlow<List<HomeBanner>> = homeBannersRepository
        .observeHomeBanners()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                // Os produtos vêm automaticamente via Flow do repositório
                // TODO: Carregar categorias quando houver repositório
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Erro ao carregar dados"
                )
            }
        }
    }

    fun refresh() {
        loadData()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun observeProductErrors() {
        viewModelScope.launch {
            productsRepository.observeProductErrors()
                .catch { throwable ->
                    val message = throwable.message ?: "Erro desconhecido ao observar produtos"
                    _uiState.value = _uiState.value.copy(error = message)
                }
                .collect { message ->
                    _uiState.value = _uiState.value.copy(error = message)
                }
        }
    }
}

