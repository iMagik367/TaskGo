package com.taskgoapp.taskgo.domain.repository

import com.taskgoapp.taskgo.core.model.Post
import com.taskgoapp.taskgo.core.model.PostLocation
import com.taskgoapp.taskgo.core.model.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository para operações do Feed
 */
interface FeedRepository {
    
    /**
     * Observa posts do feed filtrados por região (raio em km)
     */
    fun observeFeedPosts(
        userLatitude: Double,
        userLongitude: Double,
        radiusKm: Double
    ): Flow<List<Post>>
    
    /**
     * Observa posts de um usuário específico
     */
    fun observeUserPosts(userId: String): Flow<List<Post>>
    
    /**
     * Obtém um post específico por ID
     */
    suspend fun getPostById(postId: String): Post?
    
    /**
     * Cria um novo post
     */
    suspend fun createPost(
        text: String,
        mediaUrls: List<String>,
        mediaTypes: List<String>,
        location: PostLocation
    ): Result<String>
    
    /**
     * Adiciona like em um post
     */
    suspend fun likePost(postId: String, userId: String): Result<Unit>
    
    /**
     * Remove like de um post
     */
    suspend fun unlikePost(postId: String, userId: String): Result<Unit>
    
    /**
     * Deleta um post (apenas pelo autor)
     */
    suspend fun deletePost(postId: String): Result<Unit>
    
    /**
     * Observa comentários de um post
     */
    fun observePostComments(postId: String): Flow<List<com.taskgoapp.taskgo.feature.feed.presentation.components.CommentItem>>
    
    /**
     * Cria um novo comentário em um post
     */
    suspend fun createComment(postId: String, text: String): Result<String>
    
    /**
     * Deleta um comentário (apenas pelo autor)
     */
    suspend fun deleteComment(postId: String, commentId: String): Result<Unit>
    
    /**
     * Registra interesse em um post (Tenho interesse / Não tenho interesse)
     */
    suspend fun setPostInterest(postId: String, isInterested: Boolean): Result<Unit>
    
    /**
     * Remove interesse de um post
     */
    suspend fun removePostInterest(postId: String): Result<Unit>
    
    /**
     * Verifica se o usuário tem interesse em um post
     */
    suspend fun getPostInterest(postId: String): Boolean?
    
    /**
     * Avalia um post (1-5 estrelas)
     */
    suspend fun ratePost(postId: String, rating: Int, comment: String?): Result<String>
    
    /**
     * Obtém avaliação do usuário atual para um post
     */
    suspend fun getUserPostRating(postId: String): com.taskgoapp.taskgo.core.model.PostRating?
    
    /**
     * Bloqueia um usuário
     */
    suspend fun blockUser(userId: String): Result<Unit>
    
    /**
     * Desbloqueia um usuário
     */
    suspend fun unblockUser(userId: String): Result<Unit>
    
    /**
     * Verifica se um usuário está bloqueado
     */
    suspend fun isUserBlocked(userId: String): Boolean
    
    /**
     * Obtém lista de usuários bloqueados
     */
    fun observeBlockedUsers(): Flow<List<com.taskgoapp.taskgo.core.model.BlockedUser>>
}
