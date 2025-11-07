package com.taskgoapp.taskgo.data.repository

import com.taskgoapp.taskgo.core.model.PurchaseOrder
import com.taskgoapp.taskgo.core.model.CartItem
import com.taskgoapp.taskgo.core.model.OrderStatus
import com.taskgoapp.taskgo.data.local.dao.CartDao
import com.taskgoapp.taskgo.domain.repository.OrdersRepository
import com.taskgoapp.taskgo.data.firestore.models.PurchaseOrderFirestore
import com.taskgoapp.taskgo.data.mapper.OrderMapper.toFirestore
import com.taskgoapp.taskgo.data.mapper.OrderMapper.toModel
import com.taskgoapp.taskgo.data.mapper.CartMapper.toEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreOrdersRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val cartDao: CartDao
) : OrdersRepository {
    
    private val purchaseOrdersCollection = firestore.collection("purchase_orders")

    override fun observeOrders(): Flow<List<PurchaseOrder>> = callbackFlow {
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

    override fun observeOrdersByStatus(status: OrderStatus): Flow<List<PurchaseOrder>> = callbackFlow {
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
        return try {
            val document = purchaseOrdersCollection.document(id).get().await()
            document.toObject(PurchaseOrderFirestore::class.java)?.copy(id = document.id)?.toModel()
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
            status = OrderStatus.EM_ANDAMENTO,
            items = emptyList(), // Will be populated from cart when we implement cart to order items
            paymentMethod = paymentMethod,
            deliveryAddress = addressId
        )
        
        try {
            // Convert PurchaseOrder to Firestore format
            val firestoreOrder = order.toFirestore().copy(clientId = userId)
            
            // Create order items from cart
            val orderItems = cart.mapNotNull { cartItem ->
                // Create simple order item - we'll need to fetch product details separately
                com.taskgoapp.taskgo.core.model.OrderItem(
                    productId = cartItem.productId.toLongOrNull() ?: 0L,
                    price = 0.0, // TODO: Fetch from product
                    quantity = cartItem.qty
                ).toFirestore()
            }
            
            val orderWithItems = firestoreOrder.copy(items = orderItems)
            purchaseOrdersCollection.document(orderId).set(orderWithItems).await()
            
            // Clear cart
            cartDao.clearAll()
            
            return orderId
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun updateOrderStatus(orderId: String, status: OrderStatus) {
        try {
            purchaseOrdersCollection.document(orderId).update(
                "status", status.name,
                "updatedAt", FieldValue.serverTimestamp()
            ).await()
        } catch (e: Exception) {
            // Silently fail for now
        }
    }

    private fun generateOrderId(): String {
        return "order_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
}

