package com.taskgoapp.taskgo.feature.services.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.taskgoapp.taskgo.core.design.FilterState
import com.taskgoapp.taskgo.core.location.LocationManager
import com.taskgoapp.taskgo.data.firestore.models.OrderFirestore
import com.taskgoapp.taskgo.data.repository.FirestoreOrderRepository
import com.taskgoapp.taskgo.domain.repository.CategoriesRepository
import com.taskgoapp.taskgo.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LocalServiceOrdersUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val currentUserId: String = ""
)

@HiltViewModel
class LocalServiceOrdersViewModel @Inject constructor(
    private val orderRepository: FirestoreOrderRepository,
    private val categoriesRepository: CategoriesRepository,
    private val locationManager: LocationManager,
    private val userRepository: UserRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LocalServiceOrdersUiState())
    val uiState: StateFlow<LocalServiceOrdersUiState> = _uiState.asStateFlow()
    
    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()
    
    private val _userLocation = MutableStateFlow<Pair<String?, String?>>(null to null)
    
    val categories: StateFlow<List<String>> = categoriesRepository
        .observeServiceCategories()
        .map { it.map { cat -> cat.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    
    val serviceCategoriesFull: StateFlow<List<com.taskgoapp.taskgo.core.data.models.ServiceCategory>> = categoriesRepository
        .observeServiceCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    
    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()
    
    val orders: StateFlow<List<OrderFirestore>> = combine(
        _userLocation,
        _filterState,
        _selectedCategory
    ) { location, filters, selectedCategory ->
        Triple(location, filters, selectedCategory)
    }.flatMapLatest { (location, filters, selectedCategory) ->
        // ✅ observeLocalServiceOrders agora usa LocationStateManager automaticamente
        // Usar categoria selecionada se houver, senão usar do filtro
        val categoryToFilter = selectedCategory ?: filters.selectedCategories.firstOrNull()
        orderRepository.observeLocalServiceOrders(category = categoryToFilter)
            .map { allOrders ->
                applyFilters(allOrders, filters)
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    
    init {
        loadUserLocation()
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            _uiState.value = _uiState.value.copy(
                currentUserId = currentUser.uid,
                isLoading = false
            )
        } else {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Usuário não autenticado"
            )
        }
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
                        android.util.Log.e("LocalServiceOrdersViewModel", "❌ ERRO CRÍTICO: Usuário não possui city/state válidos no cadastro. " +
                                "City: ${user?.city ?: "null"}, State: ${user?.state ?: "null"}")
                        _userLocation.value = null to null
                    } else {
                        _userLocation.value = userCity to userState
                        android.util.Log.d("LocalServiceOrdersViewModel", "📍 Localização do perfil: city=$userCity, state=$userState")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("LocalServiceOrdersViewModel", "Erro ao obter localização do perfil: ${e.message}", e)
                _userLocation.value = null to null
            }
        }
    }
    
    private fun applyFilters(
        orders: List<OrderFirestore>,
        filters: FilterState
    ): List<OrderFirestore> {
        var filtered = orders
        
        // Filtrar por busca
        if (filters.searchQuery.isNotBlank()) {
            val query = filters.searchQuery.lowercase()
            filtered = filtered.filter {
                it.details.lowercase().contains(query) ||
                it.location.lowercase().contains(query)
            }
        }
        
        // Filtrar por categoria
        if (filters.selectedCategories.isNotEmpty()) {
            filtered = filtered.filter { order ->
                filters.selectedCategories.any { category ->
                    order.details.lowercase().contains(category.lowercase())
                }
            }
        }
        
        return filtered
    }
    
    fun updateFilterState(newState: FilterState) {
        _filterState.value = newState
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
    
    fun refresh() {
        loadUserLocation()
    }
    
    fun updateSelectedCategory(category: String?) {
        _selectedCategory.value = category
        if (category != null) {
            updateFilterState(_filterState.value.copy(selectedCategories = setOf(category)))
        } else {
            updateFilterState(_filterState.value.copy(selectedCategories = emptySet()))
        }
    }
}

