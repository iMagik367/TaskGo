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
    val storeId: String? = null, // ID da loja que vendeu o produto
    val createdAt: Date? = null,
    val total: Double = 0.0,
    val subtotal: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val status: String = "", // PENDING_PAYMENT, PAID, PREPARING, SHIPPED, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED, CANCELLED
    val items: List<PurchaseOrderItemFirestore> = emptyList(),
    val paymentMethod: String = "", // "Pix" | "Crédito" | "Débito"
    val paymentIntentId: String? = null, // Stripe PaymentIntent ID
    val paymentStatus: String? = null, // PENDING, PROCESSING, SUCCEEDED, FAILED
    val trackingCode: String? = null,
    val deliveryAddress: String? = null,
    val deliveryAddressDetails: DeliveryAddressDetails? = null,
    val shippingStatus: String? = null,
    val shippedAt: Date? = null,
    val deliveredAt: Date? = null,
    val estimatedDelivery: Date? = null,
    val updatedAt: Date? = null
)

data class DeliveryAddressDetails(
    val street: String = "",
    val number: String = "",
    val neighborhood: String = "",
    val city: String = "",
    val state: String = "",
    val zipCode: String = "",
    val complement: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class PurchaseOrderItemFirestore(
    val productId: String = "",
    val productName: String? = null,
    val productImage: String? = null,
    val price: Double = 0.0,
    val quantity: Int = 0
)

