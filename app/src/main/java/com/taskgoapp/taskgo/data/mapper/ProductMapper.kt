package com.taskgoapp.taskgo.data.mapper

import com.taskgoapp.taskgo.data.local.entity.ProductEntity
import com.taskgoapp.taskgo.data.local.entity.ProductImageEntity
import com.taskgoapp.taskgo.core.model.Product
import com.taskgoapp.taskgo.data.firestore.models.ProductFirestore

object ProductMapper {
    
    fun ProductEntity.toModel(imageUris: List<String> = emptyList()): Product {
        return Product(
            id = this.id,
            title = this.title,
            price = this.price,
            description = this.description,
            sellerId = this.sellerId,
            sellerName = this.sellerName,
            imageUris = imageUris,
            rating = this.rating,
            latitude = this.latitude,
            longitude = this.longitude,
            featured = this.featured,
            discountPercentage = this.discountPercentage,
            active = this.active
        )
    }
    
    fun Product.toEntity(): ProductEntity {
        return ProductEntity(
            id = this.id,
            title = this.title,
            price = this.price,
            description = this.description,
            sellerId = this.sellerId,
            sellerName = this.sellerName,
            rating = this.rating,
            latitude = this.latitude,
            longitude = this.longitude,
            featured = this.featured ?: false,
            discountPercentage = this.discountPercentage,
            active = this.active
        )
    }
    
    fun ProductImageEntity.toModel(): String {
        return this.uri
    }
    
    fun String.toImageEntity(productId: String): ProductImageEntity {
        return ProductImageEntity(
            productId = productId,
            uri = this
        )
    }
    
    // Firestore Mappers
    fun ProductFirestore.toModel(): Product {
        return Product(
            id = this.id,
            title = this.title,
            price = this.price,
            description = this.description,
            sellerId = this.sellerId.takeIf { it.isNotBlank() },
            sellerName = this.sellerName,
            imageUris = this.imageUrls,
            category = this.category,
            rating = this.rating,
            latitude = this.latitude,
            longitude = this.longitude,
            featured = this.featured,
            discountPercentage = this.discountPercentage,
            active = this.active
        )
    }
    
    fun Product.toFirestore(): ProductFirestore {
        return ProductFirestore(
            id = this.id,
            title = this.title,
            price = this.price,
            description = this.description,
            sellerId = this.sellerId ?: "",
            sellerName = this.sellerName,
            imageUrls = this.imageUris,
            category = this.category,
            tags = emptyList(),
            active = this.active,
            featured = this.featured ?: false,
            discountPercentage = this.discountPercentage,
            rating = this.rating,
            latitude = this.latitude,
            longitude = this.longitude
        )
    }
}