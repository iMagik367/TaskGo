package com.taskgoapp.taskgo.feature.products.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskgoapp.taskgo.core.model.Product
import com.taskgoapp.taskgo.core.design.FilterState
import com.taskgoapp.taskgo.domain.repository.ProductsRepository
import com.taskgoapp.taskgo.domain.repository.CategoriesRepository
import com.taskgoapp.taskgo.data.local.datastore.FilterPreferencesManager
import com.taskgoapp.taskgo.core.location.LocationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductsViewModel @Inject constructor(
    private val productsRepository: ProductsRepository,
    private val categoriesRepository: CategoriesRepository,
    private val filterPreferencesManager: FilterPreferencesManager,
    private val locationManager: LocationManager
) : ViewModel() {

    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()

    val productCategories: StateFlow<List<String>> = categoriesRepository
        .observeProductCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allProducts: StateFlow<List<Product>> = productsRepository
        .observeProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val products: StateFlow<List<Product>> = combine(
        allProducts,
        filterState
    ) { products, filters ->
        applyFiltersSync(products, filters)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        loadSavedFilters()
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

    private fun applyFiltersSync(products: List<Product>, filters: FilterState): List<Product> {
        var filtered = products

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

        // Filtrar por localização
        filters.location?.let { location ->
            if (location.city != null && location.city.isNotEmpty()) {
                // Filtrar por cidade se o produto tiver informação de cidade
                // Por enquanto, produtos não têm campo de cidade, então pulamos
                // TODO: Adicionar campo de cidade ao Product quando disponível
            }
            if (location.state != null && location.state.isNotEmpty()) {
                // Filtrar por estado se o produto tiver informação de estado
                // Por enquanto, produtos não têm campo de estado, então pulamos
                // TODO: Adicionar campo de estado ao Product quando disponível
            }
            
            // Filtrar por raio usando GPS
            if (location.radiusKm != null && location.radiusKm > 0) {
                // Este filtro será aplicado assincronamente quando a localização do usuário estiver disponível
                // Por enquanto, apenas marca que precisa filtrar por raio
                // TODO: Implementar filtro assíncrono quando necessário usando LaunchedEffect
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
            val product = productsRepository.getProduct(productId)
            if (product != null) {
                productsRepository.deleteProduct(productId)
            }
        }
    }
}


