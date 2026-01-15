package com.taskgoapp.taskgo.data.repository

import com.taskgoapp.taskgo.data.firestore.models.ServiceFirestore
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreServicesRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val realtimeRepository: com.taskgoapp.taskgo.data.realtime.RealtimeDatabaseRepository,
    private val authRepository: FirebaseAuthRepository
) {
    // Coleção pública para queries (visualização de serviços por outros usuários)
    private val publicServicesCollection = firestore.collection("services")
    
    // Helper para obter subcoleção do usuário
    private fun getUserServicesCollection(userId: String) = 
        firestore.collection("users").document(userId).collection("services")

    /**
     * Observa todos os serviços de um prestador específico
     * Agora usa subcoleção users/{providerId}/services para isolamento total
     */
    fun observeProviderServices(providerId: String): Flow<List<ServiceFirestore>> = callbackFlow {
        try {
            val userServicesCollection = getUserServicesCollection(providerId)
            val listenerRegistration = userServicesCollection
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.e("FirestoreServicesRepo", "Erro ao observar serviços do prestador: ${error.message}", error)
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    
                    val services = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            doc.toObject(ServiceFirestore::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            android.util.Log.e("FirestoreServicesRepo", "Erro ao converter documento ${doc.id}: ${e.message}", e)
                            null
                        }
                    } ?: emptyList()
                    
                    trySend(services)
                }
            
            awaitClose { listenerRegistration.remove() }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreServicesRepo", "Erro ao configurar listener de serviços do prestador: ${e.message}", e)
            trySend(emptyList())
            close()
        }
    }

    /**
     * Observa todos os serviços ativos (sem filtro de categoria)
     * Usa coleção pública para queries eficientes de visualização
     * NOTA: Esta coleção pública é sincronizada quando serviços são criados/atualizados
     */
    fun observeAllActiveServices(): Flow<List<ServiceFirestore>> = callbackFlow {
        try {
            val listenerRegistration = publicServicesCollection
                .whereEqualTo("active", true)
                .limit(50) // Aumentar limite para melhor cobertura
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.e("FirestoreServicesRepo", "Erro ao observar serviços ativos: ${error.message}", error)
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    
                    val services = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            doc.toObject(ServiceFirestore::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            android.util.Log.e("FirestoreServicesRepo", "Erro ao converter documento ${doc.id}: ${e.message}", e)
                            null
                        }
                    }?.sortedByDescending { it.createdAt }
                    ?: emptyList()
                    
                    trySend(services)
                }
            
            awaitClose { listenerRegistration.remove() }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreServicesRepo", "Erro ao configurar listener de serviços ativos: ${e.message}", e)
            trySend(emptyList())
            close()
        }
    }
    
    /**
     * Observa todos os serviços ativos de uma categoria
     * Usa coleção pública para queries eficientes
     */
    fun observeServicesByCategory(category: String): Flow<List<ServiceFirestore>> = callbackFlow {
        try {
            val listenerRegistration = publicServicesCollection
                .whereEqualTo("category", category)
                .whereEqualTo("active", true)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.e("FirestoreServicesRepo", "Erro ao observar serviços por categoria: ${error.message}", error)
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    
                    val services = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            doc.toObject(ServiceFirestore::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            android.util.Log.e("FirestoreServicesRepo", "Erro ao converter documento ${doc.id}: ${e.message}", e)
                            null
                        }
                    } ?: emptyList()
                    
                    trySend(services)
                }
            
            awaitClose { listenerRegistration.remove() }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreServicesRepo", "Erro ao configurar listener de serviços por categoria: ${e.message}", e)
            trySend(emptyList())
            close()
        }
    }

    /**
     * Busca um serviço por ID
     * Tenta primeiro na coleção pública (para visualização), depois na subcoleção do usuário se necessário
     */
    suspend fun getService(serviceId: String): ServiceFirestore? {
        return try {
            // Primeiro tenta na coleção pública
            val publicDoc = publicServicesCollection.document(serviceId).get().await()
            if (publicDoc.exists()) {
                return publicDoc.toObject(ServiceFirestore::class.java)?.copy(id = publicDoc.id)
            }
            
            // Se não encontrou na pública, tenta buscar na subcoleção do providerId se conhecido
            // Para isso, seria necessário saber o providerId, mas como não temos, retornamos null
            // Em uma implementação completa, poderia manter um índice providerId -> serviceId
            null
        } catch (e: Exception) {
            android.util.Log.e("FirestoreServicesRepo", "Erro ao buscar serviço: ${e.message}", e)
            null
        }
    }

    /**
     * Cria um novo serviço
     * Salva na subcoleção do usuário (users/{providerId}/services) para isolamento total
     * E também na coleção pública (services) para queries eficientes de visualização
     */
    suspend fun createService(service: ServiceFirestore): Result<String> {
        return try {
            val currentUserId = authRepository.getCurrentUser()?.uid
                ?: return Result.failure(Exception("Usuário não autenticado"))
            
            // Garantir que providerId corresponde ao usuário atual
            if (service.providerId != currentUserId) {
                return Result.failure(Exception("providerId não corresponde ao usuário atual"))
            }
            
            val serviceData = service.copy(
                createdAt = java.util.Date(),
                updatedAt = java.util.Date()
            )
            
            // Criar na subcoleção do usuário (dados privados)
            val userServicesCollection = getUserServicesCollection(service.providerId)
            val docRef = userServicesCollection.add(serviceData).await()
            val serviceId = docRef.id
            
            // Criar também na coleção pública (para queries eficientes)
            try {
                publicServicesCollection.document(serviceId).set(serviceData).await()
            } catch (e: Exception) {
                android.util.Log.w("FirestoreServicesRepo", "Erro ao salvar na coleção pública: ${e.message}")
                // Não falhar se pública falhar, mas logar o erro
            }
            
            // Salvar também no Realtime Database para sincronização em tempo real
            try {
                val realtimeData = mapOf(
                    "id" to serviceId,
                    "providerId" to service.providerId,
                    "title" to service.title,
                    "description" to service.description,
                    "category" to service.category,
                    "price" to service.price,
                    "active" to service.active,
                    "latitude" to (service.latitude ?: ""),
                    "longitude" to (service.longitude ?: ""),
                    "createdAt" to (service.createdAt?.time ?: System.currentTimeMillis()),
                    "updatedAt" to (service.updatedAt?.time ?: System.currentTimeMillis())
                )
                realtimeRepository.saveService(serviceId, realtimeData)
            } catch (e: Exception) {
                android.util.Log.w("FirestoreServicesRepo", "Erro ao salvar no Realtime DB: ${e.message}")
            }
            
            Result.success(serviceId)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreServicesRepo", "Erro ao criar serviço: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Atualiza um serviço existente
     * Atualiza tanto na subcoleção do usuário quanto na coleção pública
     */
    suspend fun updateService(serviceId: String, service: ServiceFirestore): Result<Unit> {
        return try {
            val currentUserId = authRepository.getCurrentUser()?.uid
                ?: return Result.failure(Exception("Usuário não autenticado"))
            
            // Garantir que providerId corresponde ao usuário atual
            if (service.providerId != currentUserId) {
                return Result.failure(Exception("Não é possível atualizar serviço de outro usuário"))
            }
            
            val serviceData = service.copy(
                id = serviceId,
                updatedAt = java.util.Date()
            )
            
            // Atualizar na subcoleção do usuário
            val userServicesCollection = getUserServicesCollection(service.providerId)
            userServicesCollection.document(serviceId).set(serviceData).await()
            
            // Atualizar também na coleção pública
            try {
                publicServicesCollection.document(serviceId).set(serviceData).await()
            } catch (e: Exception) {
                android.util.Log.w("FirestoreServicesRepo", "Erro ao atualizar na coleção pública: ${e.message}")
            }
            
            // Atualizar também no Realtime Database
            try {
                val realtimeData = mapOf(
                    "id" to serviceId,
                    "providerId" to service.providerId,
                    "title" to service.title,
                    "description" to service.description,
                    "category" to service.category,
                    "price" to service.price,
                    "active" to service.active,
                    "latitude" to (service.latitude ?: ""),
                    "longitude" to (service.longitude ?: ""),
                    "updatedAt" to (service.updatedAt?.time ?: System.currentTimeMillis())
                )
                realtimeRepository.saveService(serviceId, realtimeData)
            } catch (e: Exception) {
                android.util.Log.w("FirestoreServicesRepo", "Erro ao atualizar no Realtime DB: ${e.message}")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreServicesRepo", "Erro ao atualizar serviço: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Deleta um serviço (soft delete - marca como inativo)
     * Atualiza tanto na subcoleção do usuário quanto na coleção pública
     */
    suspend fun deleteService(serviceId: String): Result<Unit> {
        return try {
            val currentUserId = authRepository.getCurrentUser()?.uid
                ?: return Result.failure(Exception("Usuário não autenticado"))
            
            // Primeiro, buscar o serviço para verificar se pertence ao usuário
            val service = getService(serviceId)
            if (service == null) {
                return Result.failure(Exception("Serviço não encontrado"))
            }
            
            if (service.providerId != currentUserId) {
                return Result.failure(Exception("Não é possível deletar serviço de outro usuário"))
            }
            
            // Atualizar na subcoleção do usuário
            val userServicesCollection = getUserServicesCollection(service.providerId)
            userServicesCollection.document(serviceId).update(
                "active", false,
                "updatedAt", FieldValue.serverTimestamp()
            ).await()
            
            // Atualizar também na coleção pública
            try {
                publicServicesCollection.document(serviceId).update(
                    "active", false,
                    "updatedAt", FieldValue.serverTimestamp()
                ).await()
            } catch (e: Exception) {
                android.util.Log.w("FirestoreServicesRepo", "Erro ao atualizar na coleção pública: ${e.message}")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreServicesRepo", "Erro ao deletar serviço: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Remove permanentemente um serviço do Firestore
     * Remove tanto da subcoleção do usuário quanto da coleção pública
     */
    suspend fun permanentlyDeleteService(serviceId: String): Result<Unit> {
        return try {
            val currentUserId = authRepository.getCurrentUser()?.uid
                ?: return Result.failure(Exception("Usuário não autenticado"))
            
            // Primeiro, buscar o serviço para verificar se pertence ao usuário
            val service = getService(serviceId)
            if (service == null) {
                return Result.failure(Exception("Serviço não encontrado"))
            }
            
            if (service.providerId != currentUserId) {
                return Result.failure(Exception("Não é possível deletar serviço de outro usuário"))
            }
            
            // Deletar da subcoleção do usuário
            val userServicesCollection = getUserServicesCollection(service.providerId)
            userServicesCollection.document(serviceId).delete().await()
            
            // Deletar também da coleção pública
            try {
                publicServicesCollection.document(serviceId).delete().await()
            } catch (e: Exception) {
                android.util.Log.w("FirestoreServicesRepo", "Erro ao deletar da coleção pública: ${e.message}")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreServicesRepo", "Erro ao deletar permanentemente serviço: ${e.message}", e)
            Result.failure(e)
        }
    }
}

