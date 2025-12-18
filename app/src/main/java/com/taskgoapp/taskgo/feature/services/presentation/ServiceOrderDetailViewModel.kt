package com.taskgoapp.taskgo.feature.services.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskgoapp.taskgo.data.firestore.models.OrderFirestore
import com.taskgoapp.taskgo.data.repository.FirestoreOrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ServiceOrderDetailUiState(
    val isLoading: Boolean = false,
    val order: OrderFirestore? = null,
    val error: String? = null
)

@HiltViewModel
class ServiceOrderDetailViewModel @Inject constructor(
    private val orderRepository: FirestoreOrderRepository
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
}

