package com.taskgoapp.taskgo.feature.services.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskgoapp.taskgo.data.firestore.models.ServiceFirestore
import com.taskgoapp.taskgo.data.firestore.models.UserFirestore
import com.taskgoapp.taskgo.data.repository.FirestoreServicesRepository
import com.taskgoapp.taskgo.data.repository.FirestoreUserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ServiceDetailUiState(
    val isLoading: Boolean = true,
    val service: ServiceFirestore? = null,
    val provider: UserFirestore? = null,
    val error: String? = null
)

@HiltViewModel
class ServiceDetailViewModel @Inject constructor(
    private val servicesRepository: FirestoreServicesRepository,
    private val userRepository: FirestoreUserRepository,
    private val messageRepository: com.taskgoapp.taskgo.data.repository.MessageRepositoryImpl
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
                    
                    // Buscar informações do prestador
                    var provider: UserFirestore? = null
                    if (service.providerId != null && service.providerId.isNotBlank()) {
                        try {
                            provider = userRepository.getUser(service.providerId)
                            Log.d("ServiceDetailViewModel", "Prestador carregado: ${provider?.displayName}")
                        } catch (e: Exception) {
                            Log.e("ServiceDetailViewModel", "Erro ao carregar prestador", e)
                        }
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        service = service,
                        provider = provider,
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
    
    suspend fun getOrCreateThreadForProvider(providerId: String): String {
        val msgRepo = messageRepository as? com.taskgoapp.taskgo.data.repository.MessageRepositoryImpl
            ?: throw IllegalStateException("MessageRepository não é MessageRepositoryImpl")
        
        return msgRepo.getOrCreateThreadForProvider(providerId, userRepository)
    }
}

