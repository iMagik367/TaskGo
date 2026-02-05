package com.taskgoapp.taskgo.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.taskgoapp.taskgo.core.model.Result
import com.taskgoapp.taskgo.core.model.fold
import com.taskgoapp.taskgo.core.model.Story
import com.taskgoapp.taskgo.core.model.StoryAnalytics
import com.taskgoapp.taskgo.core.model.StoryView
import com.taskgoapp.taskgo.core.model.StoryInteractions
import com.taskgoapp.taskgo.data.firestore.models.StoryFirestore
import com.taskgoapp.taskgo.data.firestore.models.UserFirestore
import com.taskgoapp.taskgo.data.mapper.StoryMapper
import com.taskgoapp.taskgo.domain.repository.StoriesRepository
import com.taskgoapp.taskgo.core.firebase.LocationHelper
import com.taskgoapp.taskgo.core.location.LocationManager
import com.taskgoapp.taskgo.core.location.LocationValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import android.util.Log
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.emitAll
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
    private val locationManager: LocationManager
) : StoriesRepository {
    
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
    
    // REMOVIDO: Coleção global - stories estão apenas em locations/{locationId}/stories
    private val storyViewsCollection = firestore.collection("story_views")
    
    /**
     * Helper para obter locationId (de LocationState ou GPS)
     * CRÍTICO: NUNCA retorna null - GPS é garantido
     * Retorna Triple(city, state, locationId) - sempre válido
     */
    private suspend fun getLocationIdForOperation(): Triple<String, String, String> {
        // LEI MÁXIMA DO TASKGO: Usar APENAS city/state do perfil do usuário (cadastro)
        // NUNCA usar fallback - se não tiver, FALHAR
        val currentUser = userRepository.observeCurrentUser().first()
        val userCity = currentUser?.city?.takeIf { it.isNotBlank() }
        val userState = currentUser?.state?.takeIf { it.isNotBlank() }
        
        if (userCity.isNullOrBlank() || userState.isNullOrBlank()) {
            val errorMsg = "ERRO CRÍTICO: Usuário não possui city/state válidos no cadastro. " +
                    "City: ${currentUser?.city ?: "null"}, State: ${currentUser?.state ?: "null"}. " +
                    "Não é possível criar story sem localização válida do cadastro."
            android.util.Log.e("FirestoreStoriesRepository", "❌ $errorMsg")
            throw Exception(errorMsg)
        }
        
        android.util.Log.d("FirestoreStoriesRepository", "📍 Usando city/state do perfil: $userCity/$userState")
        
        val locationId = try {
            LocationHelper.normalizeLocationId(userCity, userState)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreStoriesRepository", "❌ Erro ao normalizar locationId: ${e.message}", e)
            throw Exception("Erro ao normalizar locationId para city=$userCity, state=$userState: ${e.message}")
        }
        
        return Triple(userCity, userState, locationId)
    }
    
    // Helper para obter subcoleção de stories do usuário
    private fun getUserStoriesCollection(userId: String) = 
        firestore.collection("users").document(userId).collection("stories")
    
    override fun observeStories(
        currentUserId: String,
        radiusKm: Double,
        userLocation: Pair<Double, Double>?
    ): Flow<List<Story>> = callbackFlow {
        val listenerRegistration: ListenerRegistration? = try {
            val currentUser = userRepository.observeCurrentUser().first()
                ?: throw Exception("Usuário não autenticado")
            
            val userCity = currentUser.city?.takeIf { it.isNotBlank() }
                ?: throw Exception("Usuário não possui city no cadastro. Complete seu perfil.")
            val userState = currentUser.state?.takeIf { it.isNotBlank() }
                ?: throw Exception("Usuário não possui state no cadastro. Complete seu perfil.")
            
            val locationId = LocationHelper.normalizeLocationId(userCity, userState)
            val collectionToUse = LocationHelper.getLocationCollection(firestore, "stories", userCity, userState)
            
            collectionToUse
                .whereGreaterThan("expiresAt", System.currentTimeMillis())
                .orderBy("expiresAt", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    
                    val stories = snapshot?.documents?.mapNotNull { doc ->
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
                                userRole = data["userRole"] as? String, // Role do autor
                                mediaUrl = data["mediaUrl"] as? String ?: "",
                                mediaType = data["mediaType"] as? String ?: "image",
                                thumbnailUrl = data["thumbnailUrl"] as? String,
                                caption = data["caption"] as? String,
                                createdAt = createdAt,
                                expiresAt = expiresAt,
                                viewsCount = (data["viewsCount"] as? Number)?.toInt() ?: 0,
                                location = locationFirestore
                            )
                            
                            // REGRA DE NEGÓCIO: Filtrar stories baseado no AccountType do usuário atual
                            // - CLIENTE: vê apenas stories de parceiros (role = partner)
                            // - PARCEIRO: vê todas as stories (próprias + de outros parceiros)
                            val currentUserAccountType = currentUser.accountType
                            val storyAuthorRole = storyFirestore.userRole?.lowercase() ?: ""
                            
                            when (currentUserAccountType) {
                                com.taskgoapp.taskgo.core.model.AccountType.CLIENTE -> {
                                    // Cliente vê apenas stories de parceiros
                                    if (storyAuthorRole != "partner") {
                                        return@mapNotNull null
                                    }
                                }
                                com.taskgoapp.taskgo.core.model.AccountType.PARCEIRO -> {
                                    // Parceiro vê todas as stories (próprias + de outros parceiros)
                                    // Não filtrar - REMOVIDO: filter { it.userId != currentUserId }
                                }
                                else -> {
                                    // Outros tipos: não filtrar
                                }
                            }
                            
                            val isViewed = kotlinx.coroutines.runBlocking {
                                checkIfStoryViewed(doc.id, currentUserId)
                            }
                            with(StoryMapper) { storyFirestore.toModel(isViewed) }
                        } catch (e: Exception) {
                            null
                        }
                    } ?: emptyList()
                    
                    trySend(stories)
                }
        } catch (e: Exception) {
            trySend(emptyList())
            null
        }
        awaitClose { listenerRegistration?.remove() }
    }
    
    override fun observeUserStories(userId: String, currentUserId: String): Flow<List<Story>> = callbackFlow {
        try {
            val twentyFourHoursAgo = Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000)
            val timestamp = com.google.firebase.Timestamp(twentyFourHoursAgo)
            
            val currentUser = userRepository.observeCurrentUser().first()
                ?: throw Exception("Usuário não autenticado")
            
            val userCity = currentUser.city?.takeIf { it.isNotBlank() }
                ?: throw Exception("Usuário não possui city no cadastro. Complete seu perfil.")
            val userState = currentUser.state?.takeIf { it.isNotBlank() }
                ?: throw Exception("Usuário não possui state no cadastro. Complete seu perfil.")
            
            val locationId = LocationHelper.normalizeLocationId(userCity, userState)
            val collectionToUse = LocationHelper.getLocationCollection(firestore, "stories", userCity, userState)
            android.util.Log.d("FirestoreStoriesRepository", "📍 Usando coleção por localização para stories do usuário: locations/$locationId/stories")
            
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
            
            val (userCity, userState, _) = getLocationForOperation()
            
            // Obter GPS para enviar à Cloud Function
            val gpsLocation = locationManager.getCurrentLocationGuaranteed()
            
            // Preparar dados para Cloud Function com GPS e localização operacional
            val locationMap = mapOf(
                "city" to userCity,
                "state" to userState,
                "latitude" to gpsLocation.latitude,
                "longitude" to gpsLocation.longitude
            )
            
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
                onSuccess = { data: Map<String, Any> ->
                    val storyId = data["storyId"] as? String
                    if (storyId == null) {
                        android.util.Log.e("FirestoreStoriesRepository", "Story ID não retornado pela Cloud Function")
                        com.taskgoapp.taskgo.core.model.Result.Error(Exception("Story ID não retornado pela Cloud Function"))
                    } else {
                        android.util.Log.d("FirestoreStoriesRepository", "Story criada com sucesso via Cloud Function: $storyId")
                        com.taskgoapp.taskgo.core.model.Result.Success(storyId)
                    }
                },
                onFailure = { error: Throwable ->
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
                val currentUser = userRepository.observeCurrentUser().first()
                val userCity = currentUser?.city?.takeIf { it.isNotBlank() } ?: ""
                val userState = currentUser?.state?.takeIf { it.isNotBlank() } ?: ""
                
                if (userCity.isNotBlank() && userState.isNotBlank()) {
                    val locationCollection = LocationHelper.getLocationCollection(firestore, "stories", userCity, userState)
                    locationCollection.document(storyId).update(
                        "viewsCount", FieldValue.increment(1)
                    ).await()
                }
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
            
            val currentUser = userRepository.observeCurrentUser().first()
            val userCity = currentUser?.city?.takeIf { it.isNotBlank() } ?: ""
            val userState = currentUser?.state?.takeIf { it.isNotBlank() } ?: ""
            
            if (userCity.isBlank() || userState.isBlank()) {
                return com.taskgoapp.taskgo.core.model.Result.Error(Exception("Usuário não tem city/state no perfil"))
            }
            
            val locationCollection = LocationHelper.getLocationCollection(firestore, "stories", userCity, userState)
            val storyDoc = locationCollection.document(storyId).get().await()
            
            if (!storyDoc.exists()) {
                return com.taskgoapp.taskgo.core.model.Result.Error(
                    Exception("Story não encontrada")
                )
            }
            
            val story = storyDoc.toObject(StoryFirestore::class.java)
            if (story?.userId != userId) {
                return com.taskgoapp.taskgo.core.model.Result.Error(
                    Exception("Você não tem permissão para deletar esta story")
                )
            }
            
            // Deletar da coleção por localização
            locationCollection.document(storyId).delete().await()
            
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
            
            val currentUser = userRepository.observeCurrentUser().first()
            val userCity = currentUser?.city?.takeIf { it.isNotBlank() } ?: ""
            val userState = currentUser?.state?.takeIf { it.isNotBlank() } ?: ""
            
            if (userCity.isBlank() || userState.isBlank()) {
                return com.taskgoapp.taskgo.core.model.Result.Error(Exception("Usuário não tem city/state no perfil"))
            }
            
            val locationCollection = LocationHelper.getLocationCollection(firestore, "stories", userCity, userState)
            
            val snapshot = locationCollection
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
            val currentUser = userRepository.observeCurrentUser().first()
            val userCity = currentUser?.city?.takeIf { it.isNotBlank() } ?: ""
            val userState = currentUser?.state?.takeIf { it.isNotBlank() } ?: ""
            
            if (userCity.isBlank() || userState.isBlank()) {
                trySend(StoryAnalytics(storyId = storyId, userId = ownerUserId))
                awaitClose { }
                return@callbackFlow
            }
            
            val locationCollection = LocationHelper.getLocationCollection(firestore, "stories", userCity, userState)
            val storyDoc = locationCollection.document(storyId).get().await()
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
