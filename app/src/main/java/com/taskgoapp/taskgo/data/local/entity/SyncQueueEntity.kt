package com.taskgoapp.taskgo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * Entidade para gerenciar fila de sincronização com Firebase
 * Armazena operações pendentes que serão sincronizadas após 1 minuto
 */
@Entity(
    tableName = "sync_queue",
    indices = [Index(value = ["syncType", "entityId"], unique = true)]
)
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /**
     * Tipo de entidade a ser sincronizada
     * Ex: "product", "service", "user_profile", "settings", etc.
     */
    val syncType: String,
    
    /**
     * ID da entidade a ser sincronizada
     */
    val entityId: String,
    
    /**
     * Tipo de operação: "create", "update", "delete"
     */
    val operation: String,
    
    /**
     * Dados da entidade em formato JSON para sincronização
     */
    val data: String,
    
    /**
     * Timestamp de quando a operação foi criada
     */
    val createdAt: Long = System.currentTimeMillis(),
    
    /**
     * Timestamp de quando deve ser sincronizada (createdAt + 1 minuto)
     */
    val syncAt: Long = System.currentTimeMillis() + 60_000,
    
    /**
     * Número de tentativas de sincronização
     */
    val retryCount: Int = 0,
    
    /**
     * Status da sincronização: "pending", "syncing", "completed", "failed"
     */
    val status: String = "pending",
    
    /**
     * Mensagem de erro (se houver)
     */
    val errorMessage: String? = null
)

