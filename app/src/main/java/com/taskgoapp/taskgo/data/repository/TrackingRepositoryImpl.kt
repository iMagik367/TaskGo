package com.taskgoapp.taskgo.data.repository

import com.taskgoapp.taskgo.data.local.dao.TrackingDao
import com.taskgoapp.taskgo.data.mapper.TrackingMapper.toEntity
import com.taskgoapp.taskgo.data.mapper.TrackingMapper.toModel
import com.taskgoapp.taskgo.domain.repository.TrackingRepository
import com.taskgoapp.taskgo.core.model.TrackingEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackingRepositoryImpl @Inject constructor(
    private val trackingDao: TrackingDao
) : TrackingRepository {

    override fun observeTrackingEvents(orderId: String): Flow<List<TrackingEvent>> {
        return trackingDao.observeByOrderId(orderId).map { entities ->
            entities.map { it.toModel() }
        }
    }

    override suspend fun seedTimeline(orderId: String) {
        val defaultEvents = listOf(
            TrackingEvent("Pedido confirmado", System.currentTimeMillis(), false),
            TrackingEvent("Preparando pedido", System.currentTimeMillis() + 3600000, false),
            TrackingEvent("Saiu para entrega", System.currentTimeMillis() + 7200000, false),
            TrackingEvent("Entregue", System.currentTimeMillis() + 10800000, false)
        )
        defaultEvents.forEach { event ->
            trackingDao.upsert(event.toEntity(orderId))
        }
    }

    override suspend fun updateEventDone(eventId: String, done: Boolean) {
        trackingDao.updateDone(eventId, done)
    }

    private fun generateId(): String {
        return "track_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
}