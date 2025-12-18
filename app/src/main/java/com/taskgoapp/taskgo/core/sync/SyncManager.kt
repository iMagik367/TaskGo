package com.taskgoapp.taskgo.core.sync

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import kotlinx.coroutines.tasks.await
import com.taskgoapp.taskgo.data.local.dao.SyncQueueDao
import com.taskgoapp.taskgo.data.local.entity.SyncQueueEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gerenciador de sincronização local -> Firebase
 * 
 * Lógica:
 * 1. Dados são salvos localmente primeiro (instantâneo)
 * 2. Após 1 minuto, sincroniza com Firebase
 * 3. Dados permanecem locais para melhorar carregamento
 * 4. Dados só são apagados quando reescritos pelo usuário
 */
@Singleton
class SyncManager @Inject constructor(
    private val syncQueueDao: SyncQueueDao,
    private val firestore: FirebaseFirestore,
    private val gson: Gson
) {
    private val TAG = "SyncManager"
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var syncJob: Job? = null
    
    companion object {
        private const val SYNC_DELAY_MS = 60_000L // 1 minuto
        private const val MAX_RETRIES = 3
    }
    
    /**
     * Agenda uma operação para sincronização após 1 minuto
     * Se já existe uma pendência para o mesmo tipo e ID, atualiza em vez de criar nova
     */
    suspend fun scheduleSync(
        syncType: String,
        entityId: String,
        operation: String,
        data: Any
    ) {
        try {
            val dataJson = gson.toJson(data)
            val syncAt = System.currentTimeMillis() + SYNC_DELAY_MS
            
            val syncEntity = SyncQueueEntity(
                syncType = syncType,
                entityId = entityId,
                operation = operation,
                data = dataJson,
                syncAt = syncAt,
                status = "pending"
            )
            
            syncQueueDao.upsert(syncEntity)
            Log.d(TAG, "Sincronização agendada: $syncType/$entityId ($operation) para ${syncAt}")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao agendar sincronização: ${e.message}", e)
        }
    }
    
    /**
     * Inicia o processo de sincronização contínua
     */
    fun startSync() {
        if (syncJob?.isActive == true) {
            Log.d(TAG, "Sincronização já está em execução")
            return
        }
        
        syncJob = syncScope.launch {
            while (true) {
                try {
                    syncPendingItems()
                    delay(30_000) // Verifica a cada 30 segundos
                } catch (e: Exception) {
                    Log.e(TAG, "Erro no loop de sincronização: ${e.message}", e)
                    delay(60_000) // Em caso de erro, espera 1 minuto
                }
            }
        }
        
        Log.d(TAG, "Sincronização iniciada")
    }
    
    /**
     * Para o processo de sincronização
     */
    fun stopSync() {
        syncJob?.cancel()
        syncJob = null
        Log.d(TAG, "Sincronização parada")
    }
    
    /**
     * Sincroniza itens pendentes que já passaram do tempo de espera
     */
    private suspend fun syncPendingItems() {
        val pendingSyncs = syncQueueDao.getPendingSyncs()
        
        if (pendingSyncs.isEmpty()) {
            return
        }
        
        Log.d(TAG, "Sincronizando ${pendingSyncs.size} itens pendentes...")
        
        pendingSyncs.forEach { sync ->
            try {
                syncQueueDao.markAsSyncing(sync.id)
                val success = performSync(sync)
                
                if (success) {
                    syncQueueDao.markAsCompleted(sync.id)
                    Log.d(TAG, "Sincronização concluída: ${sync.syncType}/${sync.entityId}")
                } else {
                    if (sync.retryCount < MAX_RETRIES) {
                        syncQueueDao.reschedule(sync.id)
                        Log.d(TAG, "Sincronização reagendada (tentativa ${sync.retryCount + 1}): ${sync.syncType}/${sync.entityId}")
                    } else {
                        syncQueueDao.markAsFailed(sync.id, "Máximo de tentativas atingido")
                        Log.e(TAG, "Sincronização falhou após $MAX_RETRIES tentativas: ${sync.syncType}/${sync.entityId}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao sincronizar ${sync.syncType}/${sync.entityId}: ${e.message}", e)
                
                if (sync.retryCount < MAX_RETRIES) {
                    syncQueueDao.reschedule(sync.id)
                } else {
                    syncQueueDao.markAsFailed(sync.id, e.message ?: "Erro desconhecido")
                }
            }
        }
        
        // Limpa sincronizações concluídas e falhas após muitas tentativas
        syncQueueDao.cleanupCompletedAndFailed()
    }
    
    /**
     * Executa um ciclo único de sincronização (exposto para Workers)
     */
    suspend fun runOneSyncCycle() {
        syncPendingItems()
    }
    
    /**
     * Executa a sincronização real com Firebase
     */
    private suspend fun performSync(sync: SyncQueueEntity): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            when (sync.syncType) {
                "product" -> syncProduct(sync)
                "service" -> syncService(sync)
                "user_profile" -> syncUserProfile(sync)
                "settings" -> syncSettings(sync)
                "order" -> syncOrder(sync)
                "address" -> syncAddress(sync)
                "card" -> syncCard(sync)
                else -> {
                    Log.w(TAG, "Tipo de sincronização não suportado: ${sync.syncType}")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao executar sincronização: ${e.message}", e)
            false
        }
    }
    
    private suspend fun syncProduct(sync: SyncQueueEntity): Boolean {
        return try {
            val productData = gson.fromJson(sync.data, Map::class.java) as Map<String, Any>
            val collection = firestore.collection("products")
            
            when (sync.operation) {
                "create", "update" -> {
                    val docRef = if (sync.entityId.isNotEmpty()) {
                        collection.document(sync.entityId)
                    } else {
                        collection.document()
                    }
                    docRef.set(productData).await()
                    true
                }
                "delete" -> {
                    collection.document(sync.entityId).update("active", false).await()
                    true
                }
                else -> false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao sincronizar produto: ${e.message}", e)
            false
        }
    }
    
    private suspend fun syncService(sync: SyncQueueEntity): Boolean {
        return try {
            val serviceData = gson.fromJson(sync.data, Map::class.java) as Map<String, Any>
            val collection = firestore.collection("services")
            
            when (sync.operation) {
                "create", "update" -> {
                    val docRef = if (sync.entityId.isNotEmpty()) {
                        collection.document(sync.entityId)
                    } else {
                        collection.document()
                    }
                    docRef.set(serviceData).await()
                    true
                }
                "delete" -> {
                    collection.document(sync.entityId).delete().await()
                    true
                }
                else -> false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao sincronizar serviço: ${e.message}", e)
            false
        }
    }
    
    private suspend fun syncUserProfile(sync: SyncQueueEntity): Boolean {
        return try {
            val userData = gson.fromJson(sync.data, Map::class.java) as Map<String, Any>
            val collection = firestore.collection("users")
            
            when (sync.operation) {
                "create", "update" -> {
                    collection.document(sync.entityId).set(userData, SetOptions.merge()).await()
                    true
                }
                else -> false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao sincronizar perfil: ${e.message}", e)
            false
        }
    }
    
    private suspend fun syncSettings(sync: SyncQueueEntity): Boolean {
        return try {
            val settingsData = gson.fromJson(sync.data, Map::class.java) as Map<String, Any>
            val collection = firestore.collection("users")
            
            // Settings são salvos no documento do usuário
            val userId = sync.entityId
            collection.document(userId).update(settingsData).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao sincronizar configurações: ${e.message}", e)
            false
        }
    }
    
    private suspend fun syncOrder(sync: SyncQueueEntity): Boolean {
        return try {
            val orderData = gson.fromJson(sync.data, Map::class.java) as Map<String, Any>
            val collection = firestore.collection("purchase_orders")
            
            when (sync.operation) {
                "create", "update" -> {
                    collection.document(sync.entityId).set(orderData).await()
                    true
                }
                else -> false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao sincronizar pedido: ${e.message}", e)
            false
        }
    }
    
    private suspend fun syncAddress(sync: SyncQueueEntity): Boolean {
        return try {
            val addressData = gson.fromJson(sync.data, Map::class.java) as Map<String, Any>
            val collection = firestore.collection("addresses")
            
            when (sync.operation) {
                "create", "update" -> {
                    collection.document(sync.entityId).set(addressData).await()
                    true
                }
                "delete" -> {
                    collection.document(sync.entityId).delete().await()
                    true
                }
                else -> false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao sincronizar endereço: ${e.message}", e)
            false
        }
    }
    
    private suspend fun syncCard(sync: SyncQueueEntity): Boolean {
        return try {
            val cardData = gson.fromJson(sync.data, Map::class.java) as Map<String, Any>
            val collection = firestore.collection("cards")
            
            when (sync.operation) {
                "create", "update" -> {
                    collection.document(sync.entityId).set(cardData).await()
                    true
                }
                "delete" -> {
                    collection.document(sync.entityId).delete().await()
                    true
                }
                else -> false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao sincronizar cartão: ${e.message}", e)
            false
        }
    }
    
    /**
     * Força sincronização imediata de um item específico
     */
    suspend fun forceSync(syncType: String, entityId: String): Boolean {
        val sync = syncQueueDao.getPendingSync(syncType, entityId) ?: return false
        syncQueueDao.markAsSyncing(sync.id)
        val success = performSync(sync)
        
        if (success) {
            syncQueueDao.markAsCompleted(sync.id)
        } else {
            syncQueueDao.reschedule(sync.id)
        }
        
        return success
    }
    
    /**
     * Observa o número de pendências de sincronização
     */
    fun observePendingCount(): Flow<Int> = flow {
        while (true) {
            val count = syncQueueDao.countByStatus("pending")
            emit(count)
            delay(10_000) // Atualiza a cada 10 segundos
        }
    }
}

