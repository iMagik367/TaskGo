package br.com.taskgo.taskgo.feature.products.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskgoapp.core.model.Product
import com.example.taskgoapp.domain.repository.ProductsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProductsViewModel(
    private val productsRepository: ProductsRepository
) : ViewModel() {

    val products: StateFlow<List<Product>> = productsRepository
        .observeProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            val product = productsRepository.getProduct(productId)
            if (product != null) {
                productsRepository.deleteProduct(productId)
            }
        }
    }
}


