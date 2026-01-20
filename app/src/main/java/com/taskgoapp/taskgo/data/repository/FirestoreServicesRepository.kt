package com.taskgoapp.taskgo.data.repository

import com.taskgoapp.taskgo.data.firestore.models.ServiceFirestore
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.taskgoapp.taskgo.core.firebase.LocationHelper
import com.taskgoapp.taskgo.core.location.LocationStateManager
import com.taskgoapp.taskgo.core.location.LocationState
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.channels.awaitClose
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreServicesRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val realtimeRepository: com.taskgoapp.taskgo.data.realtime.RealtimeDatabaseRepository,
    private val authRepository: FirebaseAuthRepository,
    private val functionsService: com.taskgoapp.taskgo.data.firebase.FirebaseFunctionsService,
    private val locationStateManager: LocationStateManager
) {
    // Coleção pública para queries (visualização de serviços por outros usuários)
    // DEBUG ONLY - Mantida apenas para compatibilidade durante migração
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
     * ✅ Agora usa coleção por localização locations/{locationId}/services
     * NOTA: Esta coleção é sincronizada quando serviços são criados/atualizados
     */
    fun observeAllActiveServices(): Flow<List<ServiceFirestore>> = locationStateManager.locationState
        .flatMapLatest { locationState ->
            when (locationState) {
                is LocationState.Loading -> {
                    Log.w("BLOCKED_QUERY", "Firestore query blocked: location not ready (Loading) - observeAllActiveServices")
                    flowOf(emptyList())
                }
                is LocationState.Error -> {
                    Log.e("BLOCKED_QUERY", "Firestore query blocked: location error - ${locationState.reason} - observeAllActiveServices")
                    flowOf(emptyList())
                }
                is LocationState.Ready -> {
                    // ✅ Localização pronta - fazer query Firestore
                    val locationId = locationState.locationId
                    
                    // 🚨 PROTEÇÃO: Nunca permitir "unknown" como locationId válido
                    if (locationId == "unknown" || locationId.isBlank()) {
                        Log.e("FATAL_LOCATION", "Attempted Firestore query with invalid locationId: $locationId - observeAllActiveServices")
                        flowOf(emptyList())
                    } else {
                        observeAllActiveServicesFromFirestore(locationState)
                    }
                }
            }
        }
    
    private fun observeAllActiveServicesFromFirestore(
        locationState: LocationState.Ready
    ): Flow<List<ServiceFirestore>> = callbackFlow {
        var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
        try {
            // ✅ Usar coleção por localização
            val collectionToUse = LocationHelper.getLocationCollection(
                firestore,
                "services",
                locationState.city,
                locationState.state
            )
            
            Log.d("FirestoreServicesRepository", """
                📍 Querying Firestore with location:
                City: ${locationState.city}
                State: ${locationState.state}
                LocationId: ${locationState.locationId}
                Firestore Path: locations/${locationState.locationId}/services
            """.trimIndent())
            
            listenerRegistration = collectionToUse
                .whereEqualTo("active", true)
                .limit(50) // Aumentar limite para melhor cobertura
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.e("FirestoreServicesRepo", "Erro ao observar serviços ativos: ${error.message}", error)
                        try {
                            trySend(emptyList())
                        } catch (e: kotlinx.coroutines.channels.ClosedSendChannelException) {
                            // Canal já foi fechado, ignorar
                        } catch (e: Exception) {
                            android.util.Log.w("FirestoreServicesRepo", "Erro ao enviar dados (canal pode estar fechado): ${e.message}")
                        }
                        return@addSnapshotListener
                    }
                    
                    try {
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
                    } catch (e: kotlinx.coroutines.channels.ClosedSendChannelException) {
                        // Canal já foi fechado, ignorar
                    } catch (e: Exception) {
                        android.util.Log.w("FirestoreServicesRepo", "Erro ao enviar dados (canal pode estar fechado): ${e.message}")
                    }
                }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreServicesRepo", "Erro ao configurar listener de serviços ativos: ${e.message}", e)
            try {
                trySend(emptyList())
            } catch (ex: Exception) {
                // Ignorar se não conseguir enviar
            }
        }
        
        awaitClose { 
            try {
                listenerRegistration?.remove()
            } catch (e: Exception) {
                android.util.Log.w("FirestoreServicesRepo", "Erro ao remover listener: ${e.message}")
            }
        }
    }
    
    /**
     * Observa todos os serviços ativos de uma categoria
     * ✅ Agora usa coleção por localização locations/{locationId}/services
     */
    fun observeServicesByCategory(category: String): Flow<List<ServiceFirestore>> = locationStateManager.locationState
        .flatMapLatest { locationState ->
            when (locationState) {
                is LocationState.Loading -> {
                    Log.w("BLOCKED_QUERY", "Firestore query blocked: location not ready (Loading) - observeServicesByCategory")
                    flowOf(emptyList())
                }
                is LocationState.Error -> {
                    Log.e("BLOCKED_QUERY", "Firestore query blocked: location error - ${locationState.reason} - observeServicesByCategory")
                    flowOf(emptyList())
                }
                is LocationState.Ready -> {
                    // ✅ Localização pronta - fazer query Firestore
                    val locationId = locationState.locationId
                    
                    // 🚨 PROTEÇÃO: Nunca permitir "unknown" como locationId válido
                    if (locationId == "unknown" || locationId.isBlank()) {
                        Log.e("FATAL_LOCATION", "Attempted Firestore query with invalid locationId: $locationId - observeServicesByCategory")
                        flowOf(emptyList())
                    } else {
                        observeServicesByCategoryFromFirestore(locationState, category)
                    }
                }
            }
        }
    
    private fun observeServicesByCategoryFromFirestore(
        locationState: LocationState.Ready,
        category: String
    ): Flow<List<ServiceFirestore>> = callbackFlow {
        try {
            // ✅ Usar coleção por localização
            val collectionToUse = LocationHelper.getLocationCollection(
                firestore,
                "services",
                locationState.city,
                locationState.state
            )
            
            Log.d("FirestoreServicesRepository", """
                📍 Querying Firestore with location:
                City: ${locationState.city}
                State: ${locationState.state}
                LocationId: ${locationState.locationId}
                Category: $category
                Firestore Path: locations/${locationState.locationId}/services
            """.trimIndent())
            
            val listenerRegistration = collectionToUse
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
            
            // Usar Cloud Function createService (backend como autoridade)
            val result = functionsService.createService(
                title = service.title,
                description = service.description,
                category = service.category,
                price = service.price,
                latitude = service.latitude,
                longitude = service.longitude,
                active = service.active
            )
            
            result.fold(
                onSuccess = { data ->
                    val serviceId = data["serviceId"] as? String
                        ?: return Result.failure(Exception("Service ID não retornado pela Cloud Function"))
                    
                    android.util.Log.d("FirestoreServicesRepo", "Serviço criado com sucesso via Cloud Function: $serviceId")
                    Result.success(serviceId)
                },
                onFailure = { error ->
                    android.util.Log.e("FirestoreServicesRepo", "Erro ao criar serviço via Cloud Function: ${error.message}", error)
                    Result.failure(error)
                }
            )
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
            
            // Usar Cloud Function updateService (backend como autoridade)
            val updates = mutableMapOf<String, Any>().apply {
                put("title", service.title)
                put("description", service.description)
                put("category", service.category)
                put("price", service.price)
                service.latitude?.let { put("latitude", it) }
                service.longitude?.let { put("longitude", it) }
                put("active", service.active)
            }
            
            val result = functionsService.updateService(serviceId, updates)
            
            result.fold(
                onSuccess = {
                    android.util.Log.d("FirestoreServicesRepo", "Serviço atualizado com sucesso via Cloud Function: $serviceId")
                    Result.success(Unit)
                },
                onFailure = { error ->
                    android.util.Log.e("FirestoreServicesRepo", "Erro ao atualizar serviço via Cloud Function: ${error.message}", error)
                    Result.failure(error)
                }
            )
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
            // Usar Cloud Function deleteService (backend como autoridade)
            val result = functionsService.deleteService(serviceId)
            
            result.fold(
                onSuccess = {
                    android.util.Log.d("FirestoreServicesRepo", "Serviço deletado com sucesso via Cloud Function: $serviceId")
                    Result.success(Unit)
                },
                onFailure = { error ->
                    android.util.Log.e("FirestoreServicesRepo", "Erro ao deletar serviço via Cloud Function: ${error.message}", error)
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            android.util.Log.e("FirestoreServicesRepo", "Erro ao deletar serviço: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    @Deprecated("Usar deleteService que usa Cloud Function")
    suspend fun deleteServiceOld(serviceId: String): Result<Unit> {
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

