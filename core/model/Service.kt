package com.example.taskgoapp.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Service(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: ServiceCategory = ServiceCategory.OTHER,
    val providerId: String = "",
    val providerName: String = "",
    val providerImage: String? = null,
    val price: Double = 0.0,
    val priceType: PriceType = PriceType.FIXED,
    val images: List<String> = emptyList(),
    val rating: Float = 0f,
    val totalReviews: Int = 0,
    val isActive: Boolean = true,
    val tags: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class ServiceCategory {
    CLEANING,      // Limpeza
    MAINTENANCE,   // Manutenção
    MOVING,        // Mudança
    GARDENING,     // Jardinagem
    ELECTRICAL,    // Elétrica
    PLUMBING,      // Encanamento
    PAINTING,      // Pintura
    CARPENTRY,     // Carpintaria
    OTHER          // Outros
}

enum class PriceType {
    FIXED,         // Preço fixo
    HOURLY,        // Por hora
    NEGOTIABLE    // Negociável
}
