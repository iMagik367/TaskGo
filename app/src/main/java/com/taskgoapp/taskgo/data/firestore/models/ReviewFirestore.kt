package com.taskgoapp.taskgo.data.firestore.models

import java.util.Date

data class ReviewFirestore(
    val id: String = "",
    val type: String = "", // "PRODUCT", "SERVICE", "PROVIDER"
    val targetId: String = "",
    val reviewerId: String = "",
    val reviewerName: String = "",
    val reviewerAvatarUri: String? = null,
    val rating: Int = 0,
    val comment: String? = null,
    val photoUrls: List<String> = emptyList(),
    val createdAt: Date? = null,
    val updatedAt: Date? = null,
    val orderId: String? = null,
    val helpfulCount: Int = 0,
    val verifiedPurchase: Boolean = false,
    val locationId: String? = null // CRÍTICO: locationId do produto/serviço para atualização eficiente de rating
)

