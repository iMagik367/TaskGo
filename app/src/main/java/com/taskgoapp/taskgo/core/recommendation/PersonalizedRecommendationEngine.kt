package com.taskgoapp.taskgo.core.recommendation

import android.util.Log
import com.taskgoapp.taskgo.core.model.Product
import com.taskgoapp.taskgo.core.model.ServiceOrder
import com.taskgoapp.taskgo.data.firestore.models.ReviewFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * Motor de recomendação personalizado avançado
 * Analisa padrões de compra, avaliações, promoções e comportamento do usuário
 * para criar recomendações altamente personalizadas que aumentam retenção
 */
@Singleton
class PersonalizedRecommendationEngine @Inject constructor() {
    
    companion object {
        private const val TAG = "RecommendationEngine"
        
        // Pesos para diferentes fatores de recomendação
        private const val WEIGHT_PURCHASE_HISTORY = 0.35f
        private const val WEIGHT_REVIEWS = 0.25f
        private const val WEIGHT_PROMOTIONS = 0.15f
        private const val WEIGHT_CATEGORY_PREFERENCE = 0.15f
        private const val WEIGHT_LOCATION = 0.10f
        
        // Fatores de decaimento temporal (quanto mais recente, mais relevante)
        private const val TIME_DECAY_FACTOR = 0.1f
    }
    
    /**
     * Calcula score de recomendação para um produto
     */
    suspend fun calculateProductScore(
        product: Product,
        userPurchaseHistory: List<UserPurchase>,
        userReviews: List<ReviewFirestore>,
        userLocation: UserLocation?,
        currentPromotions: List<Promotion>,
        userCategoryPreferences: Map<String, Float>
    ): Float {
        var totalScore = 0f
        
        // 1. Histórico de compras (35%)
        val purchaseScore = calculatePurchaseHistoryScore(
            product = product,
            purchaseHistory = userPurchaseHistory
        )
        totalScore += purchaseScore * WEIGHT_PURCHASE_HISTORY
        
        // 2. Avaliações (25%)
        val reviewScore = calculateReviewScore(
            product = product,
            userReviews = userReviews,
            allProductReviews = emptyList() // Será passado do repositório
        )
        totalScore += reviewScore * WEIGHT_REVIEWS
        
        // 3. Promoções (15%)
        val promotionScore = calculatePromotionScore(
            product = product,
            promotions = currentPromotions
        )
        totalScore += promotionScore * WEIGHT_PROMOTIONS
        
        // 4. Preferências de categoria (15%)
        val categoryScore = calculateCategoryPreferenceScore(
            product = product,
            categoryPreferences = userCategoryPreferences
        )
        totalScore += categoryScore * WEIGHT_CATEGORY_PREFERENCE
        
        // 5. Localização (10%)
        val locationScore = calculateLocationScore(
            product = product,
            userLocation = userLocation
        )
        totalScore += locationScore * WEIGHT_LOCATION
        
        return totalScore.coerceIn(0f, 1f)
    }
    
    /**
     * Calcula score baseado no histórico de compras
     */
    private fun calculatePurchaseHistoryScore(
        product: Product,
        purchaseHistory: List<UserPurchase>
    ): Float {
        if (purchaseHistory.isEmpty()) return 0.5f // Score neutro se não houver histórico
        
        // Busca compras similares
        // Como Product não tem category nem brand, comparamos apenas por preço similar
        val productPrice = product.price.toFloat()
        val similarPurchases = purchaseHistory.filter { purchase ->
            // Compara se o preço está em uma faixa similar (±30%)
            val priceDiff = kotlin.math.abs(purchase.price - productPrice) / productPrice
            priceDiff <= 0.3f
        }
        
        if (similarPurchases.isEmpty()) return 0.3f
        
        // Calcula score baseado em frequência e recência
        var score = 0f
        val now = System.currentTimeMillis()
        
        similarPurchases.forEach { purchase ->
            val timeDecay = exp(-TIME_DECAY_FACTOR * (now - purchase.timestamp) / (1000 * 60 * 60 * 24)) // Decaimento diário
            val frequencyBonus = min(similarPurchases.size / 10f, 1f) // Bônus por frequência
            score += (0.5f + frequencyBonus * 0.5f) * timeDecay.toFloat()
        }
        
        return (score / similarPurchases.size).coerceIn(0f, 1f)
    }
    
