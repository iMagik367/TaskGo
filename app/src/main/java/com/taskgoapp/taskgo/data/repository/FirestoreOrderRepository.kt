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
        try {
            val field = if (role == "client") "clientId" else "providerId"
            val listenerRegistration = ordersCollection
                .whereEqualTo(field, userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        // Log do erro mas não crashar o app
                        android.util.Log.e("FirestoreOrderRepo", "Erro ao observar ordens: ${error.message}", error)
                        // Emite lista vazia em caso de erro ao invés de fechar o channel
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    
                    val orders = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            doc.toObject(OrderFirestore::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            android.util.Log.e("FirestoreOrderRepo", "Erro ao converter documento ${doc.id}: ${e.message}", e)
                            null
                        }
                    } ?: emptyList()
                    
                    trySend(orders)
                }
            
            awaitClose { listenerRegistration.remove() }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreOrderRepo", "Erro ao configurar listener de ordens: ${e.message}", e)
            // Emite lista vazia e fecha o channel normalmente
            trySend(emptyList())
            close()
        }
    }

    fun observeOrdersByStatus(userId: String, role: String, status: String): Flow<List<OrderFirestore>> = callbackFlow {
        try {
            val field = if (role == "client") "clientId" else "providerId"
            val listenerRegistration = ordersCollection
                .whereEqualTo(field, userId)
                .whereEqualTo("status", status)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        // Log do erro mas não crashar o app
                        android.util.Log.e("FirestoreOrderRepo", "Erro ao observar ordens por status: ${error.message}", error)
                        // Emite lista vazia em caso de erro ao invés de fechar o channel
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    
                    val orders = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            doc.toObject(OrderFirestore::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            android.util.Log.e("FirestoreOrderRepo", "Erro ao converter documento ${doc.id}: ${e.message}", e)
                            null
                        }
                    } ?: emptyList()
                    
                    trySend(orders)
                }
            
            awaitClose { listenerRegistration.remove() }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreOrderRepo", "Erro ao configurar listener de ordens por status: ${e.message}", e)
            // Emite lista vazia e fecha o channel normalmente
            trySend(emptyList())
            close()
        }
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
    
    /**
     * Observa ordens de serviço disponíveis na região do usuário
     * Filtra por cidade/estado e status pending
     */
    fun observeLocalServiceOrders(
        city: String? = null,
        state: String? = null,
        category: String? = null
    ): Flow<List<OrderFirestore>> = callbackFlow {
        try {
            var query = ordersCollection
                .whereEqualTo("status", "pending")
                .whereEqualTo("deleted", false)
            
            // Filtrar por categoria se fornecida
            if (category != null && category.isNotBlank()) {
                query = query.whereEqualTo("category", category)
            }
            
            val listenerRegistration = query
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        // Log do erro mas não crashar o app
                        android.util.Log.e("FirestoreOrderRepo", "Erro ao observar ordens locais: ${error.message}", error)
                        // Emite lista vazia em caso de erro ao invés de fechar o channel
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    
                    val orders = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            doc.toObject(OrderFirestore::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            android.util.Log.e("FirestoreOrderRepo", "Erro ao converter documento ${doc.id}: ${e.message}", e)
                            null
                        }
                    }?.filter { order ->
                        // Filtrar por localização se fornecida
                        if (city != null || state != null) {
                            val location = order.location.lowercase()
                            val matchesCity = city == null || location.contains(city.lowercase())
                            val matchesState = state == null || location.contains(state.lowercase())
                            matchesCity && matchesState
                        } else {
                            true
                        }
                    } ?: emptyList()
                    
                    trySend(orders)
                }
            
            awaitClose { listenerRegistration.remove() }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreOrderRepo", "Erro ao configurar listener de ordens locais: ${e.message}", e)
            // Emite lista vazia e fecha o channel normalmente
            trySend(emptyList())
            close()
        }
    }
    
    /**
     * Busca ordens por categoria (para prestadores verem ordens na categoria deles)
     */
    fun observeOrdersByCategory(category: String): Flow<List<OrderFirestore>> = callbackFlow {
        try {
            val listenerRegistration = ordersCollection
                .whereEqualTo("category", category)
                .whereEqualTo("status", "pending")
                .whereEqualTo("deleted", false)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        // Log do erro mas não crashar o app
                        android.util.Log.e("FirestoreOrderRepo", "Erro ao observar ordens por categoria: ${error.message}", error)
                        // Emite lista vazia em caso de erro ao invés de fechar o channel
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    
                    val orders = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            doc.toObject(OrderFirestore::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            android.util.Log.e("FirestoreOrderRepo", "Erro ao converter documento ${doc.id}: ${e.message}", e)
                            null
                        }
                    } ?: emptyList()
                    
                    trySend(orders)
                }
            
            awaitClose { listenerRegistration.remove() }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreOrderRepo", "Erro ao configurar listener de ordens por categoria: ${e.message}", e)
            // Emite lista vazia e fecha o channel normalmente
            trySend(emptyList())
            close()
        }
    }
}





