package com.taskgoapp.taskgo.data.firestore.models

import java.util.Date

data class NotificationFirestore(
    val id: String = "",
    val userId: String = "",
    val orderId: String? = null,
    val type: String = "", // order_created, order_accepted, order_completed, payment_received, review_received, system_alert
    val title: String = "",
    val message: String = "",
    val data: Map<String, Any>? = null,
    val read: Boolean = false,
    val readAt: Date? = null,
    val createdAt: Date? = null
)





