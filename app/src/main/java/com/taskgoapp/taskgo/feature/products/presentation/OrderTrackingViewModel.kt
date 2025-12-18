package com.taskgoapp.taskgo.feature.products.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taskgoapp.taskgo.core.model.PurchaseOrder
import com.taskgoapp.taskgo.core.model.TrackingEvent
import com.taskgoapp.taskgo.data.repository.FirestoreTrackingRepository
import com.taskgoapp.taskgo.domain.repository.OrdersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class OrderTrackingUiState(
    val order: PurchaseOrder? = null,
    val trackingEvents: List<TrackingEvent> = emptyList(),
    val trackingCode: String = "",
    val carrier: String? = null,
    val trackingUrl: String? = null,
    val isLocalDelivery: Boolean = false,
    val deliveryTime: String? = null,
    val shipmentStatus: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class OrderTrackingViewModel @Inject constructor(
    private val ordersRepository: OrdersRepository,
    private val trackingRepository: FirestoreTrackingRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(OrderTrackingUiState())
    val uiState: StateFlow<OrderTrackingUiState> = _uiState.asStateFlow()
    
    fun loadOrderTracking(orderId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val order = ordersRepository.getOrder(orderId)
                if (order == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Pedido não encontrado"
                    )
                    return@launch
                }
                
                // Buscar informações de shipment
                val shipment = loadShipmentInfo(orderId)
                
                val trackingCode = shipment?.trackingCode ?: trackingRepository.generateTrackingCode(orderId)
                
                // Atualizar estado inicial
                _uiState.value = _uiState.value.copy(
                    order = order,
                    trackingCode = trackingCode,
                    carrier = shipment?.carrier,
                    trackingUrl = shipment?.customTrackingUrl,
                    isLocalDelivery = shipment?.isLocalDelivery ?: false,
                    deliveryTime = shipment?.deliveryTime,
                    shipmentStatus = shipment?.status,
                    isLoading = false
                )
                
                // Observar eventos de rastreamento em tempo real em um job separado
                viewModelScope.launch {
                    trackingRepository.observeTrackingEvents(orderId)
                        .collect { events ->
                            _uiState.value = _uiState.value.copy(
                                trackingEvents = events
                            )
                        }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Erro ao carregar rastreamento"
                )
            }
        }
    }
    
    private suspend fun loadShipmentInfo(orderId: String): ShipmentInfo? {
        return try {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val snapshot = firestore.collection("shipments")
                .whereEqualTo("purchaseOrderId", orderId)
                .limit(1)
                .get()
                .await()
            
            snapshot.documents.firstOrNull()?.let { doc ->
                ShipmentInfo(
                    trackingCode = doc.getString("trackingCode") ?: "",
                    carrier = doc.getString("carrier"),
                    customTrackingUrl = doc.getString("customTrackingUrl"),
                    isLocalDelivery = doc.getBoolean("isLocalDelivery") ?: false,
                    deliveryTime = doc.getString("deliveryTime"),
                    status = doc.getString("status")
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("OrderTrackingVM", "Erro ao buscar shipment: ${e.message}", e)
            null
        }
    }
    
    private data class ShipmentInfo(
        val trackingCode: String,
        val carrier: String?,
        val customTrackingUrl: String?,
        val isLocalDelivery: Boolean,
        val deliveryTime: String?,
        val status: String?
    )
}

