package com.taskgoapp.taskgo.feature.services.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.taskgoapp.taskgo.data.firestore.models.ServiceFirestore
import com.taskgoapp.taskgo.data.repository.FirestoreServicesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyServicesUiState(
    val isLoading: Boolean = false,
    val services: List<ServiceFirestore> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class MyServicesViewModel @Inject constructor(
    private val servicesRepository: FirestoreServicesRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyServicesUiState())
    val uiState: StateFlow<MyServicesUiState> = _uiState.asStateFlow()

    init {
        loadServices()
    }

    private fun loadServices() {
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
            
            servicesRepository.observeProviderServices(currentUser.uid)
                .catch { e ->
                    Log.e("MyServicesViewModel", "Erro ao carregar serviços", e)
                    _uiState.value = _uiState.value.copy(
                        error = "Erro ao carregar serviços: ${e.message}",
                        isLoading = false
                    )
                }
                .collect { services ->
                    Log.d("MyServicesViewModel", "Serviços carregados: ${services.size}")
                    _uiState.value = _uiState.value.copy(
                        services = services,
                        isLoading = false,
                        error = null
                    )
                }
        }
    }

    fun deleteService(serviceId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = servicesRepository.deleteService(serviceId)
            result.fold(
                onSuccess = {
                    Log.d("MyServicesViewModel", "Serviço deletado com sucesso: $serviceId")
                    // O Flow vai atualizar automaticamente a lista
                },
                onFailure = { e ->
                    Log.e("MyServicesViewModel", "Erro ao deletar serviço", e)
                    _uiState.value = _uiState.value.copy(
                        error = "Erro ao deletar serviço: ${e.message}",
                        isLoading = false
                    )
                }
            )
        }
    }

    fun refreshServices() {
        loadServices()
    }
}

