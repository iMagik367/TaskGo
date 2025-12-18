package com.taskgoapp.taskgo.feature.services.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.taskgoapp.taskgo.data.repository.FirestoreOrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class MyServiceOrdersState(
    val orders: List<ServiceOrderItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MyServiceOrdersViewModel @Inject constructor(
    private val orderRepository: FirestoreOrderRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MyServiceOrdersState(isLoading = true))
    val uiState: StateFlow<MyServiceOrdersState> = _uiState.asStateFlow()
    
    init {
        loadOrders()
    }
    
    private fun loadOrders() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Usuário não autenticado"
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                orderRepository.observeOrders(currentUser.uid, "client")
                    .catch { e ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Erro ao carregar ordens: ${e.message}"
                        )
                    }
                    .collect { orders ->
                        val orderItems = orders.map { order ->
                            ServiceOrderItem(
                                id = order.id,
                                category = order.serviceId.takeIf { it.isNotBlank() } ?: "Serviço",
                                details = order.details,
                                location = order.location,
                                budget = order.budget,
                                dueDate = parseDueDate(order.dueDate),
                                status = order.status ?: "pending",
                                createdAt = order.createdAt
                            )
                        }
                        _uiState.value = _uiState.value.copy(
                            orders = orderItems,
                            isLoading = false,
                            error = null
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erro ao carregar ordens: ${e.message}"
                )
            }
        }
    }
    
    fun deleteOrder(orderId: String) {
        viewModelScope.launch {
            try {
                // Soft delete - atualizar status para cancelled
                val result = orderRepository.updateOrderStatus(orderId, "cancelled")
                if (result.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        error = "Erro ao excluir ordem: ${result.exceptionOrNull()?.message ?: "Erro desconhecido"}"
                    )
                }
                // A lista será atualizada automaticamente via observeOrders
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Erro ao excluir ordem: ${e.message}"
                )
            }
        }
    }
}

private val possibleDateFormats = listOf(
    "dd/MM/yyyy",
    "yyyy-MM-dd",
    "yyyy-MM-dd'T'HH:mm:ss'Z'",
    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
)

private fun parseDueDate(dueDate: String?): Date? {
    if (dueDate.isNullOrBlank()) return null
    
    for (pattern in possibleDateFormats) {
        try {
            val formatter = SimpleDateFormat(pattern, Locale.getDefault())
            return formatter.parse(dueDate)
        } catch (ignored: ParseException) {
        }
    }
    return null
}

