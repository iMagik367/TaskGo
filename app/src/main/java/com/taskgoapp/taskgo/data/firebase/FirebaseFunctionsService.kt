package com.taskgoapp.taskgo.data.firebase

import android.util.Log
import com.taskgoapp.taskgo.data.firestore.models.ProposalDetails
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.HttpsCallableResult
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseFunctionsService @Inject constructor(
    private val functions: FirebaseFunctions
) {

    // Auth Functions
    suspend fun getUserEmailByDocument(document: String): Result<Map<String, Any>> {
        val data = mapOf("document" to document)
        return executeFunction("getUserEmailByDocument", data)
    }

    suspend fun setInitialUserRole(role: String, accountType: String? = null): Result<Map<String, Any>> {
        val data = mapOf(
            "role" to role
        ).plus(accountType?.let { mapOf("accountType" to it) } ?: emptyMap())
        return executeFunction("setInitialUserRole", data)
    }

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
        serviceId: String? = null,
        category: String? = null,
        details: String,
        location: String,
        budget: Double? = null,
        dueDate: String? = null
    ): Result<Map<String, Any>> {
        require(serviceId != null || category != null) { "Either serviceId or category must be provided" }
        
        val data = mutableMapOf<String, Any>(
            "details" to details,
            "location" to location
        )
        
        serviceId?.let { data["serviceId"] = it }
        category?.let { data["category"] = it }
        budget?.let { data["budget"] = it }
        dueDate?.let { data["dueDate"] = it }
        
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

    // Services Functions
    suspend fun createService(
        title: String,
        description: String,
        category: String,
        price: Double? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        active: Boolean = true
    ): Result<Map<String, Any>> {
        val data = mapOf(
            "title" to title,
            "description" to description,
            "category" to category,
            "active" to active
        ).plus(price?.let { mapOf("price" to it) } ?: emptyMap())
         .plus(latitude?.let { mapOf("latitude" to it) } ?: emptyMap())
         .plus(longitude?.let { mapOf("longitude" to it) } ?: emptyMap())
        
        return executeFunction("createService", data)
    }
    
    suspend fun updateService(
        serviceId: String,
        updates: Map<String, Any>
    ): Result<Map<String, Any>> {
        val data = mapOf(
            "serviceId" to serviceId,
            "updates" to updates
        )
        return executeFunction("updateService", data)
    }
    
    suspend fun deleteService(serviceId: String): Result<Map<String, Any>> {
        val data = mapOf("serviceId" to serviceId)
        return executeFunction("deleteService", data)
    }

    // Stories Functions
    suspend fun createStory(
        mediaUrl: String,
        mediaType: String = "image",
        caption: String? = null,
        thumbnailUrl: String? = null,
        location: Map<String, Any>? = null,
        expiresAt: Long? = null
    ): Result<Map<String, Any>> {
        val data = mapOf(
            "mediaUrl" to mediaUrl,
            "mediaType" to mediaType
        ).plus(caption?.let { mapOf("caption" to it) } ?: emptyMap())
         .plus(thumbnailUrl?.let { mapOf("thumbnailUrl" to it) } ?: emptyMap())
         .plus(location?.let { mapOf("location" to it) } ?: emptyMap())
         .plus(expiresAt?.let { mapOf("expiresAt" to it) } ?: emptyMap())
        
        return executeFunction("createStory", data)
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
    
    suspend fun listConversations(limit: Int = 50): Result<Map<String, Any>> {
        val data = mapOf("limit" to limit)
        return executeFunction("listConversations", data)
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

    // User Preferences Functions
    suspend fun updateNotificationSettings(settings: Map<String, Boolean>): Result<Map<String, Any>> {
        return executeFunction("updateNotificationSettings", settings.mapValues { it.value as Any })
    }
    
    suspend fun updatePrivacySettings(settings: Map<String, Boolean>): Result<Map<String, Any>> {
        return executeFunction("updatePrivacySettings", settings.mapValues { it.value as Any })
    }
    
    suspend fun updateLanguagePreference(languageCode: String): Result<Map<String, Any>> {
        val data = mapOf("language" to languageCode)
        return executeFunction("updateLanguagePreference", data)
    }
    
    suspend fun updateUserPreferences(categories: List<String>): Result<Map<String, Any>> {
        val data = mapOf("categories" to categories)
        return executeFunction("updateUserPreferences", data)
    }

    suspend fun getUserPreferences(): Result<Map<String, Any>> {
        return executeFunction("getUserPreferences", null)
    }
    
    suspend fun getUserSettings(): Result<Map<String, Any>> {
        return executeFunction("getUserSettings", null)
    }
    
    // Account Deletion Function
    suspend fun deleteUserAccount(): Result<Map<String, Any>> {
        return executeFunction("deleteUserAccount", null)
    }
    
    // Two Factor Authentication Functions
    suspend fun sendTwoFactorCode(): Result<Map<String, Any>> {
        return executeFunction("sendTwoFactorCode", null)
    }
    
    suspend fun verifyTwoFactorCode(code: String): Result<Map<String, Any>> {
        val data = mapOf("code" to code)
        return executeFunction("verifyTwoFactorCode", data)
    }
    
    // Identity Verification Function
    suspend fun startIdentityVerification(
        documentFrontUrl: String,
        documentBackUrl: String?,
        selfieUrl: String,
        addressProofUrl: String? = null
    ): Result<Map<String, Any>> {
        val data = mapOf(
            "documentFrontUrl" to documentFrontUrl,
            "selfieUrl" to selfieUrl
        ).plus(documentBackUrl?.let { mapOf("documentBackUrl" to it) } ?: emptyMap())
         .plus(addressProofUrl?.let { mapOf("addressProofUrl" to it) } ?: emptyMap())
        return executeFunction("startIdentityVerification", data)
    }
    
    // Product Functions
    suspend fun createProduct(
        title: String,
        description: String,
        category: String,
        price: Double,
        images: List<String> = emptyList(),
        stock: Int? = null,
        active: Boolean = true
    ): Result<Map<String, Any>> {
        val data = mapOf(
            "title" to title,
            "description" to description,
            "category" to category,
            "price" to price,
            "images" to images,
            "active" to active
        ).plus(stock?.let { mapOf("stock" to it) } ?: emptyMap())
        return executeFunction("createProduct", data)
    }
    
    suspend fun updateProduct(
        productId: String,
        updates: Map<String, Any>
    ): Result<Map<String, Any>> {
        val data = mapOf(
            "productId" to productId,
            "updates" to updates
        )
        return executeFunction("updateProduct", data)
    }
    
    suspend fun deleteProduct(productId: String): Result<Map<String, Any>> {
        val data = mapOf("productId" to productId)
        return executeFunction("deleteProduct", data)
    }
    
    // Product Payment Functions
    suspend fun createProductPaymentIntent(orderId: String): Result<Map<String, Any>> {
        val data = mapOf("orderId" to orderId)
        return executeFunction("createProductPaymentIntent", data)
    }
    
    suspend fun confirmProductPayment(paymentIntentId: String): Result<Map<String, Any>> {
        val data = mapOf("paymentIntentId" to paymentIntentId)
        return executeFunction("confirmProductPayment", data)
    }
    
    suspend fun transferPaymentToSeller(orderId: String): Result<Map<String, Any>> {
        val data = mapOf("orderId" to orderId)
        return executeFunction("transferPaymentToSeller", data)
    }
    
    suspend fun refundProductPayment(orderId: String, reason: String? = null): Result<Map<String, Any>> {
        val data = mapOf("orderId" to orderId).plus(reason?.let { mapOf("reason" to it) } ?: emptyMap())
        return executeFunction("refundProductPayment", data)
    }

    // PIX Payment Functions
    suspend fun createPixPayment(orderId: String): Result<Map<String, Any>> {
        val data = mapOf("orderId" to orderId)
        return executeFunction("createPixPayment", data)
    }
    
    suspend fun verifyPixPayment(paymentId: String): Result<Map<String, Any>> {
        val data = mapOf("paymentId" to paymentId)
        return executeFunction("verifyPixPayment", data)
    }
    
    suspend fun confirmPixPayment(paymentId: String): Result<Map<String, Any>> {
        val data = mapOf("paymentId" to paymentId)
        return executeFunction("confirmPixPayment", data)
    }
    
    // Stripe Configuration
    suspend fun getStripePublishableKey(): Result<Map<String, Any>> {
        return executeFunction("getStripePublishableKey", null)
    }
    
    // Shipment Tracking Functions
    suspend fun updateShipmentTracking(
        shipmentId: String,
        status: String,
        trackingCode: String? = null,
        carrier: String? = null,
        customTrackingUrl: String? = null,
        deliveryConfirmationPhotoUrl: String? = null,
        isLocalDelivery: Boolean = false
    ): Result<Map<String, Any>> {
        val data = hashMapOf<String, Any>(
            "shipmentId" to shipmentId,
            "status" to status
        )
        trackingCode?.let { data["trackingCode"] = it }
        carrier?.let { data["carrier"] = it }
        customTrackingUrl?.let { data["customTrackingUrl"] = it }
        deliveryConfirmationPhotoUrl?.let { data["deliveryConfirmationPhotoUrl"] = it }
        data["isLocalDelivery"] = isLocalDelivery
        return executeFunction("updateShipmentTracking", data)
    }
    
    suspend fun trackCorreiosOrder(shipmentId: String): Result<Map<String, Any>> {
        val data = mapOf("shipmentId" to shipmentId)
        return executeFunction("trackCorreiosOrder", data)
    }
    
    // Helper function
    private suspend fun executeFunction(
        functionName: String,
        data: Map<String, Any>?
    ): Result<Map<String, Any>> {
        return try {
            Log.d("FirebaseFunctionsService", "Chamando função: $functionName com dados: $data")
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
            
            Log.d("FirebaseFunctionsService", "Função $functionName executada com sucesso")
            Result.success(resultData ?: emptyMap())
        } catch (e: FirebaseFunctionsException) {
            val code = e.code
            val message = e.message ?: "Erro desconhecido"
            val details = e.details
            
            Log.e("FirebaseFunctionsService", "Erro na função $functionName: code=$code, message=$message, details=$details", e)
            
            // Criar mensagem de erro mais clara
            val errorMessage = when (code) {
                FirebaseFunctionsException.Code.PERMISSION_DENIED -> {
                    "Permissão negada: $message"
                }
                FirebaseFunctionsException.Code.UNAUTHENTICATED -> {
                    "Não autenticado: Faça login novamente"
                }
                FirebaseFunctionsException.Code.INVALID_ARGUMENT -> {
                    "Dados inválidos: $message"
                }
                FirebaseFunctionsException.Code.NOT_FOUND -> {
                    "Recurso não encontrado: $message"
                }
                FirebaseFunctionsException.Code.FAILED_PRECONDITION -> {
                    "Pré-condição falhou: $message"
                }
                else -> {
                    "Erro ao executar $functionName: $message"
                }
            }
            
            Result.failure(Exception(errorMessage, e))
        } catch (e: Exception) {
            Log.e("FirebaseFunctionsService", "Erro inesperado na função $functionName: ${e.message}", e)
            Result.failure(e)
        }
    }
}





