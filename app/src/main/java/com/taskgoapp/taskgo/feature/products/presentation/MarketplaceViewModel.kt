package com.taskgoapp.taskgo.feature.products.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskgoapp.taskgo.domain.repository.ProductsRepository
import com.taskgoapp.taskgo.core.model.Product
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MarketplaceViewModel @Inject constructor(
    private val productsRepository: ProductsRepository
) : ViewModel() {

    val products: StateFlow<List<Product>> = productsRepository
        .observeProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun getProductById(id: String): Product? {
        // Note: getProductById is not available in ProductsRepository flow,
        // so we'll need to get it from the products list
        return products.value.firstOrNull { it.id == id }
    }
}
