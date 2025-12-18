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
     * Salva um produto no Realtime Database
     */
    suspend fun saveProduct(productId: String, productData: Map<String, Any>) {
        try {
            rootRef.child("products").child(productId).setValue(productData).await()
        } catch (e: Exception) {
            android.util.Log.e("RealtimeDB", "Erro ao salvar produto: ${e.message}", e)
        }
    }
    
    /**
     * Salva um serviço no Realtime Database
     */
    suspend fun saveService(serviceId: String, serviceData: Map<String, Any>) {
        try {
            rootRef.child("services").child(serviceId).setValue(serviceData).await()
        } catch (e: Exception) {
            android.util.Log.e("RealtimeDB", "Erro ao salvar serviço: ${e.message}", e)
        }
    }
    
    /**
     * Salva uma ordem de serviço no Realtime Database
     */
    suspend fun saveOrder(orderId: String, orderData: Map<String, Any>) {
        try {
            rootRef.child("orders").child(orderId).setValue(orderData).await()
        } catch (e: Exception) {
            android.util.Log.e("RealtimeDB", "Erro ao salvar ordem: ${e.message}", e)
        }
    }
    
    /**
     * Salva um pedido de compra (purchase_order) no Realtime Database
     */
    suspend fun savePurchaseOrder(orderId: String, orderData: Map<String, Any>) {
        try {
            rootRef.child("purchase_orders").child(orderId).setValue(orderData).await()
        } catch (e: Exception) {
            android.util.Log.e("RealtimeDB", "Erro ao salvar pedido de compra: ${e.message}", e)
        }
    }
    
    /**
     * Remove um produto do Realtime Database
     */
    suspend fun deleteProduct(productId: String) {
        try {
            rootRef.child("products").child(productId).removeValue().await()
        } catch (e: Exception) {
            android.util.Log.e("RealtimeDB", "Erro ao deletar produto: ${e.message}", e)
        }
    }
    
    /**
     * Remove um serviço do Realtime Database
     */
    suspend fun deleteService(serviceId: String) {
        try {
            rootRef.child("services").child(serviceId).removeValue().await()
        } catch (e: Exception) {
            android.util.Log.e("RealtimeDB", "Erro ao deletar serviço: ${e.message}", e)
        }
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

