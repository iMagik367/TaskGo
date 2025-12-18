package com.taskgoapp.taskgo.feature.products.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskgoapp.taskgo.core.model.Product
import com.taskgoapp.taskgo.domain.repository.ProductsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.onEach

data class GerenciarProdutosState(
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class GerenciarProdutosViewModel @Inject constructor(
    private val productsRepository: ProductsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GerenciarProdutosState())
    val uiState: StateFlow<GerenciarProdutosState> = _uiState.asStateFlow()

    init {
        observeProducts()
    }

    private fun observeProducts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            // Usar getMyProducts que já retorna produtos do usuário
            // e observar mudanças em tempo real
            try {
                // Carregar produtos imediatamente do cache
                val initialProducts = productsRepository.getMyProducts()
                _uiState.value = _uiState.value.copy(
                    products = initialProducts,
                    isLoading = false,
                    error = null
                )
                
                // Observar mudanças em tempo real
                productsRepository.observeProducts()
                    .onEach { allProducts ->
                        // Filtrar apenas produtos do usuário atual usando getMyProducts
                        val myProducts = productsRepository.getMyProducts()
                        _uiState.value = _uiState.value.copy(
                            products = myProducts,
                            isLoading = false,
                            error = null
                        )
                    }
                    .catch { e ->
                        android.util.Log.e("GerenciarProdutosVM", "Erro ao observar produtos: ${e.message}", e)
                        // Não atualizar erro aqui para não sobrescrever produtos já carregados
                    }
                    .collect { }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erro ao carregar produtos: ${e.message}"
                )
            }
        }
    }

    fun loadProducts() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                val products = productsRepository.getMyProducts()
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
