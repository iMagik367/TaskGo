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
import com.taskgoapp.taskgo.core.firebase.LocationHelper
import kotlinx.coroutines.flow.first
import android.util.Log
import com.taskgoapp.taskgo.domain.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreOrderRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: FirebaseAuthRepository,
    private val userRepository: UserRepository
) {
    // REMOVIDO: Coleção global - orders estão apenas em locations/{locationId}/orders
    
    // REMOVIDO: getUserOrdersCollection - orders estão apenas em locations/{locationId}/orders

    /**
     * Observa ordens de um usuário (cliente ou prestador)
     * Para clientes: busca na subcoleção users/{userId}/orders
     * Para prestadores: busca na coleção pública onde providerId == userId
     */
    fun observeOrders(userId: String, role: String = "client"): Flow<List<OrderFirestore>> = callbackFlow {
        try {
            val currentUser = userRepository.observeCurrentUser().first()
                ?: throw Exception("Usuário não autenticado")
            
            val userCity = currentUser.city?.takeIf { it.isNotBlank() }
                ?: throw Exception("Usuário não possui city no cadastro. Complete seu perfil.")
            val userState = currentUser.state?.takeIf { it.isNotBlank() }
                ?: throw Exception("Usuário não possui state no cadastro. Complete seu perfil.")
            
            val locationId = LocationHelper.normalizeLocationId(userCity, userState)
            val locationCollection = LocationHelper.getLocationCollection(
                firestore,
                "orders",
                userCity,
                userState
            )
            
            if (role == "client" || role == "user") {
                // Cliente observa orders onde clientId == userId
                android.util.Log.d("FirestoreOrderRepo", "📍 Observando ordens do cliente: locations/$locationId/orders")
                val listenerRegistration = locationCollection
                    .whereEqualTo("clientId", userId)
                    .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            android.util.Log.e("FirestoreOrderRepo", "❌ Erro ao observar ordens do cliente: ${error.message}", error)
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
                        
                        android.util.Log.d("FirestoreOrderRepo", "📦 ${orders.size} ordens encontradas para cliente $userId")
                        trySend(orders)
                    }
                
                awaitClose { listenerRegistration.remove() }
            } else {
                // Parceiro/Prestador: busca na coleção por localização onde providerId == userId
                android.util.Log.d("FirestoreOrderRepo", "📍 Observando ordens do prestador: locations/$locationId/orders")
                val listenerRegistration = locationCollection
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
            val currentUser = userRepository.observeCurrentUser().first()
                ?: throw Exception("Usuário não autenticado")
            
            val userCity = currentUser.city?.takeIf { it.isNotBlank() }
                ?: throw Exception("Usuário não possui city no cadastro. Complete seu perfil.")
            val userState = currentUser.state?.takeIf { it.isNotBlank() }
                ?: throw Exception("Usuário não possui state no cadastro. Complete seu perfil.")
            
            val locationId = LocationHelper.normalizeLocationId(userCity, userState)
            val locationCollection = LocationHelper.getLocationCollection(
                firestore,
                "orders",
                userCity,
                userState
            )
            
            if (role == "client" || role == "user") {
                android.util.Log.d("FirestoreOrderRepo", "📍 Observando ordens do cliente por status: locations/$locationId/orders")
                val listenerRegistration = locationCollection
                    .whereEqualTo("clientId", userId)
                    .whereEqualTo("status", status)
                    .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            android.util.Log.e("FirestoreOrderRepo", "❌ Erro ao observar ordens por status: ${error.message}", error)
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
                        
                        android.util.Log.d("FirestoreOrderRepo", "📦 ${orders.size} ordens encontradas para cliente $userId com status $status")
                        trySend(orders)
                    }
                
                awaitClose { listenerRegistration.remove() }
            } else {
                // Parceiro/Prestador: busca na coleção por localização
                android.util.Log.d("FirestoreOrderRepo", "📍 Observando ordens do prestador por status: locations/$locationId/orders")
                val listenerRegistration = locationCollection
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
            val currentUser = userRepository.observeCurrentUser().first()
                ?: throw Exception("Usuário não autenticado")
            
            val userCity = currentUser.city?.takeIf { it.isNotBlank() }
                ?: throw Exception("Usuário não possui city no cadastro. Complete seu perfil.")
            val userState = currentUser.state?.takeIf { it.isNotBlank() }
                ?: throw Exception("Usuário não possui state no cadastro. Complete seu perfil.")
            
            val locationId = LocationHelper.normalizeLocationId(userCity, userState)
            val locationCollection = LocationHelper.getLocationCollection(
                firestore,
                "orders",
                userCity,
                userState
            )
            val document = locationCollection.document(orderId).get().await()
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
            
            val currentUser = userRepository.observeCurrentUser().first()
                ?: return Result.Error(Exception("Usuário não autenticado"))
            
            val userCity = currentUser.city?.takeIf { it.isNotBlank() }
                ?: return Result.Error(Exception("Usuário não possui city no cadastro. Complete seu perfil."))
            val userState = currentUser.state?.takeIf { it.isNotBlank() }
                ?: return Result.Error(Exception("Usuário não possui state no cadastro. Complete seu perfil."))
            
            val locationCollection = LocationHelper.getLocationCollection(
                firestore,
                "orders",
                userCity,
                userState
            )
            locationCollection.document(orderId).update(
                "status", status,
                "updatedAt", FieldValue.serverTimestamp()
            ).await()
            
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
            
            val currentUser = userRepository.observeCurrentUser().first()
                ?: return Result.Error(Exception("Usuário não autenticado"))
            
            val userCity = currentUser.city?.takeIf { it.isNotBlank() }
                ?: return Result.Error(Exception("Usuário não possui city no cadastro. Complete seu perfil."))
            val userState = currentUser.state?.takeIf { it.isNotBlank() }
                ?: return Result.Error(Exception("Usuário não possui state no cadastro. Complete seu perfil."))
            
            val locationCollection = LocationHelper.getLocationCollection(
                firestore,
                "orders",
                userCity,
                userState
            )
            locationCollection.document(orderId).update(
                "status", "proposed",
                "proposalDetails", proposal,
                "proposedAt", FieldValue.serverTimestamp(),
                "updatedAt", FieldValue.serverTimestamp()
            ).await()
            
            Result.Success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreOrderRepo", "Erro ao adicionar proposta: ${e.message}", e)
            Result.Error(e)
        }
    }
    
    /**
     * Observa ordens de serviço disponíveis na região do usuário
     * ✅ Agora usa city/state do perfil do usuário e coleção por localização locations/{locationId}/orders
     */
    fun observeLocalServiceOrders(
        category: String? = null
    ): Flow<List<OrderFirestore>> = callbackFlow {
        var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
        try {
            val currentUser = userRepository.observeCurrentUser().first()
                ?: throw Exception("Usuário não autenticado")
            
            val userCity = currentUser.city?.takeIf { it.isNotBlank() }
                ?: throw Exception("Usuário não possui city no cadastro. Complete seu perfil.")
            val userState = currentUser.state?.takeIf { it.isNotBlank() }
                ?: throw Exception("Usuário não possui state no cadastro. Complete seu perfil.")
            
            val locationId = LocationHelper.normalizeLocationId(userCity, userState)
            val locationOrdersCollection = LocationHelper.getLocationCollection(
                firestore,
                "orders",
                userCity,
                userState
            )
            
            Log.d("FirestoreOrderRepo", """
                📍 Querying Firestore with location:
                City: $userCity
                State: $userState
                LocationId: $locationId
                Category: $category
                Firestore Path: locations/$locationId/orders
            """.trimIndent())
                
                // CRÍTICO: Mostrar apenas ordens disponíveis (status = pending, providerId == null, deleted = false)
                // Ordens com providerId já foram aceitas por algum prestador
                var query = locationOrdersCollection
                    .whereEqualTo("status", "pending")
                    .whereEqualTo("deleted", false)
                    .whereEqualTo("providerId", null) // Apenas ordens disponíveis (não aceitas)
                
                // Filtrar por categoria se fornecida
                if (category != null && category.isNotBlank()) {
                    query = query.whereEqualTo("category", category)
                }
                
                listenerRegistration = query
                    .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            android.util.Log.e("FirestoreOrderRepo", "❌ Erro ao observar ordens locais por localização: ${error.message}", error)
                            try {
                                trySend(emptyList())
                            } catch (e: kotlinx.coroutines.channels.ClosedSendChannelException) {
                                // Canal já foi fechado, ignorar
                            } catch (e: Exception) {
                                android.util.Log.w("FirestoreOrderRepo", "Erro ao enviar dados (canal pode estar fechado): ${e.message}")
                            }
                            return@addSnapshotListener
                        }
                        
                        try {
                            val orders = snapshot?.documents?.mapNotNull { doc ->
                                try {
                                    doc.toObject(OrderFirestore::class.java)?.copy(id = doc.id)
                                } catch (e: Exception) {
                                    android.util.Log.e("FirestoreOrderRepo", "Erro ao converter documento ${doc.id}: ${e.message}", e)
                                    null
                                }
                            } ?: emptyList()
                            
                            Log.d("FirestoreOrderRepo", "📦 ${orders.size} ordens encontradas na localização $userCity, $userState")
                            trySend(orders)
                        } catch (e: kotlinx.coroutines.channels.ClosedSendChannelException) {
                            // Canal já foi fechado, ignorar
                        } catch (e: Exception) {
                            android.util.Log.w("FirestoreOrderRepo", "Erro ao enviar dados (canal pode estar fechado): ${e.message}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreOrderRepo", "Erro ao configurar listener de ordens locais: ${e.message}", e)
            try {
                trySend(emptyList())
            } catch (ex: Exception) {
                // Ignorar se não conseguir enviar
            }
        }
        
        awaitClose { 
            try {
                listenerRegistration?.remove()
            } catch (e: Exception) {
                android.util.Log.w("FirestoreOrderRepo", "Erro ao remover listener: ${e.message}")
            }
        }
    }
    
    /**
     * Busca ordens por categoria (para prestadores verem ordens na categoria deles)
     * Usa coleção pública
     */
    fun observeOrdersByCategory(category: String): Flow<List<OrderFirestore>> = callbackFlow {
        try {
            val currentUser = userRepository.observeCurrentUser().first()
                ?: throw Exception("Usuário não autenticado")
            
            val userCity = currentUser.city?.takeIf { it.isNotBlank() }
                ?: throw Exception("Usuário não possui city no cadastro. Complete seu perfil.")
            val userState = currentUser.state?.takeIf { it.isNotBlank() }
                ?: throw Exception("Usuário não possui state no cadastro. Complete seu perfil.")
            
            val locationId = LocationHelper.normalizeLocationId(userCity, userState)
            val locationCollection = LocationHelper.getLocationCollection(
                firestore,
                "orders",
                userCity,
                userState
            )
            // CRÍTICO: Mostrar apenas ordens disponíveis (status = pending, providerId == null, deleted = false)
            val listenerRegistration = locationCollection
                .whereEqualTo("category", category)
                .whereEqualTo("status", "pending")
                .whereEqualTo("deleted", false)
                .whereEqualTo("providerId", null) // Apenas ordens disponíveis (não aceitas)
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
            
            // REMOVIDO: createOrder não deve ser chamado diretamente
            // Orders devem ser criadas via Cloud Function createOrder que salva em locations/{locationId}/orders
            // Este método está aqui apenas para compatibilidade, mas não deve ser usado
            android.util.Log.w("FirestoreOrderRepo", "⚠️ createOrder não deve ser usado diretamente. Use Cloud Function createOrder.")
            return Result.Error(Exception("Use Cloud Function createOrder para criar orders"))
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
            
            // Preparar dados de atualização
            val updateData = hashMapOf<String, Any>(
                "updatedAt" to FieldValue.serverTimestamp()
            )
            
            // Se a ordem não tem providerId (ordem aberta), definir providerId = currentUserId
            if (order.providerId.isNullOrBlank()) {
                updateData["providerId"] = currentUserId
                updateData["status"] = "accepted" // Mudar status para accepted quando parceiro aceita
                updateData["acceptedByProvider"] = true
                updateData["acceptedAt"] = FieldValue.serverTimestamp()
            } else {
                // Ordem já tem providerId - validar que é o provider correto
                if (order.providerId != currentUserId) {
                    return Result.Error(Exception("Apenas o prestador da ordem pode aceitar o serviço"))
                }
                
                // Verificar se já foi aceito pelo provider
                if (order.acceptedByProvider) {
                    return Result.Success(Unit) // Já foi aceito, nada a fazer
                }
                
                // Marcar como aceito pelo provider
                updateData["acceptedByProvider"] = true
                
                // Se ambos já aceitaram (cliente já aceitou), mudar status para in_progress
                if (order.acceptedByClient) {
                    updateData["status"] = "in_progress"
                    updateData["acceptedAt"] = FieldValue.serverTimestamp()
                }
            }
            
            val currentUser = userRepository.observeCurrentUser().first()
                ?: return Result.Error(Exception("Usuário não autenticado"))
            
            val userCity = currentUser.city?.takeIf { it.isNotBlank() }
                ?: return Result.Error(Exception("Usuário não possui city no cadastro. Complete seu perfil."))
            val userState = currentUser.state?.takeIf { it.isNotBlank() }
                ?: return Result.Error(Exception("Usuário não possui state no cadastro. Complete seu perfil."))
            
            val locationCollection = LocationHelper.getLocationCollection(
                firestore,
                "orders",
                userCity,
                userState
            )
            locationCollection.document(orderId).update(updateData).await()
            
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
            
            val currentUser = userRepository.observeCurrentUser().first()
                ?: return Result.Error(Exception("Usuário não autenticado"))
            
            val userCity = currentUser.city?.takeIf { it.isNotBlank() }
                ?: return Result.Error(Exception("Usuário não possui city no cadastro. Complete seu perfil."))
            val userState = currentUser.state?.takeIf { it.isNotBlank() }
                ?: return Result.Error(Exception("Usuário não possui state no cadastro. Complete seu perfil."))
            
            val locationCollection = LocationHelper.getLocationCollection(
                firestore,
                "orders",
                userCity,
                userState
            )
            locationCollection.document(orderId).update(updateData).await()
            
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
            
            val currentUser = userRepository.observeCurrentUser().first()
                ?: return Result.Error(Exception("Usuário não autenticado"))
            
            val userCity = currentUser.city?.takeIf { it.isNotBlank() }
                ?: return Result.Error(Exception("Usuário não possui city no cadastro. Complete seu perfil."))
            val userState = currentUser.state?.takeIf { it.isNotBlank() }
                ?: return Result.Error(Exception("Usuário não possui state no cadastro. Complete seu perfil."))
            
            val locationCollection = LocationHelper.getLocationCollection(
                firestore,
                "orders",
                userCity,
                userState
            )
            locationCollection.document(orderId).update(updateData).await()
            
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
            
            val currentUser = userRepository.observeCurrentUser().first()
                ?: return Result.Error(Exception("Usuário não autenticado"))
            
            val userCity = currentUser.city?.takeIf { it.isNotBlank() }
                ?: return Result.Error(Exception("Usuário não possui city no cadastro. Complete seu perfil."))
            val userState = currentUser.state?.takeIf { it.isNotBlank() }
                ?: return Result.Error(Exception("Usuário não possui state no cadastro. Complete seu perfil."))
            
            val locationCollection = LocationHelper.getLocationCollection(
                firestore,
                "orders",
                userCity,
                userState
            )
            locationCollection.document(orderId).update(updateData).await()
            
            Result.Success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreOrderRepo", "Erro ao concluir ordem: ${e.message}", e)
            Result.Error(e)
        }
    }
}
