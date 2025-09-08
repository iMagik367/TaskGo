package com.example.taskgoapp.data.mapper

import com.example.taskgoapp.data.local.entity.CartItemEntity
import com.example.taskgoapp.core.model.CartItem

object CartMapper {
    
    fun CartItemEntity.toModel(): CartItem {
        return CartItem(
            productId = this.productId,
            qty = this.qty
        )
    }
    
    fun CartItem.toEntity(): CartItemEntity {
        return CartItemEntity(
            productId = this.productId,
            qty = this.qty
        )
    }
}