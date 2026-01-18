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
            android.util.Log.e("MyServiceOrdersVM", "Usuário não autenticado")
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Usuário não autenticado"
            )
            return
        }
        
        android.util.Log.d("MyServiceOrdersVM", "🔵 Carregando ordens para cliente: ${currentUser.uid}")
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // CRÍTICO: Observar coleção pública 'orders' onde clientId == userId
                // A Cloud Function createOrder salva na coleção pública, não na subcoleção
                orderRepository.observeOrders(currentUser.uid, "client")
                    .catch { e ->
                        android.util.Log.e("MyServiceOrdersVM", "❌ Erro ao observar ordens: ${e.message}", e)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Erro ao carregar ordens: ${e.message}"
                        )
                    }
                    .collect { orders ->
                        android.util.Log.d("MyServiceOrdersVM", "📦 Ordens recebidas: ${orders.size}")
                        orders.forEach { order ->
                            android.util.Log.d("MyServiceOrdersVM", "   - Order ${order.id}: status=${order.status}, category=${order.category}")
                        }
                        
                        val orderItems = orders.map { order ->
                            ServiceOrderItem(
                                id = order.id,
                                category = order.category ?: order.serviceId?.takeIf { it.isNotBlank() } ?: "Serviço",
                                details = order.details,
                                location = order.location,
                                budget = order.budget,
                                dueDate = parseDueDate(order.dueDate),
                                status = order.status ?: "pending",
                                createdAt = order.createdAt
                            )
                        }
                        android.util.Log.d("MyServiceOrdersVM", "✅ ${orderItems.size} ordens processadas e atualizadas na UI")
                        _uiState.value = _uiState.value.copy(
                            orders = orderItems,
                            isLoading = false,
                            error = null
                        )
                    }
            } catch (e: Exception) {
                android.util.Log.e("MyServiceOrdersVM", "❌ Erro ao carregar ordens: ${e.message}", e)
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
                when (result) {
                    is com.taskgoapp.taskgo.core.model.Result.Error -> {
                        _uiState.value = _uiState.value.copy(
                            error = "Erro ao excluir ordem: ${result.exception.message ?: "Erro desconhecido"}"
                        )
                    }
                    is com.taskgoapp.taskgo.core.model.Result.Success -> {
                        // Sucesso - a lista será atualizada automaticamente via observeOrders
                    }
                    is com.taskgoapp.taskgo.core.model.Result.Loading -> {
                        // Nada a fazer
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Erro ao excluir ordem: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Força recarregamento das ordens
     * Útil após criar uma nova ordem para garantir que aparece na lista
     */
    fun refreshOrders() {
        android.util.Log.d("MyServiceOrdersVM", "🔄 Forçando recarregamento de ordens...")
        loadOrders()
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

