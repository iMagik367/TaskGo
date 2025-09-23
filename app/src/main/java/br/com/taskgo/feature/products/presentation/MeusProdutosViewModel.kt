package br.com.taskgo.taskgo.feature.products.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskgoapp.core.data.PreferencesManager
import com.example.taskgoapp.core.model.Product
import com.example.taskgoapp.domain.repository.ProductsRepository
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
                var products = productsRepository.getMyProducts()
                
                // Se não há produtos, criar alguns de exemplo
                if (products.isEmpty()) {
                    val exampleProducts = listOf(
                        Product(
                            id = "1",
                            title = "Forno de Embutir",
                            price = 1200.0,
                            description = "Forno elétrico 30L com timer, função",
                            sellerName = "Usuário",
                            imageUris = emptyList()
                        ),
                        Product(
                            id = "2", 
                            title = "Furadeira sem fio",
                            price = 250.0,
                            description = "Furadeira 18V com 2 baterias,",
                            sellerName = "Usuário",
                            imageUris = emptyList()
                        ),
                        Product(
                            id = "3",
                            title = "Guarda Roupa", 
                            price = 750.0,
                            description = "Guarda roupa 6 portas com",
                            sellerName = "Usuário",
                            imageUris = emptyList()
                        )
                    )
                    
                    // Salvar produtos de exemplo no banco
                    exampleProducts.forEach { product ->
                        productsRepository.upsertProduct(product)
                    }
                    
                    products = exampleProducts
                }
                
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
