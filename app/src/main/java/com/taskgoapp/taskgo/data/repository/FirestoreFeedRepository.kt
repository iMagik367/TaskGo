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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreFeedRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: FirebaseAuthRepository
) : FeedRepository {
    
    // CRÍTICO: Agora usamos coleções por localização, mas mantemos esta para compatibilidade
    private val postsCollection = firestore.collection("posts")
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
        val listenerRegistration: ListenerRegistration
        
        try {
            // Buscar todos os posts ordenados por data de criação (mais recentes primeiro)
            // O filtro por distância será feito em memória após buscar os posts
            listenerRegistration = postsCollection
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(100) // Limitar a 100 posts por vez para performance
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.e("FirestoreFeedRepository", "Erro ao observar posts: ${error.message}", error)
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
                            
                            PostFirestore(
                                id = doc.id,
                                userId = postData["userId"] as? String ?: "",
                                userName = postData["userName"] as? String ?: "",
                                userAvatarUrl = postData["userAvatarUrl"] as? String,
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
                        } catch (e: Exception) {
                            android.util.Log.e("FirestoreFeedRepository", "Erro ao converter post: ${e.message}", e)
                            null
                        }
                    } ?: emptyList()
                    
                    // Filtrar por distância usando fórmula de Haversine
                    val userLocation = PostLocationFirestore(
                        city = "",
                        state = "",
                        latitude = userLatitude,
                        longitude = userLongitude
                    )
                    
                    // Filtrar por distância usando fórmula de Haversine
                    val filteredPosts = posts.filter { post ->
                        post.location?.let { postLocation ->
                            val distance = calculateDistance(
                                userLocation.latitude,
                                userLocation.longitude,
                                postLocation.latitude,
                                postLocation.longitude
                            )
                            distance <= radiusKm
                        } ?: false // Excluir posts sem localização
                    }
                    
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
                            
                            trySend(sortedPosts)
                        } catch (e: Exception) {
                            android.util.Log.e("FirestoreFeedRepository", "Erro ao processar feed personalizado: ${e.message}", e)
                            // Em caso de erro, enviar posts filtrados sem personalização
                            val fallbackPosts = filteredPosts.map { post ->
                                with(PostMapper) {
                                    post.toModel(currentUserId)
                                }
                            }
                            trySend(fallbackPosts)
                        }
                    }
                }
            
            awaitClose {
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
            val doc = postsCollection.document(postId).get().await()
            if (!doc.exists()) {
                return null
            }
            
            val postData = doc.data ?: return null
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
            listenerRegistration = postsCollection
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
                            
                            PostFirestore(
                                id = doc.id,
                                userId = postData["userId"] as? String ?: "",
                                userName = postData["userName"] as? String ?: "",
                                userAvatarUrl = postData["userAvatarUrl"] as? String,
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
            val userDoc = firestore.collection("users").document(userId).get().await()
            val userData = userDoc.data
            val userName = userData?.get("displayName") as? String ?: "Usuário"
            val userAvatarUrl = userData?.get("photoURL") as? String
            
            // Converter PostLocation do domínio para Firestore
            val locationFirestore = with(PostMapper) {
                location.toFirestore()
            }
            
            // CRÍTICO: Extrair cidade e estado da localização para salvar na coleção correta
            val city = locationFirestore.city
            val state = locationFirestore.state
            
            if (city.isBlank() || state.isBlank()) {
                android.util.Log.w("FirestoreFeedRepository", "⚠️ Post sem localização completa (city=$city, state=$state), será salvo em 'unknown'")
            }
            
            val postData = hashMapOf<String, Any>(
                "userId" to userId,
                "userName" to userName,
                "userAvatarUrl" to (userAvatarUrl ?: ""),
                "text" to text,
                "mediaUrls" to mediaUrls,
                "mediaTypes" to mediaTypes,
                "city" to city, // Adicionar cidade explicitamente
                "state" to state, // Adicionar estado explicitamente
                "location" to hashMapOf(
                    "city" to locationFirestore.city,
                    "state" to locationFirestore.state,
                    "latitude" to locationFirestore.latitude,
                    "longitude" to locationFirestore.longitude
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
            try {
                val locationId = LocationHelper.normalizeLocationId(city.ifBlank { "unknown" }, state.ifBlank { "unknown" })
                val locationPostsCollection = firestore.collection("locations").document(locationId).collection("posts")
                locationPostsCollection.document(postId).set(postData).await()
                android.util.Log.d("FirestoreFeedRepository", "✅ Post salvo na coleção por localização: locations/$locationId/posts")
            } catch (e: Exception) {
                android.util.Log.e("FirestoreFeedRepository", "❌ Erro ao salvar post na coleção por localização: ${e.message}", e)
            }
            
            // Também salvar na coleção global para compatibilidade (será removido futuramente)
            try {
                postsCollection.document(postId).set(postData).await()
            } catch (e: Exception) {
                android.util.Log.w("FirestoreFeedRepository", "Erro ao salvar post na coleção global: ${e.message}")
                // Não falhar se pública falhar, mas logar o erro
            }
            
            Result.Success(postId)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreFeedRepository", "Erro ao criar post: ${e.message}", e)
            Result.Error(e)
        }
    }
    
    override suspend fun likePost(postId: String, userId: String): Result<Unit> {
        return try {
            val postRef = postsCollection.document(postId)
            
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
            val postRef = postsCollection.document(postId)
            
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
            
            // Se não existe na subcoleção, verificar na coleção pública
            if (!userPostDoc.exists()) {
                val postDoc = postsCollection.document(postId).get().await()
                val postData = postDoc.data
                
                if (!postDoc.exists() || postData?.get("userId") != userId) {
                    return Result.Error(Exception("Post não encontrado ou você não tem permissão para excluir"))
                }
                
                // Se existe apenas na coleção pública, deletar apenas dela
                postsCollection.document(postId).delete().await()
            } else {
                // Se existe na subcoleção do usuário, deletar de ambas
                // Deletar da subcoleção primeiro (fonte de verdade)
                // A Cloud Function vai sincronizar e deletar da coleção pública automaticamente
                userPostsCollection.document(postId).delete().await()
                
                // Também deletar da coleção pública para garantir sincronização imediata
                try {
                    postsCollection.document(postId).delete().await()
                } catch (e: Exception) {
                    android.util.Log.w("FirestoreFeedRepository", "Erro ao deletar post da coleção pública: ${e.message}")
                    // Não falhar se pública falhar, a Cloud Function vai fazer a limpeza
                }
            }
            
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
    private fun getPostCommentsCollection(postId: String) = 
        postsCollection.document(postId).collection("comments")
    
    override fun observePostComments(postId: String): Flow<List<com.taskgoapp.taskgo.feature.feed.presentation.components.CommentItem>> = callbackFlow {
        val listenerRegistration: ListenerRegistration
        
        try {
            val commentsCollection = getPostCommentsCollection(postId)
            
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
            } catch (e: Exception) {
                android.util.Log.w("FirestoreFeedRepository", "Erro ao atualizar contador de comentários: ${e.message}")
                // Não falhar se não conseguir atualizar o contador
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
            
            // Buscar dados do usuário
            val userDoc = firestore.collection("users").document(userId).get().await()
            val userData = userDoc.data
            val userName = userData?.get("displayName") as? String ?: "Usuário"
            val userAvatarUrl = userData?.get("photoURL") as? String
            
            val ratingsCollection = firestore
                .collection("posts")
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
            
            val ratingsCollection = firestore
                .collection("posts")
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
            val ratingsCollection = firestore
                .collection("posts")
                .document(postId)
                .collection("ratings")
            
            val ratings = ratingsCollection.get().await()
            val ratingsList = ratings.documents.mapNotNull { doc ->
                (doc.data?.get("rating") as? Number)?.toInt()
            }
            
            if (ratingsList.isNotEmpty()) {
                val average = ratingsList.average()
                val count = ratingsList.size
                
                postsCollection.document(postId).update(
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
