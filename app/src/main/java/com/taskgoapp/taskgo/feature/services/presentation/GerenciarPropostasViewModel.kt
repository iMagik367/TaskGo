package com.taskgoapp.taskgo.feature.services.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.taskgoapp.taskgo.data.firestore.models.OrderFirestore
import com.taskgoapp.taskgo.data.repository.FirestoreOrderRepository
import com.taskgoapp.taskgo.data.repository.FirestoreUserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

data class ProposalItemUi(
    val orderId: String,
    val proposalId: String, // Usar orderId como proposalId já que a proposta está na ordem
    val providerName: String,
    val providerId: String?,
    val serviceTitle: String,
    val serviceDescription: String,
    val price: Double,
    val status: String,
    val proposedAt: java.util.Date?
)

data class GerenciarPropostasUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val proposals: List<ProposalItemUi> = emptyList()
)

@HiltViewModel
class GerenciarPropostasViewModel @Inject constructor(
    private val orderRepository: FirestoreOrderRepository,
    private val userRepository: FirestoreUserRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(GerenciarPropostasUiState(isLoading = true))
    val uiState: StateFlow<GerenciarPropostasUiState> = _uiState.asStateFlow()

    init {
        loadProposals()
    }

    private fun loadProposals() {
        val currentUser = firebaseAuth.currentUser ?: run {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Usuário não autenticado"
            )
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                // Buscar ordens do cliente atual que têm propostas
                orderRepository.observeOrders(currentUser.uid, "client")
                    .map { orders ->
                        orders.filter { order ->
                            // Filtrar ordens que têm propostas (status = "proposed" ou têm proposalDetails)
                            (order.status == "proposed" || order.proposalDetails != null) &&
                            !order.deleted
                        }
                    }
                    .collect { ordersWithProposals ->
                        coroutineScope {
                            // Mapear ordens para propostas (buscar nomes em paralelo)
                            val proposals = ordersWithProposals.mapNotNull { order ->
                                val proposalDetails = order.proposalDetails ?: return@mapNotNull null
                                val providerId = order.providerId

                                ProposalItemUi(
                                    orderId = order.id,
                                    proposalId = order.id, // Usar orderId como ID da proposta
                                    providerName = "Prestador", // Será atualizado abaixo
                                    providerId = providerId,
                                    serviceTitle = order.details.takeIf { it.isNotBlank() } ?: "Serviço",
                                    serviceDescription = proposalDetails.description.takeIf { it.isNotBlank() } ?: order.details,
                                    price = proposalDetails.price,
                                    status = when (order.status) {
                                        "proposed" -> "Pendente"
                                        "accepted" -> "Aceita"
                                        "rejected" -> "Recusada"
                                        else -> order.status.replaceFirstChar { it.uppercaseChar() }
                                    },
                                    proposedAt = order.proposedAt
                                )
                            }

                            // Buscar nomes dos prestadores em paralelo
                            val proposalsWithNames = proposals.map { proposal ->
                                if (proposal.providerId != null) {
                                    async(Dispatchers.IO) {
                                        try {
                                            val provider = userRepository.getUser(proposal.providerId)
                                            proposal.copy(providerName = provider?.displayName ?: "Prestador")
                                        } catch (e: Exception) {
                                            proposal.copy(providerName = "Prestador")
                                        }
                                    }
                                } else {
                                    async { proposal }
                                }
                            }.awaitAll()

                            _uiState.value = _uiState.value.copy(
                                proposals = proposalsWithNames.sortedByDescending { it.proposedAt?.time ?: 0L },
                                isLoading = false,
                                error = null
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Erro ao carregar propostas: ${e.message}"
                )
            }
        }
    }

    fun refresh() {
        loadProposals()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

