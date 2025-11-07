package com.taskgoapp.taskgo.data.mapper

import com.taskgoapp.taskgo.data.local.entity.CardEntity
import com.taskgoapp.taskgo.core.model.Card

object CardMapper {
    
    fun CardEntity.toModel(): Card {
        return Card(
            id = this.id,
            holder = this.holder,
            numberMasked = this.numberMasked,
            brand = this.brand,
            expMonth = this.expMonth,
            expYear = this.expYear,
            type = this.type
        )
    }
    
    fun Card.toEntity(): CardEntity {
        return CardEntity(
            id = this.id,
            holder = this.holder,
            numberMasked = this.numberMasked,
            brand = this.brand,
            expMonth = this.expMonth,
            expYear = this.expYear,
            type = this.type
        )
    }
}


