package com.example.taskgoapp.data.mapper

import com.example.taskgoapp.data.local.entity.TrackingEventEntity
import com.example.taskgoapp.core.model.TrackingEvent

object TrackingMapper {
    
    fun TrackingEventEntity.toModel(): TrackingEvent {
        return TrackingEvent(
            label = this.label,
            date = this.date,
            done = this.done
        )
    }
    
    fun TrackingEvent.toEntity(orderId: String): TrackingEventEntity {
        return TrackingEventEntity(
            id = java.util.UUID.randomUUID().toString(),
            orderId = orderId,
            label = this.label,
            date = this.date,
            done = this.done
        )
    }
}