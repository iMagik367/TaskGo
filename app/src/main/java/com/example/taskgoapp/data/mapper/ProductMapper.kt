package com.example.taskgoapp.data.mapper

import com.example.taskgoapp.data.local.entity.ProductEntity
import com.example.taskgoapp.data.local.entity.ProductImageEntity
import com.example.taskgoapp.core.model.Product

object ProductMapper {
    
    fun ProductEntity.toModel(imageUris: List<String> = emptyList()): Product {
        return Product(
            id = this.id,
            title = this.title,
            price = this.price,
            description = this.description,
            sellerName = this.sellerName,
            imageUris = imageUris
        )
    }
    
    fun Product.toEntity(): ProductEntity {
        return ProductEntity(
            id = this.id,
            title = this.title,
            price = this.price,
            description = this.description,
            sellerName = this.sellerName
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
}