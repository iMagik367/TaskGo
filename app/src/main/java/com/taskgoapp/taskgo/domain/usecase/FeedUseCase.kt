package com.taskgoapp.taskgo.domain.usecase

import com.taskgoapp.taskgo.core.model.Post
import com.taskgoapp.taskgo.core.model.PostLocation
import com.taskgoapp.taskgo.core.model.Result
import com.taskgoapp.taskgo.domain.repository.FeedRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * Use cases para operações do Feed
 */
class CreatePostUseCase @Inject constructor(
    private val feedRepository: FeedRepository
) {
    suspend operator fun invoke(
        text: String,
        mediaUrls: List<String>,
        mediaTypes: List<String>,
        location: PostLocation
    ): Result<String> {
        android.util.Log.d("CreatePostUseCase", "🔵 INVOKE: Iniciando CreatePostUseCase")
        android.util.Log.d("CreatePostUseCase", "   text.length=${text.length}, mediaUrls.size=${mediaUrls.size}, location.city=${location.city}")
        
        return try {
            val result = feedRepository.createPost(text, mediaUrls, mediaTypes, location)
            when (result) {
                is com.taskgoapp.taskgo.core.model.Result.Success -> {
                    android.util.Log.d("CreatePostUseCase", "✅ SUCESSO: Post criado com ID=${result.data}")
                    result
                }
                is com.taskgoapp.taskgo.core.model.Result.Error -> {
                    android.util.Log.e("CreatePostUseCase", "🔴 ERRO: Falha ao criar post", result.exception)
                    android.util.Log.e("CreatePostUseCase", "   Tipo: ${result.exception.javaClass.simpleName}, Mensagem: ${result.exception.message}")
                    result
                }
                is com.taskgoapp.taskgo.core.model.Result.Loading -> {
                    android.util.Log.d("CreatePostUseCase", "🟡 LOADING: Criando post...")
                    result
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("CreatePostUseCase", "🔴 EXCEPTION: Exceção não capturada", e)
            android.util.Log.e("CreatePostUseCase", "   Tipo: ${e.javaClass.simpleName}, Mensagem: ${e.message}")
            com.taskgoapp.taskgo.core.model.Result.Error(e)
        }
    }
}

class GetFeedPostsUseCase @Inject constructor(
    private val feedRepository: FeedRepository
) {
    operator fun invoke(
        userLatitude: Double,
        userLongitude: Double,
        radiusKm: Double
    ): Flow<List<Post>> {
        android.util.Log.d("GetFeedPostsUseCase", "🔵 INVOKE: Iniciando GetFeedPostsUseCase")
        android.util.Log.d("GetFeedPostsUseCase", "   Parâmetros: lat=$userLatitude, lng=$userLongitude, radius=$radiusKm")
        
        return feedRepository.observeFeedPosts(userLatitude, userLongitude, radiusKm)
            .onStart {
                android.util.Log.d("GetFeedPostsUseCase", "🟢 ON_START: Flow iniciado, aguardando dados do repositório...")
            }
            .onEach { posts ->
                android.util.Log.d("GetFeedPostsUseCase", "🟡 ON_EACH: Recebidos ${posts.size} posts do repositório")
                if (posts.isEmpty()) {
                    android.util.Log.w("GetFeedPostsUseCase", "⚠️ AVISO: Lista de posts está vazia")
                } else {
                    android.util.Log.d("GetFeedPostsUseCase", "   Primeiro post: userId=${posts.first().userId}, userName=${posts.first().userName}")
                }
            }
            .catch { e ->
                android.util.Log.e("GetFeedPostsUseCase", "🔴 CATCH: Erro no Flow do repositório", e)
                android.util.Log.e("GetFeedPostsUseCase", "   Tipo de erro: ${e.javaClass.simpleName}")
                android.util.Log.e("GetFeedPostsUseCase", "   Mensagem: ${e.message}")
                throw e
            }
            .onCompletion { cause ->
                if (cause == null) {
                    android.util.Log.d("GetFeedPostsUseCase", "✅ ON_COMPLETION: Flow completado com sucesso")
                } else {
                    android.util.Log.e("GetFeedPostsUseCase", "❌ ON_COMPLETION: Flow completado com erro", cause)
                }
            }
    }
}

class GetUserPostsUseCase @Inject constructor(
    private val feedRepository: FeedRepository
) {
    operator fun invoke(userId: String): Flow<List<Post>> {
        android.util.Log.d("GetUserPostsUseCase", "🔵 INVOKE: Iniciando GetUserPostsUseCase para userId=$userId")
        
        if (userId.isBlank()) {
            android.util.Log.e("GetUserPostsUseCase", "🔴 ERRO: userId está vazio!")
            return flowOf(emptyList())
        }
        
        return feedRepository.observeUserPosts(userId)
            .onStart {
                android.util.Log.d("GetUserPostsUseCase", "🟢 ON_START: Flow iniciado para userId=$userId")
            }
            .onEach { posts ->
                android.util.Log.d("GetUserPostsUseCase", "🟡 ON_EACH: Recebidos ${posts.size} posts do usuário $userId")
                if (posts.isEmpty()) {
                    android.util.Log.w("GetUserPostsUseCase", "⚠️ AVISO: Usuário $userId não tem posts")
                }
            }
            .catch { e ->
                android.util.Log.e("GetUserPostsUseCase", "🔴 CATCH: Erro no Flow do repositório para userId=$userId", e)
                android.util.Log.e("GetUserPostsUseCase", "   Tipo: ${e.javaClass.simpleName}, Mensagem: ${e.message}")
                throw e
            }
            .onCompletion { cause ->
                if (cause == null) {
                    android.util.Log.d("GetUserPostsUseCase", "✅ ON_COMPLETION: Flow completado com sucesso para userId=$userId")
                } else {
                    android.util.Log.e("GetUserPostsUseCase", "❌ ON_COMPLETION: Flow completado com erro para userId=$userId", cause)
                }
            }
    }
}

class LikePostUseCase @Inject constructor(
    private val feedRepository: FeedRepository
) {
    suspend operator fun invoke(postId: String, userId: String): Result<Unit> {
        android.util.Log.d("LikePostUseCase", "🔵 INVOKE: postId=$postId, userId=$userId")
        val result = feedRepository.likePost(postId, userId)
        when (result) {
            is com.taskgoapp.taskgo.core.model.Result.Success -> android.util.Log.d("LikePostUseCase", "✅ SUCESSO")
            is com.taskgoapp.taskgo.core.model.Result.Error -> android.util.Log.e("LikePostUseCase", "🔴 ERRO: ${result.exception.message}", result.exception)
            is com.taskgoapp.taskgo.core.model.Result.Loading -> android.util.Log.d("LikePostUseCase", "🟡 LOADING")
        }
        return result
    }
}

class UnlikePostUseCase @Inject constructor(
    private val feedRepository: FeedRepository
) {
    suspend operator fun invoke(postId: String, userId: String): Result<Unit> {
        android.util.Log.d("UnlikePostUseCase", "🔵 INVOKE: postId=$postId, userId=$userId")
        val result = feedRepository.unlikePost(postId, userId)
        when (result) {
            is com.taskgoapp.taskgo.core.model.Result.Success -> android.util.Log.d("UnlikePostUseCase", "✅ SUCESSO")
            is com.taskgoapp.taskgo.core.model.Result.Error -> android.util.Log.e("UnlikePostUseCase", "🔴 ERRO: ${result.exception.message}", result.exception)
            is com.taskgoapp.taskgo.core.model.Result.Loading -> android.util.Log.d("UnlikePostUseCase", "🟡 LOADING")
        }
        return result
    }
}

class DeletePostUseCase @Inject constructor(
    private val feedRepository: FeedRepository
) {
    suspend operator fun invoke(postId: String): Result<Unit> {
        android.util.Log.d("DeletePostUseCase", "🔵 INVOKE: postId=$postId")
        val result = feedRepository.deletePost(postId)
        when (result) {
            is com.taskgoapp.taskgo.core.model.Result.Success -> android.util.Log.d("DeletePostUseCase", "✅ SUCESSO")
            is com.taskgoapp.taskgo.core.model.Result.Error -> android.util.Log.e("DeletePostUseCase", "🔴 ERRO: ${result.exception.message}", result.exception)
            is com.taskgoapp.taskgo.core.model.Result.Loading -> android.util.Log.d("DeletePostUseCase", "🟡 LOADING")
        }
        return result
    }
}
