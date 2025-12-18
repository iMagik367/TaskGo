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
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.UUID

data class ProductFormState(
    val id: String? = null,
    val title: String = "",
    val price: String = "",
    val description: String = "",
    val sellerName: String = "",
    val imageUris: List<String> = emptyList(),
    val featured: Boolean = false, // Produto em destaque
    val discountPercentage: String = "", // Porcentagem de desconto
    val isSaving: Boolean = false,
    val error: String? = null,
    val canSave: Boolean = false,
    val saved: Boolean = false
)

@HiltViewModel
class ProductFormViewModel @Inject constructor(
    private val productsRepository: ProductsRepository,
    private val documentVerificationManager: com.taskgoapp.taskgo.core.security.DocumentVerificationManager,
    private val locationManager: com.taskgoapp.taskgo.core.location.LocationManager,
    private val storageRepository: com.taskgoapp.taskgo.data.repository.FirebaseStorageRepository,
    private val authRepository: com.taskgoapp.taskgo.data.repository.FirebaseAuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductFormState())
    val uiState: StateFlow<ProductFormState> = _uiState.asStateFlow()
    
    private val _isVerified = MutableStateFlow(false)
    val isVerified: StateFlow<Boolean> = _isVerified.asStateFlow()
    
    init {
        checkVerificationStatus()
    }
    
    private fun checkVerificationStatus() {
        viewModelScope.launch {
            _isVerified.value = documentVerificationManager.hasDocumentsVerified()
        }
    }

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
                        imageUris = p.imageUris,
                        featured = p.featured ?: false
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

    fun onFeaturedChange(value: Boolean) {
        try {
            Log.d("ProductFormViewModel", "Featured changed to: $value")
            _uiState.value = _uiState.value.copy(featured = value)
        } catch (e: Exception) {
            Log.e("ProductFormViewModel", "Error changing featured", e)
        }
    }
    
    fun onDiscountPercentageChange(value: String) {
        try {
            Log.d("ProductFormViewModel", "Discount percentage changed to: $value")
            _uiState.value = _uiState.value.copy(discountPercentage = value)
        } catch (e: Exception) {
            Log.e("ProductFormViewModel", "Error changing discount percentage", e)
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
        
        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            _uiState.value = _uiState.value.copy(error = "Usuário não autenticado. Faça login novamente.")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = s.copy(isSaving = true, error = null)
            
            try {
                val userId = currentUser.uid
                val productId = s.id ?: UUID.randomUUID().toString()
                
                // Upload de imagens (igual aos serviços)
                val imageUrls = mutableListOf<String>()
                
                s.imageUris.forEachIndexed { index, imageUri ->
                    // Se já é URL, adiciona diretamente
                    if (imageUri.startsWith("http://") || imageUri.startsWith("https://")) {
                        imageUrls.add(imageUri)
                    } else {
                        // Faz upload
                        val uri = android.net.Uri.parse(imageUri)
                        val result = storageRepository.uploadProductImage(
                            userId = userId,
                            productId = productId,
                            uri = uri,
                            imageIndex = index
                        )
                        result.fold(
                            onSuccess = { url ->
                                imageUrls.add(url)
                            },
                            onFailure = { e ->
                                throw e
                            }
                        )
                    }
                }
                
                // Capturar localização
                var latitude: Double? = null
                var longitude: Double? = null
                try {
                    val location = locationManager.getCurrentLocation()
                    location?.let {
                        latitude = it.latitude
                        longitude = it.longitude
                    }
                } catch (e: Exception) {
                    // Ignora erro de localização
                }
                
                val product = Product(
                    id = productId,
                    title = s.title.trim(),
                    price = s.price.replace(",", ".").toDouble(),
                    description = s.description.ifBlank { null },
                    sellerName = s.sellerName.ifBlank { null },
                    imageUris = imageUrls,
                    featured = s.featured,
                    latitude = latitude,
                    longitude = longitude
                )
                
                productsRepository.upsertProduct(product)
                _uiState.value = _uiState.value.copy(isSaving = false, saved = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "Erro ao salvar produto: ${e.message}"
                )
            }
        }
    }
}


