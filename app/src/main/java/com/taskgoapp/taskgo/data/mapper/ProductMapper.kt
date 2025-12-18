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
            sellerName = this.sellerName,
            imageUris = imageUris,
            rating = this.rating,
            latitude = this.latitude,
            longitude = this.longitude,
            featured = this.featured
        )
    }
    
    fun Product.toEntity(): ProductEntity {
        return ProductEntity(
            id = this.id,
            title = this.title,
            price = this.price,
            description = this.description,
            sellerName = this.sellerName,
            rating = this.rating,
            latitude = this.latitude,
            longitude = this.longitude,
            featured = this.featured ?: false
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
            sellerName = this.sellerName,
            imageUris = this.imageUrls,
            category = this.category,
            rating = this.rating,
            latitude = this.latitude,
            longitude = this.longitude,
            featured = this.featured
        )
    }
    
    fun Product.toFirestore(): ProductFirestore {
        return ProductFirestore(
            id = this.id,
            title = this.title,
            price = this.price,
            description = this.description,
            sellerId = "", // TODO: Get from auth
            sellerName = this.sellerName,
            imageUrls = this.imageUris,
            category = this.category,
            tags = emptyList(),
            active = true,
            featured = this.featured ?: false,
            rating = this.rating,
            latitude = this.latitude,
            longitude = this.longitude
        )
    }
}