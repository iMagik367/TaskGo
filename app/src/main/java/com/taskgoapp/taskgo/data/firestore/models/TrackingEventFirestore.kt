package com.taskgoapp.taskgo.data.firestore.models

import java.util.Date

/**
 * TrackingEvent para Firestore
 * Representa eventos de rastreamento de pedidos
 */
data class TrackingEventFirestore(
    val id: String = "",
    val orderId: String = "",
    val type: String = "", // PENDING_PAYMENT, PAID, PREPARING, SHIPPED, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED
    val description: String = "",
    val location: TrackingLocation? = null,
    val timestamp: Date? = null,
    val done: Boolean = false
)

data class TrackingLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String? = null
)

