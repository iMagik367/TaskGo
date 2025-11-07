package com.taskgoapp.taskgo.data.mapper

import com.taskgoapp.taskgo.data.local.entity.CartItemEntity
import com.taskgoapp.taskgo.core.model.CartItem

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