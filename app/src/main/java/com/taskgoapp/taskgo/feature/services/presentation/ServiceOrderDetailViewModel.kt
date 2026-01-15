package com.taskgoapp.taskgo.feature.services.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskgoapp.taskgo.core.model.PostLocation
import com.taskgoapp.taskgo.core.model.Result
import com.taskgoapp.taskgo.data.firestore.models.OrderFirestore
import com.taskgoapp.taskgo.data.repository.FeedMediaRepository
import com.taskgoapp.taskgo.data.repository.FirestoreOrderRepository
import com.taskgoapp.taskgo.domain.usecase.CreatePostUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ServiceOrderDetailUiState(
    val isLoading: Boolean = false,
    val order: OrderFirestore? = null,
    val error: String? = null,
    val isCancelling: Boolean = false,
    val isCompleting: Boolean = false
)

@HiltViewModel
class ServiceOrderDetailViewModel @Inject constructor(
    private val orderRepository: FirestoreOrderRepository,
    private val feedMediaRepository: FeedMediaRepository,
    private val createPostUseCase: CreatePostUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ServiceOrderDetailUiState())
    val uiState: StateFlow<ServiceOrderDetailUiState> = _uiState.asStateFlow()
    
    fun loadOrder(orderId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val order = orderRepository.getOrder(orderId)
                _uiState.value = _uiState.value.copy(
                    order = order,
                    isLoading = false,
                    error = if (order == null) "Ordem não encontrada" else null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erro ao carregar ordem: ${e.message}"
                )
            }
        }
    }
    
    fun cancelOrder(reason: String, refundAmount: Double?) {
        val order = _uiState.value.order ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCancelling = true, error = null)
            
            try {
                val result = orderRepository.cancelOrder(order.id, reason, refundAmount)
                
                // Type-safe check: Kotlin smart cast preserva Result.Success<Unit> dentro do bloco
                when (result) {
                    is com.taskgoapp.taskgo.core.model.Result.Success -> {
                        // result é automaticamente Result.Success<Unit> aqui devido ao smart cast
                        // Recarregar ordem para atualizar estado
                        loadOrder(order.id)
                        _uiState.value = _uiState.value.copy(isCancelling = false)
                    }
                    is com.taskgoapp.taskgo.core.model.Result.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isCancelling = false,
                            error = result.exception.message ?: "Erro ao cancelar ordem"
                        )
                    }
                    is com.taskgoapp.taskgo.core.model.Result.Loading -> {
                        // Nada a fazer, já está em loading
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ServiceOrderDetailViewModel", "Erro ao cancelar ordem: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isCancelling = false,
                    error = "Erro ao cancelar ordem: ${e.message}"
                )
            }
        }
    }
    
    fun completeOrder(
        description: String,
        time: String,
        mediaUris: List<Uri>,
        userId: String
    ) {
        val order = _uiState.value.order ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCompleting = true, error = null)
            
            try {
                // 1. Fazer upload das mídias
                val mediaUrls: List<String> = if (mediaUris.isNotEmpty()) {
                    // Determinar tipos de mídia
                    val mediaTypes = mediaUris.map { uri ->
                        val path = uri.toString().lowercase()
                        when {
                            path.contains("video") || path.contains(".mp4") || path.contains(".mov") -> "video"
                            else -> "image"
                        }
                    }
                    
                    val uploadResult = feedMediaRepository.uploadPostMediaBatch(
                        mediaUris,
                        userId,
                        mediaTypes
                    )
                    
                    // Type-safe check: Kotlin smart cast preserva Result.Success<List<String>> dentro do bloco
                    when (uploadResult) {
                        is com.taskgoapp.taskgo.core.model.Result.Success -> {
                            // uploadResult é automaticamente Result.Success<List<String>> aqui devido ao smart cast
                            uploadResult.data
                        }
                        is com.taskgoapp.taskgo.core.model.Result.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isCompleting = false,
                                error = "Erro ao fazer upload das mídias: ${uploadResult.exception.message}"
                            )
                            return@launch
                        }
                        is com.taskgoapp.taskgo.core.model.Result.Loading -> {
                            // Nada a fazer, aguardar
                            emptyList<String>()
                        }
                    }
                } else {
                    emptyList()
                }
                
                // 2. Atualizar ordem com dados de conclusão
                val result = orderRepository.completeOrder(
                    orderId = order.id,
                    description = description,
                    time = time,
                    mediaUrls = mediaUrls
                )
                
                // Type-safe check: Kotlin smart cast preserva Result.Success<Unit> dentro do bloco
                when (result) {
                    is com.taskgoapp.taskgo.core.model.Result.Success -> {
                        // result é automaticamente Result.Success<Unit> aqui devido ao smart cast
                        // 3. Criar post no feed
                        if (mediaUrls.isNotEmpty() && description.isNotBlank()) {
                            // Usar localização da ordem ou padrão
                            val postText = "$description\n\nTempo: $time"
                            val location = PostLocation(
                                city = order.location.split(",").getOrNull(0) ?: "",
                                state = order.location.split(",").getOrNull(1)?.trim() ?: "",
                                latitude = 0.0, // Pode ser melhorado com geocoding se necessário
                                longitude = 0.0
                            )
                            
                            val mediaTypes = mediaUris.map { uri ->
                                val path = uri.toString().lowercase()
                                when {
                                    path.contains("video") || path.contains(".mp4") || path.contains(".mov") -> "video"
                                    else -> "image"
                                }
                            }
                            
                            createPostUseCase(
                                text = postText,
                                mediaUrls = mediaUrls,
                                mediaTypes = mediaTypes,
                                location = location
                            )
                        }
                        
                        // Recarregar ordem para atualizar estado
                        loadOrder(order.id)
                        _uiState.value = _uiState.value.copy(isCompleting = false)
                    }
                    is com.taskgoapp.taskgo.core.model.Result.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isCompleting = false,
                            error = result.exception.message ?: "Erro ao concluir ordem"
                        )
                    }
                    is com.taskgoapp.taskgo.core.model.Result.Loading -> {
                        // Nada a fazer, já está em loading
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ServiceOrderDetailViewModel", "Erro ao concluir ordem: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isCompleting = false,
                    error = "Erro ao concluir ordem: ${e.message}"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

