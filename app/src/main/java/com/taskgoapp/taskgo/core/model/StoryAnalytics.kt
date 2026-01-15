package com.taskgoapp.taskgo.core.model

import java.util.Date

/**
 * Model de domínio para Analytics de Story (similar ao Instagram)
 * Contém todas as métricas e visualizações de uma story
 */
data class StoryAnalytics(
    val storyId: String,
    val userId: String,
    val views: List<StoryView> = emptyList(),
    val accountsReached: Int = 0,
    val impressions: Int = 0,
    val followers: Int = 0,
    val navigation: Int = 0, // Navegação para o perfil
    val back: Int = 0, // Voltar (swipe down ou tap esquerdo)
    val alignments: Int = 0, // Swipe para cima (abrir analytics)
    val interactions: StoryInteractions = StoryInteractions()
)

/**
 * Model de visualização de Story
 */
data class StoryView(
    val userId: String,
    val userName: String,
    val userAvatarUrl: String? = null,
    val viewedAt: Date,
    val isFollower: Boolean = false // Se o usuário que visualizou é seguidor
)

/**
 * Model de interações com Story
 */
data class StoryInteractions(
    val profileVisits: Int = 0, // Visitas ao perfil a partir da story
    val linkClicks: Int = 0 // Cliques em links (para futuras implementações)
)
