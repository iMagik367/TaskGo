package com.taskgoapp.taskgo.data.local.dao

import androidx.room.*
import com.taskgoapp.taskgo.data.local.entity.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para gerenciar fila de sincronização
 */
@Dao
interface SyncQueueDao {
    
    /**
     * Observa todas as pendências de sincronização
     */
    @Query("SELECT * FROM sync_queue WHERE status = 'pending' AND syncAt <= :currentTime ORDER BY syncAt ASC")
    fun observePendingSyncs(currentTime: Long = System.currentTimeMillis()): Flow<List<SyncQueueEntity>>
    
    /**
     * Busca pendências prontas para sincronização
     */
    @Query("SELECT * FROM sync_queue WHERE status = 'pending' AND syncAt <= :currentTime ORDER BY syncAt ASC LIMIT :limit")
    suspend fun getPendingSyncs(currentTime: Long = System.currentTimeMillis(), limit: Int = 50): List<SyncQueueEntity>
    
    /**
     * Busca pendência específica por tipo e ID
     */
    @Query("SELECT * FROM sync_queue WHERE syncType = :syncType AND entityId = :entityId AND status = 'pending'")
    suspend fun getPendingSync(syncType: String, entityId: String): SyncQueueEntity?
    
    /**
     * Insere ou atualiza pendência de sincronização
     * Se já existe uma pendência para o mesmo tipo e ID, atualiza em vez de criar nova
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(sync: SyncQueueEntity)
    
    /**
     * Marca sincronização como em progresso
     */
    @Query("UPDATE sync_queue SET status = 'syncing' WHERE id = :id")
    suspend fun markAsSyncing(id: Long)
    
    /**
     * Marca sincronização como concluída
     */
    @Query("UPDATE sync_queue SET status = 'completed' WHERE id = :id")
    suspend fun markAsCompleted(id: Long)
    
    /**
     * Marca sincronização como falha e incrementa retry count
     */
    @Query("UPDATE sync_queue SET status = 'failed', retryCount = retryCount + 1, errorMessage = :errorMessage WHERE id = :id")
    suspend fun markAsFailed(id: Long, errorMessage: String)
    
    /**
     * Reagenda sincronização para nova tentativa (após 1 minuto)
     */
    @Query("UPDATE sync_queue SET status = 'pending', syncAt = :newSyncAt, retryCount = retryCount + 1 WHERE id = :id")
    suspend fun reschedule(id: Long, newSyncAt: Long = System.currentTimeMillis() + 60_000)
    
    /**
     * Remove sincronização concluída ou falha após muitas tentativas
     */
    @Query("DELETE FROM sync_queue WHERE status = 'completed' OR (status = 'failed' AND retryCount >= 3)")
    suspend fun cleanupCompletedAndFailed()
    
    /**
     * Remove todas as pendências de um tipo específico
     */
    @Query("DELETE FROM sync_queue WHERE syncType = :syncType AND entityId = :entityId")
    suspend fun removeByTypeAndId(syncType: String, entityId: String)
    
    /**
     * Remove todas as pendências
     */
    @Query("DELETE FROM sync_queue")
    suspend fun clearAll()
    
    /**
     * Conta pendências por status
     */
    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = :status")
    suspend fun countByStatus(status: String): Int
}

