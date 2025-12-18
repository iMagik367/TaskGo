package com.taskgoapp.taskgo.feature.services.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskgoapp.taskgo.core.design.FilterState
import com.taskgoapp.taskgo.core.location.LocationManager
import com.taskgoapp.taskgo.core.model.AccountType
import com.taskgoapp.taskgo.core.model.ServiceOrder
import com.taskgoapp.taskgo.data.firestore.models.ServiceFirestore
import com.taskgoapp.taskgo.data.local.datastore.FilterPreferencesManager
import com.taskgoapp.taskgo.data.repository.FirestoreServicesRepository
import com.taskgoapp.taskgo.data.repository.FirestoreOrderRepository
import com.taskgoapp.taskgo.data.repository.FirestoreProvidersRepository
import com.taskgoapp.taskgo.data.firestore.models.OrderFirestore
import com.taskgoapp.taskgo.domain.repository.CategoriesRepository
import com.taskgoapp.taskgo.domain.repository.ServiceRepository
import com.taskgoapp.taskgo.domain.repository.UserRepository
import com.taskgoapp.taskgo.domain.repository.PreferencesRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ServicesUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val serviceOrders: List<ServiceOrder> = emptyList()
)

@HiltViewModel
class ServicesViewModel @Inject constructor(
    private val serviceRepository: ServiceRepository,
    private val categoriesRepository: CategoriesRepository,
    private val filterPreferencesManager: FilterPreferencesManager,
    private val locationManager: LocationManager,
    private val userRepository: UserRepository,
    private val firestoreServicesRepository: FirestoreServicesRepository,
    private val orderRepository: FirestoreOrderRepository,
    private val providersRepository: FirestoreProvidersRepository,
    private val firebaseAuth: FirebaseAuth,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServicesUiState())
    val uiState: StateFlow<ServicesUiState> = _uiState.asStateFlow()

    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()

    private val _accountType = MutableStateFlow(AccountType.CLIENTE)
    val accountType: StateFlow<AccountType> = _accountType.asStateFlow()

    val serviceCategories: StateFlow<List<String>> = categoriesRepository
        .observeServiceCategories()
        .map { categories -> categories.map { it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    
    private val preferredCategories: StateFlow<List<String>> = preferencesRepository
        .observeCategories()
        .map { parsePreferredCategories(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val serviceCategoriesFull: StateFlow<List<com.taskgoapp.taskgo.core.data.models.ServiceCategory>> = combine(
        categoriesRepository.observeServiceCategories(),
        preferredCategories
    ) { categories, preferences ->
        if (preferences.isEmpty()) {
            categories
        } else {
            categories.sortedBy { category ->
                val idx = preferences.indexOfFirst { it.equals(category.name, ignoreCase = true) }
                if (idx == -1) preferences.size + categories.indexOf(category) else idx
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Para prestadores, observar todas as ordens pendentes
    private val _selectedCategoryForOrders = MutableStateFlow<String?>(null)
    
    val allOrdersFirestore: StateFlow<List<OrderFirestore>> = combine(
        _accountType,
        _selectedCategoryForOrders
    ) { accountType, category ->
        if (accountType == AccountType.PRESTADOR) {
            if (category != null && category.isNotBlank()) {
                orderRepository.observeOrdersByCategory(category)
            } else {
                // Buscar todas as ordens pendentes (sem filtro de categoria)
                orderRepository.observeLocalServiceOrders(category = null)
            }
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList<OrderFirestore>())
        }
    }.flatMapLatest { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    
    val allServiceOrders: StateFlow<List<ServiceOrder>> = serviceRepository
        .observeServiceOrders()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    val serviceOrders: StateFlow<List<ServiceOrder>> = combine(
        allServiceOrders,
        filterState,
        preferredCategories
    ) { orders, filters, preferences ->
        applyFiltersSync(orders, filters, preferences)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )
    
    // Ordens Firestore para prestadores (com category)
    val serviceOrdersFirestore: StateFlow<List<com.taskgoapp.taskgo.data.firestore.models.OrderFirestore>> = 
        combine(
            allOrdersFirestore,
            filterState,
            preferredCategories
        ) { orders, filters, preferences ->
            applyOrderFilters(orders, filters, preferences)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    private val rawProviderServices: StateFlow<List<ServiceFirestore>> = firestoreServicesRepository
        .observeAllActiveServices()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )
    
    // Prestadores para filtrar por preferredCategories
    private val _allProviders = MutableStateFlow<List<com.taskgoapp.taskgo.data.firestore.models.UserFirestore>>(emptyList())
    
    init {
        loadProviders()
    }
    
    private fun loadProviders() {
        viewModelScope.launch {
            try {
                val providers = providersRepository.getFeaturedProviders(limit = 100)
                _allProviders.value = providers.map { it.provider }
            } catch (e: Exception) {
                android.util.Log.e("ServicesViewModel", "Erro ao carregar prestadores: ${e.message}", e)
            }
        }
    }

    val availableServices: StateFlow<List<ServiceFirestore>> = combine(
        rawProviderServices,
        _allProviders,
        filterState,
        preferredCategories
    ) { services, providers, filters, preferences ->
        applyServiceFilters(services, providers, filters, preferences)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )
    
    // Prestadores filtrados por categoria (para cliente/vendedor)
    val filteredProviders: StateFlow<List<com.taskgoapp.taskgo.data.firestore.models.UserFirestore>> = combine(
        _allProviders,
        filterState
    ) { providers, filters ->
        if (filters.selectedCategories.isEmpty()) {
            emptyList()
        } else {
            providers.filter { provider ->
                provider.preferredCategories?.any { category ->
                    filters.selectedCategories.contains(category)
                } == true
            }
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    init {
        loadServices()
        loadSavedFilters()
        observeAccountType()
    }

    private fun loadSavedFilters() {
        viewModelScope.launch {
            filterPreferencesManager.getServiceFilters().collect { savedFilters ->
                savedFilters?.let {
                    _filterState.value = it
                }
            }
        }
    }

    private fun observeAccountType() {
        viewModelScope.launch {
            userRepository.observeCurrentUser().collect { user ->
                _accountType.value = user?.accountType ?: AccountType.CLIENTE
            }
        }
    }

    fun updateFilterState(newState: FilterState) {
        _filterState.value = newState
        viewModelScope.launch {
            filterPreferencesManager.saveServiceFilters(newState)
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
    
    fun updateSelectedCategory(category: String?) {
        _selectedCategoryForOrders.value = category
    }

    private fun applyFiltersSync(
        orders: List<ServiceOrder>,
        filters: FilterState,
        preferences: List<String>
    ): List<ServiceOrder> {
        var filtered = orders

        // Busca por texto
        if (filters.searchQuery.isNotBlank()) {
            val query = filters.searchQuery.lowercase()
            filtered = filtered.filter { order ->
                order.category.lowercase().contains(query) ||
                order.description.lowercase().contains(query)
            }
        }

        // Filtrar por categorias
        if (filters.selectedCategories.isNotEmpty()) {
            filtered = filtered.filter { order ->
                filters.selectedCategories.contains(order.category)
            }
        }

        // Filtrar por localização
        filters.location?.let { location ->
            if (location.city != null && location.city.isNotEmpty()) {
                filtered = filtered.filter { order ->
                    order.city.equals(location.city, ignoreCase = true)
                }
            }
            if (location.state != null && location.state.isNotEmpty()) {
                filtered = filtered.filter { order ->
                    order.state.equals(location.state, ignoreCase = true)
                }
            }
            
            // Filtrar por raio usando GPS (será aplicado assincronamente quando necessário)
            // Por enquanto, apenas filtra por cidade se especificada
            // TODO: Implementar filtro assíncrono quando necessário usando LaunchedEffect
        }

        // Filtrar por avaliação
        filters.minRating?.let { minRating ->
            filtered = filtered.filter { order ->
                order.rating != null && order.rating >= minRating
            }
        }

        // Ordenar
        filtered = when (filters.sortBy) {
            com.taskgoapp.taskgo.core.design.SortOption.NEWEST -> 
                filtered.sortedByDescending { it.date }
            com.taskgoapp.taskgo.core.design.SortOption.RATING -> 
                filtered.sortedByDescending { it.rating ?: 0.0 }
            com.taskgoapp.taskgo.core.design.SortOption.RELEVANCE -> 
                filtered // Manter ordem original
            else -> filtered
        }

        return filtered
    }

    private fun applyOrderFilters(
        orders: List<OrderFirestore>,
        filters: FilterState,
        preferences: List<String>
    ): List<OrderFirestore> {
        var filtered = orders

        if (filters.searchQuery.isNotBlank()) {
            val query = filters.searchQuery.lowercase()
            filtered = filtered.filter { order ->
                order.details.lowercase().contains(query) ||
                    order.location.lowercase().contains(query) ||
                    (order.category ?: "").lowercase().contains(query)
            }
        }

        if (filters.selectedCategories.isNotEmpty()) {
            filtered = filtered.filter { order ->
                filters.selectedCategories.contains(order.category)
            }
        }

        filters.priceRange?.let { range ->
            filtered = filtered.filter { order ->
                val minOk = range.min?.let { order.budget >= it } ?: true
                val maxOk = range.max?.let { order.budget <= it } ?: true
                minOk && maxOk
            }
        }

        filtered = when (filters.sortBy) {
            com.taskgoapp.taskgo.core.design.SortOption.NEWEST ->
                filtered.sortedByDescending { it.createdAt?.time ?: 0L }
            com.taskgoapp.taskgo.core.design.SortOption.RATING ->
                filtered // Ordens ainda não possuem rating agregado
            else -> filtered
        }

        if (preferences.isNotEmpty()) {
            filtered = filtered.sortedBy { order ->
                val idx = preferences.indexOfFirst { it.equals(order.category, ignoreCase = true) }
                if (idx == -1) preferences.size else idx
            }
        }

        return filtered
    }

    private fun applyServiceFilters(
        services: List<ServiceFirestore>,
        providers: List<com.taskgoapp.taskgo.data.firestore.models.UserFirestore>,
        filters: FilterState,
        preferences: List<String>
    ): List<ServiceFirestore> {
        var filtered = services

        if (filters.searchQuery.isNotBlank()) {
            val query = filters.searchQuery.lowercase()
            filtered = filtered.filter { service ->
                service.title.lowercase().contains(query) ||
                    service.description.lowercase().contains(query) ||
                    service.category.lowercase().contains(query)
            }
        }

        if (filters.selectedCategories.isNotEmpty()) {
            // Filtrar serviços cuja categoria corresponde OU prestador tem a categoria em preferredCategories
            val providerIdsWithCategory = providers
                .filter { provider ->
                    provider.preferredCategories?.any { category ->
                        filters.selectedCategories.contains(category)
                    } == true
                }
                .map { it.uid }
                .toSet()
            
            filtered = filtered.filter { service ->
                filters.selectedCategories.contains(service.category) ||
                providerIdsWithCategory.contains(service.providerId)
            }
        }

        filters.priceRange?.let { range ->
            filtered = filtered.filter { service ->
                val minOk = range.min?.let { service.price >= it } ?: true
                val maxOk = range.max?.let { service.price <= it } ?: true
                minOk && maxOk
            }
        }

        filtered = when (filters.sortBy) {
            com.taskgoapp.taskgo.core.design.SortOption.PRICE_LOW_TO_HIGH ->
                filtered.sortedBy { it.price }
            com.taskgoapp.taskgo.core.design.SortOption.PRICE_HIGH_TO_LOW ->
                filtered.sortedByDescending { it.price }
            com.taskgoapp.taskgo.core.design.SortOption.NEWEST ->
                filtered.sortedByDescending { it.createdAt?.time ?: 0L }
            com.taskgoapp.taskgo.core.design.SortOption.RATING ->
                filtered // Placeholder: serviços ainda não possuem rating
            else -> filtered
        }

        if (preferences.isNotEmpty()) {
            filtered = filtered.sortedBy { service ->
                val idx = preferences.indexOfFirst { it.equals(service.category, ignoreCase = true) }
                if (idx == -1) preferences.size else idx
            }
        }

        return filtered
    }

    private fun parsePreferredCategories(raw: String): List<String> {
        if (raw.isBlank() || raw == "[]") return emptyList()
        return raw
            .removePrefix("[")
            .removeSuffix("]")
            .split(",")
            .map { it.trim().removeSurrounding("\"") }
            .filter { it.isNotBlank() }
    }

    private fun loadServices() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                // Os dados vêm automaticamente via Flow do repositório
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Erro ao carregar serviços"
                )
            }
        }
    }

    fun refresh() {
        loadServices()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

