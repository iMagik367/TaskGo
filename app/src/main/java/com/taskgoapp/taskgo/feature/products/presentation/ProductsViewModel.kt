package com.taskgoapp.taskgo.feature.products.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskgoapp.taskgo.core.model.Product
import com.taskgoapp.taskgo.core.model.AccountType
import com.taskgoapp.taskgo.core.design.FilterState
import com.taskgoapp.taskgo.domain.repository.ProductsRepository
import com.taskgoapp.taskgo.domain.repository.CategoriesRepository
import com.taskgoapp.taskgo.domain.repository.UserRepository
import com.taskgoapp.taskgo.data.local.datastore.FilterPreferencesManager
import com.taskgoapp.taskgo.core.location.LocationManager
import com.taskgoapp.taskgo.core.location.calculateDistance
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductsViewModel @Inject constructor(
    private val productsRepository: ProductsRepository,
    private val categoriesRepository: CategoriesRepository,
    private val filterPreferencesManager: FilterPreferencesManager,
    private val locationManager: LocationManager,
    private val userRepository: UserRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()

    private val _accountType = MutableStateFlow(AccountType.CLIENTE)
    val accountType: StateFlow<AccountType> = _accountType.asStateFlow()
    
    private val _userLocation = MutableStateFlow<android.location.Location?>(null)
    val userLocation: StateFlow<android.location.Location?> = _userLocation.asStateFlow()

    val productCategories: StateFlow<List<String>> = categoriesRepository
        .observeProductCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allProducts: StateFlow<List<Product>> = productsRepository
        .observeProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val products: StateFlow<List<Product>> = combine(
        allProducts,
        filterState,
        _accountType,
        _userLocation
    ) { products, filters, accountType, userLocation ->
        val currentUserId = firebaseAuth.currentUser?.uid ?: ""
        // Filtrar produtos baseado no tipo de conta
        // Na tela principal de produtos (ProductsScreen), todos devem ver produtos de outros (para comprar)
        // Apenas na tela de gerenciamento (ManageProductsScreen) deve mostrar apenas produtos próprios
        val filteredByAccountType = when (accountType) {
            AccountType.PARCEIRO, AccountType.VENDEDOR, AccountType.PRESTADOR -> {
                // PARCEIRO/VENDEDOR: mostrar produtos de outros (para comprar na loja principal)
                products.filter { it.sellerId != currentUserId && it.sellerId != null && it.sellerId.isNotBlank() }
            }
            AccountType.CLIENTE -> {
                // CLIENTE: mostrar apenas produtos de outros usuários (para comprar)
                products.filter { it.sellerId != currentUserId && it.sellerId != null && it.sellerId.isNotBlank() }
            }
        }
        applyFiltersSync(filteredByAccountType, filters, userLocation)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        loadSavedFilters()
        loadAccountType()
        loadUserLocation()
    }
    
    private fun loadUserLocation() {
        viewModelScope.launch {
            try {
                val location = locationManager.getCurrentLocation()
                _userLocation.value = location
            } catch (e: Exception) {
                android.util.Log.e("ProductsViewModel", "Erro ao obter localização: ${e.message}", e)
            }
        }
    }

    private fun loadAccountType() {
        viewModelScope.launch {
            userRepository.observeCurrentUser().collect { user ->
                _accountType.value = user?.accountType ?: AccountType.CLIENTE
            }
        }
    }

    private fun loadSavedFilters() {
        viewModelScope.launch {
            filterPreferencesManager.getProductFilters().collect { savedFilters ->
                savedFilters?.let {
                    _filterState.value = it
                }
            }
        }
    }

    fun updateFilterState(newState: FilterState) {
        _filterState.value = newState
        viewModelScope.launch {
            filterPreferencesManager.saveProductFilters(newState)
        }
    }

    fun toggleCategory(category: String) {
        val currentCategories = _filterState.value.selectedCategories
        val newCategories = if (currentCategories.contains(category)) {
            currentCategories - category
        } else {
            currentCategories + category
        }
        updateFilterState(_filterState.value.copy(selectedCategories = newCategories))
    }

    fun updateSearchQuery(query: String) {
        updateFilterState(_filterState.value.copy(searchQuery = query))
    }

    private fun applyFiltersSync(
        products: List<Product>, 
        filters: FilterState,
        userLocation: android.location.Location?
    ): List<Product> {
        var filtered = products.filter { it.active }

        // Busca por texto
        if (filters.searchQuery.isNotBlank()) {
            val query = filters.searchQuery.lowercase()
            filtered = filtered.filter { product ->
                product.title.lowercase().contains(query) ||
                product.description?.lowercase()?.contains(query) == true
            }
        }

        // Filtrar por categorias (quando Product tiver campo de categoria)
        // TODO: Implementar quando Product tiver campo de categoria

        // Filtrar por preço
        filters.priceRange?.let { range ->
            filtered = filtered.filter { product ->
                val price = product.price
                (range.min == null || price >= range.min) &&
                (range.max == null || price <= range.max)
            }
        }

        // Filtrar por avaliação
        filters.minRating?.let { minRating ->
            filtered = filtered.filter { product ->
                product.rating != null && product.rating >= minRating
            }
        }

        // Filtrar por localização quando houver localização do usuário.
        // Sem localização, mantemos a lista filtrada (não esvaziamos a vitrine).
        if (userLocation != null) {
            filtered = filtered.filter { product ->
                val prodLat = product.latitude
                val prodLng = product.longitude
                if (prodLat != null && prodLng != null) {
                    val distance = calculateDistance(
                        userLocation.latitude,
                        userLocation.longitude,
                        prodLat,
                        prodLng
                    )
                    distance <= 100.0 // Raio de 100km
                } else {
                    // Produto sem localização não é exibido quando o usuário compartilha localização
                    false
                }
            }
        }

        // Ordenar
        filtered = when (filters.sortBy) {
            com.taskgoapp.taskgo.core.design.SortOption.PRICE_LOW_TO_HIGH -> 
                filtered.sortedBy { it.price }
            com.taskgoapp.taskgo.core.design.SortOption.PRICE_HIGH_TO_LOW -> 
                filtered.sortedByDescending { it.price }
            com.taskgoapp.taskgo.core.design.SortOption.RATING -> 
                filtered.sortedByDescending { it.rating ?: 0.0 }
            com.taskgoapp.taskgo.core.design.SortOption.NEWEST -> 
                filtered // TODO: Ordenar por data quando disponível
            else -> filtered
        }

        return filtered
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            // Cache local desativado: remover diretamente no backend
            productsRepository.deleteProduct(productId)
        }
    }
}


