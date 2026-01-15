package com.taskgoapp.taskgo.data.repository

import com.taskgoapp.taskgo.data.firestore.models.OrderFirestore
import com.taskgoapp.taskgo.data.firestore.models.ProposalDetails
import com.taskgoapp.taskgo.core.model.Result
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
    private val firestore: FirebaseFirestore,
    private val authRepository: FirebaseAuthRepository
) {
    // Coleção pública para queries (prestadores precisam ver ordens pendentes)
    private val publicOrdersCollection = firestore.collection("orders")
    
    // Helper para obter subcoleção do usuário
    private fun getUserOrdersCollection(userId: String) = 
        firestore.collection("users").document(userId).collection("orders")

    /**
     * Observa ordens de um usuário (cliente ou prestador)
     * Para clientes: busca na subcoleção users/{userId}/orders
     * Para prestadores: busca na coleção pública onde providerId == userId
     */
    fun observeOrders(userId: String, role: String = "client"): Flow<List<OrderFirestore>> = callbackFlow {
        try {
            if (role == "client") {
                // Cliente: busca na subcoleção própria
                val userOrdersCollection = getUserOrdersCollection(userId)
                val listenerRegistration = userOrdersCollection
                    .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            android.util.Log.e("FirestoreOrderRepo", "Erro ao observar ordens do cliente: ${error.message}", error)
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
            } else {
                // Parceiro/Prestador: busca na coleção pública onde providerId == userId (tratar "partner", "provider" e "seller" da mesma forma)
                val listenerRegistration = publicOrdersCollection
                    .whereEqualTo("providerId", userId)
                    .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            android.util.Log.e("FirestoreOrderRepo", "Erro ao observar ordens do prestador: ${error.message}", error)
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
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreOrderRepo", "Erro ao configurar listener de ordens: ${e.message}", e)
            trySend(emptyList())
            close()
        }
    }

    /**
     * Observa ordens por status
     * Para clientes: busca na subcoleção
     * Para prestadores: busca na coleção pública
     */
    fun observeOrdersByStatus(userId: String, role: String, status: String): Flow<List<OrderFirestore>> = callbackFlow {
        try {
            if (role == "client") {
                // Cliente: busca na subcoleção própria
                val userOrdersCollection = getUserOrdersCollection(userId)
                val listenerRegistration = userOrdersCollection
                    .whereEqualTo("status", status)
                    .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            android.util.Log.e("FirestoreOrderRepo", "Erro ao observar ordens por status: ${error.message}", error)
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
            } else {
                // Parceiro/Prestador: busca na coleção pública (tratar "partner" e "provider" da mesma forma)
                val listenerRegistration = publicOrdersCollection
                    .whereEqualTo("providerId", userId)
                    .whereEqualTo("status", status)
                    .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            android.util.Log.e("FirestoreOrderRepo", "Erro ao observar ordens por status: ${error.message}", error)
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
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreOrderRepo", "Erro ao configurar listener de ordens por status: ${e.message}", e)
            trySend(emptyList())
            close()
        }
    }

    /**
     * Busca uma ordem por ID
     * Tenta primeiro na coleção pública
     */
    suspend fun getOrder(orderId: String): OrderFirestore? {
        return try {
            val document = publicOrdersCollection.document(orderId).get().await()
            if (document.exists()) {
                document.toObject(OrderFirestore::class.java)?.copy(id = document.id)
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreOrderRepo", "Erro ao buscar ordem: ${e.message}", e)
            null
        }
    }

    /**
     * Atualiza status de uma ordem
     * Atualiza tanto na subcoleção do cliente quanto na coleção pública
     */
    suspend fun updateOrderStatus(orderId: String, status: String): Result<Unit> {
        return try {
            // Buscar ordem para obter clientId
            val order = getOrder(orderId)
            if (order == null) {
                return Result.Error(Exception("Ordem não encontrada"))
            }
            
            // Atualizar na coleção pública (sempre)
            publicOrdersCollection.document(orderId).update(
                "status", status,
                "updatedAt", FieldValue.serverTimestamp()
            ).await()
            
            // Atualizar também na subcoleção do cliente se existir
            if (order.clientId.isNotBlank()) {
                try {
                    val userOrdersCollection = getUserOrdersCollection(order.clientId)
                    userOrdersCollection.document(orderId).update(
                        "status", status,
                        "updatedAt", FieldValue.serverTimestamp()
                    ).await()
                } catch (e: Exception) {
                    android.util.Log.w("FirestoreOrderRepo", "Erro ao atualizar na subcoleção do cliente: ${e.message}")
                }
            }
            
            Result.Success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreOrderRepo", "Erro ao atualizar status da ordem: ${e.message}", e)
            Result.Error(e)
        }
    }

    /**
     * Adiciona proposta a uma ordem
     * Atualiza tanto na subcoleção do cliente quanto na coleção pública
     */
    suspend fun addProposal(orderId: String, proposal: ProposalDetails): Result<Unit> {
        return try {
            // Buscar ordem para obter clientId
            val order = getOrder(orderId)
            if (order == null) {
                return Result.Error(Exception("Ordem não encontrada"))
            }
            
            // Atualizar na coleção pública
            publicOrdersCollection.document(orderId).update(
                "status", "proposed",
                "proposalDetails", proposal,
                "proposedAt", FieldValue.serverTimestamp(),
                "updatedAt", FieldValue.serverTimestamp()
            ).await()
            
            // Atualizar também na subcoleção do cliente
            if (order.clientId.isNotBlank()) {
                try {
                    val userOrdersCollection = getUserOrdersCollection(order.clientId)
                    userOrdersCollection.document(orderId).update(
                        "status", "proposed",
                        "proposalDetails", proposal,
                        "proposedAt", FieldValue.serverTimestamp(),
                        "updatedAt", FieldValue.serverTimestamp()
                    ).await()
                } catch (e: Exception) {
                    android.util.Log.w("FirestoreOrderRepo", "Erro ao atualizar na subcoleção do cliente: ${e.message}")
                }
            }
            
            Result.Success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreOrderRepo", "Erro ao adicionar proposta: ${e.message}", e)
            Result.Error(e)
        }
    }
    
    /**
     * Observa ordens de serviço disponíveis na região do usuário
     * Usa coleção pública para prestadores verem ordens pendentes
     */
    fun observeLocalServiceOrders(
        city: String? = null,
        state: String? = null,
        category: String? = null
    ): Flow<List<OrderFirestore>> = callbackFlow {
        try {
            var query = publicOrdersCollection
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
                        android.util.Log.e("FirestoreOrderRepo", "Erro ao observar ordens locais: ${error.message}", error)
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
            trySend(emptyList())
            close()
        }
    }
    
    /**
     * Busca ordens por categoria (para prestadores verem ordens na categoria deles)
     * Usa coleção pública
     */
    fun observeOrdersByCategory(category: String): Flow<List<OrderFirestore>> = callbackFlow {
        try {
            val listenerRegistration = publicOrdersCollection
                .whereEqualTo("category", category)
                .whereEqualTo("status", "pending")
                .whereEqualTo("deleted", false)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.e("FirestoreOrderRepo", "Erro ao observar ordens por categoria: ${error.message}", error)
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
            trySend(emptyList())
            close()
        }
    }
    
    /**
     * Cria uma nova ordem (chamado por Cloud Function ou pelo app)
     * Salva tanto na subcoleção do cliente quanto na coleção pública
     */
    suspend fun createOrder(order: OrderFirestore): Result<String> {
        return try {
            val currentUserId = authRepository.getCurrentUser()?.uid
                ?: return Result.Error(Exception("Usuário não autenticado"))
            
            // Garantir que clientId corresponde ao usuário atual
            if (order.clientId != currentUserId) {
                return Result.Error(Exception("clientId não corresponde ao usuário atual"))
            }
            
            // Criar na subcoleção do cliente
            val userOrdersCollection = getUserOrdersCollection(order.clientId)
            val docRef = userOrdersCollection.add(order).await()
            val orderId = docRef.id
            
            // Criar também na coleção pública (para prestadores verem)
            try {
                publicOrdersCollection.document(orderId).set(order).await()
            } catch (e: Exception) {
                android.util.Log.w("FirestoreOrderRepo", "Erro ao salvar na coleção pública: ${e.message}")
            }
            
            Result.Success(orderId)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreOrderRepo", "Erro ao criar ordem: ${e.message}", e)
            Result.Error(e)
        }
    }
    
    /**
     * Aceita serviço pelo prestador
     * Marca acceptedByProvider = true e verifica se ambos aceitaram para mudar status para in_progress
     */
    suspend fun acceptServiceByProvider(orderId: String): Result<Unit> {
        return try {
            val currentUserId = authRepository.getCurrentUser()?.uid
                ?: return Result.Error(Exception("Usuário não autenticado"))
            
            // Buscar ordem para validar
            val order = getOrder(orderId)
            if (order == null) {
                return Result.Error(Exception("Ordem não encontrada"))
            }
            
            // Validar que o usuário atual é o provider da ordem
            if (order.providerId != currentUserId) {
                return Result.Error(Exception("Apenas o prestador da ordem pode aceitar o serviço"))
            }
            
            // Verificar se já foi aceito pelo provider
            if (order.acceptedByProvider) {
                return Result.Success(Unit) // Já foi aceito, nada a fazer
            }
            
            // Preparar dados de atualização
            val updateData = hashMapOf<String, Any>(
                "acceptedByProvider" to true,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            
            // Se ambos já aceitaram (cliente já aceitou), mudar status para in_progress
            if (order.acceptedByClient) {
                updateData["status"] = "in_progress"
                updateData["acceptedAt"] = FieldValue.serverTimestamp()
            }
            
            // Atualizar na coleção pública
            publicOrdersCollection.document(orderId).update(updateData).await()
            
            // Atualizar também na subcoleção do cliente
            if (order.clientId.isNotBlank()) {
                try {
                    val userOrdersCollection = getUserOrdersCollection(order.clientId)
                    userOrdersCollection.document(orderId).update(updateData).await()
                } catch (e: Exception) {
                    android.util.Log.w("FirestoreOrderRepo", "Erro ao atualizar na subcoleção do cliente: ${e.message}")
                }
            }
            
            Result.Success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreOrderRepo", "Erro ao aceitar serviço pelo prestador: ${e.message}", e)
            Result.Error(e)
        }
    }
    
    /**
     * Aceita orçamento pelo cliente
     * Marca acceptedByClient = true e verifica se ambos aceitaram para mudar status para in_progress
     */
    suspend fun acceptQuoteByClient(orderId: String): Result<Unit> {
        return try {
            val currentUserId = authRepository.getCurrentUser()?.uid
                ?: return Result.Error(Exception("Usuário não autenticado"))
            
            // Buscar ordem para validar
            val order = getOrder(orderId)
            if (order == null) {
                return Result.Error(Exception("Ordem não encontrada"))
            }
            
            // Validar que o usuário atual é o client da ordem
            if (order.clientId != currentUserId) {
                return Result.Error(Exception("Apenas o cliente da ordem pode aceitar o orçamento"))
            }
            
            // Verificar se já foi aceito pelo cliente
            if (order.acceptedByClient) {
                return Result.Success(Unit) // Já foi aceito, nada a fazer
            }
            
            // Preparar dados de atualização
            val updateData = hashMapOf<String, Any>(
                "acceptedByClient" to true,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            
            // Se ambos já aceitaram (prestador já aceitou), mudar status para in_progress
            if (order.acceptedByProvider) {
                updateData["status"] = "in_progress"
                updateData["acceptedAt"] = FieldValue.serverTimestamp()
            }
            
            // Atualizar na coleção pública
            publicOrdersCollection.document(orderId).update(updateData).await()
            
            // Atualizar também na subcoleção do cliente
            try {
                val userOrdersCollection = getUserOrdersCollection(order.clientId)
                userOrdersCollection.document(orderId).update(updateData).await()
            } catch (e: Exception) {
                android.util.Log.w("FirestoreOrderRepo", "Erro ao atualizar na subcoleção do cliente: ${e.message}")
            }
            
            Result.Success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreOrderRepo", "Erro ao aceitar orçamento pelo cliente: ${e.message}", e)
            Result.Error(e)
        }
    }
    
    /**
     * Cancela uma ordem com motivo e valor de reembolso
     */
    suspend fun cancelOrder(orderId: String, reason: String, refundAmount: Double?): Result<Unit> {
        return try {
            val currentUserId = authRepository.getCurrentUser()?.uid
                ?: return Result.Error(Exception("Usuário não autenticado"))
            
            // Buscar ordem para validar
            val order = getOrder(orderId)
            if (order == null) {
                return Result.Error(Exception("Ordem não encontrada"))
            }
            
            // Validar que o usuário atual é o provider ou client da ordem
            if (order.providerId != currentUserId && order.clientId != currentUserId) {
                return Result.Error(Exception("Apenas o prestador ou cliente da ordem podem cancelar"))
            }
            
            // Validar que a ordem está em andamento
            if (order.status != "in_progress") {
                return Result.Error(Exception("Apenas ordens em andamento podem ser canceladas"))
            }
            
            // Preparar dados de atualização
            val updateData = hashMapOf<String, Any>(
                "status" to "cancelled",
                "cancelledReason" to reason,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            
            if (refundAmount != null && refundAmount > 0) {
                updateData["cancelledRefundAmount"] = refundAmount
            }
            
            // Atualizar na coleção pública
            publicOrdersCollection.document(orderId).update(updateData).await()
            
            // Atualizar também na subcoleção do cliente
            if (order.clientId.isNotBlank()) {
                try {
                    val userOrdersCollection = getUserOrdersCollection(order.clientId)
                    userOrdersCollection.document(orderId).update(updateData).await()
                } catch (e: Exception) {
                    android.util.Log.w("FirestoreOrderRepo", "Erro ao atualizar na subcoleção do cliente: ${e.message}")
                }
            }
            
            Result.Success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreOrderRepo", "Erro ao cancelar ordem: ${e.message}", e)
            Result.Error(e)
        }
    }
    
    /**
     * Conclui uma ordem com descrição, tempo e URLs das mídias
     */
    suspend fun completeOrder(
        orderId: String,
        description: String,
        time: String,
        mediaUrls: List<String>
    ): Result<Unit> {
        return try {
            val currentUserId = authRepository.getCurrentUser()?.uid
                ?: return Result.Error(Exception("Usuário não autenticado"))
            
            // Buscar ordem para validar
            val order = getOrder(orderId)
            if (order == null) {
                return Result.Error(Exception("Ordem não encontrada"))
            }
            
            // Validar que o usuário atual é o provider da ordem
            if (order.providerId != currentUserId) {
                return Result.Error(Exception("Apenas o prestador da ordem pode concluir o serviço"))
            }
            
            // Validar que a ordem está em andamento
            if (order.status != "in_progress") {
                return Result.Error(Exception("Apenas ordens em andamento podem ser concluídas"))
            }
            
            // Preparar dados de atualização
            val updateData = hashMapOf<String, Any>(
                "status" to "completed",
                "completedDescription" to description,
                "completedTime" to time,
                "completedMediaUrls" to mediaUrls,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            
            // Atualizar na coleção pública
            publicOrdersCollection.document(orderId).update(updateData).await()
            
            // Atualizar também na subcoleção do cliente
            if (order.clientId.isNotBlank()) {
                try {
                    val userOrdersCollection = getUserOrdersCollection(order.clientId)
                    userOrdersCollection.document(orderId).update(updateData).await()
                } catch (e: Exception) {
                    android.util.Log.w("FirestoreOrderRepo", "Erro ao atualizar na subcoleção do cliente: ${e.message}")
                }
            }
            
            Result.Success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreOrderRepo", "Erro ao concluir ordem: ${e.message}", e)
            Result.Error(e)
        }
    }
}