    /**
     * Calcula score baseado em avaliações
     */
    private fun calculateReviewScore(
        product: Product,
        userReviews: List<ReviewFirestore>,
        allProductReviews: List<ReviewFirestore>
    ): Float {
        // Avaliações do próprio usuário para produtos similares
        val similarReviews = userReviews.filter { review ->
            review.type == "PRODUCT" && review.targetId == product.id.toString()
        }
        
        var score = 0.5f // Score base
        
        if (similarReviews.isNotEmpty()) {
            val avgRating = similarReviews.map { it.rating }.average().toFloat()
            score = (avgRating / 5f) // Normaliza para 0-1
        }
        
        // Considera avaliações gerais do produto (se disponíveis)
        val productReviews = allProductReviews.filter { it.targetId == product.id.toString() }
        if (productReviews.isNotEmpty()) {
            val avgProductRating = productReviews.map { it.rating }.average().toFloat()
            val generalScore = avgProductRating / 5f
            // Combina avaliação pessoal (70%) com geral (30%)
            score = score * 0.7f + generalScore * 0.3f
        }
        
        return score.coerceIn(0f, 1f)
    }
    
    /**
     * Calcula score baseado em promoções ativas
     */
    private fun calculatePromotionScore(
        product: Product,
        promotions: List<Promotion>
    ): Float {
        val activePromotion = promotions.find { it.productId == product.id.toString() }
        
        if (activePromotion == null) return 0.5f
        
        // Score baseado no desconto
        val discountPercent = activePromotion.discountPercent
        val score = when {
            discountPercent >= 50 -> 1.0f
            discountPercent >= 30 -> 0.8f
            discountPercent >= 20 -> 0.6f
            discountPercent >= 10 -> 0.4f
            else -> 0.2f
        }
        
        // Bônus por urgência (promoção acabando)
        if (activePromotion.isUrgent) {
            return min(1.0f, score * 1.2f)
        }
        
        return score
    }
    
    /**
     * Calcula score baseado em preferências de categoria
     */
    private fun calculateCategoryPreferenceScore(
        product: Product,
        categoryPreferences: Map<String, Float>
    ): Float {
        // Product não tem category, então retornamos score neutro
        // Em uma implementação futura, poderia extrair categoria do título ou descrição
        return 0.5f
    }
    
    /**
     * Calcula score baseado em localização
     */
    private fun calculateLocationScore(
        product: Product,
        userLocation: UserLocation?
    ): Float {
        if (userLocation == null || product.latitude == null || product.longitude == null) {
            return 0.5f
        }
        
        // Calcula distância
        val distance = calculateDistance(
            lat1 = userLocation.latitude,
            lon1 = userLocation.longitude,
            lat2 = product.latitude.toDouble(),
            lon2 = product.longitude.toDouble()
        )
        
        // Score baseado na distância (quanto mais perto, maior o score)
        return when {
            distance < 5 -> 1.0f      // Menos de 5km
            distance < 10 -> 0.8f     // Menos de 10km
            distance < 25 -> 0.6f     // Menos de 25km
            distance < 50 -> 0.4f     // Menos de 50km
            else -> 0.2f              // Mais de 50km
        }
    }
    
    /**
     * Calcula distância entre duas coordenadas (Haversine)
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0 // Raio da Terra em km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }
    
    /**
     * Analisa padrões de comportamento do usuário
     */
    fun analyzeUserPatterns(
        purchaseHistory: List<UserPurchase>,
        reviews: List<ReviewFirestore>,
        searchHistory: List<String>
    ): UserBehaviorPattern {
        // Categorias mais compradas
        val categoryFrequency = purchaseHistory.groupingBy { it.category }.eachCount()
        val topCategories = categoryFrequency.toList()
            .sortedByDescending { it.second }
            .take(5)
            .map { it.first }
        
        // Faixa de preço preferida
        val prices = purchaseHistory.map { it.price }
        val avgPrice = prices.average().toFloat()
        val priceRange = PriceRange(
            min = prices.minOrNull() ?: 0f,
            max = prices.maxOrNull() ?: Float.MAX_VALUE,
            average = avgPrice
        )
        
        // Horários de compra mais frequentes
        val purchaseHours = purchaseHistory.map { purchase ->
            java.util.Calendar.getInstance().apply {
                timeInMillis = purchase.timestamp
            }.get(java.util.Calendar.HOUR_OF_DAY)
        }
        val mostActiveHour = purchaseHours.groupingBy { it }.eachCount()
            .maxByOrNull { it.value }?.key ?: 12
        
        // Padrões de busca
        val searchTerms = searchHistory.flatMap { it.split(" ") }
        val topSearchTerms = searchTerms.groupingBy { it.lowercase() }.eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(10)
            .map { it.first }
        
        return UserBehaviorPattern(
            topCategories = topCategories,
            preferredPriceRange = priceRange,
            mostActiveHour = mostActiveHour,
            topSearchTerms = topSearchTerms,
            averageRatingGiven = reviews.map { it.rating }.average().toFloat(),
            totalPurchases = purchaseHistory.size,
            totalReviews = reviews.size
        )
    }
    
