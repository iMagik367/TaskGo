package com.taskgoapp.taskgo.data.mapper

import com.taskgoapp.taskgo.data.local.entity.PurchaseOrderEntity
import com.taskgoapp.taskgo.data.local.entity.PurchaseOrderItemEntity
import com.taskgoapp.taskgo.core.model.PurchaseOrder
import com.taskgoapp.taskgo.core.model.OrderItem
import com.taskgoapp.taskgo.core.model.OrderStatus
import com.taskgoapp.taskgo.data.firestore.models.PurchaseOrderFirestore
import com.taskgoapp.taskgo.data.firestore.models.PurchaseOrderItemFirestore

object OrderMapper {
    
    fun PurchaseOrderEntity.toModel(items: List<OrderItem> = emptyList()): PurchaseOrder {
        return PurchaseOrder(
            id = this.id,
            orderNumber = this.orderNumber ?: "",
            createdAt = this.createdAt,
            total = this.total,
            subtotal = this.subtotal ?: 0.0,
            deliveryFee = this.deliveryFee ?: 0.0,
            status = when (this.status) {
                "EM_ANDAMENTO" -> OrderStatus.EM_ANDAMENTO
                "CONCLUIDO" -> OrderStatus.CONCLUIDO
                "CANCELADO" -> OrderStatus.CANCELADO
                else -> OrderStatus.EM_ANDAMENTO
            },
            items = items,
            paymentMethod = this.paymentMethod,
            trackingCode = this.trackingCode,
            deliveryAddress = this.deliveryAddress
        )
    }
    
    fun PurchaseOrder.toEntity(): PurchaseOrderEntity {
        return PurchaseOrderEntity(
            id = this.id,
            orderNumber = this.orderNumber,
            createdAt = this.createdAt,
            total = this.total,
            subtotal = this.subtotal,
            deliveryFee = this.deliveryFee,
            status = this.status.name,
            paymentMethod = this.paymentMethod,
            trackingCode = this.trackingCode,
            deliveryAddress = this.deliveryAddress
        )
    }
    
    fun PurchaseOrderItemEntity.toModel(): OrderItem {
        return OrderItem(
            productId = this.productId.toLongOrNull() ?: 0L,
            price = this.price,
            quantity = this.qty
        )
    }
    
    fun OrderItem.toEntity(orderId: String): PurchaseOrderItemEntity {
        return PurchaseOrderItemEntity(
            orderId = orderId,
            productId = this.productId.toString(),
            productName = null,
            productImage = null,
            price = this.price,
            qty = this.quantity,
            deliveryDate = null
        )
    }
    
    // Firestore Mappers for PurchaseOrder
    fun PurchaseOrderFirestore.toModel(): PurchaseOrder {
        return PurchaseOrder(
            id = this.id,
            orderNumber = this.orderNumber,
            createdAt = this.createdAt?.time ?: System.currentTimeMillis(),
            total = this.total,
            subtotal = this.subtotal,
            deliveryFee = this.deliveryFee,
            status = when (this.status) {
                "EM_ANDAMENTO" -> OrderStatus.EM_ANDAMENTO
                "CONCLUIDO" -> OrderStatus.CONCLUIDO
                "CANCELADO" -> OrderStatus.CANCELADO
                else -> OrderStatus.EM_ANDAMENTO
            },
            items = this.items.map { it.toModel() },
            paymentMethod = this.paymentMethod,
            trackingCode = this.trackingCode,
            deliveryAddress = this.deliveryAddress
        )
    }
    
    fun PurchaseOrder.toFirestore(): PurchaseOrderFirestore {
        return PurchaseOrderFirestore(
            id = this.id,
            orderNumber = this.orderNumber,
            clientId = "", // TODO: Get from auth
            createdAt = java.util.Date(this.createdAt),
            total = this.total,
            subtotal = this.subtotal,
            deliveryFee = this.deliveryFee,
            status = this.status.name,
            items = this.items.map { it.toFirestore() },
            paymentMethod = this.paymentMethod,
            trackingCode = this.trackingCode,
            deliveryAddress = this.deliveryAddress
        )
    }
    
    fun PurchaseOrderItemFirestore.toModel(): OrderItem {
        return OrderItem(
            productId = this.productId.toLongOrNull() ?: 0L,
            price = this.price,
            quantity = this.quantity
        )
    }
    
    fun OrderItem.toFirestore(): PurchaseOrderItemFirestore {
        return PurchaseOrderItemFirestore(
            productId = this.productId.toString(),
            productName = null,
            productImage = null,
            price = this.price,
            quantity = this.quantity
        )
    }
}