package com.taskgoapp.taskgo.data.repository

import com.taskgoapp.taskgo.core.model.TrackingEvent
import com.taskgoapp.taskgo.data.firestore.models.TrackingEventFirestore
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreTrackingRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val trackingCollection = firestore.collection("tracking_events")
    
    /**
     * Observa eventos de rastreamento de um pedido em tempo real
     */
    fun observeTrackingEvents(orderId: String): Flow<List<TrackingEvent>> = callbackFlow {
        val listenerRegistration = trackingCollection
            .whereEqualTo("orderId", orderId)
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val events = snapshot?.documents?.mapNotNull { doc ->
                    val event = doc.toObject(TrackingEventFirestore::class.java)
                        ?.copy(id = doc.id)
                    event?.toModel()
                } ?: emptyList()
                
                trySend(events)
            }
        
        awaitClose { listenerRegistration.remove() }
    }
    
    /**
     * Cria um evento de rastreamento
     */
    suspend fun createTrackingEvent(
        orderId: String,
        type: String,
        description: String,
        location: com.taskgoapp.taskgo.data.firestore.models.TrackingLocation? = null,
        eventId: String? = null,
        done: Boolean = false
    ): String {
        val event = TrackingEventFirestore(
            id = eventId ?: "",
            orderId = orderId,
            type = type,
            description = description,
            location = location,
            timestamp = java.util.Date(),
            done = done
        )
        
        return if (eventId != null) {
            trackingCollection.document(eventId)
                .set(event.copy(id = eventId))
                .await()
            eventId
        } else {
            val docRef = trackingCollection.add(event).await()
            docRef.id
        }
    }
    
    /**
     * Atualiza status de um evento
     */
    suspend fun updateEventDone(eventId: String, done: Boolean) {
        trackingCollection.document(eventId).update("done", done).await()
    }
    
    /**
     * Gera código de rastreamento único
     */
    fun generateTrackingCode(orderId: String): String {
        val prefix = "TG"
        val suffix = "BR"
        val middle = orderId.hashCode().toString().padStart(9, '0').take(9)
        return "$prefix$middle$suffix"
    }
}

// Extensão para converter TrackingEventFirestore para TrackingEvent
private fun TrackingEventFirestore.toModel(): TrackingEvent {
    return TrackingEvent(
        label = description,
        date = timestamp?.time ?: System.currentTimeMillis(),
        done = done
    )
}

