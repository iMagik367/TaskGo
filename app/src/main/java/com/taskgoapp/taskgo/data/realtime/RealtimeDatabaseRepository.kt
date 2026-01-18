package com.taskgoapp.taskgo.data.realtime

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositório para operações no Realtime Database
 * Garante que dados críticos sejam salvos em tempo real
 */
@Singleton
class RealtimeDatabaseRepository @Inject constructor(
    private val database: FirebaseDatabase
) {
    private val rootRef: DatabaseReference = database.reference
    
    /**
     * DEPRECATED: Salva um produto no Realtime Database
     * 
     * ATENÇÃO: Escrita direta em 'products' está bloqueada por regras de segurança.
     * Use Cloud Function 'createProduct' ou 'updateProduct' ao invés deste método.
     * 
     * Este método foi mantido apenas para compatibilidade, mas sempre falhará.
     * Remova chamadas a este método e use FirebaseFunctionsService.createProduct().
     */
    @Deprecated(
        message = "Use FirebaseFunctionsService.createProduct() ou updateProduct() ao invés de escrita direta",
        level = DeprecationLevel.ERROR
    )
    suspend fun saveProduct(productId: String, productData: Map<String, Any>) {
        android.util.Log.w("RealtimeDB", "⚠️ saveProduct está DEPRECATED. Use Cloud Function 'createProduct' ou 'updateProduct'.")
        throw UnsupportedOperationException(
            "Escrita direta em 'products' está bloqueada. Use Cloud Function 'createProduct' ou 'updateProduct'."
        )
    }
    
    /**
     * DEPRECATED: Salva um serviço no Realtime Database
     * 
     * ATENÇÃO: Escrita direta em 'services' está bloqueada por regras de segurança.
     * Use Cloud Function 'createService' ou 'updateService' ao invés deste método.
     * 
     * Este método foi mantido apenas para compatibilidade, mas sempre falhará.
     * Remova chamadas a este método e use FirebaseFunctionsService.createService().
     */
    @Deprecated(
        message = "Use FirebaseFunctionsService.createService() ou updateService() ao invés de escrita direta",
        level = DeprecationLevel.ERROR
    )
    suspend fun saveService(serviceId: String, serviceData: Map<String, Any>) {
        android.util.Log.w("RealtimeDB", "⚠️ saveService está DEPRECATED. Use Cloud Function 'createService' ou 'updateService'.")
        throw UnsupportedOperationException(
            "Escrita direta em 'services' está bloqueada. Use Cloud Function 'createService' ou 'updateService'."
        )
    }
    
    /**
     * DEPRECATED: Salva uma ordem de serviço no Realtime Database
     * 
     * ATENÇÃO: Escrita direta em 'orders' está bloqueada por regras de segurança.
     * Use Cloud Function 'createOrder' ou 'updateOrderStatus' ao invés deste método.
     * 
     * Este método foi mantido apenas para compatibilidade, mas sempre falhará.
     * Remova chamadas a este método e use FirebaseFunctionsService.createOrder().
     */
    @Deprecated(
        message = "Use FirebaseFunctionsService.createOrder() ou updateOrderStatus() ao invés de escrita direta",
        level = DeprecationLevel.ERROR
    )
    suspend fun saveOrder(orderId: String, orderData: Map<String, Any>) {
        android.util.Log.w("RealtimeDB", "⚠️ saveOrder está DEPRECATED. Use Cloud Function 'createOrder' ou 'updateOrderStatus'.")
        throw UnsupportedOperationException(
            "Escrita direta em 'orders' está bloqueada. Use Cloud Function 'createOrder' ou 'updateOrderStatus'."
        )
    }
    
    /**
     * DEPRECATED: Salva um pedido de compra (purchase_order) no Realtime Database
     * 
     * ATENÇÃO: Escrita direta em 'purchase_orders' está bloqueada por regras de segurança.
     * Use Cloud Functions apropriadas ao invés deste método.
     * 
     * Este método foi mantido apenas para compatibilidade, mas sempre falhará.
     */
    @Deprecated(
        message = "Use Cloud Functions apropriadas ao invés de escrita direta",
        level = DeprecationLevel.ERROR
    )
    suspend fun savePurchaseOrder(orderId: String, orderData: Map<String, Any>) {
        android.util.Log.w("RealtimeDB", "⚠️ savePurchaseOrder está DEPRECATED. Use Cloud Functions apropriadas.")
        throw UnsupportedOperationException(
            "Escrita direta em 'purchase_orders' está bloqueada. Use Cloud Functions apropriadas."
        )
    }
    
    /**
     * DEPRECATED: Remove um produto do Realtime Database
     * 
     * ATENÇÃO: Escrita direta em 'products' está bloqueada por regras de segurança.
     * Use Cloud Function 'deleteProduct' ao invés deste método.
     * 
     * Este método foi mantido apenas para compatibilidade, mas sempre falhará.
     * Remova chamadas a este método e use FirebaseFunctionsService.deleteProduct().
     */
    @Deprecated(
        message = "Use FirebaseFunctionsService.deleteProduct() ao invés de escrita direta",
        level = DeprecationLevel.ERROR
    )
    suspend fun deleteProduct(productId: String) {
        android.util.Log.w("RealtimeDB", "⚠️ deleteProduct está DEPRECATED. Use Cloud Function 'deleteProduct'.")
        throw UnsupportedOperationException(
            "Escrita direta em 'products' está bloqueada. Use Cloud Function 'deleteProduct'."
        )
    }
    
    /**
     * DEPRECATED: Remove um serviço do Realtime Database
     * 
     * ATENÇÃO: Escrita direta em 'services' está bloqueada por regras de segurança.
     * Use Cloud Function 'deleteService' ao invés deste método.
     * 
     * Este método foi mantido apenas para compatibilidade, mas sempre falhará.
     * Remova chamadas a este método e use FirebaseFunctionsService.deleteService().
     */
    @Deprecated(
        message = "Use FirebaseFunctionsService.deleteService() ao invés de escrita direta",
        level = DeprecationLevel.ERROR
    )
    suspend fun deleteService(serviceId: String) {
        android.util.Log.w("RealtimeDB", "⚠️ deleteService está DEPRECATED. Use Cloud Function 'deleteService'.")
        throw UnsupportedOperationException(
            "Escrita direta em 'services' está bloqueada. Use Cloud Function 'deleteService'."
        )
    }
    
    /**
     * Atualiza presença do usuário (online/offline)
     */
    suspend fun updatePresence(userId: String, isOnline: Boolean) {
        try {
            val presenceRef = rootRef.child("presence").child(userId)
            if (isOnline) {
                presenceRef.setValue(true).await()
                // Remover automaticamente quando desconectar
                presenceRef.onDisconnect().removeValue()
            } else {
                presenceRef.removeValue().await()
            }
        } catch (e: Exception) {
            android.util.Log.e("RealtimeDB", "Erro ao atualizar presença: ${e.message}", e)
        }
    }
}

