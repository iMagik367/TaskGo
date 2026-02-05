package com.taskgoapp.taskgo.data.repository

import com.taskgoapp.taskgo.data.firestore.models.UserFirestore
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.taskgoapp.taskgo.core.firebase.LocationHelper
import com.taskgoapp.taskgo.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositório para buscar prestadores de serviços com algoritmo de classificação por avaliações
 * CRÍTICO: Busca prestadores de locations/{locationId}/users onde role == "partner" baseado no city/state do usuário atual
 */
@Singleton
class FirestoreProvidersRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val userRepository: UserRepository
) {
    private val usersCollection = firestore.collection("users")
    // REMOVIDO: reviewsCollection global - reviews estão SEMPRE em locations/{locationId}/reviews
    
    /**
     * Busca prestadores em destaque ordenados por algoritmo de avaliações
     * CRÍTICO: Busca em locations/{locationId}/users onde role == "partner" baseado no city/state do usuário atual
     * Algoritmo considera:
     * - Média de avaliações (peso 40%)
     * - Número de avaliações (peso 20%)
     * - Qualidade dos comentários (peso 20%)
     * - Recência das avaliações (peso 20%)
     */
    suspend fun getFeaturedProviders(limit: Int = 10): List<ProviderWithScore> {
        return try {
            val currentUser = userRepository.observeCurrentUser().first()
            val city = currentUser?.city?.takeIf { it.isNotBlank() } ?: ""
            val state = currentUser?.state?.takeIf { it.isNotBlank() } ?: ""
            
            if (city.isBlank() || state.isBlank()) {
                Log.w("FirestoreProvidersRepo", "City/state do usuário atual estão vazios, não é possível buscar providers")
                return emptyList()
            }
            
            val locationId = LocationHelper.normalizeLocationId(city, state)
            Log.d("FirestoreProvidersRepo", "📍 Buscando providers em destaque em locations/$locationId/users (city=$city, state=$state)")
            
            // CRÍTICO: Buscar em locations/{locationId}/users onde role == "partner"
            val providers = mutableListOf<UserFirestore>()
            
            val providersSnapshot = firestore.collection("locations")
                .document(locationId)
                .collection("users")
                .whereEqualTo("role", "partner")
                .get()
                .await()
            
            Log.d("FirestoreProvidersRepo", "Encontrados ${providersSnapshot.size()} providers em locations/$locationId/users")
            
            providersSnapshot.documents.forEach { doc ->
                val provider = doc.toObject(UserFirestore::class.java)?.copy(uid = doc.id)
                if (provider != null) {
                    providers.add(provider)
                }
            }
            
            // Calcular score para cada prestador
            val providersWithScore = providers.map { provider ->
                calculateProviderScore(provider)
            }.filter { it.score > 0.0 } // Filtrar apenas prestadores com avaliações
            
            Log.d("FirestoreProvidersRepo", "✅ Providers com score > 0: ${providersWithScore.size}")
            
            // Ordenar por score (maior primeiro) e retornar top N
            val result = providersWithScore
                .sortedByDescending { it.score }
                .take(limit)
            
            Log.d("FirestoreProvidersRepo", "✅ Retornando ${result.size} providers em destaque")
            result
        } catch (e: Exception) {
            Log.e("FirestoreProvidersRepository", "Erro ao buscar prestadores: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Calcula score de um prestador baseado em avaliações
     */
    private suspend fun calculateProviderScore(provider: UserFirestore): ProviderWithScore {
        return try {
            // CRÍTICO: Buscar city/state do provider e usar locations/{locationId}/reviews
            val providerCity = provider.city?.takeIf { it.isNotBlank() }
            val providerState = provider.state?.takeIf { it.isNotBlank() }
            
            if (providerCity.isNullOrBlank() || providerState.isNullOrBlank()) {
                Log.w("FirestoreProvidersRepo", "Provider ${provider.uid} não tem city/state definido")
                return ProviderWithScore(provider, 0.0)
            }
            
            // Buscar todas as avaliações do prestador em locations/{locationId}/reviews
            val reviewsCollection = LocationHelper.getLocationCollection(
                firestore,
                "reviews",
                providerCity,
                providerState
            )
            
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
            val providers = mutableListOf<UserFirestore>()
            
            // LEI MÁXIMA DO TASKGO: Buscar em locations/{locationId}/users quando temos city/state
            if (city != null && state != null) {
                try {
                    val locationId = com.taskgoapp.taskgo.core.firebase.LocationHelper.normalizeLocationId(city, state)
                    val locationQuery = firestore.collection("locations").document(locationId)
                        .collection("users")
                        .whereEqualTo("role", "partner")
                        .get()
                        .await()
                    
                    android.util.Log.d("FirestoreProvidersRepository", "Buscando em locations/$locationId/users: ${locationQuery.size()} documentos")
                    
                    locationQuery.documents.forEach { doc ->
                        val provider = doc.toObject(UserFirestore::class.java)?.copy(uid = doc.id)
                        if (provider != null) {
                            // Verificar categoria se necessário
                            val matchesCategory = if (category != null) {
                                provider.preferredCategories?.any { 
                                    it.equals(category, ignoreCase = true) 
                                } == true
                            } else {
                                true
                            }
                            
                            if (matchesCategory) {
                                providers.add(provider)
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("FirestoreProvidersRepository", "Erro ao buscar em locations: ${e.message}, tentando users global")
                }
            }
            
            // Fallback: Buscar em users global (legacy) se não encontrou em locations ou se não tem city/state
            if (providers.isEmpty() || city == null || state == null) {
                android.util.Log.d("FirestoreProvidersRepository", "Buscando em users global (legacy)...")
                var query: Query = usersCollection.whereEqualTo("role", "partner")
                val snapshot = query.get().await()
                
                snapshot.documents.forEach { doc ->
                    val provider = doc.toObject(UserFirestore::class.java)?.copy(uid = doc.id)
                    if (provider != null) {
                        var matches = true
                        
                        // Lei 1: Ler city/state APENAS da raiz do documento
                        if (city != null) {
                            matches = matches && provider.city?.equals(city, ignoreCase = true) == true
                        }
                        
                        if (state != null) {
                            matches = matches && provider.state?.equals(state, ignoreCase = true) == true
                        }
                        
                        if (category != null) {
                            matches = matches && provider.preferredCategories?.any { 
                                it.equals(category, ignoreCase = true) 
                            } == true
                        }
                        
                        if (matches) {
                            providers.add(provider)
                        }
                    }
                }
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

