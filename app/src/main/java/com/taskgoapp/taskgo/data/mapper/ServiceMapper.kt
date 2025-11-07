package com.taskgoapp.taskgo.data.mapper

import com.taskgoapp.taskgo.data.local.entity.ServiceOrderEntity
import com.taskgoapp.taskgo.data.local.entity.ProposalEntity
import com.taskgoapp.taskgo.core.model.ServiceOrder
import com.taskgoapp.taskgo.core.model.Proposal

object ServiceMapper {
    
    fun ServiceOrderEntity.toModel(): ServiceOrder {
        return ServiceOrder(
            id = this.id,
            category = this.category,
            description = this.description,
            date = this.date,
            addressLine = this.addressLine,
            city = this.city,
            state = this.state
        )
    }
    
    fun ServiceOrder.toEntity(): ServiceOrderEntity {
        return ServiceOrderEntity(
            id = this.id,
            category = this.category,
            description = this.description,
            date = this.date,
            addressLine = this.addressLine,
            city = this.city,
            state = this.state
        )
    }
    
    fun ProposalEntity.toModel(): Proposal {
        return Proposal(
            id = this.id,
            orderId = this.orderId,
            providerName = this.providerName,
            rating = this.rating,
            amount = this.amount,
            message = this.message,
            scheduledDate = this.scheduledDate,
            address = this.address,
            accepted = this.accepted
        )
    }
    
    fun Proposal.toEntity(): ProposalEntity {
        return ProposalEntity(
            id = this.id,
            orderId = this.orderId,
            providerName = this.providerName,
            rating = this.rating,
            amount = this.amount,
            message = this.message,
            scheduledDate = this.scheduledDate,
            address = this.address,
            accepted = this.accepted
        )
    }
}