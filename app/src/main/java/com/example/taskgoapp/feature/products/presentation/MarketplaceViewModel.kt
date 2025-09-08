package com.example.taskgoapp.feature.products.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskgoapp.core.data.repositories.MarketplaceRepository
import com.example.taskgoapp.core.data.models.Product
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MarketplaceViewModel @Inject constructor(
    private val marketplaceRepository: MarketplaceRepository
) : ViewModel() {

    val products: StateFlow<List<Product>> = marketplaceRepository
        .getProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun getProductById(id: Long): StateFlow<Product?> = marketplaceRepository
        .getProductById(id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
