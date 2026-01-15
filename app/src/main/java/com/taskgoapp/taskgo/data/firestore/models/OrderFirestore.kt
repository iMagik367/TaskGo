package com.taskgoapp.taskgo.data.firestore.models

import java.util.Date

data class OrderFirestore(
    val id: String = "",
    val clientId: String = "",
    val providerId: String? = null,
    val serviceId: String = "",
    val category: String? = null, // Categoria do serviço (ex: "Montagem", "Jardinagem")
    val details: String = "",
    val location: String = "",
    val latitude: Double? = null, // Latitude da localização da ordem
    val longitude: Double? = null, // Longitude da localização da ordem
    val budget: Double = 0.0,
    val status: String = "pending", // pending, proposed, accepted, payment_pending, paid, in_progress, completed, cancelled, disputed
    val proposalDetails: ProposalDetails? = null,
    val proposedAt: Date? = null,
    val acceptedAt: Date? = null,
    val disputeReason: String? = null,
    val disputedAt: Date? = null,
    val deleted: Boolean = false,
    val deletedAt: Date? = null,
    val dueDate: String? = null,
    val createdAt: Date? = null,
    val updatedAt: Date? = null,
    // Campos para rastreamento de aceitação mútua
    val acceptedByProvider: Boolean = false,
    val acceptedByClient: Boolean = false,
    // Campos para cancelamento
    val cancelledReason: String? = null,
    val cancelledRefundAmount: Double? = null,
    // Campos para conclusão
    val completedDescription: String? = null,
    val completedTime: String? = null,
    val completedMediaUrls: List<String>? = null
)

data class ProposalDetails(
    val price: Double = 0.0,
    val description: String = "",
    val estimatedCompletionDate: String? = null,
    val notes: String? = null
)





