package com.taskgoapp.taskgo.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.taskgoapp.taskgo.core.model.Review
import com.taskgoapp.taskgo.core.model.ReviewSummary
import com.taskgoapp.taskgo.core.model.ReviewType
import com.taskgoapp.taskgo.core.model.Result
import com.taskgoapp.taskgo.data.firestore.models.ReviewFirestore
import com.taskgoapp.taskgo.data.mapper.ReviewMapper.toFirestore
import com.taskgoapp.taskgo.data.mapper.ReviewMapper.toModel
import com.taskgoapp.taskgo.domain.repository.ReviewsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreReviewsRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : ReviewsRepository {
    
    private val reviewsCollection = firestore.collection("reviews")
    
    override fun observeReviews(targetId: String, type: ReviewType): Flow<List<Review>> = callbackFlow {
        val typeString = when (type) {
            ReviewType.PRODUCT -> "PRODUCT"
            ReviewType.SERVICE -> "SERVICE"
            ReviewType.PROVIDER -> "PROVIDER"
        }
        
        val listenerRegistration = reviewsCollection
            .whereEqualTo("targetId", targetId)
            .whereEqualTo("type", typeString)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val reviews = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ReviewFirestore::class.java)?.copy(id = doc.id)?.toModel()
                } ?: emptyList()
                
                trySend(reviews)
            }
        
        awaitClose { listenerRegistration.remove() }
    }
    
    override suspend fun getReview(reviewId: String): Review? {
        return try {
            val document = reviewsCollection.document(reviewId).get().await()
            document.toObject(ReviewFirestore::class.java)?.copy(id = document.id)?.toModel()
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun createReview(review: Review): Result<String> {
        return try {
            val reviewFirestore = review.toFirestore()
            val docRef = reviewsCollection.add(reviewFirestore).await()
            
            // Atualizar média de avaliações do target
            updateTargetRating(review.targetId, review.type)
            
            Result.Success(docRef.id)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override suspend fun updateReview(
        reviewId: String,
        rating: Int?,
        comment: String?,
        photoUrls: List<String>?
    ): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>()
            rating?.let { updates["rating"] = it }
            comment?.let { updates["comment"] = it }
            photoUrls?.let { updates["photoUrls"] = it }
            updates["updatedAt"] = FieldValue.serverTimestamp()
            
            reviewsCollection.document(reviewId).update(updates).await()
            
            // Recuperar review para atualizar média
            val review = getReview(reviewId)
            review?.let {
                updateTargetRating(it.targetId, it.type)
            }
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override suspend fun deleteReview(reviewId: String): Result<Unit> {
        return try {
            val review = getReview(reviewId)
            reviewsCollection.document(reviewId).delete().await()
            
            // Atualizar média de avaliações do target
            review?.let {
                updateTargetRating(it.targetId, it.type)
            }
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override suspend fun getReviewSummary(targetId: String, type: ReviewType): ReviewSummary {
        return try {
            val typeString = when (type) {
                ReviewType.PRODUCT -> "PRODUCT"
                ReviewType.SERVICE -> "SERVICE"
                ReviewType.PROVIDER -> "PROVIDER"
            }
            
            val snapshot = reviewsCollection
                .whereEqualTo("targetId", targetId)
                .whereEqualTo("type", typeString)
                .get()
                .await()
            
            val reviews = snapshot.documents.mapNotNull { doc ->
                doc.toObject(ReviewFirestore::class.java)?.rating
            }
            
            if (reviews.isEmpty()) {
                ReviewSummary(0.0, 0)
            } else {
                val averageRating = reviews.average()
                val ratingDistribution = reviews.groupingBy { it }.eachCount()
                
                ReviewSummary(
                    averageRating = averageRating,
                    totalReviews = reviews.size,
                    ratingDistribution = ratingDistribution
                )
            }
        } catch (e: Exception) {
            ReviewSummary(0.0, 0)
        }
    }
    
    override suspend fun markReviewAsHelpful(reviewId: String): Result<Unit> {
        return try {
            reviewsCollection.document(reviewId)
                .update("helpfulCount", FieldValue.increment(1))
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
    
    override suspend fun canUserReview(targetId: String, type: ReviewType, userId: String): Boolean {
        return try {
            val typeString = when (type) {
                ReviewType.PRODUCT -> "PRODUCT"
                ReviewType.SERVICE -> "SERVICE"
                ReviewType.PROVIDER -> "PROVIDER"
            }
            
            val snapshot = reviewsCollection
                .whereEqualTo("targetId", targetId)
                .whereEqualTo("type", typeString)
                .whereEqualTo("reviewerId", userId)
                .get()
                .await()
            
            snapshot.isEmpty
        } catch (e: Exception) {
            false
        }
    }
    
    private suspend fun updateTargetRating(targetId: String, type: ReviewType) {
        try {
            val summary = getReviewSummary(targetId, type)
            val collectionName = when (type) {
                ReviewType.PRODUCT -> "products"
                ReviewType.SERVICE -> "services" // Corrigido: usar "services" em vez de "service_orders"
                ReviewType.PROVIDER -> "users"
            }
            
            firestore.collection(collectionName).document(targetId)
                .update("rating", summary.averageRating)
                .await()
        } catch (e: Exception) {
            // Ignore errors - rating update is not critical
            android.util.Log.w("FirestoreReviewsRepo", "Erro ao atualizar rating: ${e.message}", e)
        }
    }
    
    override fun observeUserReviewsAsReviewer(userId: String): Flow<List<Review>> = callbackFlow {
        val listenerRegistration = reviewsCollection
            .whereEqualTo("reviewerId", userId)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val reviews = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ReviewFirestore::class.java)?.copy(id = doc.id)?.toModel()
                } ?: emptyList()
                
                trySend(reviews)
            }
        
        awaitClose { listenerRegistration.remove() }
    }
    
    override fun observeUserReviewsAsTarget(userId: String): Flow<List<Review>> = callbackFlow {
        // Buscar avaliações onde o usuário é prestador/vendedor
        // Isso requer buscar em produtos e serviços onde o usuário é o seller/provider
        // Por enquanto, vamos buscar avaliações do tipo PROVIDER onde targetId = userId
        
        var listenerRegistration: ListenerRegistration? = null
        
        try {
            listenerRegistration = reviewsCollection
                .whereEqualTo("type", "PROVIDER")
                .whereEqualTo("targetId", userId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        // Se houver erro de índice, tentar sem orderBy
                        listenerRegistration?.remove()
                        listenerRegistration = reviewsCollection
                            .whereEqualTo("type", "PROVIDER")
                            .whereEqualTo("targetId", userId)
                            .addSnapshotListener { snapshot2, error2 ->
                                if (error2 != null) {
                                    close(error2)
                                    return@addSnapshotListener
                                }
                                
                                val reviews = snapshot2?.documents?.mapNotNull { doc ->
                                    doc.toObject(ReviewFirestore::class.java)?.copy(id = doc.id)?.toModel()
                                }?.sortedByDescending { it.createdAt } ?: emptyList()
                                
                                trySend(reviews)
                            }
                    } else {
                        val reviews = snapshot?.documents?.mapNotNull { doc ->
                            doc.toObject(ReviewFirestore::class.java)?.copy(id = doc.id)?.toModel()
                        } ?: emptyList()
                        
                        trySend(reviews)
                    }
                }
        } catch (e: Exception) {
            // Se falhar com orderBy, tentar sem orderBy
            listenerRegistration = reviewsCollection
                .whereEqualTo("type", "PROVIDER")
                .whereEqualTo("targetId", userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    
                    val reviews = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(ReviewFirestore::class.java)?.copy(id = doc.id)?.toModel()
                    }?.sortedByDescending { it.createdAt } ?: emptyList()
                    
                    trySend(reviews)
                }
        }
        
        awaitClose { listenerRegistration?.remove() }
    }
    
    /**
     * Observa avaliações de um prestador específico
     */
    fun observeProviderReviews(providerId: String): Flow<List<ReviewFirestore>> = callbackFlow {
        var listenerRegistration: ListenerRegistration? = null
        
        try {
            listenerRegistration = reviewsCollection
                .whereEqualTo("type", "PROVIDER")
                .whereEqualTo("targetId", providerId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        // Se houver erro de índice, tentar sem orderBy
                        listenerRegistration?.remove()
                        listenerRegistration = reviewsCollection
                            .whereEqualTo("type", "PROVIDER")
                            .whereEqualTo("targetId", providerId)
                            .addSnapshotListener { snapshot2, error2 ->
                                if (error2 != null) {
                                    close(error2)
                                    return@addSnapshotListener
                                }
                                
                                val reviews = snapshot2?.documents?.mapNotNull { doc ->
                                    doc.toObject(ReviewFirestore::class.java)?.copy(id = doc.id)
                                }?.sortedByDescending { it.createdAt?.time ?: 0L } ?: emptyList()
                                
                                trySend(reviews)
                            }
                    } else {
                        val reviews = snapshot?.documents?.mapNotNull { doc ->
                            doc.toObject(ReviewFirestore::class.java)?.copy(id = doc.id)
                        } ?: emptyList()
                        
                        trySend(reviews)
                    }
                }
        } catch (e: Exception) {
            // Se falhar com orderBy, tentar sem orderBy
            listenerRegistration = reviewsCollection
                .whereEqualTo("type", "PROVIDER")
                .whereEqualTo("targetId", providerId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    
                    val reviews = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(ReviewFirestore::class.java)?.copy(id = doc.id)
                    }?.sortedByDescending { it.createdAt?.time ?: 0L } ?: emptyList()
                    
                    trySend(reviews)
                }
        }
        
        awaitClose { listenerRegistration?.remove() }
    }
    
    override suspend fun getUserReviewSummaryAsTarget(userId: String): ReviewSummary {
        return try {
            val snapshot = reviewsCollection
                .whereEqualTo("type", "PROVIDER")
                .whereEqualTo("targetId", userId)
                .get()
                .await()
            
            val reviews = snapshot.documents.mapNotNull { doc ->
                doc.toObject(ReviewFirestore::class.java)?.rating
            }
            
            if (reviews.isEmpty()) {
                ReviewSummary(0.0, 0)
            } else {
                val averageRating = reviews.average()
                val ratingDistribution = reviews.groupingBy { it }.eachCount()
                
                ReviewSummary(
                    averageRating = averageRating,
                    totalReviews = reviews.size,
                    ratingDistribution = ratingDistribution
                )
            }
        } catch (e: Exception) {
            ReviewSummary(0.0, 0)
        }
    }
}

