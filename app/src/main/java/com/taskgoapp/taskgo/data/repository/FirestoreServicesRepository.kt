package com.taskgoapp.taskgo.data.repository

import com.taskgoapp.taskgo.data.firestore.models.ServiceFirestore
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.taskgoapp.taskgo.core.firebase.LocationHelper
import com.taskgoapp.taskgo.domain.repository.UserRepository
import com.taskgoapp.taskgo.core.model.Result
import com.taskgoapp.taskgo.core.model.fold
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
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
    private val userRepository: UserRepository
) {
    // REMOVIDO: Coleção global - serviços estão apenas em locations/{locationId}/services
    
    // Helper para obter subcoleção do usuário
    private fun getUserServicesCollection(userId: String) = 
        firestore.collection("users").document(userId).collection("services")

    /**
     * Observa todos os serviços de um prestador específico
     * CRÍTICO: Buscar de locations/{locationId}/services filtrando por providerId
     * (serviços são salvos apenas em locations/{locationId}/services, não em users/{userId}/services)
     */
    fun observeProviderServices(providerId: String): Flow<List<ServiceFirestore>> = callbackFlow {
        var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
        try {
            val currentUser = userRepository.observeCurrentUser().first()
            val userCity = currentUser?.city?.takeIf { it.isNotBlank() } ?: ""
            val userState = currentUser?.state?.takeIf { it.isNotBlank() } ?: ""
            
            if (userCity.isBlank() || userState.isBlank()) {
                trySend(emptyList())
                awaitClose { }
                return@callbackFlow
            }
            
            val collectionToUse = LocationHelper.getLocationCollection(
                firestore,
                "services",
                userCity,
                userState
            )
            
            val locationId = LocationHelper.normalizeLocationId(userCity, userState)
            
            // Filtrar por providerId e ordenar por data de criação
            listenerRegistration = collectionToUse
                .whereEqualTo("providerId", providerId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.e("FirestoreServicesRepo", "Erro ao observar serviços do prestador: ${error.message}", error)
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
                        } ?: emptyList()
                        
                        Log.d("FirestoreServicesRepo", "✅ Serviços do prestador $providerId encontrados: ${services.size}")
                        trySend(services)
                    } catch (e: kotlinx.coroutines.channels.ClosedSendChannelException) {
                        // Canal já foi fechado, ignorar
                    } catch (e: Exception) {
                        android.util.Log.w("FirestoreServicesRepo", "Erro ao enviar dados (canal pode estar fechado): ${e.message}")
                    }
                }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreServicesRepo", "Erro ao configurar listener de serviços do prestador: ${e.message}", e)
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
     * Observa todos os serviços ativos (sem filtro de categoria)
     * ✅ Agora usa coleção por localização locations/{locationId}/services
     * NOTA: Esta coleção é sincronizada quando serviços são criados/atualizados
     */
    fun observeAllActiveServices(): Flow<List<ServiceFirestore>> = callbackFlow {
        var listenerRegistration: ListenerRegistration? = null
        try {
            val currentUser = authRepository.getCurrentUser()
            val userId = currentUser?.uid
            if (userId == null) {
                trySend(emptyList())
                awaitClose { }
                return@callbackFlow
            }
            
            val userDoc = firestore.collection("users").document(userId).get().await()
            val userData = userDoc.data
            val userCity = userData?.get("city") as? String ?: ""
            val userState = userData?.get("state") as? String ?: ""
            
            if (userCity.isBlank() || userState.isBlank()) {
                trySend(emptyList())
                awaitClose { }
                return@callbackFlow
            }
            
            val collectionToUse = LocationHelper.getLocationCollection(firestore, "services", userCity, userState)
            
            listenerRegistration = collectionToUse
                .whereEqualTo("active", true)
                .limit(50)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    
                    val services = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            doc.toObject(ServiceFirestore::class.java)?.copy(id = doc.id)
                        } catch (e: Exception) {
                            null
                        }
                    } ?: emptyList()
                    
                    trySend(services)
                }
        } catch (e: Exception) {
            trySend(emptyList())
        }
        
        awaitClose { listenerRegistration?.remove() }
    }
    
    private suspend fun getLocationForOperation(): Triple<String, String, String> {
        val currentUser = userRepository.observeCurrentUser().first()
        val userCity = currentUser?.city?.takeIf { it.isNotBlank() } ?: ""
        val userState = currentUser?.state?.takeIf { it.isNotBlank() } ?: ""
        
        if (userCity.isBlank() || userState.isBlank()) {
            throw Exception("City e state são obrigatórios")
        }
        
        val locationId = LocationHelper.normalizeLocationId(userCity, userState)
        return Triple(userCity, userState, locationId)
    }
    
    private fun observeAllActiveServicesFromFirestore(
        userCity: String,
        userState: String
    ): Flow<List<ServiceFirestore>> = callbackFlow {
        var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
        try {
            // ✅ Usar coleção por localização
            val collectionToUse = LocationHelper.getLocationCollection(
                firestore,
                "services",
                userCity,
                userState
            )
            
            Log.d("FirestoreServicesRepository", """
                📍 Querying Firestore with location:
                City: $userCity
                State: $userState
                LocationId: ${LocationHelper.normalizeLocationId(userCity, userState)}
                Firestore Path: locations/${LocationHelper.normalizeLocationId(userCity, userState)}/services
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
    fun observeServicesByCategory(category: String): Flow<List<ServiceFirestore>> = callbackFlow {
        try {
            val currentUser = userRepository.observeCurrentUser().first()
            val userCity = currentUser?.city?.takeIf { it.isNotBlank() } ?: ""
            val userState = currentUser?.state?.takeIf { it.isNotBlank() } ?: ""
            
            if (userCity.isBlank() || userState.isBlank()) {
                trySend(emptyList())
                awaitClose { }
                return@callbackFlow
            }
            
            val collectionToUse = LocationHelper.getLocationCollection(
                firestore,
                "services",
                userCity,
                userState
            )
            
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
            val currentUser = userRepository.observeCurrentUser().first()
            val userCity = currentUser?.city?.takeIf { it.isNotBlank() } ?: ""
            val userState = currentUser?.state?.takeIf { it.isNotBlank() } ?: ""
            
            if (userCity.isBlank() || userState.isBlank()) {
                return null
            }
            
            val locationCollection = LocationHelper.getLocationCollection(
                firestore,
                "services",
                userCity,
                userState
            )
            val serviceDoc = locationCollection.document(serviceId).get().await()
            if (serviceDoc.exists()) {
                serviceDoc.toObject(ServiceFirestore::class.java)?.copy(id = serviceDoc.id)
            } else {
                null
            }
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
                ?: return Result.Error(Exception("Usuário não autenticado"))
            
            // Garantir que providerId corresponde ao usuário atual
            if (service.providerId != currentUserId) {
                return Result.Error(Exception("providerId não corresponde ao usuário atual"))
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
                onSuccess = { data: Map<String, Any> ->
                    val serviceId = data["serviceId"] as? String
                    if (serviceId == null) {
                        android.util.Log.e("FirestoreServicesRepo", "Service ID não retornado pela Cloud Function")
                        Result.Error(Exception("Service ID não retornado pela Cloud Function"))
                    } else {
                        android.util.Log.d("FirestoreServicesRepo", "Serviço criado com sucesso via Cloud Function: $serviceId")
                        Result.Success(serviceId)
                    }
                },
                onFailure = { error: Throwable ->
                    android.util.Log.e("FirestoreServicesRepo", "Erro ao criar serviço via Cloud Function: ${error.message}", error)
                    Result.Error(error)
                }
            )
        } catch (e: Exception) {
            android.util.Log.e("FirestoreServicesRepo", "Erro ao criar serviço: ${e.message}", e)
            Result.Error(e)
        }
    }

    /**
     * Atualiza um serviço existente
     * Atualiza tanto na subcoleção do usuário quanto na coleção pública
     */
    suspend fun updateService(serviceId: String, service: ServiceFirestore): Result<Unit> {
        return try {
            val currentUserId = authRepository.getCurrentUser()?.uid
                ?: return Result.Error(Exception("Usuário não autenticado"))
            
            // Garantir que providerId corresponde ao usuário atual
            if (service.providerId != currentUserId) {
                return Result.Error(Exception("Não é possível atualizar serviço de outro usuário"))
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
                onSuccess = { _: Map<String, Any> ->
                    android.util.Log.d("FirestoreServicesRepo", "Serviço atualizado com sucesso via Cloud Function: $serviceId")
                    Result.Success(Unit)
                },
                onFailure = { error: Throwable ->
                    android.util.Log.e("FirestoreServicesRepo", "Erro ao atualizar serviço via Cloud Function: ${error.message}", error)
                    Result.Error(error)
                }
            )
        } catch (e: Exception) {
            android.util.Log.e("FirestoreServicesRepo", "Erro ao atualizar serviço: ${e.message}", e)
            Result.Error(e)
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
                onSuccess = { _: Map<String, Any> ->
                    android.util.Log.d("FirestoreServicesRepo", "Serviço deletado com sucesso via Cloud Function: $serviceId")
                    Result.Success(Unit)
                },
                onFailure = { error: Throwable ->
                    android.util.Log.e("FirestoreServicesRepo", "Erro ao deletar serviço via Cloud Function: ${error.message}", error)
                    Result.Error(error)
                }
            )
        } catch (e: Exception) {
            android.util.Log.e("FirestoreServicesRepo", "Erro ao deletar serviço: ${e.message}", e)
            Result.Error(e)
        }
    }
    
    @Deprecated("Usar deleteService que usa Cloud Function")
    suspend fun deleteServiceOld(serviceId: String): Result<Unit> {
        return try {
            val currentUserId = authRepository.getCurrentUser()?.uid
                ?: return Result.Error(Exception("Usuário não autenticado"))
            
            // Primeiro, buscar o serviço para verificar se pertence ao usuário
            val service = getService(serviceId)
            if (service == null) {
                return Result.Error(Exception("Serviço não encontrado"))
            }
            
            if (service.providerId != currentUserId) {
                return Result.Error(Exception("Não é possível deletar serviço de outro usuário"))
            }
            
            // Atualizar na subcoleção do usuário
            val userServicesCollection = getUserServicesCollection(service.providerId)
            userServicesCollection.document(serviceId).update(
                "active", false,
                "updatedAt", FieldValue.serverTimestamp()
            ).await()
            
            // REMOVIDO: Atualização na coleção global - serviços estão apenas em locations/{locationId}/services
            
            Result.Success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreServicesRepo", "Erro ao deletar serviço: ${e.message}", e)
            Result.Error(e)
        }
    }

    /**
     * Remove permanentemente um serviço do Firestore
     * Remove tanto da subcoleção do usuário quanto da coleção pública
     */
    suspend fun permanentlyDeleteService(serviceId: String): Result<Unit> {
        return try {
            val currentUserId = authRepository.getCurrentUser()?.uid
                ?: return Result.Error(Exception("Usuário não autenticado"))
            
            // Primeiro, buscar o serviço para verificar se pertence ao usuário
            val service = getService(serviceId)
            if (service == null) {
                return Result.Error(Exception("Serviço não encontrado"))
            }
            
            if (service.providerId != currentUserId) {
                return Result.Error(Exception("Não é possível deletar serviço de outro usuário"))
            }
            
            // Deletar da subcoleção do usuário
            val userServicesCollection = getUserServicesCollection(service.providerId)
            userServicesCollection.document(serviceId).delete().await()
            
            // REMOVIDO: Deleção da coleção global - serviços estão apenas em locations/{locationId}/services
            
            Result.Success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreServicesRepo", "Erro ao deletar permanentemente serviço: ${e.message}", e)
            Result.Error(e)
        }
    }
}

