package com.taskgoapp.taskgo.feature.checkout.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskgoapp.taskgo.core.model.Address
import com.taskgoapp.taskgo.domain.repository.AddressRepository
import com.taskgoapp.taskgo.domain.repository.UserRepository
import com.taskgoapp.taskgo.data.repository.FirebaseAuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddressFormUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

@HiltViewModel
class AddressFormViewModel @Inject constructor(
    private val addressRepository: AddressRepository,
    private val authRepository: FirebaseAuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddressFormUiState())
    val uiState: StateFlow<AddressFormUiState> = _uiState.asStateFlow()

    fun saveAddress(
        name: String,
        street: String,
        number: String,
        complement: String?,
        neighborhood: String,
        district: String,
        cep: String,
        zipCode: String,
        phone: String = ""
    ) {
        if (street.isEmpty() || number.isEmpty() || cep.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "Preencha todos os campos obrigatórios")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUser()?.uid
                    ?: throw IllegalStateException("Usuário não autenticado. Faça login novamente.")

                // CRÍTICO: Obter city/state automaticamente do perfil do usuário
                val currentUser = userRepository.observeCurrentUser().first()
                val city = currentUser?.city ?: ""
                val state = currentUser?.state ?: ""
                
                if (city.isEmpty() || state.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Localização não disponível. Por favor, atualize seu perfil."
                    )
                    return@launch
                }

                val address = Address(
                    id = "", // Novo endereço
                    name = name.ifEmpty { "Endereço ${System.currentTimeMillis()}" },
                    phone = phone,
                    street = street,
                    number = number,
                    complement = complement,
                    neighborhood = neighborhood,
                    district = district,
                    city = city, // Obtido automaticamente do perfil
                    state = state, // Obtido automaticamente do perfil
                    cep = cep,
                    zipCode = zipCode.ifEmpty { cep }
                )

                addressRepository.upsertAddress(address)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    success = true,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Erro ao salvar endereço"
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = AddressFormUiState()
    }
}

