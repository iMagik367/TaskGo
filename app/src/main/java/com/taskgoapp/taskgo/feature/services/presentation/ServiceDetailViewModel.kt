package com.taskgoapp.taskgo.feature.services.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskgoapp.taskgo.data.firestore.models.ServiceFirestore
import com.taskgoapp.taskgo.data.repository.FirestoreServicesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ServiceDetailUiState(
    val isLoading: Boolean = true,
    val service: ServiceFirestore? = null,
    val error: String? = null
)

@HiltViewModel
class ServiceDetailViewModel @Inject constructor(
    private val servicesRepository: FirestoreServicesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServiceDetailUiState())
    val uiState: StateFlow<ServiceDetailUiState> = _uiState.asStateFlow()

    fun loadService(serviceId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val service = servicesRepository.getService(serviceId)
                if (service != null) {
                    Log.d("ServiceDetailViewModel", "Serviço carregado: ${service.title}")
                    _uiState.value = _uiState.value.copy(
                        service = service,
                        isLoading = false,
                        error = null
                    )
                } else {
                    Log.e("ServiceDetailViewModel", "Serviço não encontrado: $serviceId")
                    _uiState.value = _uiState.value.copy(
                        error = "Serviço não encontrado",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e("ServiceDetailViewModel", "Erro ao carregar serviço", e)
                _uiState.value = _uiState.value.copy(
                    error = "Erro ao carregar serviço: ${e.message}",
                    isLoading = false
                )
            }
        }
    }
}

