package com.taskgoapp.taskgo.data.mapper

import com.taskgoapp.taskgo.data.local.entity.TrackingEventEntity
import com.taskgoapp.taskgo.core.model.TrackingEvent

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