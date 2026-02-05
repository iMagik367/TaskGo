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
import com.taskgoapp.taskgo.data.repository.FirestoreUserRepository
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
import kotlinx.coroutines.flow.firstOrNull
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
    private val preferencesRepository: PreferencesRepository,
    private val firestoreUserRepository: FirestoreUserRepository
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
    
    // CRÍTICO: Armazenar cidade e estado separadamente para usar nas queries por localização
    private val _userCity = MutableStateFlow<String?>(null)
    private val _userState = MutableStateFlow<String?>(null)
    
    init {
        // Carregar localização do usuário ao inicializar
        loadUserLocation()
    }
    
    private fun loadUserLocation() {
        // LEI MÁXIMA DO TASKGO: Usar APENAS city/state do perfil do usuário (cadastro)
        // NUNCA usar GPS para city/state - GPS apenas para coordenadas (mapa)
        viewModelScope.launch {
            try {
                userRepository.observeCurrentUser().collect { user ->
                    val userCity = user?.city?.takeIf { it.isNotBlank() }
                    val userState = user?.state?.takeIf { it.isNotBlank() }
                    
                    if (userCity.isNullOrBlank() || userState.isNullOrBlank()) {
                        android.util.Log.e("ServicesViewModel", "❌ ERRO CRÍTICO: Usuário não possui city/state válidos no cadastro. " +
                                "City: ${user?.city ?: "null"}, State: ${user?.state ?: "null"}. " +
                                "O usuário DEVE completar o cadastro com cidade e estado.")
                        // Não definir como null - manter valores anteriores ou lançar erro
                        throw Exception("Usuário não possui city/state no cadastro. Complete seu perfil.")
                    }
                    
                    _userCity.value = userCity
                    _userState.value = userState
                    android.util.Log.d("ServicesViewModel", "📍 Localização do perfil obtida: city=$userCity, state=$userState")
                }
            } catch (e: Exception) {
                android.util.Log.e("ServicesViewModel", "Erro ao obter localizacao do perfil: ${e.message}", e)
            }
        }
    }
    
    val allOrdersFirestore: StateFlow<List<OrderFirestore>> = combine(
        _accountType,
        _selectedCategoryForOrders,
        preferredCategories,
        _userCity,
        _userState
    ) { accountType, category, preferredCategoriesList, userCity, userState ->
        if (accountType == AccountType.PARCEIRO 
            && userCity != null && userState != null) { // Suporta legacy PRESTADOR
            // CRÍTICO: Usar cidade e estado para observar ordens da região correta
            
            // Se há categorias preferidas e nenhuma categoria específica foi selecionada,
            // filtrar ordens por preferredCategories
            if (category != null && category.isNotBlank()) {
                orderRepository.observeLocalServiceOrders(category = category)
            } else if (preferredCategoriesList.isNotEmpty()) {
                // Filtrar ordens pelas categorias preferidas do Parceiro
                // Como observeLocalServiceOrders aceita apenas uma categoria, vamos buscar todas e filtrar depois
                orderRepository.observeLocalServiceOrders(category = null)
            } else {
                // Buscar todas as ordens pendentes (sem filtro de categoria)
                orderRepository.observeLocalServiceOrders(category = null)
            }
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList<OrderFirestore>())
        }
    }.flatMapLatest { it }
        .map { orders ->
            // Filtrar ordens por preferredCategories se houver e nenhuma categoria específica foi selecionada
            val accountTypeValue = _accountType.value
            val preferredCategoriesList = preferredCategories.value
            val selectedCategory = _selectedCategoryForOrders.value
            // REMOVIDO: _userLocation não existe, usar _userCity e _userState para localização
            val userCityValue = _userCity.value
            val userStateValue = _userState.value
            
            var filtered = orders
            
            // CRÍTICO: Filtrar ordens por preferredCategories do parceiro
            // REGRA: Parceiro vê apenas ordens nas categorias que ele escolheu no cadastro
            if (accountTypeValue == AccountType.PARCEIRO 
                && selectedCategory == null 
                && preferredCategoriesList.isNotEmpty()) {
                filtered = filtered.filter { order -> 
                    val orderCategory = order.category?.lowercase() ?: ""
                    preferredCategoriesList.any { preferredCategory ->
                        preferredCategory.lowercase() == orderCategory
                    }
                }
            }
            
            // REMOVIDO: Filtro por GPS usando _userLocation que não existe
            // TODO: Implementar filtro por GPS quando necessário usando LocationManager diretamente
            // Por enquanto, o filtro por localização já é feito via observeLocalServiceOrders com city/state
            
            filtered
        }
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
                // CRÍTICO: Carregar prestadores filtrados por localização e categoria
                val user = userRepository.observeCurrentUser().firstOrNull()
                val userCity = user?.city?.takeIf { it.isNotBlank() }
                val userState = user?.state?.takeIf { it.isNotBlank() }
                
                val providers = if (userCity != null && userState != null) {
                    // Buscar prestadores por localização (sem filtro de categoria inicial)
                    // O filtro por categoria será aplicado no combine
                    providersRepository.getProvidersByLocationAndCategory(
                        city = userCity,
                        state = userState,
                        category = null, // Categoria será filtrada no combine
                        limit = 100
                    )
                } else {
                    // Fallback: buscar todos os prestadores em destaque
                    providersRepository.getFeaturedProviders(limit = 100)
                }
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
    
    private val _selectedCategoryForProviders = MutableStateFlow<String?>(null)
    
    // Prestadores filtrados por categoria (para cliente/vendedor)
    val filteredProviders: StateFlow<List<com.taskgoapp.taskgo.data.firestore.models.UserFirestore>> = combine(
        _allProviders,
        _selectedCategoryForProviders
    ) { providers, selectedCategory ->
        if (selectedCategory == null) {
            emptyList()
        } else {
            providers.filter { provider ->
                provider.preferredCategories?.any { category ->
                    category.equals(selectedCategory, ignoreCase = true)
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
        // REMOVIDO: loadUserLocation() duplicado que usava _userLocation inexistente
        // A função loadUserLocation() que usa _userCity e _userState já está sendo chamada no primeiro init
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
        _selectedCategoryForProviders.value = category
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

        // REMOVIDO: Filtro por localização manual
        // Localização sempre automática do perfil do usuário

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

