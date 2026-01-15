package com.taskgoapp.taskgo.data.firestore.models

import java.util.Date

data class ProductFirestore(
    val id: String = "",
    val title: String = "",
    val price: Double = 0.0,
    val description: String? = null,
    val sellerId: String = "",
    val sellerName: String? = null,
    val imageUrls: List<String> = emptyList(),
    val category: String? = null,
    val tags: List<String> = emptyList(),
    val active: Boolean = true,
    val featured: Boolean = false, // Produto em destaque/promocional
    val discountPercentage: Double? = null, // Percentual de desconto para destaque/promoções
    val createdAt: Date? = null,
    val updatedAt: Date? = null,
    val rating: Double? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

