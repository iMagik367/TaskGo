package com.taskgoapp.taskgo.data.repository

import com.taskgoapp.taskgo.core.model.TrackingEvent
import com.taskgoapp.taskgo.domain.repository.TrackingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Adapter que implementa TrackingRepository usando FirestoreTrackingRepository
 */
class FirestoreTrackingRepositoryAdapter @Inject constructor(
    private val firestoreTrackingRepository: FirestoreTrackingRepository
) : TrackingRepository {
    
    override fun observeTrackingEvents(orderId: String): Flow<List<TrackingEvent>> {
        return firestoreTrackingRepository.observeTrackingEvents(orderId)
    }
    
    override suspend fun seedTimeline(orderId: String) {
        val seeds = listOf(
            TrackingSeed(
                id = "created_$orderId",
                type = "ORDER_CREATED",
                description = "Pedido criado e aguardando confirmação",
                done = true
            ),
            TrackingSeed(
                id = "payment_$orderId",
                type = "PAYMENT_CONFIRMED",
                description = "Pagamento confirmado",
                done = false
            ),
            TrackingSeed(
                id = "shipped_$orderId",
                type = "ORDER_SHIPPED",
                description = "Pedido enviado",
                done = false
            ),
            TrackingSeed(
                id = "transit_$orderId",
                type = "IN_TRANSIT",
                description = "Pedido em trânsito",
                done = false
            ),
            TrackingSeed(
                id = "delivery_$orderId",
                type = "OUT_FOR_DELIVERY",
                description = "Saiu para entrega",
                done = false
            ),
            TrackingSeed(
                id = "delivered_$orderId",
                type = "DELIVERED",
                description = "Pedido entregue",
                done = false
            )
        )
        
        seeds.forEach { seed ->
            firestoreTrackingRepository.createTrackingEvent(
                orderId = orderId,
                type = seed.type,
                description = seed.description,
                eventId = seed.id,
                done = seed.done
            )
        }
    }
    
    override suspend fun updateEventDone(eventId: String, done: Boolean) {
        firestoreTrackingRepository.updateEventDone(eventId, done)
    }
}

private data class TrackingSeed(
    val id: String,
    val type: String,
    val description: String,
    val done: Boolean
)

