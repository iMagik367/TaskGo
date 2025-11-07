package com.taskgoapp.taskgo.domain.usecase

import com.taskgoapp.taskgo.core.model.TrackingEvent
import com.taskgoapp.taskgo.domain.repository.OrdersRepository
import com.taskgoapp.taskgo.domain.repository.TrackingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TrackingUseCase @Inject constructor(
    private val ordersRepository: OrdersRepository,
    private val trackingRepository: TrackingRepository
) {
    
    fun observeOrderTracking(orderId: String): Flow<OrderTrackingInfo> {
        return trackingRepository.observeTrackingEvents(orderId).map { events: List<TrackingEvent> ->
            val order = kotlinx.coroutines.runBlocking {
                ordersRepository.getOrder(orderId)
            }
            OrderTrackingInfo(
                order = order!!,
                events = events,
                trackingCode = generateTrackingCode(orderId),
                currentStatus = getCurrentStatus(events)
            )
        }
    }
    
    private fun generateTrackingCode(orderId: String): String {
        // Generate a tracking code like "LP123456789BR"
        val prefix = "LP"
        val suffix = "BR"
        val middle = orderId.hashCode().toString().padStart(9, '0').take(9)
        return "$prefix$middle$suffix"
    }
    
    private fun getCurrentStatus(events: List<TrackingEvent>): String {
        val completedEvents = events.filter { it.done }
        return when (completedEvents.size) {
            0 -> "Processando"
            1 -> "Postado"
            2 -> "Em trânsito"
            3 -> "Saiu para entrega"
            4 -> "Entregue"
            else -> "Processando"
        }
    }
    
    suspend fun updateTrackingStatus(orderId: String, eventId: String, done: Boolean) {
        trackingRepository.updateEventDone(eventId, done)
    }
}

data class OrderTrackingInfo(
    val order: com.taskgoapp.taskgo.core.model.PurchaseOrder,
    val events: List<TrackingEvent>,
    val trackingCode: String,
    val currentStatus: String
)
