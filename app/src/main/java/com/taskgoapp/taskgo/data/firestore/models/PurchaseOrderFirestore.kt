package com.taskgoapp.taskgo.data.firestore.models

import java.util.Date

/**
 * PurchaseOrder para Firestore
 * Representa pedidos de compra de produtos no marketplace
 */
data class PurchaseOrderFirestore(
    val id: String = "",
    val orderNumber: String = "",
    val clientId: String = "",
    val createdAt: Date? = null,
    val total: Double = 0.0,
    val subtotal: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val status: String = "", // EM_ANDAMENTO, CONCLUIDO, CANCELADO
    val items: List<PurchaseOrderItemFirestore> = emptyList(),
    val paymentMethod: String = "", // "Pix" | "Crédito" | "Débito"
    val paymentIntentId: String? = null, // Stripe PaymentIntent ID
    val trackingCode: String? = null,
    val deliveryAddress: String? = null,
    val shippingStatus: String? = null,
    val updatedAt: Date? = null
)

data class PurchaseOrderItemFirestore(
    val productId: String = "",
    val productName: String? = null,
    val productImage: String? = null,
    val price: Double = 0.0,
    val quantity: Int = 0
)

