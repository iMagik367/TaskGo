package com.taskgoapp.taskgo.feature.services.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskgoapp.taskgo.data.repository.FirestoreProvidersRepository
import com.taskgoapp.taskgo.data.repository.ProviderWithScore
import com.taskgoapp.taskgo.core.design.FilterState
import com.taskgoapp.taskgo.domain.repository.CategoriesRepository
import com.taskgoapp.taskgo.data.local.datastore.FilterPreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LocalProvidersUiState(
    val isLoading: Boolean = false,
    val featuredProviders: List<ProviderWithScore> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class LocalProvidersViewModel @Inject constructor(
    private val providersRepository: FirestoreProvidersRepository,
    private val categoriesRepository: CategoriesRepository,
    private val filterPreferencesManager: FilterPreferencesManager,
    private val userRepository: com.taskgoapp.taskgo.domain.repository.UserRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LocalProvidersUiState())
    val uiState: StateFlow<LocalProvidersUiState> = _uiState.asStateFlow()
    
    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()
    
    val serviceCategories: StateFlow<List<String>> = categoriesRepository
        .observeServiceCategories()
        .map { categories -> categories.map { it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    
    val serviceCategoriesFull: StateFlow<List<com.taskgoapp.taskgo.core.data.models.ServiceCategory>> = categoriesRepository
        .observeServiceCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    
    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()
    
    private val _allProviders = MutableStateFlow<List<ProviderWithScore>>(emptyList())
    
    val filteredProviders: StateFlow<List<ProviderWithScore>> = combine(
        _allProviders,
        filterState
    ) { providers, filters ->
        applyFiltersSync(providers, filters)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )
    
    init {
        loadFeaturedProviders()
        loadSavedFilters()
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
    
    private fun applyFiltersSync(providers: List<ProviderWithScore>, filters: FilterState): List<ProviderWithScore> {
        var filtered = providers
        
        // Busca por texto
        if (filters.searchQuery.isNotBlank()) {
            val query = filters.searchQuery.lowercase()
            filtered = filtered.filter { providerWithScore ->
                val provider = providerWithScore.provider
                provider.displayName?.lowercase()?.contains(query) == true ||
                provider.preferredCategories?.any { it.lowercase().contains(query) } == true
            }
        }
        
        // Filtrar por categorias
        if (filters.selectedCategories.isNotEmpty()) {
            filtered = filtered.filter { providerWithScore ->
                val provider = providerWithScore.provider
                provider.preferredCategories?.any { category ->
                    filters.selectedCategories.contains(category)
                } == true
            }
        }
        
        // Filtrar por avaliação mínima
        filters.minRating?.let { minRating ->
            filtered = filtered.filter { providerWithScore ->
                val rating = providerWithScore.provider.rating ?: 0.0
                rating >= minRating
            }
        }
        
        // Ordenar
        filtered = when (filters.sortBy) {
            com.taskgoapp.taskgo.core.design.SortOption.RATING -> 
                filtered.sortedByDescending { it.provider.rating ?: 0.0 }
            com.taskgoapp.taskgo.core.design.SortOption.RELEVANCE -> 
                filtered.sortedByDescending { it.score } // Ordenar por score do algoritmo
            else -> filtered
        }
        
        return filtered
    }
    
    private fun loadFeaturedProviders() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val currentUser = userRepository.observeCurrentUser().firstOrNull()
                val userCity = currentUser?.city?.takeIf { it.isNotBlank() }
                val userState = currentUser?.state?.takeIf { it.isNotBlank() }
                
                val providers = if (userCity != null && userState != null) {
                    providersRepository.getProvidersByLocationAndCategory(
                        city = userCity,
                        state = userState,
                        category = null,
                        limit = 50
                    )
                } else {
                    providersRepository.getFeaturedProviders(limit = 50)
                }
                
                Log.d("LocalProvidersViewModel", "Prestadores carregados: ${providers.size}")
                _allProviders.value = providers
                _uiState.value = _uiState.value.copy(
                    featuredProviders = providers,
                    isLoading = false
                )
            } catch (e: Exception) {
                Log.e("LocalProvidersViewModel", "Erro ao carregar prestadores: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erro ao carregar prestadores: ${e.message}"
                )
            }
        }
    }
    
    fun refresh() {
        loadFeaturedProviders()
    }
    
    fun updateSelectedCategory(category: String?) {
        _selectedCategory.value = category
    }
}

