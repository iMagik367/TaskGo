package com.taskgoapp.taskgo.data.firestore.models

import java.util.Date

/**
 * Informações de envio do pedido
 */
data class Shipment(
    val id: String = "",
    val orderId: String = "", // ID do pedido
    val sellerId: String = "", // ID do vendedor
    val clientId: String = "", // ID do cliente
    val isSameCity: Boolean = false, // Se o pedido é na mesma cidade
    val shippingMethod: String? = null, // "CORREIOS", "OTHER", "SAME_CITY"
    val trackingCode: String? = null, // Código de rastreamento (Correios ou outro)
    val trackingUrl: String? = null, // URL de rastreamento (para outras transportadoras)
    val carrierName: String? = null, // Nome da transportadora
    val shippedAt: Date? = null, // Data/hora do envio
    val estimatedDelivery: Date? = null, // Data estimada de entrega
    val deliveredAt: Date? = null, // Data/hora da entrega (para pedidos na mesma cidade)
    val deliveryAddress: DeliveryAddressDetails? = null,
    val status: String = "PENDING", // PENDING, SHIPPED, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED
    val createdAt: Date? = null,
    val updatedAt: Date? = null
)

/**
 * Evento de rastreamento do pedido
 */
data class TrackingEvent(
    val id: String = "",
    val orderId: String = "",
    val shipmentId: String? = null,
    val eventType: String = "", // SHIPPED, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED, etc.
    val description: String = "",
    val location: String? = null, // Localização do evento
    val timestamp: Date? = null,
    val done: Boolean = false, // Se o evento já foi concluído
    val source: String = "MANUAL" // MANUAL, CORREIOS_API, OTHER_API
)

