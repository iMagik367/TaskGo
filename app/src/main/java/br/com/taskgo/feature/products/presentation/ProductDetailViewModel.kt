package br.com.taskgo.taskgo.feature.products.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskgoapp.core.data.models.Product
import com.example.taskgoapp.core.data.repositories.MarketplaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val marketplaceRepository: MarketplaceRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()
    
    fun loadProduct(productId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                marketplaceRepository.getProductById(productId.toLong()).collect { product ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        product = product,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    fun addToCart(productId: String, quantity: Int = 1) {
        viewModelScope.launch {
            try {
                // TODO: Implementar adicionar ao carrinho no MarketplaceRepository
                _uiState.value = _uiState.value.copy(
                    showAddToCartSuccess = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Erro ao adicionar ao carrinho: ${e.message}"
                )
            }
        }
    }
    
    fun dismissAddToCartSuccess() {
        _uiState.value = _uiState.value.copy(showAddToCartSuccess = false)
    }
    
    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class ProductDetailUiState(
    val isLoading: Boolean = false,
    val product: Product? = null,
    val error: String? = null,
    val showAddToCartSuccess: Boolean = false
)
