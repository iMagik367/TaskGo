package com.taskgoapp.taskgo.data.firestore.models

import java.util.Date

data class ServiceFirestore(
    val id: String = "",
    val providerId: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val price: Double = 0.0,
    val images: List<String> = emptyList(), // URLs das imagens no Firebase Storage
    val videos: List<String> = emptyList(), // URLs dos vídeos MP4 no Firebase Storage
    val tags: List<String> = emptyList(),
    val active: Boolean = true,
    val featured: Boolean = false, // Serviço em destaque
    val rating: Double? = null, // Avaliação média do serviço
    val createdAt: Date? = null,
    val updatedAt: Date? = null,
    val latitude: Double? = null, // Latitude da localização do serviço
    val longitude: Double? = null // Longitude da localização do serviço
)





