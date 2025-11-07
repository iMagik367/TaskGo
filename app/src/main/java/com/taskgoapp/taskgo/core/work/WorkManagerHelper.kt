package com.taskgoapp.taskgo.core.work

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerHelper @Inject constructor(
    private val context: Context
) {
    
    private val workManager = WorkManager.getInstance(context)
    
    fun scheduleOrderTracking(orderId: String) {
        // Schedule "Pedido Enviado" notification after 5 seconds
        val orderShippedRequest = OneTimeWorkRequestBuilder<OrderTrackingWorker>()
            .setInputData(workDataOf(
                "order_id" to orderId,
                "event_type" to "order_shipped"
            ))
            .setInitialDelay(5, TimeUnit.SECONDS)
            .build()
        
        // Schedule "Em Trânsito" after 1 day
        val inTransitRequest = OneTimeWorkRequestBuilder<OrderTrackingWorker>()
            .setInputData(workDataOf(
                "order_id" to orderId,
                "event_type" to "in_transit"
            ))
            .setInitialDelay(1, TimeUnit.DAYS)
            .build()
        
        // Schedule "Saiu para Entrega" after 2 days
        val outForDeliveryRequest = OneTimeWorkRequestBuilder<OrderTrackingWorker>()
            .setInputData(workDataOf(
                "order_id" to orderId,
                "event_type" to "out_for_delivery"
            ))
            .setInitialDelay(2, TimeUnit.DAYS)
            .build()
        
        // Schedule "Entregue" after 3 days
        val deliveredRequest = OneTimeWorkRequestBuilder<OrderTrackingWorker>()
            .setInputData(workDataOf(
                "order_id" to orderId,
                "event_type" to "delivered"
            ))
            .setInitialDelay(3, TimeUnit.DAYS)
            .build()
        
        // Enqueue all work requests
        workManager.enqueueUniqueWork(
            "order_tracking_$orderId",
            ExistingWorkPolicy.REPLACE,
            orderShippedRequest
        )
        
        workManager.enqueue(inTransitRequest)
        workManager.enqueue(outForDeliveryRequest)
        workManager.enqueue(deliveredRequest)
    }
    
    fun scheduleProposalResponse(proposalId: String, delayMinutes: Long = 30) {
        val proposalRequest = OneTimeWorkRequestBuilder<OrderTrackingWorker>()
            .setInputData(workDataOf(
                "proposal_id" to proposalId,
                "event_type" to "proposal_approved"
            ))
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .build()
        
        workManager.enqueue(proposalRequest)
    }
    
    fun scheduleChatResponse(threadId: String, delaySeconds: Long = 10) {
        val chatRequest = OneTimeWorkRequestBuilder<OrderTrackingWorker>()
            .setInputData(workDataOf(
                "thread_id" to threadId,
                "event_type" to "chat_response"
            ))
            .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
            .build()
        
        workManager.enqueue(chatRequest)
    }
    
    fun cancelOrderTracking(orderId: String) {
        workManager.cancelUniqueWork("order_tracking_$orderId")
    }
}
