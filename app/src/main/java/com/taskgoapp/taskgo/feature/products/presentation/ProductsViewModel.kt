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
// ✅ REMOVIDO: LocationManager e calculateDistance - não são mais usados
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
    // ✅ REMOVIDO: locationManager - não é mais usado
    private val userRepository: UserRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()

    private val _accountType = MutableStateFlow(AccountType.CLIENTE)
    val accountType: StateFlow<AccountType> = _accountType.asStateFlow()
    
    // ✅ REMOVIDO: _userLocation - produtos já vêm filtrados por city/state do Firestore
    // LEI MÁXIMA DO TASKGO: NUNCA usar GPS para filtrar produtos - todos os produtos do mesmo city/state devem aparecer

    val productCategories: StateFlow<List<String>> = categoriesRepository
        .observeProductCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allProducts: StateFlow<List<Product>> = productsRepository
        .observeProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val products: StateFlow<List<Product>> = combine(
        allProducts,
        filterState,
        _accountType
    ) { products, filters, accountType ->
        val currentUserId = firebaseAuth.currentUser?.uid ?: ""
        
        // REGRA DE NEGÓCIO:
        // - PARCEIRO na loja: vê seus próprios produtos + produtos de outros parceiros do mesmo city_state
        // - CLIENTE na loja: vê apenas produtos de parceiros do mesmo city_state
        // - Meus Produtos (GerenciarProdutosViewModel): apenas produtos próprios (já implementado)
        val filteredByAccountType = when (accountType) {
            AccountType.PARCEIRO -> {
                // Parceiro vê todos os produtos (próprios + de outros parceiros)
                products.filter { 
                    it.sellerId != null && it.sellerId.isNotBlank() 
                }
            }
            AccountType.CLIENTE -> {
                // Cliente vê apenas produtos de parceiros (não próprios, pois cliente não vende)
                products.filter { 
                    it.sellerId != currentUserId && it.sellerId != null && it.sellerId.isNotBlank() 
                }
            }
        }
        applyFiltersSync(filteredByAccountType, filters)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        loadSavedFilters()
        loadAccountType()
        // ✅ REMOVIDO: loadUserLocation() - produtos já vêm filtrados por city/state do Firestore
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
        filters: FilterState
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

        // ✅ REMOVIDO: Filtro de distância GPS
        // LEI MÁXIMA DO TASKGO: Produtos já vêm filtrados por city/state do Firestore
        // NUNCA usar GPS para filtrar produtos - todos os produtos do mesmo city/state devem aparecer

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


