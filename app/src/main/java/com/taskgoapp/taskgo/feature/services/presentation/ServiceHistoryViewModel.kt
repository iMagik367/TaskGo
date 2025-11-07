package com.taskgoapp.taskgo.feature.services.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskgoapp.taskgo.core.model.ServiceOrder
import com.taskgoapp.taskgo.domain.repository.ServiceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class ServiceHistoryUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val serviceHistory: List<ServiceHistoryItem> = emptyList()
)

@HiltViewModel
class ServiceHistoryViewModel @Inject constructor(
    private val serviceRepository: ServiceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServiceHistoryUiState())
    val uiState: StateFlow<ServiceHistoryUiState> = _uiState.asStateFlow()

    // Converter ServiceOrder para ServiceHistoryItem
    val serviceHistory: StateFlow<List<ServiceHistoryItem>> = serviceRepository
        .observeServiceOrders()
        .map { orders ->
            orders.map { order ->
                ServiceHistoryItem(
                    id = order.id,
                    serviceTitle = order.description,
                    providerName = "Prestador", // TODO: Buscar do repositório quando disponível
                    clientLocation = "${order.city}, ${order.state}",
                    serviceDate = formatDate(order.date),
                    status = ServiceStatus.COMPLETED, // TODO: Determinar status do repositório
                    rating = null
                )
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                // Os dados vêm automaticamente via Flow do repositório
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Erro ao carregar histórico"
                )
            }
        }
    }

    private fun formatDate(timestamp: Long): String {
        val date = Date(timestamp)
        val formatter = SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale("pt", "BR"))
        return formatter.format(date)
    }

    fun refresh() {
        loadHistory()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

