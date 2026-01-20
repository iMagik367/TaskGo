package com.taskgoapp.taskgo.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.taskgoapp.taskgo.core.model.Result
import com.taskgoapp.taskgo.core.model.Story
import com.taskgoapp.taskgo.core.model.StoryAnalytics
import com.taskgoapp.taskgo.core.model.StoryView
import com.taskgoapp.taskgo.core.model.StoryInteractions
import com.taskgoapp.taskgo.data.firestore.models.StoryFirestore
import com.taskgoapp.taskgo.data.firestore.models.UserFirestore
import com.taskgoapp.taskgo.data.mapper.StoryMapper
import com.taskgoapp.taskgo.domain.repository.StoriesRepository
import com.taskgoapp.taskgo.core.firebase.LocationHelper
import com.taskgoapp.taskgo.core.location.LocationStateManager
import com.taskgoapp.taskgo.core.location.LocationState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import android.util.Log
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.runBlocking as runBlockingKt
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreStoriesRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: FirebaseAuthRepository,
    private val functionsService: com.taskgoapp.taskgo.data.firebase.FirebaseFunctionsService,
    private val userRepository: com.taskgoapp.taskgo.domain.repository.UserRepository,
    private val locationStateManager: LocationStateManager
) : StoriesRepository {
    
    // DEBUG ONLY - Coleção global mantida apenas para compatibilidade durante migração
    // REMOVER APÓS VALIDAÇÃO COMPLETA
    private val storiesCollectionGlobal = firestore.collection("stories")
    private val storyViewsCollection = firestore.collection("story_views")
    
    // Helper para obter subcoleção de stories do usuário
    private fun getUserStoriesCollection(userId: String) = 
        firestore.collection("users").document(userId).collection("stories")
    
    override fun observeStories(
        currentUserId: String,
        radiusKm: Double,
        userLocation: Pair<Double, Double>?
    ): Flow<List<Story>> = locationStateManager.locationState
        .flatMapLatest { locationState ->
            when (locationState) {
                is LocationState.Loading -> {
                    Log.w("BLOCKED_QUERY", "Firestore query blocked: location not ready (Loading) - observeStories")
                    flowOf(emptyList())
                }
                is LocationState.Error -> {
                    Log.e("BLOCKED_QUERY", "Firestore query blocked: location error - ${locationState.reason} - observeStories")
                    flowOf(emptyList())
                }
                is LocationState.Ready -> {
                    // ✅ Localização pronta - fazer query Firestore
                    val locationId = locationState.locationId
                    
                    // 🚨 PROTEÇÃO: Nunca permitir "unknown" como locationId válido
                    if (locationId == "unknown" || locationId.isBlank()) {
                        Log.e("FATAL_LOCATION", "Attempted Firestore query with invalid locationId: $locationId - observeStories")
                        flowOf(emptyList())
                    } else {
                        observeStoriesFromFirestore(locationState, currentUserId, radiusKm, userLocation)
                    }
                }
            }
        }
    
    private fun observeStoriesFromFirestore(
        locationState: LocationState.Ready,
        currentUserId: String,
        radiusKm: Double,
        userLocation: Pair<Double, Double>?
    ): Flow<List<Story>> = callbackFlow {
        try {
            // Timestamp de 24 horas atrás (stories não expiradas)
            val twentyFourHoursAgo = Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000)
            val timestamp = com.google.firebase.Timestamp(twentyFourHoursAgo)
            
            val collectionToUse = LocationHelper.getLocationCollection(
                firestore,
                "stories",
                locationState.city,
                locationState.state
            )
            
            Log.d("FirestoreStoriesRepository", """
                📍 Querying Firestore with location:
                City: ${locationState.city}
                State: ${locationState.state}
                LocationId: ${locationState.locationId}
                Firestore Path: locations/${locationState.locationId}/stories
            """.trimIndent())
            
            // Query: stories não expiradas, ordenadas por data de criação (mais recentes primeiro)
            // Nota: Firestore requer índice composto para múltiplos orderBy, então usamos apenas createdAt
            val query = collectionToUse
                .whereGreaterThan("expiresAt", timestamp)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(100) // Limitar para performance
            
            val listenerRegistration = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreStoriesRepository", 
                        "❌ Erro ao observar stories: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                if (snapshot == null) {
                    Log.w("FirestoreStoriesRepository", 
                        "⚠️ Snapshot vazio (sem stories encontradas)")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                Log.d("FirestoreStoriesRepository", 
                    "Snapshot recebido: size=${snapshot.size()}, collection=locations/${locationState.locationId}/stories")
                
                // 📍 SNAPSHOT PROOF - Logar TUDO que vem do Firestore
                Log.d("FirestoreSnapshot", """
                    📍 FRONTEND SNAPSHOT PROOF
                    Collection path: ${collectionToUse.path}
                    Snapshot empty: ${snapshot.isEmpty}
                    Snapshot size: ${snapshot.size()}
                    Documents count: ${snapshot.documents.size}
                """.trimIndent())
                
                snapshot.documents.forEachIndexed { index, doc ->
                    Log.d("FirestoreSnapshot", """
                        📍 FRONTEND SNAPSHOT PROOF - Document $index
                        Doc ID: ${doc.id}
                        Doc data keys: ${doc.data?.keys?.joinToString(", ") ?: "null"}
                        Doc has expiresAt: ${doc.data?.containsKey("expiresAt")}
                        Doc has createdAt: ${doc.data?.containsKey("createdAt")}
                    """.trimIndent())
                }
                
                
                val storiesList = mutableListOf<Story>()
                snapshot?.documents?.forEach { doc ->
                    try {
                        val data = doc.data ?: return@forEach
                        val locationData = data["location"] as? Map<*, *>
                        
                        val locationFirestore = locationData?.let {
                            com.taskgoapp.taskgo.data.firestore.models.StoryLocationFirestore(
                                city = it["city"] as? String ?: "",
                                state = it["state"] as? String ?: "",
                                latitude = (it["latitude"] as? Number)?.toDouble() ?: 0.0,
                                longitude = (it["longitude"] as? Number)?.toDouble() ?: 0.0
                            )
                        }
                        
                        // Converter createdAt e expiresAt corretamente (pode vir como Long ou Timestamp)
                        val createdAtValue = data["createdAt"]
                        val createdAt = when (createdAtValue) {
                            is com.google.firebase.Timestamp -> createdAtValue
                            is Long -> com.google.firebase.Timestamp(createdAtValue / 1000, ((createdAtValue % 1000) * 1_000_000).toInt())
                            is java.util.Date -> com.google.firebase.Timestamp(createdAtValue)
                            else -> null
                        }
                        
                        val expiresAtValue = data["expiresAt"]
                        val expiresAt = when (expiresAtValue) {
                            is com.google.firebase.Timestamp -> expiresAtValue
                            is Long -> com.google.firebase.Timestamp(expiresAtValue / 1000, ((expiresAtValue % 1000) * 1_000_000).toInt())
                            is java.util.Date -> com.google.firebase.Timestamp(expiresAtValue)
                            else -> null
                        }
                        
                        val storyFirestore = StoryFirestore(
                            id = doc.id,
                            userId = data["userId"] as? String ?: "",
                            userName = data["userName"] as? String ?: "",
                            userAvatarUrl = data["userAvatarUrl"] as? String,
                            mediaUrl = data["mediaUrl"] as? String ?: "",
                            mediaType = data["mediaType"] as? String ?: "image",
                            thumbnailUrl = data["thumbnailUrl"] as? String,
                            caption = data["caption"] as? String,
                            createdAt = createdAt,
                            expiresAt = expiresAt,
                            viewsCount = (data["viewsCount"] as? Number)?.toInt() ?: 0,
                            location = locationFirestore
                        )
                        
                        kotlinx.coroutines.runBlocking {
                            val isViewed = checkIfStoryViewed(doc.id, currentUserId)
                            storiesList.add(with(StoryMapper) {
                                storyFirestore.toModel(isViewed)
                            })
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("FirestoreStoriesRepository", "Erro ao processar story: ${e.message}", e)
                    }
                }
                
                val stories = storiesList
                
                // Filtrar stories do próprio usuário (já que é para o feed de outros)
                var filteredStories = stories.filter { it.userId != currentUserId }
                
                // Filtrar por distância usando GPS (raio de 100km)
                if (userLocation != null) {
                    val (userLat, userLng) = userLocation
                    filteredStories = filteredStories.filter { story ->
                        story.location?.let { storyLocation ->
                            if (storyLocation.latitude != 0.0 && storyLocation.longitude != 0.0) {
                                val distance = com.taskgoapp.taskgo.core.location.calculateDistance(
                                    userLat,
                                    userLng,
                                    storyLocation.latitude,
                                    storyLocation.longitude
                                )
                                distance <= radiusKm // Usar o raio fornecido (padrão 50km, mas pode ser 100km)
                            } else {
                                false // Se não tem localização GPS, não mostrar
                            }
                        } ?: false // Se não tem localização, não mostrar
                    }
                }
                
                trySend(filteredStories)
            }
            
            awaitClose { listenerRegistration.remove() }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreStoriesRepository", "Erro ao observar stories: ${e.message}", e)
            trySend(emptyList())
            awaitClose { }
        }
    }
    
    override fun observeUserStories(userId: String, currentUserId: String): Flow<List<Story>> = callbackFlow {
        try {
            val twentyFourHoursAgo = Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000)
            val timestamp = com.google.firebase.Timestamp(twentyFourHoursAgo)
            
            // CRÍTICO: Obter localização do usuário para buscar da coleção correta
            var currentCity: String? = null
            var currentState: String = ""
            var collectionToUse: com.google.firebase.firestore.CollectionReference = storiesCollectionGlobal
            
            try {
                val user = withTimeoutOrNull(2000) {
                    userRepository.observeCurrentUser().firstOrNull()
                }
                currentCity = user?.city?.takeIf { it.isNotBlank() }
                currentState = user?.state?.takeIf { it.isNotBlank() } ?: ""
                
                if (currentCity != null && currentState.isNotBlank()) {
                    // Usar coleção por localização
                    collectionToUse = LocationHelper.getLocationCollection(firestore, "stories", currentCity!!, currentState)
                    android.util.Log.d("FirestoreStoriesRepository", "🔵 Usando coleção por localização para stories do usuário: locations/${LocationHelper.normalizeLocationId(currentCity!!, currentState)}/stories")
                } else {
                    // Fallback: usar coleção global
                    collectionToUse = storiesCollectionGlobal
                    android.util.Log.w("FirestoreStoriesRepository", "⚠️ Localização não disponível para stories do usuário, usando coleção global")
                }
            } catch (e: Exception) {
                android.util.Log.w("FirestoreStoriesRepository", "Erro ao obter usuário para localização de stories: ${e.message}")
                collectionToUse = storiesCollectionGlobal
            }
            
            val listenerRegistration = collectionToUse
                .whereEqualTo("userId", userId)
                .whereGreaterThan("expiresAt", timestamp)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.e("FirestoreStoriesRepository", "Erro ao observar stories do usuário: ${error.message}", error)
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    
                    val storiesList = mutableListOf<Story>()
                    snapshot?.documents?.forEach { doc ->
                        try {
                            val data = doc.data ?: return@forEach
                            val locationData = data["location"] as? Map<*, *>
                            
                            val locationFirestore = locationData?.let {
                                com.taskgoapp.taskgo.data.firestore.models.StoryLocationFirestore(
                                    city = it["city"] as? String ?: "",
                                    state = it["state"] as? String ?: "",
                                    latitude = (it["latitude"] as? Number)?.toDouble() ?: 0.0,
                                    longitude = (it["longitude"] as? Number)?.toDouble() ?: 0.0
                                )
                            }
                            
                            // Converter createdAt e expiresAt corretamente (pode vir como Long ou Timestamp)
                            val createdAtValue = data["createdAt"]
                            val createdAt = when (createdAtValue) {
                                is com.google.firebase.Timestamp -> createdAtValue
                                is Long -> com.google.firebase.Timestamp(createdAtValue / 1000, ((createdAtValue % 1000) * 1_000_000).toInt())
                                is java.util.Date -> com.google.firebase.Timestamp(createdAtValue)
                                else -> null
                            }
                            
                            val expiresAtValue = data["expiresAt"]
                            val expiresAt = when (expiresAtValue) {
                                is com.google.firebase.Timestamp -> expiresAtValue
                                is Long -> com.google.firebase.Timestamp(expiresAtValue / 1000, ((expiresAtValue % 1000) * 1_000_000).toInt())
                                is java.util.Date -> com.google.firebase.Timestamp(expiresAtValue)
                                else -> null
                            }
                            
                            val storyFirestore = StoryFirestore(
                                id = doc.id,
                                userId = data["userId"] as? String ?: "",
                                userName = data["userName"] as? String ?: "",
                                userAvatarUrl = data["userAvatarUrl"] as? String,
                                mediaUrl = data["mediaUrl"] as? String ?: "",
                                mediaType = data["mediaType"] as? String ?: "image",
                                thumbnailUrl = data["thumbnailUrl"] as? String,
                                caption = data["caption"] as? String,
                                createdAt = createdAt,
                                expiresAt = expiresAt,
                                viewsCount = (data["viewsCount"] as? Number)?.toInt() ?: 0,
                                location = locationFirestore
                            )
                            
                            kotlinx.coroutines.runBlocking {
                                val isViewed = checkIfStoryViewed(doc.id, currentUserId)
                                storiesList.add(with(StoryMapper) { storyFirestore.toModel(isViewed) })
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("FirestoreStoriesRepository", "Erro ao processar story: ${e.message}", e)
                        }
                    }
                    
                    val stories = storiesList
                    
                    trySend(stories)
                }
            
            awaitClose { listenerRegistration.remove() }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreStoriesRepository", "Erro ao observar stories do usuário: ${e.message}", e)
            trySend(emptyList())
            awaitClose { }
        }
    }
    
    override suspend fun createStory(story: Story): Result<String> {
        return try {
            val currentUserId = authRepository.getCurrentUser()?.uid
                ?: return com.taskgoapp.taskgo.core.model.Result.Error(Exception("Usuário não autenticado"))
            
            // Validar que o userId da story corresponde ao usuário autenticado
            if (story.userId != currentUserId) {
                return com.taskgoapp.taskgo.core.model.Result.Error(Exception("userId da story não corresponde ao usuário autenticado"))
            }
            
            // Preparar dados para Cloud Function
            val locationMap = story.location?.let { location ->
                mapOf(
                    "city" to location.city,
                    "state" to location.state,
                    "latitude" to location.latitude,
                    "longitude" to location.longitude
                )
            }
            
            // Usar Cloud Function createStory (backend como autoridade)
            val result = functionsService.createStory(
                mediaUrl = story.mediaUrl,
                mediaType = story.mediaType,
                caption = story.caption,
                thumbnailUrl = story.thumbnailUrl,
                location = locationMap,
                expiresAt = story.expiresAt.time
            )
            
            // Converter de kotlin.Result para com.taskgoapp.taskgo.core.model.Result
            return result.fold(
                onSuccess = { data ->
                    val storyId = data["storyId"] as? String
                        ?: return com.taskgoapp.taskgo.core.model.Result.Error(Exception("Story ID não retornado pela Cloud Function"))
                    
                    android.util.Log.d("FirestoreStoriesRepository", "Story criada com sucesso via Cloud Function: $storyId")
                    com.taskgoapp.taskgo.core.model.Result.Success(storyId)
                },
                onFailure = { error ->
                    android.util.Log.e("FirestoreStoriesRepository", "Erro ao criar story via Cloud Function: ${error.message}", error)
                    com.taskgoapp.taskgo.core.model.Result.Error(error)
                }
            )
        } catch (e: Exception) {
            android.util.Log.e("FirestoreStoriesRepository", "Erro ao criar story: ${e.message}", e)
            com.taskgoapp.taskgo.core.model.Result.Error(e)
        }
    }
    
    override suspend fun markStoryAsViewed(storyId: String, userId: String): Result<Unit> {
        return try {
            // Verificar se já foi marcada como visualizada
            val viewDoc = storyViewsCollection
                .document(storyId)
                .collection("views")
                .document(userId)
                .get()
                .await()
            
            if (!viewDoc.exists()) {
                // Buscar informações do usuário que está visualizando
                val userInfo = getUserInfo(userId)
                
                // Marcar como visualizada com informações do usuário
                storyViewsCollection
                    .document(storyId)
                    .collection("views")
                    .document(userId)
                    .set(mapOf(
                        "viewedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                        "userId" to userId,
                        "userName" to (userInfo?.displayName ?: "Usuário"),
                        "userAvatarUrl" to (userInfo?.photoURL ?: "")
                    ))
                    .await()
                
                // Incrementar contador de visualizações na story
                storiesCollectionGlobal.document(storyId).update(
                    "viewsCount", FieldValue.increment(1)
                ).await()
            }
            
            com.taskgoapp.taskgo.core.model.Result.Success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreStoriesRepository", "Erro ao marcar story como visualizada: ${e.message}", e)
            com.taskgoapp.taskgo.core.model.Result.Error(e)
        }
    }
    
    override suspend fun deleteStory(storyId: String, userId: String): Result<Unit> {
        return try {
            val currentUserId = authRepository.getCurrentUser()?.uid
                ?: return com.taskgoapp.taskgo.core.model.Result.Error(Exception("Usuário não autenticado"))
            
            // Verificar na subcoleção do usuário primeiro (fonte de verdade)
            val userStoriesCollection = getUserStoriesCollection(userId)
            val userStoryDoc = userStoriesCollection.document(storyId).get().await()
            
            // Se não existe na subcoleção, verificar na coleção pública
            if (!userStoryDoc.exists()) {
                val storyDoc = storiesCollectionGlobal.document(storyId).get().await()
                val story = storyDoc.toObject(StoryFirestore::class.java)
                
                // Verificar se o usuário é o dono da story
                if (!storyDoc.exists() || story?.userId != userId) {
                    return com.taskgoapp.taskgo.core.model.Result.Error(
                        Exception("Story não encontrada ou você não tem permissão para deletar")
                    )
                }
                
                // Se existe apenas na coleção pública, deletar apenas dela
                storiesCollectionGlobal.document(storyId).delete().await()
            } else {
                // Se existe na subcoleção do usuário, deletar de ambas
                // Deletar da subcoleção primeiro (fonte de verdade)
                userStoriesCollection.document(storyId).delete().await()
                
                // Também deletar da coleção pública para garantir sincronização imediata
                try {
                    storiesCollectionGlobal.document(storyId).delete().await()
                } catch (e: Exception) {
                    android.util.Log.w("FirestoreStoriesRepository", "Erro ao deletar story da coleção pública: ${e.message}")
                    // Não falhar se pública falhar, a Cloud Function vai fazer a limpeza
                }
            }
            
            android.util.Log.d("FirestoreStoriesRepository", "Story deletada: $storyId")
            com.taskgoapp.taskgo.core.model.Result.Success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreStoriesRepository", "Erro ao deletar story: ${e.message}", e)
            com.taskgoapp.taskgo.core.model.Result.Error(e)
        }
    }
    
    override suspend fun getStoriesNearby(
        currentUserId: String,
        latitude: Double,
        longitude: Double,
        radiusKm: Double
    ): Result<List<Story>> {
        return try {
            // Por enquanto, retornar todas as stories não expiradas
            // TODO: Implementar filtro geográfico baseado em latitude/longitude
            val twentyFourHoursAgo = Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000)
            val timestamp = com.google.firebase.Timestamp(twentyFourHoursAgo)
            
            val snapshot = storiesCollectionGlobal
                .whereGreaterThan("expiresAt", timestamp)
                .whereNotEqualTo("userId", currentUserId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()
            
            val stories = snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    val locationData = data["location"] as? Map<*, *>
                    
                    val locationFirestore = locationData?.let {
                        com.taskgoapp.taskgo.data.firestore.models.StoryLocationFirestore(
                            city = it["city"] as? String ?: "",
                            state = it["state"] as? String ?: "",
                            latitude = (it["latitude"] as? Number)?.toDouble() ?: 0.0,
                            longitude = (it["longitude"] as? Number)?.toDouble() ?: 0.0
                        )
                    }
                    
                    // Converter createdAt e expiresAt corretamente (pode vir como Long ou Timestamp)
                    val createdAtValue = data["createdAt"]
                    val createdAt: com.google.firebase.Timestamp? = when (createdAtValue) {
                        is com.google.firebase.Timestamp -> createdAtValue
                        is Long -> com.google.firebase.Timestamp(createdAtValue / 1000, ((createdAtValue % 1000) * 1_000_000).toInt())
                        is java.util.Date -> com.google.firebase.Timestamp(createdAtValue)
                        else -> null
                    }
                    
                    val expiresAtValue = data["expiresAt"]
                    val expiresAt: com.google.firebase.Timestamp? = when (expiresAtValue) {
                        is com.google.firebase.Timestamp -> expiresAtValue
                        is Long -> com.google.firebase.Timestamp(expiresAtValue / 1000, ((expiresAtValue % 1000) * 1_000_000).toInt())
                        is java.util.Date -> com.google.firebase.Timestamp(expiresAtValue)
                        else -> null
                    }
                    
                    val storyFirestore = StoryFirestore(
                        id = doc.id,
                        userId = data["userId"] as? String ?: "",
                        userName = data["userName"] as? String ?: "",
                        userAvatarUrl = data["userAvatarUrl"] as? String,
                        mediaUrl = data["mediaUrl"] as? String ?: "",
                        mediaType = data["mediaType"] as? String ?: "image",
                        thumbnailUrl = data["thumbnailUrl"] as? String,
                        caption = data["caption"] as? String,
                        createdAt = createdAt,
                        expiresAt = expiresAt,
                        viewsCount = (data["viewsCount"] as? Number)?.toInt() ?: 0,
                        location = locationFirestore
                    )
                    
                    val isViewed = kotlinx.coroutines.runBlocking {
                        checkIfStoryViewed(doc.id, currentUserId)
                    }
                    with(StoryMapper) {
                        storyFirestore.toModel(isViewed)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("FirestoreStoriesRepository", "Erro ao processar story: ${e.message}", e)
                    null
                }
            }
            
            com.taskgoapp.taskgo.core.model.Result.Success(stories)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreStoriesRepository", "Erro ao buscar stories próximas: ${e.message}", e)
            com.taskgoapp.taskgo.core.model.Result.Error(e)
        }
    }
    
    /**
     * Verifica se uma story foi visualizada por um usuário
     */
    private suspend fun checkIfStoryViewed(storyId: String, userId: String): Boolean {
        return try {
            val viewDoc = storyViewsCollection
                .document(storyId)
                .collection("views")
                .document(userId)
                .get()
                .await()
            viewDoc.exists()
        } catch (e: Exception) {
            false
        }
    }
    
    override fun observeStoryAnalytics(storyId: String, ownerUserId: String): Flow<StoryAnalytics> = callbackFlow {
        try {
            // Verificar se a story pertence ao usuário
            val storyDoc = storiesCollectionGlobal.document(storyId).get().await()
            val storyData = storyDoc.data
            if (storyData == null || storyData["userId"] != ownerUserId) {
                android.util.Log.w("FirestoreStoriesRepository", "Story não encontrada ou usuário não é o dono")
                trySend(StoryAnalytics(storyId = storyId, userId = ownerUserId))
                awaitClose { }
                return@callbackFlow
            }
            
            // Observar visualizações da story
            val viewsCollection = storyViewsCollection
                .document(storyId)
                .collection("views")
            
            val listenerRegistration = viewsCollection
                .orderBy("viewedAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.e("FirestoreStoriesRepository", "Erro ao observar views: ${error.message}", error)
                        trySend(StoryAnalytics(storyId = storyId, userId = ownerUserId))
                        return@addSnapshotListener
                    }
                    
                    val viewsList = mutableListOf<StoryView>()
                    snapshot?.documents?.forEach { viewDoc ->
                        try {
                            val viewData = viewDoc.data ?: return@forEach
                            val viewedAt = (viewData["viewedAt"] as? com.google.firebase.Timestamp)?.toDate()
                                ?: Date()
                            
                            // Buscar informações do usuário que visualizou
                            val viewerUserId = viewDoc.id
                            val viewerUser = kotlinx.coroutines.runBlocking {
                                try {
                                    firestore.collection("users").document(viewerUserId).get().await()
                                        .toObject(com.taskgoapp.taskgo.data.firestore.models.UserFirestore::class.java)
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            
                            viewsList.add(
                                StoryView(
                                    userId = viewerUserId,
                                    userName = viewData["userName"] as? String ?: viewerUser?.displayName ?: "Usuário",
                                    userAvatarUrl = viewData["userAvatarUrl"] as? String ?: viewerUser?.photoURL,
                                    viewedAt = viewedAt,
                                    isFollower = false // TODO: Implementar sistema de followers
                                )
                            )
                        } catch (e: Exception) {
                            android.util.Log.e("FirestoreStoriesRepository", "Erro ao processar view: ${e.message}", e)
                        }
                    }
                    
                    // Buscar ações e interações
                    kotlinx.coroutines.runBlocking {
                        try {
                            val actionsCollection = storyViewsCollection
                                .document(storyId)
                                .collection("actions")
                            
                            val actionsSnapshot = actionsCollection.get().await()
                            var navigation = 0
                            var back = 0
                            var alignments = 0
                            
                            actionsSnapshot.documents.forEach { actionDoc ->
                                val actionData = actionDoc.data ?: return@forEach
                                when (actionData["action"] as? String) {
                                    "navigation" -> navigation++
                                    "back" -> back++
                                    "swipe_up" -> alignments++
                                }
                            }
                            
                            val interactionsCollection = storyViewsCollection
                                .document(storyId)
                                .collection("interactions")
                            
                            val interactionsSnapshot = interactionsCollection.get().await()
                            val profileVisits = interactionsSnapshot.documents.count { doc ->
                                (doc.data?.get("type") as? String) == "profile_visit"
                            }
                            
                            val impressions = viewsList.size
                            val accountsReached = viewsList.map { it.userId }.distinct().size
                            val followers = viewsList.count { it.isFollower }
                            
                            val analytics = StoryAnalytics(
                                storyId = storyId,
                                userId = ownerUserId,
                                views = viewsList,
                                accountsReached = accountsReached,
                                impressions = impressions,
                                followers = followers,
                                navigation = navigation,
                                back = back,
                                alignments = alignments,
                                interactions = StoryInteractions(
                                    profileVisits = profileVisits,
                                    linkClicks = 0
                                )
                            )
                            
                            trySend(analytics)
                        } catch (e: Exception) {
                            android.util.Log.e("FirestoreStoriesRepository", "Erro ao buscar métricas: ${e.message}", e)
                            // Enviar analytics básico
                            val analytics = StoryAnalytics(
                                storyId = storyId,
                                userId = ownerUserId,
                                views = viewsList,
                                accountsReached = viewsList.map { it.userId }.distinct().size,
                                impressions = viewsList.size
                            )
                            trySend(analytics)
                        }
                    }
                }
            
            awaitClose { listenerRegistration.remove() }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreStoriesRepository", "Erro ao observar analytics: ${e.message}", e)
            trySend(StoryAnalytics(storyId = storyId, userId = ownerUserId))
            awaitClose { }
        }
    }
    
    override suspend fun trackStoryAction(
        storyId: String,
        userId: String,
        action: String,
        metadata: Map<String, Any>?
    ): Result<Unit> {
        return try {
            val actionsCollection = storyViewsCollection
                .document(storyId)
                .collection("actions")
            
            val actionData = hashMapOf<String, Any>(
                "userId" to userId,
                "action" to action,
                "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            
            metadata?.let { actionData.putAll(it) }
            
            actionsCollection.add(actionData).await()
            
            com.taskgoapp.taskgo.core.model.Result.Success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreStoriesRepository", "Erro ao rastrear ação: ${e.message}", e)
            com.taskgoapp.taskgo.core.model.Result.Error(e)
        }
    }
    
    /**
     * Registra interação (visita ao perfil) a partir de uma story
     */
    suspend fun trackProfileVisit(storyId: String, userId: String): Result<Unit> {
        return try {
            val interactionsCollection = storyViewsCollection
                .document(storyId)
                .collection("interactions")
            
            interactionsCollection.add(mapOf(
                "userId" to userId,
                "type" to "profile_visit",
                "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )).await()
            
            com.taskgoapp.taskgo.core.model.Result.Success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreStoriesRepository", "Erro ao rastrear visita ao perfil: ${e.message}", e)
            com.taskgoapp.taskgo.core.model.Result.Error(e)
        }
    }
    
    /**
     * Helper para buscar informações básicas do usuário
     */
    private suspend fun getUserInfo(userId: String): UserFirestore? {
        return try {
            firestore.collection("users").document(userId).get().await()
                .toObject(UserFirestore::class.java)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreStoriesRepository", "Erro ao buscar usuário: ${e.message}", e)
            null
        }
    }
}

