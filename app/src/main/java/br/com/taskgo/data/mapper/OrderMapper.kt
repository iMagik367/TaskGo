package br.com.taskgo.taskgo.data.mapper

import com.example.taskgoapp.data.local.entity.PurchaseOrderEntity
import com.example.taskgoapp.data.local.entity.PurchaseOrderItemEntity
import com.example.taskgoapp.core.model.PurchaseOrder
import com.example.taskgoapp.core.model.OrderItem
import com.example.taskgoapp.core.model.OrderStatus

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
            productId = this.productId,
            price = this.price,
            quantity = this.qty
        )
    }
    
    fun OrderItem.toEntity(orderId: String): PurchaseOrderItemEntity {
        return PurchaseOrderItemEntity(
            orderId = orderId,
            productId = this.productId,
            productName = null,
            productImage = null,
            price = this.price,
            qty = this.quantity,
            deliveryDate = null
        )
    }
}