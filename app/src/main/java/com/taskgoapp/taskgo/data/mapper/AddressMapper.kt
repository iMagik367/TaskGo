package com.taskgoapp.taskgo.data.mapper

import com.taskgoapp.taskgo.data.local.entity.AddressEntity
import com.taskgoapp.taskgo.core.model.Address

object AddressMapper {
    
    fun AddressEntity.toModel(): Address {
        return Address(
            id = this.id,
            name = this.name,
            phone = this.phone,
            cep = this.cep,
            street = this.street,
            district = this.district,
            city = this.city,
            state = this.state
        )
    }
    
    fun Address.toEntity(): AddressEntity {
        return AddressEntity(
            id = this.id,
            name = this.name,
            phone = this.phone,
            cep = this.cep,
            street = this.street,
            district = this.district,
            city = this.city,
            state = this.state
        )
    }
}