    /**
     * Gera recomendações personalizadas para o usuário
     */
    suspend fun generatePersonalizedRecommendations(
        availableProducts: List<Product>,
        userContext: UserContext
    ): Flow<List<RecommendedItem>> = flow {
        val recommendations = availableProducts.map { product ->
            val score = calculateProductScore(
                product = product,
                userPurchaseHistory = userContext.purchaseHistory,
                userReviews = userContext.reviews,
                userLocation = userContext.location,
                currentPromotions = userContext.activePromotions,
                userCategoryPreferences = userContext.categoryPreferences
            )
            
            RecommendedItem(
                product = product,
                score = score,
                reasons = generateRecommendationReasons(product, userContext, score)
            )
        }
        .sortedByDescending { it.score }
        .take(20) // Top 20 recomendações
        
        emit(recommendations)
    }
    
    /**
     * Gera razões para a recomendação (aumenta transparência e confiança)
     */
    private fun generateRecommendationReasons(
        product: Product,
        userContext: UserContext,
        score: Float
    ): List<String> {
        val reasons = mutableListOf<String>()
        
        // Razão baseada em compras similares (por preço)
        val productPrice = product.price.toFloat()
        val similarPurchases = userContext.purchaseHistory.count { purchase ->
            val priceDiff = kotlin.math.abs(purchase.price - productPrice) / productPrice
            priceDiff <= 0.3f
        }
        if (similarPurchases > 0) {
            reasons.add("Você já comprou $similarPurchases ${if (similarPurchases == 1) "item similar" else "itens similares"}")
        }
        
        // Razão baseada em promoção
        val promotion = userContext.activePromotions.find { it.productId == product.id.toString() }
        if (promotion != null) {
            reasons.add("${promotion.discountPercent}% de desconto disponível")
        }
        
        // Razão baseada em localização
        if (userContext.location != null && product.latitude != null && product.longitude != null) {
            val distance = calculateDistance(
                userContext.location.latitude,
                userContext.location.longitude,
                product.latitude,
                product.longitude
            )
            if (distance < 10) {
                reasons.add("Próximo da sua localização (${distance.toInt()}km)")
            }
        }
        
        // Razão baseada em avaliações
        val productReviews = userContext.reviews.filter { it.targetId == product.id.toString() }
        if (productReviews.isNotEmpty()) {
            val avgRating = productReviews.map { it.rating }.average()
            if (avgRating >= 4.0) {
                reasons.add("Bem avaliado (${String.format("%.1f", avgRating)}/5.0)")
            }
        }
        
        return reasons.ifEmpty { listOf("Recomendado para você") }
    }
}

// Modelos de dados para o motor de recomendação
data class UserPurchase(
    val productId: String,
    val category: String,
    val price: Float,
    val brand: String?,
    val timestamp: Long,
    val rating: Float? = null
)

data class UserLocation(
    val latitude: Double,
    val longitude: Double
)

data class Promotion(
    val productId: String,
    val discountPercent: Int,
    val isUrgent: Boolean = false,
    val endDate: Long? = null
)

data class PriceRange(
    val min: Float,
    val max: Float,
    val average: Float
) {
    fun overlaps(price: Float): Boolean = price in min..max
}

data class UserBehaviorPattern(
    val topCategories: List<String>,
    val preferredPriceRange: PriceRange,
    val mostActiveHour: Int,
    val topSearchTerms: List<String>,
    val averageRatingGiven: Float,
    val totalPurchases: Int,
    val totalReviews: Int
)

data class UserContext(
    val purchaseHistory: List<UserPurchase>,
    val reviews: List<ReviewFirestore>,
    val location: UserLocation?,
    val activePromotions: List<Promotion>,
    val categoryPreferences: Map<String, Float>
)

data class RecommendedItem(
    val product: Product,
    val score: Float,
    val reasons: List<String>
)

