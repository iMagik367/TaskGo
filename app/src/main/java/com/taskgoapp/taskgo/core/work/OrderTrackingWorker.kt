package com.taskgoapp.taskgo.core.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.taskgoapp.taskgo.core.notifications.NotificationManager
import com.taskgoapp.taskgo.domain.repository.TrackingRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class OrderTrackingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val trackingRepository: TrackingRepository,
    private val notificationManager: NotificationManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val orderId = inputData.getString("order_id") ?: return Result.failure()
        val eventType = inputData.getString("event_type") ?: return Result.failure()
        
        return try {
            when (eventType) {
                "order_shipped" -> {
                    trackingRepository.updateEventDone("shipped_$orderId", true)
                    notificationManager.showOrderShippedNotification(orderId)
                }
                "in_transit" -> {
                    trackingRepository.updateEventDone("transit_$orderId", true)
                }
                "out_for_delivery" -> {
                    trackingRepository.updateEventDone("delivery_$orderId", true)
                }
                "delivered" -> {
                    trackingRepository.updateEventDone("delivered_$orderId", true)
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
