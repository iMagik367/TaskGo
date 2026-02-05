package com.taskgoapp.taskgo.feature.search.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskgoapp.taskgo.core.design.FilterState
import com.taskgoapp.taskgo.core.model.Product
import com.taskgoapp.taskgo.core.model.ServiceOrder
import com.taskgoapp.taskgo.domain.repository.ProductsRepository
import com.taskgoapp.taskgo.domain.repository.ServiceRepository
import com.taskgoapp.taskgo.domain.repository.CategoriesRepository
import com.taskgoapp.taskgo.data.local.datastore.FilterPreferencesManager
// ✅ REMOVIDO: LocationManager e calculateDistance - não são mais usados
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UniversalSearchUiState(
    val isLoading: Boolean = false,
    val products: List<Product> = emptyList(),
    val services: List<ServiceOrder> = emptyList(),
    val categories: List<String> = emptyList()
)

@HiltViewModel
class UniversalSearchViewModel @Inject constructor(
    private val productsRepository: ProductsRepository,
    private val serviceRepository: ServiceRepository,
    private val categoriesRepository: CategoriesRepository,
    private val filterPreferencesManager: FilterPreferencesManager,
    // ✅ REMOVIDO: locationManager - não é mais usado
) : ViewModel() {

    private val _uiState = MutableStateFlow(UniversalSearchUiState())
    val uiState: StateFlow<UniversalSearchUiState> = _uiState.asStateFlow()

    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()

    val allProducts = productsRepository
        .observeProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allServices = serviceRepository
        .observeServiceOrders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val productCategories = categoriesRepository
        .observeProductCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val serviceCategories = categoriesRepository
        .observeServiceCategories()
        .map { categories -> categories.map { it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val filteredProducts = combine(
        allProducts,
        filterState
    ) { products, filters ->
        applyFiltersToProductsSync(products, filters)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val filteredServices = combine(
        allServices,
        filterState
    ) { services, filters ->
        applyFiltersToServicesSync(services, filters)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            combine(
                filteredProducts,
                filteredServices,
                productCategories,
                serviceCategories
            ) { products, services, prodCats, servCats ->
                UniversalSearchUiState(
                    isLoading = false,
                    products = products,
                    services = services,
                    categories = (prodCats + servCats).distinct()
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun updateFilterState(newState: FilterState) {
        _filterState.value = newState
        viewModelScope.launch {
            filterPreferencesManager.saveProductFilters(newState)
            filterPreferencesManager.saveServiceFilters(newState)
        }
    }

    fun updateSearchQuery(query: String) {
        updateFilterState(_filterState.value.copy(searchQuery = query))
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

    private fun applyFiltersToProductsSync(
        products: List<Product>,
        filters: FilterState
    ): List<Product> {
        var filtered = products

        // Busca por texto
        if (filters.searchQuery.isNotBlank()) {
            val query = filters.searchQuery.lowercase()
            filtered = filtered.filter { product ->
                product.title.lowercase().contains(query) ||
                product.description?.lowercase()?.contains(query) == true ||
                product.sellerName?.lowercase()?.contains(query) == true
            }
        }

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

        // ✅ REMOVIDO: Filtro de raio GPS
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

    private fun applyFiltersToServicesSync(
        services: List<ServiceOrder>,
        filters: FilterState
    ): List<ServiceOrder> {
        var filtered = services

        // Busca por texto
        if (filters.searchQuery.isNotBlank()) {
            val query = filters.searchQuery.lowercase()
            filtered = filtered.filter { service ->
                service.category.lowercase().contains(query) ||
                service.description.lowercase().contains(query) ||
                service.city.lowercase().contains(query) ||
                service.state.lowercase().contains(query)
            }
        }

        // Filtrar por categorias
        if (filters.selectedCategories.isNotEmpty()) {
            filtered = filtered.filter { service ->
                filters.selectedCategories.contains(service.category)
            }
        }

        // REMOVIDO: Filtro por localização manual - localização agora é automática do perfil do usuário

        // Filtrar por avaliação
        filters.minRating?.let { minRating ->
            filtered = filtered.filter { service ->
                service.rating != null && service.rating >= minRating
            }
        }

        // Ordenar - serviços destacados primeiro
        filtered = when (filters.sortBy) {
            com.taskgoapp.taskgo.core.design.SortOption.NEWEST -> 
                filtered.sortedWith(compareByDescending<ServiceOrder> { it.featured == true }
                    .thenByDescending { it.date })
            com.taskgoapp.taskgo.core.design.SortOption.RATING -> 
                filtered.sortedWith(compareByDescending<ServiceOrder> { it.featured == true }
                    .thenByDescending { it.rating ?: 0.0 })
            com.taskgoapp.taskgo.core.design.SortOption.RELEVANCE -> 
                filtered.sortedByDescending { it.featured == true }
            else -> filtered.sortedByDescending { it.featured == true }
        }

        return filtered
    }
}

