package com.taskgoapp.taskgo.core.model

import java.util.Date

/**
 * Model de domínio para Post
 */
data class Post(
    val id: String,
    val userId: String,
    val userName: String,
    val userAvatarUrl: String?,
    val text: String,
    val mediaUrls: List<String>,
    val mediaTypes: List<String>, // "image" ou "video"
    val location: PostLocation?,
    val createdAt: Date?,
    val updatedAt: Date?,
    val likesCount: Int,
    val commentsCount: Int,
    val isLiked: Boolean, // Se o usuário atual curtiu o post
    val tags: List<String>?
)

/**
 * Localização do post
 */
data class PostLocation(
    val city: String,
    val state: String,
    val latitude: Double,
    val longitude: Double
) {
    /**
     * Calcula a distância em km entre duas localizações usando a fórmula de Haversine
     */
    fun distanceTo(other: PostLocation): Double {
        val earthRadius = 6371.0 // Raio da Terra em km
        
        val lat1Rad = Math.toRadians(latitude)
        val lat2Rad = Math.toRadians(other.latitude)
        val deltaLat = Math.toRadians(other.latitude - latitude)
        val deltaLon = Math.toRadians(other.longitude - longitude)
        
        val a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2)
        
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        
        return earthRadius * c
    }
}
