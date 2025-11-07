package com.taskgoapp.taskgo.data.firestore.models

import java.util.Date

data class ServiceFirestore(
    val id: String = "",
    val providerId: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val price: Double = 0.0,
    val images: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val active: Boolean = true,
    val createdAt: Date? = null,
    val updatedAt: Date? = null
)





