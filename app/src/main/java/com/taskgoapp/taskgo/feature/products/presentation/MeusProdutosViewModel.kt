package com.taskgoapp.taskgo.feature.products.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskgoapp.taskgo.core.data.PreferencesManager
import com.taskgoapp.taskgo.core.model.Product
import com.taskgoapp.taskgo.domain.repository.ProductsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MeusProdutosState(
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MeusProdutosViewModel @Inject constructor(
    private val productsRepository: ProductsRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val preferencesManager = PreferencesManager(context)

    private val _uiState = MutableStateFlow(MeusProdutosState())
    val uiState: StateFlow<MeusProdutosState> = _uiState.asStateFlow()

    init {
        loadProducts()
    }

    fun loadProducts() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                val products = productsRepository.getMyProducts()
                
                // Não criar produtos de exemplo - app limpo para o primeiro usuário criar manualmente
                _uiState.value = _uiState.value.copy(
                    products = products,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erro ao carregar produtos: ${e.message}"
                )
            }
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            try {
                productsRepository.deleteProduct(productId)
                loadProducts() // Reload the list
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Erro ao deletar produto: ${e.message}"
                )
            }
        }
    }
}
