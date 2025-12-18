package com.taskgoapp.taskgo.data.repository

import com.taskgoapp.taskgo.data.local.dao.PurchaseOrderDao
import com.taskgoapp.taskgo.data.local.dao.CartDao
import com.taskgoapp.taskgo.data.mapper.OrderMapper.toEntity
import com.taskgoapp.taskgo.data.mapper.OrderMapper.toModel
import com.taskgoapp.taskgo.domain.repository.OrdersRepository
import com.taskgoapp.taskgo.core.model.PurchaseOrder
import com.taskgoapp.taskgo.core.model.OrderStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrdersRepositoryImpl @Inject constructor(
    private val purchaseOrderDao: PurchaseOrderDao,
    private val cartDao: CartDao
) : OrdersRepository {

    override fun observeOrders(): Flow<List<PurchaseOrder>> {
        return purchaseOrderDao.observeAll().map { entities ->
            entities.map { entity ->
                val items = purchaseOrderDao.getItemsByOrderId(entity.id).map { it.toModel() }
                entity.toModel(items)
            }
        }
    }

    override fun observeOrdersByStatus(status: OrderStatus): Flow<List<PurchaseOrder>> {
        return purchaseOrderDao.observeByStatus(status.name).map { entities ->
            entities.map { entity ->
                val items = purchaseOrderDao.getItemsByOrderId(entity.id).map { it.toModel() }
                entity.toModel(items)
            }
        }
    }

    override suspend fun getOrder(id: String): PurchaseOrder? {
        val entity = purchaseOrderDao.getById(id) ?: return null
        val items = purchaseOrderDao.getItemsByOrderId(id).map { it.toModel() }
        return entity.toModel(items)
    }

    override suspend fun createOrder(
        cart: List<com.taskgoapp.taskgo.core.model.CartItem>,
        total: Double,
        paymentMethod: String,
        addressId: String
    ): String {
        val orderId = generateOrderId()
        val orderNumber = "TG${System.currentTimeMillis()}"
        // Use the provided total
        
        val order = PurchaseOrder(
            id = orderId,
            orderNumber = orderNumber,
            createdAt = System.currentTimeMillis(),
            total = total,
            subtotal = total,
            deliveryFee = 0.0,
            status = OrderStatus.EM_ANDAMENTO,
            items = emptyList(),
            paymentMethod = paymentMethod,
            deliveryAddress = addressId
        )
        
        purchaseOrderDao.upsert(order.toEntity())
        
        // Create order items
        val orderItems = cart.map { cartItem ->
            com.taskgoapp.taskgo.core.model.OrderItem(
                productId = cartItem.productId,
                price = 0.0, // Will be loaded from product
                quantity = cartItem.qty
            ).toEntity(orderId)
        }
        purchaseOrderDao.upsertItems(orderItems)
        
        // Clear cart after order creation
        cartDao.clearAll()
        
        return orderId
    }

    override suspend fun updateOrderStatus(orderId: String, status: OrderStatus) {
        purchaseOrderDao.updateStatus(orderId, status.name)
    }

    private fun generateOrderId(): String {
        return "order_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
}