package com.taskgoapp.taskgo.domain.repository

import com.taskgoapp.taskgo.core.model.Story
import com.taskgoapp.taskgo.core.model.Result
import com.taskgoapp.taskgo.core.model.StoryAnalytics
import kotlinx.coroutines.flow.Flow

/**
 * Repositório para gerenciar Stories
 */
interface StoriesRepository {
    
    /**
     * Observa stories de usuários seguidos/próximos (para o feed)
     * Retorna apenas stories não expiradas (menos de 24 horas)
     */
    fun observeStories(
        currentUserId: String,
        radiusKm: Double = 50.0,
        userLocation: Pair<Double, Double>? = null // latitude, longitude
    ): Flow<List<Story>>
    
    /**
     * Observa stories de um usuário específico
     */
    fun observeUserStories(userId: String, currentUserId: String): Flow<List<Story>>
    
    /**
     * Cria uma nova story
     */
    suspend fun createStory(story: Story): Result<String>
    
    /**
     * Marca uma story como visualizada
     */
    suspend fun markStoryAsViewed(storyId: String, userId: String): Result<Unit>
    
    /**
     * Deleta uma story
     */
    suspend fun deleteStory(storyId: String, userId: String): Result<Unit>
    
    /**
     * Busca stories de usuários próximos (dentro de um raio)
     */
    suspend fun getStoriesNearby(
        currentUserId: String,
        latitude: Double,
        longitude: Double,
        radiusKm: Double = 50.0
    ): Result<List<Story>>
    
    /**
     * Obtém analytics de uma story (visualizações, métricas, etc.)
     * Apenas o dono da story pode ver os analytics
     */
    fun observeStoryAnalytics(storyId: String, ownerUserId: String): Flow<StoryAnalytics>
    
    /**
     * Registra uma ação do usuário na story (navegação para perfil, voltar, swipe up)
     */
    suspend fun trackStoryAction(
        storyId: String,
        userId: String,
        action: String, // "navigation", "back", "swipe_up"
        metadata: Map<String, Any>? = null
    ): Result<Unit>
}

