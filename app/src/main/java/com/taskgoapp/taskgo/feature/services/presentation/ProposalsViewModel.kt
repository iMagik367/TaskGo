package com.taskgoapp.taskgo.feature.services.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskgoapp.taskgo.core.model.Proposal
import com.taskgoapp.taskgo.core.model.ServiceOrder
import com.taskgoapp.taskgo.domain.repository.ServiceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProposalsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val proposals: List<Proposal> = emptyList(),
    val orderId: String? = null
)

@HiltViewModel
class ProposalsViewModel @Inject constructor(
    private val serviceRepository: ServiceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProposalsUiState())
    val uiState: StateFlow<ProposalsUiState> = _uiState.asStateFlow()

    fun getProposalsForOrder(orderId: String): StateFlow<List<Proposal>> {
        return serviceRepository
            .observeProposals(orderId)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )
    }

    fun loadProposals(orderId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                orderId = orderId
            )
            try {
                // Os dados vêm automaticamente via Flow do repositório
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Erro ao carregar propostas"
                )
            }
        }
    }

    fun acceptProposal(proposalId: String) {
        viewModelScope.launch {
            try {
                serviceRepository.acceptProposal(proposalId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Erro ao aceitar proposta"
                )
            }
        }
    }
    
    fun rejectProposal(proposalId: String) {
        viewModelScope.launch {
            try {
                serviceRepository.rejectProposal(proposalId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Erro ao rejeitar proposta"
                )
            }
        }
    }

    fun refresh(orderId: String) {
        loadProposals(orderId)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

