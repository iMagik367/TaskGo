package br.com.taskgo.taskgo.data.mapper

import com.example.taskgoapp.data.local.entity.ProposalEntity
import com.example.taskgoapp.core.model.Proposal

object ProposalMapper {
    
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
