package com.taskgoapp.taskgo.data.firestore.models

import java.util.Date

data class AccountChangeRequest(
    val id: String = "",
    val userId: String = "",
    val currentAccountType: String = "", // "PRESTADOR", "VENDEDOR", "CLIENTE"
    val requestedAccountType: String = "", // "PRESTADOR", "VENDEDOR", "CLIENTE"
    val status: String = "PENDING", // PENDING, APPROVED, PROCESSED, REJECTED
    val requestedAt: Date = Date(),
    val scheduledProcessDate: Date? = null, // Data agendada para processamento (1 dia útil)
    val processedAt: Date? = null,
    val processedBy: String? = null, // ID do admin ou sistema que processou
    val rejectionReason: String? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

