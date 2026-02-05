package com.taskgoapp.taskgo.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.taskgoapp.taskgo.core.model.Post
import com.taskgoapp.taskgo.core.model.PostLocation
import com.taskgoapp.taskgo.core.model.Result
import com.taskgoapp.taskgo.data.firestore.models.PostFirestore
import com.taskgoapp.taskgo.data.firestore.models.PostLocation as PostLocationFirestore
import com.taskgoapp.taskgo.data.mapper.PostMapper
import com.taskgoapp.taskgo.domain.repository.FeedRepository
import com.taskgoapp.taskgo.core.firebase.LocationHelper
import com.taskgoapp.taskgo.core.location.LocationManager
import com.taskgoapp.taskgo.core.location.LocationValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreFeedRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: FirebaseAuthRepository,
    private val locationManager: LocationManager,
    private val userRepository: com.taskgoapp.taskgo.domain.repository.UserRepository
) : FeedRepository {
    
    // REMOVIDO: Coleção global - posts estão apenas em locations/{locationId}/posts
    private val currentUserId: String?
        get() = authRepository.getCurrentUser()?.uid
    
    // Helper para obter subcoleção de posts do usuário
    private fun getUserPostsCollection(userId: String) = 
        firestore.collection("users").document(userId).collection("posts")
    
    
    override fun observeFeedPosts(
        userLatitude: Double,
        userLongitude: Double,
        radiusKm: Double
    ): Flow<List<Post>> = callbackFlow {
        var listenerRegistration: ListenerRegistration? = null
        try {
            val currentUser = userRepository.observeCurrentUser().first()
                ?: throw Exception("Usuário não autenticado")
            
            val userCity = currentUser.city?.takeIf { it.isNotBlank() }
                ?: throw Exception("Usuário não possui city no cadastro. Complete seu perfil.")
            val userState = currentUser.state?.takeIf { it.isNotBlank() }
                ?: throw Exception("Usuário não possui state no cadastro. Complete seu perfil.")
            
            val locationId = LocationHelper.normalizeLocationId(userCity, userState)
            val collectionToUse = LocationHelper.getLocationCollection(firestore, "posts", userCity, userState)
            
            listenerRegistration = collectionToUse
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(100)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    
                    val posts = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            val data = doc.data ?: return@mapNotNull null
                            val locationData = data["location"] as? Map<*, *>
                            val location = if (locationData != null) {
                                PostLocationFirestore(
                                    city = locationData["city"] as? String ?: "",
                                    state = locationData["state"] as? String ?: "",
                                    latitude = (locationData["latitude"] as? Number)?.toDouble() ?: 0.0,
                                    longitude = (locationData["longitude"] as? Number)?.toDouble() ?: 0.0
                                )
                            } else null
                            val createdAt = (data["createdAt"] as? com.google.firebase.Timestamp)?.toDate()
                            val updatedAt = (data["updatedAt"] as? com.google.firebase.Timestamp)?.toDate()
                            val postFirestore = PostFirestore(
                                id = doc.id,
                                userId = data["userId"] as? String ?: "",
                                userName = data["userName"] as? String ?: "",
                                userAvatarUrl = data["userAvatarUrl"] as? String,
                                userRole = data["userRole"] as? String, // Role do autor
                                text = data["text"] as? String ?: "",
                                mediaUrls = (data["mediaUrls"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                                mediaTypes = (data["mediaTypes"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                                location = location,
                                createdAt = createdAt,
                                updatedAt = updatedAt,
                                likesCount = (data["likesCount"] as? Number)?.toInt() ?: 0,
                                commentsCount = (data["commentsCount"] as? Number)?.toInt() ?: 0,
                                likedBy = (data["likedBy"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                                tags = (data["tags"] as? List<*>)?.mapNotNull { it as? String }
                            )
                            
                            // REGRA DE NEGÓCIO: Filtrar posts baseado no AccountType do usuário atual
                            // - CLIENTE: vê apenas posts de parceiros (role = partner)
                            // - PARCEIRO: vê todos os posts (próprios + de outros parceiros)
                            val currentUserAccountType = currentUser.accountType
                            val postAuthorRole = postFirestore.userRole?.lowercase() ?: ""
                            
                            when (currentUserAccountType) {
                                com.taskgoapp.taskgo.core.model.AccountType.CLIENTE -> {
                                    // Cliente vê apenas posts de parceiros
                                    if (postAuthorRole != "partner") {
                                        return@mapNotNull null
                                    }
                                }
                                com.taskgoapp.taskgo.core.model.AccountType.PARCEIRO -> {
                                    // Parceiro vê todos os posts (próprios + de outros parceiros)
                                    // Não filtrar
                                }
                                else -> {
                                    // Outros tipos: não filtrar
                                }
                            }
                            
                            with(PostMapper) { postFirestore.toModel(currentUserId) }
                        } catch (e: Exception) {
                            null
                        }
                    } ?: emptyList()
                    
                    trySend(posts)
                }
        } catch (e: Exception) {
            trySend(emptyList())
            null
        }
        awaitClose { 
            try {
                listenerRegistration?.remove()
            } catch (e: Exception) {
                // Ignorar erro ao remover listener
            }
        }
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
    
    private fun observeFeedPostsFromFirestore(
        userCity: String,
        userState: String,
        userLatitude: Double,
        userLongitude: Double,
        radiusKm: Double
    ): Flow<List<Post>> = callbackFlow {
        val listenerRegistration: ListenerRegistration
        
        try {
            // ✅ Usar coleção por localização (posts, não feed)
            val (userCity, userState, _) = getLocationForOperation()
            val collectionToUse = LocationHelper.getLocationCollection(
                firestore,
                "posts",
                userCity,
                userState
            )
            
            val locationId = LocationHelper.normalizeLocationId(userCity, userState)
            Log.d("FirestoreFeedRepository", """
                📍 Querying Firestore with location:
                City: $userCity
                State: $userState
                LocationId: $locationId
                Firestore Path: locations/$locationId/posts
            """.trimIndent())
            
            // ✅ CRÍTICO: Logar criação do listener
            Log.d("FirestoreFeedRepository", "🔵 CRIANDO LISTENER para locations/$locationId/posts")
            
            // Buscar todos os posts ordenados por data de criação (mais recentes primeiro)
            // O filtro por distância será feito em memória após buscar os posts
            listenerRegistration = collectionToUse
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(100) // Limitar a 100 posts por vez para performance
                .addSnapshotListener { snapshot, error ->
                    // ✅ CRÍTICO: Logar TODOS os snapshots recebidos
                    Log.d("FirestoreFeedRepository", """
                        🔵 SNAPSHOT RECEBIDO:
                        Path: locations/$locationId/posts
                        HasError: ${error != null}
                        Error: ${error?.message ?: "null"}
                        SnapshotNull: ${snapshot == null}
                        SnapshotEmpty: ${snapshot?.isEmpty ?: "null"}
                        SnapshotSize: ${snapshot?.size() ?: "null"}
                        DocumentsCount: ${snapshot?.documents?.size ?: "null"}
                    """.trimIndent())
                    if (error != null) {
                        android.util.Log.e("FirestoreFeedRepository", "Erro ao observar posts: ${error.message}", error)
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    
                    // Buscar AccountType do usuário atual uma vez para usar no filtro
                    // Usar runBlocking pois estamos dentro de um callback (não coroutine)
                    val currentUserForFilter = try {
                        kotlinx.coroutines.runBlocking {
                            userRepository.observeCurrentUser().first()
                        }
                    } catch (e: Exception) {
                        null
                    }
                    
                    val posts = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            val postData = doc.data ?: return@mapNotNull null
                            val locationData = postData["location"] as? Map<*, *>
                            
                            val location = if (locationData != null) {
                                PostLocationFirestore(
                                    city = locationData["city"] as? String ?: "",
                                    state = locationData["state"] as? String ?: "",
                                    latitude = (locationData["latitude"] as? Number)?.toDouble() ?: 0.0,
                                    longitude = (locationData["longitude"] as? Number)?.toDouble() ?: 0.0
                                )
                            } else null
                            
                            val createdAt = (postData["createdAt"] as? com.google.firebase.Timestamp)?.toDate()
                            val updatedAt = (postData["updatedAt"] as? com.google.firebase.Timestamp)?.toDate()
                            
                            val postFirestore = PostFirestore(
                                id = doc.id,
                                userId = postData["userId"] as? String ?: "",
                                userName = postData["userName"] as? String ?: "",
                                userAvatarUrl = postData["userAvatarUrl"] as? String,
                                userRole = postData["userRole"] as? String, // Role do autor
                                text = postData["text"] as? String ?: "",
                                mediaUrls = (postData["mediaUrls"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                                mediaTypes = (postData["mediaTypes"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                                location = location,
                                createdAt = createdAt,
                                updatedAt = updatedAt,
                                likesCount = (postData["likesCount"] as? Number)?.toInt() ?: 0,
                                commentsCount = (postData["commentsCount"] as? Number)?.toInt() ?: 0,
                                likedBy = (postData["likedBy"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                                tags = (postData["tags"] as? List<*>)?.mapNotNull { it as? String }
                            )
                            
                            // REGRA DE NEGÓCIO: Filtrar posts baseado no AccountType do usuário atual
                            // - CLIENTE: vê apenas posts de parceiros (role = partner)
                            // - PARCEIRO: vê todos os posts (próprios + de outros parceiros)
                            val currentUserAccountType = currentUserForFilter?.accountType
                            val postAuthorRole = postFirestore.userRole?.lowercase() ?: ""
                            
                            when (currentUserAccountType) {
                                com.taskgoapp.taskgo.core.model.AccountType.CLIENTE -> {
                                    // Cliente vê apenas posts de parceiros
                                    if (postAuthorRole != "partner") {
                                        return@mapNotNull null
                                    }
                                }
                                com.taskgoapp.taskgo.core.model.AccountType.PARCEIRO -> {
                                    // Parceiro vê todos os posts (próprios + de outros parceiros)
                                    // Não filtrar
                                }
                                else -> {
                                    // Outros tipos: não filtrar
                                }
                            }
                            
                            postFirestore
                        } catch (e: Exception) {
                            android.util.Log.e("FirestoreFeedRepository", "Erro ao converter post: ${e.message}", e)
                            null
                        }
                    } ?: emptyList()
                    
                    // ✅ REMOVIDO: Filtro por GPS - mostrar TODOS os posts do city_state
                    // Os posts já estão filtrados por localização (locations/{locationId}/posts)
                    // Não precisamos filtrar por distância GPS - todos os posts do mesmo city_state devem aparecer
                    val filteredPosts = posts
                    
                    // Processar posts de forma assíncrona para evitar bloqueio da thread
                    // Usar CoroutineScope para executar funções suspend sem bloquear
                    val userId = currentUserId
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val userInterests = if (userId != null) {
                                getUserInterestsSync(userId)
                            } else emptyMap()
                            
                            val blockedUserIds = if (userId != null) {
                                getBlockedUserIdsSync(userId)
                            } else emptySet()
                            
                            // Filtrar posts de usuários bloqueados
                            val postsWithoutBlocked = filteredPosts.filter { post ->
                                post.userId !in blockedUserIds
                            }
                            
                            // Buscar dados de rating dos posts (já temos os dados no snapshot)
                            val postsWithRatings = postsWithoutBlocked.map { post ->
                                val postDoc = snapshot?.documents?.find { it.id == post.id }
                                val postData = postDoc?.data
                                val ratingAverage = (postData?.get("ratingAverage") as? Number)?.toDouble() ?: 0.0
                                val ratingCount = (postData?.get("ratingCount") as? Number)?.toInt() ?: 0
                                Triple(post, ratingAverage, ratingCount)
                            }
                            
                            // Converter para modelo de domínio e calcular score de relevância
                            val domainPostsWithScore = postsWithRatings.map { (post, ratingAverage, ratingCount) ->
                                val domainPost = with(PostMapper) {
                                    post.toModel(currentUserId)
                                }
                                val interestScore = calculatePostRelevanceScore(
                                    postId = post.id,
                                    userInterests = userInterests,
                                    postLikes = post.likesCount,
                                    postComments = post.commentsCount,
                                    postRating = ratingAverage,
                                    postRatingCount = ratingCount,
                                    postCreatedAt = post.createdAt
                                )
                                Pair(domainPost, interestScore)
                            }
                            
                            // Ordenar por score de relevância (maior primeiro) e depois por data
                            val sortedPosts = domainPostsWithScore
                                .sortedWith(compareByDescending<Pair<Post, Float>> { it.second }
                                    .thenByDescending { it.first.createdAt?.time ?: 0L })
                                .map { it.first }
                            
                            // ✅ CRÍTICO: Logar antes de enviar
                            Log.d("FirestoreFeedRepository", "🔵 ENVIANDO ${sortedPosts.size} posts para o Flow")
                            val sent = trySend(sortedPosts)
                            Log.d("FirestoreFeedRepository", "🔵 trySend result: isSuccess=${sent.isSuccess}, isClosed=${sent.isClosed}")
                        } catch (e: Exception) {
                            android.util.Log.e("FirestoreFeedRepository", "Erro ao processar feed personalizado: ${e.message}", e)
                            // Em caso de erro, enviar posts filtrados sem personalização
                            val fallbackPosts = filteredPosts.map { post ->
                                with(PostMapper) {
                                    post.toModel(currentUserId)
                                }
                            }
                            Log.d("FirestoreFeedRepository", "🔵 ENVIANDO ${fallbackPosts.size} posts (fallback) para o Flow")
                            trySend(fallbackPosts)
                        }
                    }
                }
            
            // ✅ CRÍTICO: Logar após criar listener
            Log.d("FirestoreFeedRepository", "✅ LISTENER CRIADO com sucesso para locations/$locationId/posts")
            
            awaitClose {
                Log.d("FirestoreFeedRepository", "🔴 REMOVENDO LISTENER para locations/$locationId/posts")
                listenerRegistration.remove()
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreFeedRepository", "Erro ao configurar listener de posts: ${e.message}", e)
            trySend(emptyList())
            awaitClose { }
        }
    }
    
    override suspend fun getPostById(postId: String): Post? {
        return try {
            // Obter locationId para buscar post (GPS é garantido, nunca null)
            val (userCity, userState, _) = getLocationForOperation()
            val locationCollection = LocationHelper.getLocationCollection(
                firestore,
                "posts",
                userCity,
                userState
            )
            val doc = locationCollection.document(postId).get().await()
            if (!doc.exists()) {
                return null
            }
            
            val postData = doc.data ?: return null
            val locationData = postData["location"] as? Map<*, *>
            
            val postLocation = if (locationData != null) {
                PostLocationFirestore(
                    city = locationData["city"] as? String ?: "",
                    state = locationData["state"] as? String ?: "",
                    latitude = (locationData["latitude"] as? Number)?.toDouble() ?: 0.0,
                    longitude = (locationData["longitude"] as? Number)?.toDouble() ?: 0.0
                )
            } else null
            
            val createdAt = (postData["createdAt"] as? com.google.firebase.Timestamp)?.toDate()
            val updatedAt = (postData["updatedAt"] as? com.google.firebase.Timestamp)?.toDate()
            
            val postFirestore = PostFirestore(
                id = doc.id,
                userId = postData["userId"] as? String ?: "",
                userName = postData["userName"] as? String ?: "",
                userAvatarUrl = postData["userAvatarUrl"] as? String,
                text = postData["text"] as? String ?: "",
                mediaUrls = (postData["mediaUrls"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                mediaTypes = (postData["mediaTypes"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                location = postLocation,
                createdAt = createdAt,
                updatedAt = updatedAt,
                likesCount = (postData["likesCount"] as? Number)?.toInt() ?: 0,
                commentsCount = (postData["commentsCount"] as? Number)?.toInt() ?: 0,
                likedBy = (postData["likedBy"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                tags = (postData["tags"] as? List<*>)?.mapNotNull { it as? String }
            )
            
            with(PostMapper) {
                postFirestore.toModel(currentUserId)
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreFeedRepository", "Erro ao obter post: ${e.message}", e)
            null
        }
    }
    
    override fun observeUserPosts(userId: String): Flow<List<Post>> = callbackFlow {
        val listenerRegistration: ListenerRegistration
        
        try {
            val currentUser = userRepository.observeCurrentUser().first()
                ?: throw Exception("Usuário não autenticado")
            
            val userCity = currentUser.city?.takeIf { it.isNotBlank() }
                ?: throw Exception("Usuário não possui city no cadastro. Complete seu perfil.")
            val userState = currentUser.state?.takeIf { it.isNotBlank() }
                ?: throw Exception("Usuário não possui state no cadastro. Complete seu perfil.")
            
            val locationCollection = LocationHelper.getLocationCollection(
                firestore,
                "posts",
                userCity,
                userState
            )
            listenerRegistration = locationCollection
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.e("FirestoreFeedRepository", "Erro ao observar posts do usuário: ${error.message}", error)
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    
                    val posts = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            val postData = doc.data ?: return@mapNotNull null
                            val locationData = postData["location"] as? Map<*, *>
                            
                            val location = if (locationData != null) {
                                PostLocationFirestore(
                                    city = locationData["city"] as? String ?: "",
                                    state = locationData["state"] as? String ?: "",
                                    latitude = (locationData["latitude"] as? Number)?.toDouble() ?: 0.0,
                                    longitude = (locationData["longitude"] as? Number)?.toDouble() ?: 0.0
                                )
                            } else null
                            
                            val createdAt = (postData["createdAt"] as? com.google.firebase.Timestamp)?.toDate()
                            val updatedAt = (postData["updatedAt"] as? com.google.firebase.Timestamp)?.toDate()
                            
                            val postFirestore = PostFirestore(
                                id = doc.id,
                                userId = postData["userId"] as? String ?: "",
                                userName = postData["userName"] as? String ?: "",
                                userAvatarUrl = postData["userAvatarUrl"] as? String,
                                userRole = postData["userRole"] as? String, // Role do autor
                                text = postData["text"] as? String ?: "",
                                mediaUrls = (postData["mediaUrls"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                                mediaTypes = (postData["mediaTypes"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                                location = location,
                                createdAt = createdAt,
                                updatedAt = updatedAt,
                                likesCount = (postData["likesCount"] as? Number)?.toInt() ?: 0,
                                commentsCount = (postData["commentsCount"] as? Number)?.toInt() ?: 0,
                                likedBy = (postData["likedBy"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                                tags = (postData["tags"] as? List<*>)?.mapNotNull { it as? String }
                            )
                            
                            // REGRA DE NEGÓCIO: Filtrar posts baseado no AccountType do usuário atual
                            // - CLIENTE: vê apenas posts de parceiros (role = partner)
                            // - PARCEIRO: vê todos os posts (próprios + de outros parceiros)
                            val currentUserAccountType = currentUser.accountType
                            val postAuthorRole = postFirestore.userRole?.lowercase() ?: ""
                            
                            when (currentUserAccountType) {
                                com.taskgoapp.taskgo.core.model.AccountType.CLIENTE -> {
                                    // Cliente vê apenas posts de parceiros
                                    if (postAuthorRole != "partner") {
                                        return@mapNotNull null
                                    }
                                }
                                com.taskgoapp.taskgo.core.model.AccountType.PARCEIRO -> {
                                    // Parceiro vê todos os posts (próprios + de outros parceiros)
                                    // Não filtrar
                                }
                                else -> {
                                    // Outros tipos: não filtrar
                                }
                            }
                            
                            postFirestore
                        } catch (e: Exception) {
                            android.util.Log.e("FirestoreFeedRepository", "Erro ao converter post: ${e.message}", e)
                            null
                        }
                    } ?: emptyList()
                    
                    val domainPosts = posts.map { post ->
                        with(PostMapper) {
                            post.toModel(currentUserId)
                        }
                    }
                    trySend(domainPosts)
                }
            
            awaitClose {
                listenerRegistration.remove()
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreFeedRepository", "Erro ao configurar listener de posts do usuário: ${e.message}", e)
            trySend(emptyList())
            awaitClose { }
        }
    }
    
    override suspend fun createPost(
        text: String,
        mediaUrls: List<String>,
        mediaTypes: List<String>,
        location: PostLocation
    ): Result<String> {
        return try {
            val userId = currentUserId
                ?: return Result.Error(Exception("Usuário não autenticado"))
            
            // Buscar dados do usuário para incluir no post
            // CRÍTICO: Buscar em locations/{locationId}/users primeiro, depois fallback para users global
            val currentUser = userRepository.observeCurrentUser().first()
            val userCity = currentUser?.city?.takeIf { it.isNotBlank() }
            val userState = currentUser?.state?.takeIf { it.isNotBlank() }
            
            var userName = currentUser?.name ?: "Usuário"
            var userAvatarUrl = currentUser?.avatarUri
            // UserProfile não tem campo role, buscar do Firestore
            var userRole = "user"
            
            // Buscar role do Firestore (UserProfile não tem campo role)
            // Primeiro tentar em locations/{locationId}/users
            if (userCity != null && userState != null) {
                try {
                    val locationId = LocationHelper.normalizeLocationId(userCity, userState)
                    val locationUserDoc = firestore.collection("locations").document(locationId)
                        .collection("users").document(userId).get().await()
                    if (locationUserDoc.exists()) {
                        val locationUserData = locationUserDoc.data
                        userName = locationUserData?.get("displayName") as? String ?: userName
                        userAvatarUrl = locationUserData?.get("photoURL") as? String ?: userAvatarUrl
                        userRole = locationUserData?.get("role") as? String ?: userRole
                    }
                } catch (e: Exception) {
                    android.util.Log.w("FirestoreFeedRepository", "Erro ao buscar em locations: ${e.message}")
                }
            }
            
            // Fallback final: buscar em users global
            if (userRole == "user") {
                try {
                    val userDoc = firestore.collection("users").document(userId).get().await()
                    val userData = userDoc.data
                    if (userData != null) {
                        userName = userData["displayName"] as? String ?: userName
                        userAvatarUrl = userData["photoURL"] as? String ?: userAvatarUrl
                        userRole = userData["role"] as? String ?: userRole
                    }
                } catch (e: Exception) {
                    android.util.Log.w("FirestoreFeedRepository", "Erro ao buscar em users global: ${e.message}")
                }
            }
            
            // LEI MÁXIMA DO TASKGO: Usar APENAS city/state do perfil do usuário (cadastro)
            // NUNCA usar GPS para city/state - GPS apenas para coordenadas (mapa)
            // userCity e userState já foram obtidos acima
            if (userCity.isNullOrBlank() || userState.isNullOrBlank()) {
                val errorMsg = "ERRO CRÍTICO: Usuário não possui city/state válidos no cadastro. " +
                        "City: ${userCity ?: "null"}, State: ${userState ?: "null"}. " +
                        "Não é possível criar post sem localização válida do cadastro."
                android.util.Log.e("FirestoreFeedRepository", "❌ $errorMsg")
                return com.taskgoapp.taskgo.core.model.Result.Error(Exception(errorMsg))
            }
            
            android.util.Log.d("FirestoreFeedRepository", "📍 Usando city/state do perfil: $userCity/$userState")
            
            // Obter GPS apenas para coordenadas (mapa) - NÃO para city/state
            val gpsLocation = locationManager.getCurrentLocationGuaranteed()
            
            // Normalizar locationId
            val locationId = try {
                LocationHelper.normalizeLocationId(userCity, userState)
            } catch (e: Exception) {
                android.util.Log.e("FirestoreFeedRepository", "❌ Erro ao normalizar locationId: ${e.message}", e)
                throw Exception("Erro ao normalizar locationId para city=$userCity, state=$userState: ${e.message}")
            }
            
            val validatedCity = userCity
            val validatedState = userState
            
            android.util.Log.d("FirestoreFeedRepository", """
                ✅ Localização obtida e validada:
                City: $validatedCity
                State: $validatedState
                LocationId: $locationId
                GPS: (${gpsLocation.latitude}, ${gpsLocation.longitude})
            """.trimIndent())
            
            // Converter PostLocation do domínio para Firestore (usando GPS obtido)
            val postLocation = PostLocation(
                city = validatedCity,
                state = validatedState,
                latitude = gpsLocation.latitude,
                longitude = gpsLocation.longitude
            )
            val locationFirestore = with(PostMapper) {
                postLocation.toFirestore()
            }
            
            val postData = hashMapOf<String, Any>(
                "userId" to userId,
                "userName" to userName,
                "userAvatarUrl" to (userAvatarUrl ?: ""),
                "userRole" to userRole, // CRÍTICO: Role do autor para filtrar posts de parceiros para clientes
                "text" to text,
                "mediaUrls" to mediaUrls,
                "mediaTypes" to mediaTypes,
                "city" to validatedCity, // City obtido do perfil do usuário (cadastro)
                "state" to validatedState, // State obtido do perfil do usuário (cadastro)
                "locationId" to locationId, // CRÍTICO: Adicionar locationId para busca eficiente (SSR, etc)
                "location" to hashMapOf(
                    "city" to validatedCity,
                    "state" to validatedState,
                    "latitude" to gpsLocation.latitude,
                    "longitude" to gpsLocation.longitude
                ),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
                "likesCount" to 0,
                "commentsCount" to 0,
                "likedBy" to emptyList<String>()
            )
            
            // Criar na subcoleção do usuário (fonte de verdade - dados privados)
            val userPostsCollection = getUserPostsCollection(userId)
            val docRef = userPostsCollection.add(postData).await()
            val postId = docRef.id
            
            // CRÍTICO: Salvar na coleção pública por localização
            val locationPostsCollection = firestore.collection("locations").document(locationId).collection("posts")
            locationPostsCollection.document(postId).set(postData).await()
            android.util.Log.d("FirestoreFeedRepository", "✅ Post salvo na coleção por localização: locations/$locationId/posts")
            
            // REMOVIDO: Salvamento na coleção global - posts estão apenas em locations/{locationId}/posts
            
            Result.Success(postId)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreFeedRepository", "Erro ao criar post: ${e.message}", e)
            Result.Error(e)
        }
    }
    
    override suspend fun likePost(postId: String, userId: String): Result<Unit> {
        return try {
            val (userCity, userState, _) = getLocationForOperation()
            val locationCollection = LocationHelper.getLocationCollection(
                firestore,
                "posts",
                userCity,
                userState
            )
            val postRef = locationCollection.document(postId)
            
            // Usar transação para garantir atomicidade
            firestore.runTransaction { transaction ->
                val postDoc = transaction.get(postRef)
                if (!postDoc.exists()) {
                    throw Exception("Post não encontrado")
                }
                
                val postData = postDoc.data ?: throw Exception("Dados do post inválidos")
                val likedBy = (postData["likedBy"] as? List<*>)?.mapNotNull { it as? String }?.toMutableList() ?: mutableListOf()
                
                if (!likedBy.contains(userId)) {
                    likedBy.add(userId)
                    val likesCount = (postData["likesCount"] as? Number)?.toInt() ?: 0
                    transaction.update(postRef, mapOf(
                        "likedBy" to likedBy,
                        "likesCount" to (likesCount + 1),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ))
                }
            }.await()
            
            Result.Success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreFeedRepository", "Erro ao curtir post: ${e.message}", e)
            Result.Error(e)
        }
    }
    
    override suspend fun unlikePost(postId: String, userId: String): Result<Unit> {
        return try {
            val (userCity, userState, _) = getLocationForOperation()
            val locationCollection = LocationHelper.getLocationCollection(
                firestore,
                "posts",
                userCity,
                userState
            )
            val postRef = locationCollection.document(postId)
            
            firestore.runTransaction { transaction ->
                val postDoc = transaction.get(postRef)
                if (!postDoc.exists()) {
                    throw Exception("Post não encontrado")
                }
                
                val postData = postDoc.data ?: throw Exception("Dados do post inválidos")
                val likedBy = (postData["likedBy"] as? List<*>)?.mapNotNull { it as? String }?.toMutableList() ?: mutableListOf()
                
                if (likedBy.contains(userId)) {
                    likedBy.remove(userId)
                    val likesCount = (postData["likesCount"] as? Number)?.toInt() ?: 0
                    transaction.update(postRef, mapOf(
                        "likedBy" to likedBy,
                        "likesCount" to (likesCount - 1).coerceAtLeast(0),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ))
                }
            }.await()
            
            Result.Success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreFeedRepository", "Erro ao descurtir post: ${e.message}", e)
            Result.Error(e)
        }
    }
    
    override suspend fun deletePost(postId: String): Result<Unit> {
        return try {
            val userId = currentUserId
                ?: return Result.Error(Exception("Usuário não autenticado"))
            
            // Verificar na subcoleção do usuário primeiro (fonte de verdade)
            val userPostsCollection = getUserPostsCollection(userId)
            val userPostDoc = userPostsCollection.document(postId).get().await()
            
            // Obter locationId para buscar post (GPS é garantido, nunca null)
            val (userCity, userState, _) = getLocationForOperation()
            val locationCollection = LocationHelper.getLocationCollection(
                firestore,
                "posts",
                userCity,
                userState
            )
            val postDoc = locationCollection.document(postId).get().await()
            
            if (!postDoc.exists()) {
                return Result.Error(Exception("Post não encontrado"))
            }
            
            val postData = postDoc.data
            if (postData?.get("userId") != userId) {
                return Result.Error(Exception("Você não tem permissão para excluir este post"))
            }
            
            // Deletar da coleção por localização
            locationCollection.document(postId).delete().await()
            
            Result.Success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreFeedRepository", "Erro ao deletar post: ${e.message}", e)
            Result.Error(e)
        }
    }
    
    /**
     * Calcula a distância em km entre duas coordenadas usando a fórmula de Haversine
     */
    private fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val earthRadius = 6371.0 // Raio da Terra em km
        
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val deltaLat = Math.toRadians(lat2 - lat1)
        val deltaLon = Math.toRadians(lon2 - lon1)
        
        val a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2)
        
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        
        return earthRadius * c
    }
    
    // Helper para obter subcoleção de comentários de um post
    // Comentários são subcoleções do post, então precisamos do postId e localização
    private suspend fun getPostCommentsCollection(postId: String) = run {
        val (city, state, _) = getLocationForOperation()
        val locationCollection = LocationHelper.getLocationCollection(
            firestore,
            "posts",
            city,
            state
        )
        locationCollection.document(postId).collection("comments")
    }
    
    override fun observePostComments(postId: String): Flow<List<com.taskgoapp.taskgo.feature.feed.presentation.components.CommentItem>> = callbackFlow {
        val listenerRegistration: ListenerRegistration
        
        try {
            val currentUser = userRepository.observeCurrentUser().first()
                ?: throw Exception("Usuário não autenticado")
            
            val userCity = currentUser.city?.takeIf { it.isNotBlank() }
                ?: throw Exception("Usuário não possui city no cadastro. Complete seu perfil.")
            val userState = currentUser.state?.takeIf { it.isNotBlank() }
                ?: throw Exception("Usuário não possui state no cadastro. Complete seu perfil.")
            
            val locationCollection = LocationHelper.getLocationCollection(
                firestore,
                "posts",
                userCity,
                userState
            )
            val commentsCollection = locationCollection.document(postId).collection("comments")
            
            listenerRegistration = commentsCollection
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.e("FirestoreFeedRepository", "Erro ao observar comentários: ${error.message}", error)
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    
                    val comments = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            val data = doc.data ?: return@mapNotNull null
                            val createdAt = when (val createdAtValue = data["createdAt"]) {
                                is com.google.firebase.Timestamp -> createdAtValue.toDate()
                                is Long -> Date(createdAtValue)
                                is java.util.Date -> createdAtValue
                                else -> Date()
                            }
                            
                            com.taskgoapp.taskgo.feature.feed.presentation.components.CommentItem(
                                id = doc.id,
                                postId = postId,
                                userId = data["userId"] as? String ?: "",
                                userName = data["userName"] as? String ?: "Usuário",
                                userAvatarUrl = data["userAvatarUrl"] as? String,
                                text = data["text"] as? String ?: "",
                                createdAt = createdAt,
                                isLiked = (data["isLiked"] as? Boolean) ?: false,
                                likesCount = (data["likesCount"] as? Number)?.toInt() ?: 0
                            )
                        } catch (e: Exception) {
                            android.util.Log.e("FirestoreFeedRepository", "Erro ao converter comentário: ${e.message}", e)
                            null
                        }
                    } ?: emptyList()
                    
                    trySend(comments)
                }
            
            awaitClose {
                listenerRegistration.remove()
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreFeedRepository", "Erro ao configurar listener de comentários: ${e.message}", e)
            trySend(emptyList())
            awaitClose { }
        }
    }
    
    override suspend fun createComment(postId: String, text: String): Result<String> {
        return try {
            val userId = currentUserId
                ?: return Result.Error(Exception("Usuário não autenticado"))
            
            // Buscar dados do usuário para incluir no comentário
            val userDoc = firestore.collection("users").document(userId).get().await()
            val userData = userDoc.data
            val userName = userData?.get("displayName") as? String ?: "Usuário"
            val userAvatarUrl = userData?.get("photoURL") as? String
            
            val commentsCollection = getPostCommentsCollection(postId)
            
            val commentData = hashMapOf<String, Any>(
                "userId" to userId,
                "userName" to userName,
                "userAvatarUrl" to (userAvatarUrl ?: ""),
                "text" to text.trim(),
                "postId" to postId,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
                "isLiked" to false,
                "likesCount" to 0
            )
            
            // Criar comentário
            val docRef = commentsCollection.add(commentData).await()
            val commentId = docRef.id
            
            // Atualizar contador de comentários do post
            try {
                val currentUser = userRepository.observeCurrentUser().first()
            val userCity = currentUser?.city?.takeIf { it.isNotBlank() } ?: ""
            val userState = currentUser?.state?.takeIf { it.isNotBlank() } ?: ""
            
            if (userCity.isNotBlank() && userState.isNotBlank()) {
                val postsCollection = LocationHelper.getLocationCollection(
                    firestore,
                    "posts",
                    userCity,
                    userState
                )
                val postRef = postsCollection.document(postId)
                firestore.runTransaction { transaction ->
                    val postDoc = transaction.get(postRef)
                    if (postDoc.exists()) {
                        val currentCount = (postDoc.data?.get("commentsCount") as? Number)?.toInt() ?: 0
                        transaction.update(postRef, mapOf(
                            "commentsCount" to (currentCount + 1),
                            "updatedAt" to FieldValue.serverTimestamp()
                        ))
                    }
                }.await()
            } else {
                android.util.Log.w("FirestoreFeedRepository", "Usuário não tem city/state para atualizar contador")
            }
            } catch (e: Exception) {
                android.util.Log.w("FirestoreFeedRepository", "Erro ao atualizar contador de comentários: ${e.message}")
            }
            
            android.util.Log.d("FirestoreFeedRepository", "Comentário criado com sucesso: $commentId")
            Result.Success(commentId)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreFeedRepository", "Erro ao criar comentário: ${e.message}", e)
            Result.Error(e)
        }
    }
    
    override suspend fun deleteComment(postId: String, commentId: String): Result<Unit> {
        return try {
            val userId = currentUserId
                ?: return Result.Error(Exception("Usuário não autenticado"))
            
            val commentsCollection = getPostCommentsCollection(postId)
            val commentDoc = commentsCollection.document(commentId).get().await()
            
            if (!commentDoc.exists()) {
                return Result.Error(Exception("Comentário não encontrado"))
            }
            
            val commentData = commentDoc.data
            if (commentData?.get("userId") != userId) {
                return Result.Error(Exception("Você não tem permissão para deletar este comentário"))
            }
            
            // Deletar comentário
            commentsCollection.document(commentId).delete().await()
            
            // Atualizar contador de comentários do post
            try {
                val currentUser = userRepository.observeCurrentUser().first()
                val userCity = currentUser?.city?.takeIf { it.isNotBlank() } ?: ""
                val userState = currentUser?.state?.takeIf { it.isNotBlank() } ?: ""
                
                if (userCity.isNotBlank() && userState.isNotBlank()) {
                    val postsCollection = LocationHelper.getLocationCollection(
                        firestore,
                        "posts",
                        userCity,
                        userState
                    )
                    val postRef = postsCollection.document(postId)
                    firestore.runTransaction { transaction ->
                        val postDoc = transaction.get(postRef)
                        if (postDoc.exists()) {
                            val currentCount = (postDoc.data?.get("commentsCount") as? Number)?.toInt() ?: 0
                            transaction.update(postRef, mapOf(
                                "commentsCount" to (currentCount - 1).coerceAtLeast(0),
                                "updatedAt" to FieldValue.serverTimestamp()
                            ))
                        }
                    }.await()
                }
            } catch (e: Exception) {
                android.util.Log.w("FirestoreFeedRepository", "Erro ao atualizar contador de comentários: ${e.message}")
            }
            
            Result.Success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreFeedRepository", "Erro ao deletar comentário: ${e.message}", e)
            Result.Error(e)
        }
    }
    
    // ========== INTERESSE EM POSTS ==========
    
    override suspend fun setPostInterest(postId: String, isInterested: Boolean): Result<Unit> {
        return try {
            val userId = currentUserId
                ?: return Result.Error(Exception("Usuário não autenticado"))
            
            val interestsCollection = firestore
                .collection("users")
                .document(userId)
                .collection("postInterests")
            
            // Verificar se já existe interesse
            val existingInterest = interestsCollection
                .whereEqualTo("postId", postId)
                .limit(1)
                .get()
                .await()
            
            val interestData = hashMapOf<String, Any>(
                "postId" to postId,
                "isInterested" to isInterested,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            
            if (existingInterest.documents.isNotEmpty()) {
                // Atualizar interesse existente
                existingInterest.documents[0].reference.update(interestData).await()
            } else {
                // Criar novo interesse
                interestData["createdAt"] = FieldValue.serverTimestamp()
                interestsCollection.add(interestData).await()
            }
            
            android.util.Log.d("FirestoreFeedRepository", "Interesse em post $postId definido: $isInterested")
            Result.Success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreFeedRepository", "Erro ao definir interesse: ${e.message}", e)
            Result.Error(e)
        }
    }
    
    override suspend fun removePostInterest(postId: String): Result<Unit> {
        return try {
            val userId = currentUserId
                ?: return Result.Error(Exception("Usuário não autenticado"))
            
            val interestsCollection = firestore
                .collection("users")
                .document(userId)
                .collection("postInterests")
            
            val existingInterest = interestsCollection
                .whereEqualTo("postId", postId)
                .limit(1)
                .get()
                .await()
            
            if (existingInterest.documents.isNotEmpty()) {
                existingInterest.documents[0].reference.delete().await()
            }
            
            Result.Success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreFeedRepository", "Erro ao remover interesse: ${e.message}", e)
            Result.Error(e)
        }
    }
    
    override suspend fun getPostInterest(postId: String): Boolean? {
        return try {
            val userId = currentUserId ?: return null
            
            val interestsCollection = firestore
                .collection("users")
                .document(userId)
                .collection("postInterests")
            
            val interestDoc = interestsCollection
                .whereEqualTo("postId", postId)
                .limit(1)
                .get()
                .await()
            
            if (interestDoc.documents.isNotEmpty()) {
                interestDoc.documents[0].data?.get("isInterested") as? Boolean
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreFeedRepository", "Erro ao obter interesse: ${e.message}", e)
            null
        }
    }
    
    // ========== AVALIAÇÃO DE POSTS ==========
    
    override suspend fun ratePost(postId: String, rating: Int, comment: String?): Result<String> {
        return try {
            val userId = currentUserId
                ?: return Result.Error(Exception("Usuário não autenticado"))
            
            // Obter locationId para buscar post (GPS é garantido, nunca null)
            val (userCity, userState, _) = getLocationForOperation()
            
            // Buscar dados do usuário
            val userDoc = firestore.collection("users").document(userId).get().await()
            val userData = userDoc.data
            val userName = userData?.get("displayName") as? String ?: "Usuário"
            val userAvatarUrl = userData?.get("photoURL") as? String
            
            // CRÍTICO: Buscar na coleção regional
            val locationCollection = LocationHelper.getLocationCollection(
                firestore,
                "posts",
                userCity,
                userState
            )
            val ratingsCollection = locationCollection
                .document(postId)
                .collection("ratings")
            
            // Verificar se já existe avaliação do usuário
            val existingRating = ratingsCollection
                .whereEqualTo("userId", userId)
                .limit(1)
                .get()
                .await()
            
            val ratingData = hashMapOf<String, Any>(
                "postId" to postId,
                "userId" to userId,
                "userName" to userName,
                "userAvatarUrl" to (userAvatarUrl ?: ""),
                "rating" to rating,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            
            if (comment != null && comment.isNotBlank()) {
                ratingData["comment"] = comment.trim()
            }
            
            val ratingId: String
            if (existingRating.documents.isNotEmpty()) {
                // Atualizar avaliação existente
                ratingId = existingRating.documents[0].id
                existingRating.documents[0].reference.update(ratingData).await()
            } else {
                // Criar nova avaliação
                ratingData["createdAt"] = FieldValue.serverTimestamp()
                val docRef = ratingsCollection.add(ratingData).await()
                ratingId = docRef.id
            }
            
            // Atualizar média de avaliações do post
            updatePostRatingAverage(postId)
            
            android.util.Log.d("FirestoreFeedRepository", "Post $postId avaliado com $rating estrelas")
            Result.Success(ratingId)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreFeedRepository", "Erro ao avaliar post: ${e.message}", e)
            Result.Error(e)
        }
    }
    
    override suspend fun getUserPostRating(postId: String): com.taskgoapp.taskgo.core.model.PostRating? {
        return try {
            val userId = currentUserId ?: return null
            
            // Obter locationId para buscar post (GPS é garantido, nunca null)
            val (userCity, userState, _) = getLocationForOperation()
            
            // CRÍTICO: Buscar na coleção regional
            val locationCollection = LocationHelper.getLocationCollection(
                firestore,
                "posts",
                userCity,
                userState
            )
            
            val ratingsCollection = locationCollection
                .document(postId)
                .collection("ratings")
            
            val ratingDoc = ratingsCollection
                .whereEqualTo("userId", userId)
                .limit(1)
                .get()
                .await()
            
            if (ratingDoc.documents.isNotEmpty()) {
                val doc = ratingDoc.documents[0]
                val data = doc.data ?: return null
                
                com.taskgoapp.taskgo.core.model.PostRating(
                    id = doc.id,
                    postId = postId,
                    userId = data["userId"] as? String ?: "",
                    userName = data["userName"] as? String,
                    userAvatarUrl = data["userAvatarUrl"] as? String,
                    rating = (data["rating"] as? Number)?.toInt() ?: 0,
                    comment = data["comment"] as? String,
                    createdAt = (data["createdAt"] as? com.google.firebase.Timestamp)?.toDate() ?: java.util.Date()
                )
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("FirestoreFeedRepository", "Erro ao obter avaliação: ${e.message}", e)
            null
        }
    }
    
    private suspend fun updatePostRatingAverage(postId: String) {
        try {
            val (userCity, userState, _) = getLocationForOperation()
            val locationCollection = LocationHelper.getLocationCollection(
                firestore,
                "posts",
                userCity,
                userState
            )
            val ratingsCollection = locationCollection
                .document(postId)
                .collection("ratings")
            
            val ratings = ratingsCollection.get().await()
            val ratingsList = ratings.documents.mapNotNull { doc ->
                (doc.data?.get("rating") as? Number)?.toInt()
            }
            
            if (ratingsList.isNotEmpty()) {
                val average = ratingsList.average()
                val count = ratingsList.size
                
                locationCollection.document(postId).update(
                    mapOf(
                        "ratingAverage" to average,
                        "ratingCount" to count,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: Exception) {
            android.util.Log.w("FirestoreFeedRepository", "Erro ao atualizar média de avaliações: ${e.message}")
        }
    }
    
    // ========== BLOQUEIO DE USUÁRIOS ==========
    
    override suspend fun blockUser(userId: String): Result<Unit> {
        return try {
            val blockerId = currentUserId
                ?: return Result.Error(Exception("Usuário não autenticado"))
            
            if (blockerId == userId) {
                return Result.Error(Exception("Você não pode bloquear a si mesmo"))
            }
            
            // Buscar dados do usuário a ser bloqueado
            val userDoc = firestore.collection("users").document(userId).get().await()
            val userData = userDoc.data
            val blockedName = userData?.get("displayName") as? String
            val blockedAvatarUrl = userData?.get("photoURL") as? String
            
            val blockedUsersCollection = firestore
                .collection("users")
                .document(blockerId)
                .collection("blockedUsers")
            
            // Verificar se já está bloqueado
            val existingBlock = blockedUsersCollection
                .whereEqualTo("blockedId", userId)
                .limit(1)
                .get()
                .await()
            
            if (existingBlock.documents.isEmpty()) {
                val blockData = hashMapOf<String, Any>(
                    "blockedId" to userId,
                    "blockedName" to (blockedName ?: ""),
                    "blockedAvatarUrl" to (blockedAvatarUrl ?: ""),
                    "createdAt" to FieldValue.serverTimestamp()
                )
                
                blockedUsersCollection.add(blockData).await()
            }
            
            android.util.Log.d("FirestoreFeedRepository", "Usuário $userId bloqueado por $blockerId")
            Result.Success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreFeedRepository", "Erro ao bloquear usuário: ${e.message}", e)
            Result.Error(e)
        }
    }
    
    override suspend fun unblockUser(userId: String): Result<Unit> {
        return try {
            val blockerId = currentUserId
                ?: return Result.Error(Exception("Usuário não autenticado"))
            
            val blockedUsersCollection = firestore
                .collection("users")
                .document(blockerId)
                .collection("blockedUsers")
            
            val existingBlock = blockedUsersCollection
                .whereEqualTo("blockedId", userId)
                .limit(1)
                .get()
                .await()
            
            if (existingBlock.documents.isNotEmpty()) {
                existingBlock.documents[0].reference.delete().await()
            }
            
            Result.Success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreFeedRepository", "Erro ao desbloquear usuário: ${e.message}", e)
            Result.Error(e)
        }
    }
    
    override suspend fun isUserBlocked(userId: String): Boolean {
        return try {
            val blockerId = currentUserId ?: return false
            
            val blockedUsersCollection = firestore
                .collection("users")
                .document(blockerId)
                .collection("blockedUsers")
            
            val blockDoc = blockedUsersCollection
                .whereEqualTo("blockedId", userId)
                .limit(1)
                .get()
                .await()
            
            blockDoc.documents.isNotEmpty()
        } catch (e: Exception) {
            android.util.Log.e("FirestoreFeedRepository", "Erro ao verificar bloqueio: ${e.message}", e)
            false
        }
    }
    
    override fun observeBlockedUsers(): Flow<List<com.taskgoapp.taskgo.core.model.BlockedUser>> = callbackFlow {
        val userId = currentUserId
        if (userId == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        
        val blockedUsersCollection = firestore
            .collection("users")
            .document(userId)
            .collection("blockedUsers")
        
        val listenerRegistration = blockedUsersCollection
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirestoreFeedRepository", "Erro ao observar usuários bloqueados: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val blockedUsers = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val data = doc.data ?: return@mapNotNull null
                        com.taskgoapp.taskgo.core.model.BlockedUser(
                            id = doc.id,
                            blockerId = userId,
                            blockedId = data["blockedId"] as? String ?: "",
                            blockedName = data["blockedName"] as? String,
                            blockedAvatarUrl = data["blockedAvatarUrl"] as? String,
                            createdAt = (data["createdAt"] as? com.google.firebase.Timestamp)?.toDate() ?: java.util.Date()
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("FirestoreFeedRepository", "Erro ao converter usuário bloqueado: ${e.message}", e)
                        null
                    }
                } ?: emptyList()
                
                trySend(blockedUsers)
            }
        
        awaitClose {
            listenerRegistration.remove()
        }
    }
    
    // ========== FUNÇÕES AUXILIARES PARA ALGORITMO DE FEED ==========
    
    /**
     * Busca interesses do usuário de forma síncrona (dentro do listener)
     */
    private suspend fun getUserInterestsSync(userId: String): Map<String, Boolean> {
        return try {
            val interestsCollection = firestore
                .collection("users")
                .document(userId)
                .collection("postInterests")
            
            val interests = interestsCollection.get().await()
            interests.documents.associate { doc ->
                val postId = doc.data?.get("postId") as? String ?: ""
                val isInterested = doc.data?.get("isInterested") as? Boolean ?: false
                postId to isInterested
            }
        } catch (e: Exception) {
            android.util.Log.w("FirestoreFeedRepository", "Erro ao buscar interesses: ${e.message}")
            emptyMap()
        }
    }
    
    /**
     * Busca IDs de usuários bloqueados de forma síncrona (dentro do listener)
     */
    private suspend fun getBlockedUserIdsSync(userId: String): Set<String> {
        return try {
            val blockedUsersCollection = firestore
                .collection("users")
                .document(userId)
                .collection("blockedUsers")
            
            val blockedUsers = blockedUsersCollection.get().await()
            blockedUsers.documents.mapNotNull { doc ->
                doc.data?.get("blockedId") as? String
            }.toSet()
        } catch (e: Exception) {
            android.util.Log.w("FirestoreFeedRepository", "Erro ao buscar usuários bloqueados: ${e.message}")
            emptySet()
        }
    }
    
    /**
     * Calcula score de relevância de um post baseado em interesses, likes, comentários e avaliações
     * Score maior = mais relevante (aparece primeiro no feed)
     */
    private fun calculatePostRelevanceScore(
        postId: String,
        userInterests: Map<String, Boolean>,
        postLikes: Int,
        postComments: Int,
        postRating: Double,
        postRatingCount: Int,
        postCreatedAt: java.util.Date?
    ): Float {
        var score = 0f
        
        // 1. Interesse do usuário (peso: 40%)
        // Posts com interesse positivo ganham muito score, posts com interesse negativo perdem score
        val interest = userInterests[postId]
        when {
            interest == true -> score += 0.4f  // Tenho interesse - boost significativo
            interest == false -> score -= 0.2f // Não tenho interesse - reduz score
            else -> score += 0f // Sem interesse definido - neutro
        }
        
        // 2. Engajamento (likes + comentários) (peso: 25%)
        // Normalizar engajamento (assumindo máximo de 100 likes e 50 comentários)
        val engagementScore = ((postLikes.coerceIn(0, 100) / 100f) * 0.15f) +
                              ((postComments.coerceIn(0, 50) / 50f) * 0.10f)
        score += engagementScore
        
        // 3. Avaliação média (peso: 20%)
        // Posts bem avaliados aparecem mais
        if (postRatingCount > 0) {
            val ratingScore = (postRating / 5.0).coerceIn(0.0, 1.0).toFloat() * 0.2f
            score += ratingScore
        }
        
        // 4. Recência (peso: 15%)
        // Posts mais recentes ganham score adicional
        postCreatedAt?.let { createdAt ->
            val now = System.currentTimeMillis()
            val postTime = createdAt.time
            val hoursSinceCreation = (now - postTime) / (1000.0 * 60.0 * 60.0)
            
            // Decaimento exponencial: posts de 24h atrás têm score 0.5, posts de 1h têm score 1.0
            val recencyScore = when {
                hoursSinceCreation < 1.0 -> 1.0f
                hoursSinceCreation < 24.0 -> (1.0f - (hoursSinceCreation.toFloat() / 24.0f) * 0.5f).coerceAtLeast(0.5f)
                else -> 0.3f // Posts antigos ainda aparecem, mas com menos prioridade
            }
            score += recencyScore * 0.15f
        } ?: run {
            // Se não tem data, assume score neutro de recência
            score += 0.075f
        }
        
        return score.coerceIn(-1f, 1f)
    }
}
