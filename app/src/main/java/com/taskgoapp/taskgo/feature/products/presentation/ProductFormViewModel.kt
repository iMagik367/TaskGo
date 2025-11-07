package com.taskgoapp.taskgo.feature.products.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskgoapp.taskgo.core.model.Product
import com.taskgoapp.taskgo.core.validation.Validators
import com.taskgoapp.taskgo.domain.repository.ProductsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class ProductFormState(
    val id: String? = null,
    val title: String = "",
    val price: String = "",
    val description: String = "",
    val sellerName: String = "",
    val imageUris: List<String> = emptyList(),
    val isSaving: Boolean = false,
    val error: String? = null,
    val canSave: Boolean = false,
    val saved: Boolean = false
)

@HiltViewModel
class ProductFormViewModel @Inject constructor(
    private val productsRepository: ProductsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductFormState())
    val uiState: StateFlow<ProductFormState> = _uiState.asStateFlow()

    fun load(productId: String?) {
        if (productId == null) return
        viewModelScope.launch {
            try {
                Log.d("ProductFormViewModel", "Loading product with ID: $productId")
                val p = productsRepository.getProduct(productId)
                if (p != null) {
                    Log.d("ProductFormViewModel", "Product loaded successfully: ${p.title}")
                    _uiState.value = _uiState.value.copy(
                        id = p.id,
                        title = p.title,
                        price = p.price.toString(),
                        description = p.description.orEmpty(),
                        sellerName = p.sellerName.orEmpty(),
                        imageUris = p.imageUris
                    )
                    validate()
                } else {
                    Log.w("ProductFormViewModel", "Product not found with ID: $productId")
                }
            } catch (e: Exception) {
                Log.e("ProductFormViewModel", "Error loading product", e)
                _uiState.value = _uiState.value.copy(error = "Erro ao carregar produto: ${e.message}")
            }
        }
    }

    fun onTitleChange(value: String) {
        try {
            Log.d("ProductFormViewModel", "Title changed to: $value")
            _uiState.value = _uiState.value.copy(title = value)
            validate()
        } catch (e: Exception) {
            Log.e("ProductFormViewModel", "Error changing title", e)
        }
    }

    fun onPriceChange(value: String) {
        try {
            Log.d("ProductFormViewModel", "Price changed to: $value")
            _uiState.value = _uiState.value.copy(price = value)
            validate()
        } catch (e: Exception) {
            Log.e("ProductFormViewModel", "Error changing price", e)
        }
    }

    fun onDescriptionChange(value: String) {
        try {
            Log.d("ProductFormViewModel", "Description changed to: $value")
            _uiState.value = _uiState.value.copy(description = value)
            validate()
        } catch (e: Exception) {
            Log.e("ProductFormViewModel", "Error changing description", e)
        }
    }

    fun onSellerNameChange(value: String) {
        try {
            Log.d("ProductFormViewModel", "Seller name changed to: $value")
            _uiState.value = _uiState.value.copy(sellerName = value)
            validate()
        } catch (e: Exception) {
            Log.e("ProductFormViewModel", "Error changing seller name", e)
        }
    }

    fun addImage(uri: String) {
        try {
            Log.d("ProductFormViewModel", "Adding image: $uri")
            val updated = _uiState.value.imageUris + uri
            _uiState.value = _uiState.value.copy(imageUris = updated)
            Log.d("ProductFormViewModel", "Updated imageUris: ${_uiState.value.imageUris}")
            validate()
        } catch (e: Exception) {
            Log.e("ProductFormViewModel", "Error adding image", e)
        }
    }

    fun removeImage(uri: String) {
        try {
            Log.d("ProductFormViewModel", "Removing image: $uri")
            val updated = _uiState.value.imageUris.filterNot { it == uri }
            _uiState.value = _uiState.value.copy(imageUris = updated)
            validate()
        } catch (e: Exception) {
            Log.e("ProductFormViewModel", "Error removing image", e)
        }
    }

    private fun validate() {
        try {
            val s = _uiState.value
            val titleOk = s.title.trim().isNotEmpty()
            val priceOk = Validators.isValidPrice(s.price)
            val imagesOk = s.imageUris.isNotEmpty()
            Log.d("ProductFormViewModel", "Validation - Title: $titleOk, Price: $priceOk, Images: $imagesOk")
            _uiState.value = s.copy(
                canSave = titleOk && priceOk && imagesOk,
                error = null
            )
        } catch (e: Exception) {
            Log.e("ProductFormViewModel", "Error during validation", e)
        }
    }

    fun save() {
        val s = _uiState.value
        if (!s.canSave || s.isSaving) return
        viewModelScope.launch {
            try {
                Log.d("ProductFormViewModel", "Saving product: ${s.title}")
                Log.d("ProductFormViewModel", "Product images: ${s.imageUris}")
                _uiState.value = s.copy(isSaving = true)
                val id = s.id ?: UUID.randomUUID().toString()
                val product = Product(
                    id = id,
                    title = s.title.trim(),
                    price = s.price.replace(",", ".").toDouble(),
                    description = s.description.ifBlank { null },
                    sellerName = s.sellerName.ifBlank { null },
                    imageUris = s.imageUris
                )
                Log.d("ProductFormViewModel", "Product to save: $product")
                productsRepository.upsertProduct(product)
                Log.d("ProductFormViewModel", "Product saved successfully")
                _uiState.value = _uiState.value.copy(isSaving = false, saved = true)
            } catch (e: Exception) {
                Log.e("ProductFormViewModel", "Error saving product", e)
                _uiState.value = _uiState.value.copy(isSaving = false, error = e.message)
            }
        }
    }
}


