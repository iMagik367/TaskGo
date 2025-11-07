package com.taskgoapp.taskgo.data.repository

import com.taskgoapp.taskgo.data.firestore.models.OrderFirestore
import com.taskgoapp.taskgo.data.firestore.models.ProposalDetails
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreOrderRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val ordersCollection = firestore.collection("orders")

    fun observeOrders(userId: String, role: String = "client"): Flow<List<OrderFirestore>> = callbackFlow {
        val field = if (role == "client") "clientId" else "providerId"
        val listenerRegistration = ordersCollection
            .whereEqualTo(field, userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val orders = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(OrderFirestore::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                
                trySend(orders)
            }
        
        awaitClose { listenerRegistration.remove() }
    }

    fun observeOrdersByStatus(userId: String, role: String, status: String): Flow<List<OrderFirestore>> = callbackFlow {
        val field = if (role == "client") "clientId" else "providerId"
        val listenerRegistration = ordersCollection
            .whereEqualTo(field, userId)
            .whereEqualTo("status", status)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val orders = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(OrderFirestore::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                
                trySend(orders)
            }
        
        awaitClose { listenerRegistration.remove() }
    }

    suspend fun getOrder(orderId: String): OrderFirestore? {
        return try {
            val document = ordersCollection.document(orderId).get().await()
            document.toObject(OrderFirestore::class.java)?.copy(id = document.id)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateOrderStatus(orderId: String, status: String): Result<Unit> {
        return try {
            ordersCollection.document(orderId).update(
                "status", status,
                "updatedAt", FieldValue.serverTimestamp()
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addProposal(orderId: String, proposal: ProposalDetails): Result<Unit> {
        return try {
            ordersCollection.document(orderId).update(
                "status", "proposed",
                "proposalDetails", proposal,
                "proposedAt", FieldValue.serverTimestamp(),
                "updatedAt", FieldValue.serverTimestamp()
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}





