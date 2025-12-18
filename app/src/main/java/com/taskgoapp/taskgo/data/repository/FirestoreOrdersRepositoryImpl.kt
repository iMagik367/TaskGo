package com.taskgoapp.taskgo.data.repository

import com.taskgoapp.taskgo.core.model.PurchaseOrder
import com.taskgoapp.taskgo.core.model.CartItem
import com.taskgoapp.taskgo.core.model.OrderStatus
import com.taskgoapp.taskgo.data.local.dao.CartDao
import com.taskgoapp.taskgo.domain.repository.OrdersRepository
import com.taskgoapp.taskgo.data.firestore.models.PurchaseOrderFirestore
import com.taskgoapp.taskgo.data.mapper.OrderMapper.toFirestore
import com.taskgoapp.taskgo.data.mapper.OrderMapper.toModel
import com.taskgoapp.taskgo.data.mapper.OrderMapper.toEntity
import com.taskgoapp.taskgo.data.mapper.CartMapper.toEntity as cartItemToEntity
import com.taskgoapp.taskgo.data.local.dao.PurchaseOrderDao
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.channels.awaitClose
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreOrdersRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val cartDao: CartDao,
    private val purchaseOrderDao: PurchaseOrderDao,
    private val syncManager: com.taskgoapp.taskgo.core.sync.SyncManager,
    private val realtimeRepository: com.taskgoapp.taskgo.data.realtime.RealtimeDatabaseRepository
) : OrdersRepository {
    
    private val purchaseOrdersCollection = firestore.collection("purchase_orders")
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun observeOrders(): Flow<List<PurchaseOrder>> = flow {
        val userId = firebaseAuth.currentUser?.uid ?: return@flow
        
        // 1. Emite dados do cache local primeiro (instantâneo)
        purchaseOrderDao.observeAll().collect { cachedOrders ->
            val ordersWithItems = cachedOrders.map { entity ->
                val items = purchaseOrderDao.getItemsByOrderId(entity.id)
                entity.toModel(items.map { it.toModel() })
            }
            emit(ordersWithItems)
        }
    }.onStart {
        // 2. Sincroniza com Firebase em background quando o Flow é coletado
        val userId = firebaseAuth.currentUser?.uid ?: return@onStart
        try {
            val snapshot = purchaseOrdersCollection
                .whereEqualTo("clientId", userId)
                .orderBy("createdAt")
                .get()
                .await()
            
            snapshot.documents.forEach { doc ->
                val order = doc.toObject(PurchaseOrderFirestore::class.java)?.copy(id = doc.id)?.toModel()
                order?.let {
                    purchaseOrderDao.upsert(it.toEntity())
                    val items = it.items.map { item: com.taskgoapp.taskgo.core.model.OrderItem -> item.toEntity(it.id) }
                    purchaseOrderDao.upsertItems(items)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreOrdersRepo", "Erro ao sincronizar pedidos: ${e.message}", e)
        }
    }
    
    private fun observeOrdersOld(): Flow<List<PurchaseOrder>> = callbackFlow {
        val userId = firebaseAuth.currentUser?.uid ?: run {
            close()
            return@callbackFlow
        }
        
        val listenerRegistration = purchaseOrdersCollection
            .whereEqualTo("clientId", userId)
            .orderBy("createdAt")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val orders = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(PurchaseOrderFirestore::class.java)?.copy(id = doc.id)?.toModel()
                } ?: emptyList()
                
                trySend(orders)
            }
        
        awaitClose { listenerRegistration.remove() }
    }

    override fun observeOrdersByStatus(status: OrderStatus): Flow<List<PurchaseOrder>> = flow {
        val userId = firebaseAuth.currentUser?.uid ?: return@flow
        
        // 1. Emite dados do cache local primeiro (instantâneo)
        purchaseOrderDao.observeByStatus(status.name).collect { cachedOrders ->
            val ordersWithItems = cachedOrders.map { entity ->
                val items = purchaseOrderDao.getItemsByOrderId(entity.id)
                entity.toModel(items.map { it.toModel() })
            }
            emit(ordersWithItems)
        }
    }.onStart {
        // 2. Sincroniza com Firebase em background quando o Flow é coletado
        val userId = firebaseAuth.currentUser?.uid ?: return@onStart
        try {
            val snapshot = purchaseOrdersCollection
                .whereEqualTo("clientId", userId)
                .whereEqualTo("status", status.name)
                .orderBy("createdAt")
                .get()
                .await()
            
            snapshot.documents.forEach { doc ->
                val order = doc.toObject(PurchaseOrderFirestore::class.java)?.copy(id = doc.id)?.toModel()
                order?.let {
                    purchaseOrderDao.upsert(it.toEntity())
                    val items = it.items.map { item: com.taskgoapp.taskgo.core.model.OrderItem -> item.toEntity(it.id) }
                    purchaseOrderDao.upsertItems(items)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreOrdersRepo", "Erro ao sincronizar pedidos por status: ${e.message}", e)
        }
    }
    
    private fun observeOrdersByStatusOld(status: OrderStatus): Flow<List<PurchaseOrder>> = callbackFlow {
        val userId = firebaseAuth.currentUser?.uid ?: run {
            close()
            return@callbackFlow
        }
        
        val listenerRegistration = purchaseOrdersCollection
            .whereEqualTo("clientId", userId)
            .whereEqualTo("status", status.name)
            .orderBy("createdAt")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val orders = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(PurchaseOrderFirestore::class.java)?.copy(id = doc.id)?.toModel()
                } ?: emptyList()
                
                trySend(orders)
            }
        
        awaitClose { listenerRegistration.remove() }
    }

    override suspend fun getOrder(id: String): PurchaseOrder? {
        // 1. Tenta buscar do cache local primeiro (instantâneo)
        val cachedEntity = purchaseOrderDao.getById(id)
        if (cachedEntity != null) {
            val items = purchaseOrderDao.getItemsByOrderId(id)
            return cachedEntity.toModel(items.map { it.toModel() })
        }
        
        // 2. Se não encontrou no cache, busca do Firebase
        return try {
            val document = purchaseOrdersCollection.document(id).get().await()
            val order = document.toObject(PurchaseOrderFirestore::class.java)?.copy(id = document.id)?.toModel()
            
            // Salva no cache para próximas consultas
            order?.let {
                purchaseOrderDao.upsert(it.toEntity())
                val items = it.items.map { item: com.taskgoapp.taskgo.core.model.OrderItem -> item.toEntity(it.id) }
                purchaseOrderDao.upsertItems(items)
            }
            
            order
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun createOrder(
        cart: List<CartItem>,
        total: Double,
        paymentMethod: String,
        addressId: String
    ): String {
        val userId = firebaseAuth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")
        val orderId = generateOrderId()
        val orderNumber = "TG${System.currentTimeMillis()}"
        
        val order = PurchaseOrder(
            id = orderId,
            orderNumber = orderNumber,
            createdAt = System.currentTimeMillis(),
            total = total,
            subtotal = total,
            deliveryFee = 0.0,
            status = OrderStatus.EM_ANDAMENTO, // Será atualizado para PENDING_PAYMENT no Firestore
            items = emptyList(), // Will be populated from cart when we implement cart to order items
            paymentMethod = paymentMethod,
            deliveryAddress = addressId
        )
        
        // 1. Salva localmente primeiro (instantâneo)
        purchaseOrderDao.upsert(order.toEntity())
        
        // Salva itens do pedido localmente
        val orderItems = cart.mapNotNull { cartItem ->
            com.taskgoapp.taskgo.core.model.OrderItem(
                productId = cartItem.productId,
                price = 0.0, // TODO: Fetch from product
                quantity = cartItem.qty
            )
        }
        val itemEntities = orderItems.map { orderItem: com.taskgoapp.taskgo.core.model.OrderItem -> orderItem.toEntity(orderId) }
        purchaseOrderDao.upsertItems(itemEntities)
        
        // Clear cart
        cartDao.clearAll()
        
        // 2. Salva no Realtime Database imediatamente
        val firestoreOrder = order.toFirestore().copy(clientId = userId)
        val firestoreItems = orderItems.map { it.toFirestore() }
        val orderWithItems = firestoreOrder.copy(items = firestoreItems)
        
        val orderData = mapOf(
            "id" to orderId,
            "orderNumber" to orderNumber,
            "clientId" to userId,
            "total" to total,
            "subtotal" to total,
            "deliveryFee" to 0.0,
            "status" to "PENDING_PAYMENT", // Status inicial para produtos (aguardando pagamento)
            "paymentMethod" to paymentMethod,
            "deliveryAddress" to addressId,
            "createdAt" to System.currentTimeMillis(),
            "items" to firestoreItems.map { item ->
                mapOf(
                    "productId" to item.productId,
                    "productName" to (item.productName ?: ""),
                    "productImage" to (item.productImage ?: ""),
                    "price" to item.price,
                    "quantity" to item.quantity
                )
            }
        )
        
        syncScope.launch {
            realtimeRepository.savePurchaseOrder(orderId, orderData)
        }
        
        // 3. Agenda sincronização com Firestore após 1 minuto
        syncManager.scheduleSync(
            syncType = "order",
            entityId = orderId,
            operation = "create",
            data = orderWithItems
        )
        
        return orderId
    }

    override suspend fun updateOrderStatus(orderId: String, status: OrderStatus) {
        // 1. Atualiza localmente primeiro (instantâneo)
        purchaseOrderDao.updateStatus(orderId, status.name)
        
        // 2. Agenda sincronização com Firebase após 1 minuto
        val updateData = mapOf(
            "status" to status.name,
            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        
        syncManager.scheduleSync(
            syncType = "order",
            entityId = orderId,
            operation = "update",
            data = updateData
        )
    }

    suspend fun getPurchaseOrder(orderId: String): PurchaseOrderFirestore? {
        return try {
            val document = purchaseOrdersCollection.document(orderId).get().await()
            document.toObject(PurchaseOrderFirestore::class.java)?.copy(id = document.id)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreOrdersRepo", "Erro ao obter pedido: ${e.message}", e)
            null
        }
    }
    
    suspend fun updatePurchaseOrderStatus(orderId: String, status: String) {
        try {
            purchaseOrdersCollection.document(orderId).update(
                "status", status,
                "updatedAt", FieldValue.serverTimestamp()
            ).await()
            
            // Atualiza cache local também
            purchaseOrderDao.updateStatus(orderId, status)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreOrdersRepo", "Erro ao atualizar status: ${e.message}", e)
            throw e
        }
    }

    private fun generateOrderId(): String {
        return "order_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
}

