package com.taskgoapp.taskgo.data.firebase

import com.taskgoapp.taskgo.data.firestore.models.ProposalDetails
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableResult
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseFunctionsService @Inject constructor(
    private val functions: FirebaseFunctions
) {

    // Auth Functions
    suspend fun promoteToProvider(): Result<Map<String, Any>> {
        return executeFunction("promoteToProvider", null)
    }

    suspend fun approveProviderDocuments(providerId: String, documents: Map<String, Any>): Result<Map<String, Any>> {
        val data = mapOf(
            "providerId" to providerId,
            "documents" to documents
        )
        return executeFunction("approveProviderDocuments", data)
    }

    // Order Functions
    suspend fun createOrder(
        serviceId: String,
        details: String,
        location: String,
        budget: Double,
        dueDate: String? = null
    ): Result<Map<String, Any>> {
        val data = mapOf(
            "serviceId" to serviceId,
            "details" to details,
            "location" to location,
            "budget" to budget
        ).plus(dueDate?.let { mapOf("dueDate" to it) } ?: emptyMap())
        
        return executeFunction("createOrder", data)
    }

    suspend fun updateOrderStatus(
        orderId: String,
        status: String,
        proposalDetails: ProposalDetails? = null
    ): Result<Map<String, Any>> {
        val data = mapOf(
            "orderId" to orderId,
            "status" to status
        ).plus(
            proposalDetails?.let { mapOf("proposalDetails" to mapOf(
                "price" to it.price,
                "description" to it.description,
                "estimatedCompletionDate" to (it.estimatedCompletionDate ?: ""),
                "notes" to (it.notes ?: "")
            )) } ?: emptyMap()
        )
        
        return executeFunction("updateOrderStatus", data)
    }

    suspend fun getMyOrders(role: String? = null, status: String? = null): Result<Map<String, Any>> {
        val data = mapOf<String, Any>()
            .plus(role?.let { mapOf("role" to it) } ?: emptyMap())
            .plus(status?.let { mapOf("status" to it) } ?: emptyMap())
        
        return executeFunction("getMyOrders", data)
    }

    // Payment Functions
    suspend fun createPaymentIntent(orderId: String): Result<Map<String, Any>> {
        val data = mapOf("orderId" to orderId)
        return executeFunction("createPaymentIntent", data)
    }

    suspend fun confirmPayment(paymentIntentId: String): Result<Map<String, Any>> {
        val data = mapOf("paymentIntentId" to paymentIntentId)
        return executeFunction("confirmPayment", data)
    }

    suspend fun requestRefund(orderId: String, reason: String): Result<Map<String, Any>> {
        val data = mapOf(
            "orderId" to orderId,
            "reason" to reason
        )
        return executeFunction("requestRefund", data)
    }

    // Stripe Connect Functions
    suspend fun createOnboardingLink(): Result<Map<String, Any>> {
        return executeFunction("createOnboardingLink", null)
    }

    suspend fun getAccountStatus(): Result<Map<String, Any>> {
        return executeFunction("getAccountStatus", null)
    }

    suspend fun createDashboardLink(): Result<Map<String, Any>> {
        return executeFunction("createDashboardLink", null)
    }

    // AI Chat Functions
    suspend fun aiChatProxy(message: String, conversationId: String? = null): Result<Map<String, Any>> {
        val data = mapOf("message" to message)
            .plus(conversationId?.let { mapOf("conversationId" to it) } ?: emptyMap())
        
        return executeFunction("aiChatProxy", data)
    }

    suspend fun createConversation(): Result<Map<String, Any>> {
        return executeFunction("createConversation", null)
    }

    suspend fun getConversationHistory(conversationId: String): Result<Map<String, Any>> {
        val data = mapOf("conversationId" to conversationId)
        return executeFunction("getConversationHistory", data)
    }

    // Notification Functions
    suspend fun sendPushNotification(
        userId: String,
        title: String,
        message: String,
        data: Map<String, Any>? = null
    ): Result<Map<String, Any>> {
        val request = mapOf(
            "userId" to userId,
            "title" to title,
            "message" to message
        ).plus(data?.let { mapOf("data" to it) } ?: emptyMap())
        
        return executeFunction("sendPushNotification", request)
    }

    suspend fun getMyNotifications(limit: Int = 50, unreadOnly: Boolean = false): Result<Map<String, Any>> {
        val data = mapOf(
            "limit" to limit,
            "unreadOnly" to unreadOnly
        )
        return executeFunction("getMyNotifications", data)
    }

    suspend fun markNotificationRead(notificationId: String): Result<Map<String, Any>> {
        val data = mapOf("notificationId" to notificationId)
        return executeFunction("markNotificationRead", data)
    }

    suspend fun markAllNotificationsRead(): Result<Map<String, Any>> {
        return executeFunction("markAllNotificationsRead", null)
    }

    // Helper function
    private suspend fun executeFunction(
        functionName: String,
        data: Map<String, Any>?
    ): Result<Map<String, Any>> {
        return try {
            val callable = functions.getHttpsCallable(functionName)
            val result: Any? = if (data != null) {
                callable.call(data).await()
            } else {
                callable.call().await()
            }
            
            // Access the data property using reflection
            val dataField = result?.javaClass?.getDeclaredField("data")?.apply {
                isAccessible = true
            }
            val resultData = dataField?.get(result) as? Map<String, Any>
            
            Result.success(resultData ?: emptyMap())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}





