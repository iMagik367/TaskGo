package com.taskgoapp.taskgo.feature.services.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.taskgoapp.taskgo.data.firestore.models.OrderFirestore
import com.taskgoapp.taskgo.data.repository.FirestoreOrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyServicesUiState(
    val isLoading: Boolean = false,
    val orders: List<OrderFirestore> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class MyServicesViewModel @Inject constructor(
    private val orderRepository: FirestoreOrderRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyServicesUiState())
    val uiState: StateFlow<MyServicesUiState> = _uiState.asStateFlow()

    init {
        loadOrders()
    }

    private fun loadOrders() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            _uiState.value = _uiState.value.copy(
                error = "Usuário não autenticado",
                isLoading = false
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            orderRepository.observeOrders(currentUser.uid, "partner")
                .catch { e ->
                    Log.e("MyServicesViewModel", "Erro ao carregar ordens", e)
                    _uiState.value = _uiState.value.copy(
                        error = "Erro ao carregar ordens: ${e.message}",
                        isLoading = false
                    )
                }
                .collect { orders ->
                    Log.d("MyServicesViewModel", "Ordens carregadas: ${orders.size}")
                    _uiState.value = _uiState.value.copy(
                        orders = orders,
                        isLoading = false,
                        error = null
                    )
                }
        }
    }

    fun refreshServices() {
        loadOrders()
    }
}

