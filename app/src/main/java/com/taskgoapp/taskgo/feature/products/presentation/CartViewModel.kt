package com.taskgoapp.taskgo.feature.products.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskgoapp.taskgo.core.data.models.CartItem as UICartItem
import com.taskgoapp.taskgo.core.data.models.Product as UIProduct
import com.taskgoapp.taskgo.core.model.CartItem
import com.taskgoapp.taskgo.core.model.Product
import com.taskgoapp.taskgo.domain.repository.ProductsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CartItemWithId(
    val productId: String,
    val uiCartItem: UICartItem
)

data class CartUiState(
    val isLoading: Boolean = false,
    val cartItems: List<CartItemWithId> = emptyList(),
    val total: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val finalTotal: Double = 0.0,
    val error: String? = null
)

@HiltViewModel
class CartViewModel @Inject constructor(
    private val productsRepository: ProductsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CartUiState())
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()
    
    init {
        loadCart()
    }
    
    private fun loadCart() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            productsRepository.observeCart()
                .flatMapLatest { cartItems ->
                    if (cartItems.isEmpty()) {
                        flowOf(emptyList<UICartItem>())
                    } else {
                        // Buscar produtos para cada item do carrinho
                        val productsFlow = cartItems.map { cartItem ->
                            flow {
                                val product = productsRepository.getProduct(cartItem.productId)
                                emit(product to cartItem)
                            }
                        }
                        combine(productsFlow) { results ->
                            results.mapIndexedNotNull { index, result ->
                                val (product, cartItem) = result as Pair<Product?, CartItem>
                                product?.let { p ->
                                    CartItemWithId(
                                        productId = cartItem.productId,
                                        uiCartItem = UICartItem(
                                            id = index.toLong(),
                                            product = UIProduct(
                                                id = p.id.hashCode().toLong(),
                                                name = p.title,
                                                description = p.description ?: "",
                                                price = p.price,
                                                category = p.category ?: "",
                                                imageUrl = p.imageUris.firstOrNull(),
                                                inStock = true,
                                                seller = null
                                            ),
                                            quantity = cartItem.qty
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        error = e.message ?: "Erro ao carregar carrinho",
                        isLoading = false
                    )
                }
                .collect { cartItemsWithId ->
                    val cartItemsList = cartItemsWithId.filterIsInstance<CartItemWithId>()
                    val total = cartItemsList.sumOf { it.uiCartItem.quantity * it.uiCartItem.product.price }
                    val deliveryFee = if (total > 0) 15.0 else 0.0
                    val finalTotal = total + deliveryFee
                    
                    _uiState.value = _uiState.value.copy(
                        cartItems = cartItemsList,
                        total = total,
                        deliveryFee = deliveryFee,
                        finalTotal = finalTotal,
                        isLoading = false
                    )
                }
        }
    }
    
    fun increaseQuantity(productId: String) {
        viewModelScope.launch {
            productsRepository.addToCart(productId, 1)
        }
    }
    
    fun decreaseQuantity(productId: String) {
        viewModelScope.launch {
            productsRepository.addToCart(productId, -1)
        }
    }
    
    fun removeItem(productId: String) {
        viewModelScope.launch {
            productsRepository.removeFromCart(productId)
        }
    }
    
    fun clearCart() {
        viewModelScope.launch {
            productsRepository.clearCart()
        }
    }
}

