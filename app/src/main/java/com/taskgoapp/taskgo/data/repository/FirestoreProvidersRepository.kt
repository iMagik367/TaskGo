package com.taskgoapp.taskgo.data.repository

import com.taskgoapp.taskgo.data.firestore.models.UserFirestore
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * Repositório para buscar prestadores de serviços com algoritmo de classificação por avaliações
 */
class FirestoreProvidersRepository(
    private val firestore: FirebaseFirestore
) {
    private val usersCollection = firestore.collection("users")
    private val reviewsCollection = firestore.collection("reviews")
    
    /**
     * Busca prestadores em destaque ordenados por algoritmo de avaliações
     * Algoritmo considera:
     * - Média de avaliações (peso 40%)
     * - Número de avaliações (peso 20%)
     * - Qualidade dos comentários (peso 20%)
     * - Recência das avaliações (peso 20%)
     */
    suspend fun getFeaturedProviders(limit: Int = 10): List<ProviderWithScore> {
        return try {
            // Buscar todos os prestadores
            val providersSnapshot = usersCollection
                .whereEqualTo("role", "provider")
                .get()
                .await()
            
            val providers = providersSnapshot.documents.mapNotNull { doc ->
                doc.toObject(UserFirestore::class.java)?.copy(uid = doc.id)
            }
            
            // Calcular score para cada prestador
            val providersWithScore = providers.map { provider ->
                calculateProviderScore(provider)
            }.filter { it.score > 0.0 } // Filtrar apenas prestadores com avaliações
            
            // Ordenar por score (maior primeiro) e retornar top N
            providersWithScore
                .sortedByDescending { it.score }
                .take(limit)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreProvidersRepository", "Erro ao buscar prestadores: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Calcula score de um prestador baseado em avaliações
     */
    private suspend fun calculateProviderScore(provider: UserFirestore): ProviderWithScore {
        return try {
            // Buscar todas as avaliações do prestador
            val reviewsSnapshot = reviewsCollection
                .whereEqualTo("type", "PROVIDER")
                .whereEqualTo("targetId", provider.uid)
                .get()
                .await()
            
            val reviews = reviewsSnapshot.documents.mapNotNull { doc ->
                doc.toObject(com.taskgoapp.taskgo.data.firestore.models.ReviewFirestore::class.java)
            }
            
            if (reviews.isEmpty()) {
                return ProviderWithScore(provider, 0.0)
            }
            
            // 1. Média de avaliações (peso 40%)
            val averageRating = reviews.map { it.rating }.average()
            val ratingScore = (averageRating / 5.0) * 0.4
            
            // 2. Número de avaliações (peso 20%)
            // Normalizar: mais avaliações = melhor, mas com limite de 100 avaliações
            val reviewCount = reviews.size
            val reviewCountScore = (minOf(reviewCount, 100) / 100.0) * 0.2
            
            // 3. Qualidade dos comentários (peso 20%)
            // Avaliar comentários positivos (palavras-chave)
            val positiveKeywords = listOf("excelente", "ótimo", "recomendo", "profissional", "qualidade", "satisfeito", "perfeito", "bom")
            val commentsWithKeywords = reviews.count { review ->
                val comment = review.comment?.lowercase() ?: ""
                positiveKeywords.any { keyword -> comment.contains(keyword) }
            }
            val commentQualityScore = if (reviewCount > 0) {
                (commentsWithKeywords.toDouble() / reviewCount) * 0.2
            } else {
                0.0
            }
            
            // 4. Recência das avaliações (peso 20%)
            // Avaliações mais recentes têm mais peso
            val now = System.currentTimeMillis()
            val recentReviews = reviews.count { review ->
                val reviewTime = review.createdAt?.time ?: 0L
                val daysSinceReview = (now - reviewTime) / (1000 * 60 * 60 * 24)
                daysSinceReview <= 90 // Últimos 90 dias
            }
            val recencyScore = if (reviewCount > 0) {
                (recentReviews.toDouble() / reviewCount) * 0.2
            } else {
                0.0
            }
            
            // Score final (0.0 a 1.0)
            val finalScore = ratingScore + reviewCountScore + commentQualityScore + recencyScore
            
            ProviderWithScore(provider, finalScore)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreProvidersRepository", "Erro ao calcular score: ${e.message}", e)
            ProviderWithScore(provider, 0.0)
        }
    }
    
    /**
     * Busca prestadores por localização e categoria
     */
    suspend fun getProvidersByLocationAndCategory(
        city: String?,
        state: String?,
        category: String?,
        limit: Int = 20
    ): List<ProviderWithScore> {
        return try {
            var query: Query = usersCollection.whereEqualTo("role", "provider")
            
            // Filtrar por localização se fornecida
            // Nota: Firestore não suporta múltiplos whereEqualTo em campos diferentes
            // Então vamos buscar todos e filtrar em memória
            val snapshot = query.get().await()
            
            val providers = snapshot.documents.mapNotNull { doc ->
                doc.toObject(UserFirestore::class.java)?.copy(uid = doc.id)
            }.filter { provider ->
                var matches = true
                
                if (city != null) {
                    matches = matches && provider.address?.city?.equals(city, ignoreCase = true) == true
                }
                
                if (state != null) {
                    matches = matches && provider.address?.state?.equals(state, ignoreCase = true) == true
                }
                
                if (category != null) {
                    matches = matches && provider.preferredCategories?.any { 
                        it.equals(category, ignoreCase = true) 
                    } == true
                }
                
                matches
            }
            
            // Calcular scores e ordenar
            providers.map { provider ->
                calculateProviderScore(provider)
            }.sortedByDescending { it.score }.take(limit)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreProvidersRepository", "Erro ao buscar prestadores: ${e.message}", e)
            emptyList()
        }
    }
}

/**
 * Classe para armazenar prestador com seu score calculado
 */
data class ProviderWithScore(
    val provider: UserFirestore,
    val score: Double
)

