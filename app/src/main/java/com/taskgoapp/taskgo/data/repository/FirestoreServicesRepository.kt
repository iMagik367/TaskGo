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
    private val realtimeRepository: com.taskgoapp.taskgo.data.realtime.RealtimeDatabaseRepository
) {
    private val servicesCollection = firestore.collection("services")

    /**
     * Observa todos os serviços de um prestador específico
     * Nota: Esta query requer um índice composto no Firestore:
     * Collection: services
     * Fields: providerId (Ascending), createdAt (Descending)
     * Criar em: https://console.firebase.google.com/project/task-go-ee85f/firestore/indexes
     */
    fun observeProviderServices(providerId: String): Flow<List<ServiceFirestore>> = callbackFlow {
        try {
            val listenerRegistration = servicesCollection
                .whereEqualTo("providerId", providerId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        // Log do erro mas não crashar o app
                        android.util.Log.e("FirestoreServicesRepo", "Erro ao observar serviços do prestador: ${error.message}", error)
                        // Emite lista vazia em caso de erro ao invés de fechar o channel
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
            // Emite lista vazia e fecha o channel normalmente
            trySend(emptyList())
            close()
        }
    }

    /**
     * Observa todos os serviços ativos (sem filtro de categoria)
     */
    fun observeAllActiveServices(): Flow<List<ServiceFirestore>> = callbackFlow {
        try {
            val listenerRegistration = servicesCollection
                .whereEqualTo("active", true)
                .limit(20) // Limitar a 20 serviços para performance
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        // Log do erro mas não crashar o app
                        android.util.Log.e("FirestoreServicesRepo", "Erro ao observar serviços ativos: ${error.message}", error)
                        // Emite lista vazia em caso de erro ao invés de fechar o channel
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
                    }?.sortedByDescending { it.createdAt } // Ordenar localmente após buscar
                    ?: emptyList()
                    
                    trySend(services)
                }
            
            awaitClose { listenerRegistration.remove() }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreServicesRepo", "Erro ao configurar listener de serviços ativos: ${e.message}", e)
            // Emite lista vazia e fecha o channel normalmente
            trySend(emptyList())
            close()
        }
    }
    
    /**
     * Observa todos os serviços ativos de uma categoria
     */
    fun observeServicesByCategory(category: String): Flow<List<ServiceFirestore>> = callbackFlow {
        try {
            val listenerRegistration = servicesCollection
                .whereEqualTo("category", category)
                .whereEqualTo("active", true)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        // Log do erro mas não crashar o app
                        android.util.Log.e("FirestoreServicesRepo", "Erro ao observar serviços por categoria: ${error.message}", error)
                        // Emite lista vazia em caso de erro ao invés de fechar o channel
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
            // Emite lista vazia e fecha o channel normalmente
            trySend(emptyList())
            close()
        }
    }

    /**
     * Busca um serviço por ID
     */
    suspend fun getService(serviceId: String): ServiceFirestore? {
        return try {
            val document = servicesCollection.document(serviceId).get().await()
            document.toObject(ServiceFirestore::class.java)?.copy(id = document.id)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Cria um novo serviço
     */
    suspend fun createService(service: ServiceFirestore): Result<String> {
        return try {
            val serviceData = service.copy(
                createdAt = java.util.Date(),
                updatedAt = java.util.Date()
            )
            val docRef = servicesCollection.add(serviceData).await()
            val serviceId = docRef.id
            
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
                // Não falhar se Realtime DB falhar
            }
            
            Result.success(serviceId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Atualiza um serviço existente
     */
    suspend fun updateService(serviceId: String, service: ServiceFirestore): Result<Unit> {
        return try {
            val serviceData = service.copy(
                id = serviceId,
                updatedAt = java.util.Date()
            )
            servicesCollection.document(serviceId).set(serviceData).await()
            
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
            Result.failure(e)
        }
    }

    /**
     * Deleta um serviço (soft delete - marca como inativo)
     */
    suspend fun deleteService(serviceId: String): Result<Unit> {
        return try {
            servicesCollection.document(serviceId).update(
                "active", false,
                "updatedAt", FieldValue.serverTimestamp()
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Remove permanentemente um serviço do Firestore
     */
    suspend fun permanentlyDeleteService(serviceId: String): Result<Unit> {
        return try {
            servicesCollection.document(serviceId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

