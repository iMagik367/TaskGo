package com.taskgoapp.taskgo.feature.services.presentation

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.taskgoapp.taskgo.data.firestore.models.ServiceFirestore
import com.taskgoapp.taskgo.data.repository.FirebaseStorageRepository
import com.taskgoapp.taskgo.data.repository.FirestoreServicesRepository
import com.taskgoapp.taskgo.data.repository.FirestoreUserRepository
import com.taskgoapp.taskgo.domain.usecase.SettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

data class ServiceFormState(
    val serviceId: String? = null,
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val selectedCategories: Set<String> = emptySet(), // Categorias que o prestador oferece
    val price: String = "",
    val tags: List<String> = emptyList(),
    val imageUris: List<Uri> = emptyList(),
    val videoUris: List<Uri> = emptyList(),
    val uploadedImageUrls: List<String> = emptyList(),
    val uploadedVideoUrls: List<String> = emptyList(),
    val isActive: Boolean = true,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val uploadProgress: Float = 0f,
    val error: String? = null,
    val isSaved: Boolean = false
)

@HiltViewModel
class ServiceFormViewModel @Inject constructor(
    private val servicesRepository: FirestoreServicesRepository,
    private val storageRepository: FirebaseStorageRepository,
    private val firebaseAuth: FirebaseAuth,
    private val locationManager: com.taskgoapp.taskgo.core.location.LocationManager,
    private val firestoreUserRepository: FirestoreUserRepository,
    private val settingsUseCase: SettingsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServiceFormState())
    val uiState: StateFlow<ServiceFormState> = _uiState.asStateFlow()

    fun loadService(serviceId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val service = servicesRepository.getService(serviceId)
                val currentUser = firebaseAuth.currentUser
                var preferredCategories = emptySet<String>()
                
                // Carregar categorias preferidas do usuário
                if (currentUser != null) {
                    try {
                        val user = firestoreUserRepository.getUser(currentUser.uid)
                        preferredCategories = user?.preferredCategories?.toSet() ?: emptySet()
                    } catch (e: Exception) {
                        Log.w("ServiceFormViewModel", "Erro ao carregar categorias preferidas: ${e.message}")
                    }
                }
                
                if (service != null) {
                    _uiState.value = _uiState.value.copy(
                        serviceId = service.id,
                        title = service.title,
                        description = service.description,
                        category = service.category,
                        selectedCategories = preferredCategories,
                        price = service.price.toString(),
                        tags = service.tags,
                        uploadedImageUrls = service.images,
                        uploadedVideoUrls = service.videos,
                        isActive = service.active,
                        isLoading = false
                    )
                    Log.d("ServiceFormViewModel", "Serviço carregado: ${service.title}, categorias: $preferredCategories")
                } else {
                    _uiState.value = _uiState.value.copy(
                        selectedCategories = preferredCategories,
                        error = "Serviço não encontrado",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e("ServiceFormViewModel", "Erro ao carregar serviço", e)
                _uiState.value = _uiState.value.copy(
                    error = "Erro ao carregar serviço: ${e.message}",
                    isLoading = false
                )
            }
        }
    }
    
    fun loadUserPreferredCategories() {
        viewModelScope.launch {
            val currentUser = firebaseAuth.currentUser ?: return@launch
            try {
                val user = firestoreUserRepository.getUser(currentUser.uid)
                val preferredCategories = user?.preferredCategories?.toSet() ?: emptySet()
                _uiState.value = _uiState.value.copy(selectedCategories = preferredCategories)
                Log.d("ServiceFormViewModel", "Categorias preferidas carregadas: $preferredCategories")
            } catch (e: Exception) {
                Log.w("ServiceFormViewModel", "Erro ao carregar categorias preferidas: ${e.message}")
            }
        }
    }

    fun updateTitle(title: String) {
        _uiState.value = _uiState.value.copy(title = title)
    }

    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    fun updateCategory(category: String) {
        _uiState.value = _uiState.value.copy(category = category)
    }
    
    fun toggleCategory(category: String) {
        val current = _uiState.value.selectedCategories
        val updated = if (current.contains(category)) {
            current - category
        } else {
            current + category
        }
        _uiState.value = _uiState.value.copy(selectedCategories = updated)
    }

    fun updatePrice(price: String) {
        _uiState.value = _uiState.value.copy(price = price)
    }

    fun updateImageUris(uris: List<Uri>) {
        _uiState.value = _uiState.value.copy(imageUris = uris)
    }

    fun updateVideoUris(uris: List<Uri>) {
        _uiState.value = _uiState.value.copy(videoUris = uris)
    }

    fun toggleActive() {
        _uiState.value = _uiState.value.copy(isActive = !_uiState.value.isActive)
    }

    fun canSave(): Boolean {
        val state = _uiState.value
        return state.title.isNotBlank() &&
                state.description.isNotBlank() &&
                state.category.isNotBlank() &&
                state.price.isNotBlank() &&
                state.price.toDoubleOrNull() != null &&
                state.price.toDouble() > 0
    }

    fun saveService(onSuccess: () -> Unit) {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            _uiState.value = _uiState.value.copy(error = "Usuário não autenticado")
            return
        }

        if (!canSave()) {
            _uiState.value = _uiState.value.copy(error = "Preencha todos os campos obrigatórios")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null, uploadProgress = 0f)

            try {
                val providerId = currentUser.uid
                val serviceId = _uiState.value.serviceId ?: "temp_${System.currentTimeMillis()}"
                
                // Upload de imagens
                val imageUrls = mutableListOf<String>()
                val totalUploads = _uiState.value.imageUris.size + _uiState.value.videoUris.size
                var completedUploads = 0

                _uiState.value.imageUris.forEachIndexed { index, uri ->
                    val result = storageRepository.uploadServiceImage(
                        providerId = providerId,
                        serviceId = serviceId,
                        uri = uri,
                        imageIndex = index
                    )
                    result.fold(
                        onSuccess = { url ->
                            imageUrls.add(url)
                            completedUploads++
                            _uiState.value = _uiState.value.copy(
                                uploadProgress = completedUploads.toFloat() / totalUploads.coerceAtLeast(1)
                            )
                        },
                        onFailure = { e ->
                            Log.e("ServiceFormViewModel", "Erro ao fazer upload de imagem", e)
                            throw e
                        }
                    )
                }

                // Upload de vídeos
                val videoUrls = mutableListOf<String>()
                _uiState.value.videoUris.forEachIndexed { index, uri ->
                    val result = storageRepository.uploadServiceVideo(
                        providerId = providerId,
                        serviceId = serviceId,
                        uri = uri,
                        videoIndex = index
                    )
                    result.fold(
                        onSuccess = { url ->
                            videoUrls.add(url)
                            completedUploads++
                            _uiState.value = _uiState.value.copy(
                                uploadProgress = completedUploads.toFloat() / totalUploads.coerceAtLeast(1)
                            )
                        },
                        onFailure = { e ->
                            Log.e("ServiceFormViewModel", "Erro ao fazer upload de vídeo", e)
                            throw e
                        }
                    )
                }

                // Combinar URLs novas com as já existentes
                val allImageUrls = _uiState.value.uploadedImageUrls + imageUrls
                val allVideoUrls = _uiState.value.uploadedVideoUrls + videoUrls

                // Capturar localização do usuário para filtrar por região
                var latitude: Double? = null
                var longitude: Double? = null
                try {
                    val location = locationManager.getCurrentLocation()
                    location?.let {
                        latitude = it.latitude
                        longitude = it.longitude
                        Log.d("ServiceFormViewModel", "Localização capturada: ($latitude, $longitude)")
                    } ?: Log.w("ServiceFormViewModel", "Localização não disponível")
                } catch (e: Exception) {
                    Log.w("ServiceFormViewModel", "Erro ao capturar localização: ${e.message}")
                }

                // Criar ou atualizar serviço
                val service = ServiceFirestore(
                    id = serviceId,
                    providerId = providerId,
                    title = _uiState.value.title.trim(),
                    description = _uiState.value.description.trim(),
                    category = _uiState.value.category,
                    price = _uiState.value.price.toDoubleOrNull() ?: 0.0,
                    images = allImageUrls,
                    videos = allVideoUrls,
                    tags = _uiState.value.tags,
                    active = _uiState.value.isActive,
                    createdAt = if (_uiState.value.serviceId == null) Date() else null,
                    updatedAt = Date(),
                    latitude = latitude,
                    longitude = longitude
                )

                // Atualizar preferredCategories do usuário se houver categorias selecionadas
                if (_uiState.value.selectedCategories.isNotEmpty()) {
                    try {
                        val categoriesList = _uiState.value.selectedCategories.toList()
                        val categoriesJson = com.google.gson.Gson().toJson(categoriesList)
                        settingsUseCase.updateCategories(categoriesJson)
                        Log.d("ServiceFormViewModel", "Categorias preferidas atualizadas: $categoriesList")
                    } catch (e: Exception) {
                        Log.w("ServiceFormViewModel", "Erro ao atualizar categorias preferidas: ${e.message}")
                        // Não falhar o salvamento do serviço se a atualização de categorias falhar
                    }
                }
                
                if (_uiState.value.serviceId == null) {
                    // Criar novo serviço
                    val result = servicesRepository.createService(service)
                    result.fold(
                        onSuccess = { newServiceId ->
                            Log.d("ServiceFormViewModel", "Serviço criado com sucesso: $newServiceId")
                            _uiState.value = _uiState.value.copy(
                                isSaving = false,
                                isSaved = true,
                                serviceId = newServiceId,
                                uploadProgress = 1f
                            )
                            onSuccess()
                        },
                        onFailure = { e ->
                            Log.e("ServiceFormViewModel", "Erro ao criar serviço", e)
                            _uiState.value = _uiState.value.copy(
                                isSaving = false,
                                error = "Erro ao criar serviço: ${e.message}"
                            )
                        }
                    )
                } else {
                    // Atualizar serviço existente
                    val result = servicesRepository.updateService(serviceId, service)
                    result.fold(
                        onSuccess = {
                            Log.d("ServiceFormViewModel", "Serviço atualizado com sucesso: $serviceId")
                            _uiState.value = _uiState.value.copy(
                                isSaving = false,
                                isSaved = true,
                                uploadProgress = 1f
                            )
                            onSuccess()
                        },
                        onFailure = { e ->
                            Log.e("ServiceFormViewModel", "Erro ao atualizar serviço", e)
                            _uiState.value = _uiState.value.copy(
                                isSaving = false,
                                error = "Erro ao atualizar serviço: ${e.message}"
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e("ServiceFormViewModel", "Erro ao salvar serviço", e)
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "Erro ao salvar serviço: ${e.message}"
                )
            }
        }
    }
}

