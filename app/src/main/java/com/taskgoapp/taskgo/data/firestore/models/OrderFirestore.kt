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
    val updatedAt: Date? = null
)

data class ProposalDetails(
    val price: Double = 0.0,
    val description: String = "",
    val estimatedCompletionDate: String? = null,
    val notes: String? = null
)





