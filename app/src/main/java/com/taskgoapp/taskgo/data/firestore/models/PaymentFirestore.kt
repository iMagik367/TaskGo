package com.taskgoapp.taskgo.data.firestore.models

import java.util.Date

data class PaymentFirestore(
    val id: String = "",
    val orderId: String = "",
    val clientId: String = "",
    val providerId: String = "",
    val amount: Double = 0.0,
    val applicationFee: Double = 0.0,
    val currency: String = "usd",
    val stripePaymentIntentId: String = "",
    val status: String = "pending", // pending, processing, succeeded, failed, refunded
    val paidAt: Date? = null,
    val failedAt: Date? = null,
    val createdAt: Date? = null,
    val updatedAt: Date? = null
)





