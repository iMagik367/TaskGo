package com.taskgoapp.taskgo.domain.repository

import com.taskgoapp.taskgo.core.model.Review
import com.taskgoapp.taskgo.core.model.ReviewSummary
import com.taskgoapp.taskgo.core.model.ReviewType
import com.taskgoapp.taskgo.core.model.Result
import kotlinx.coroutines.flow.Flow

interface ReviewsRepository {
    fun observeReviews(targetId: String, type: ReviewType): Flow<List<Review>>
    suspend fun getReview(reviewId: String): Review?
    suspend fun createReview(review: Review): Result<String>
    suspend fun updateReview(reviewId: String, rating: Int?, comment: String?, photoUrls: List<String>?): Result<Unit>
    suspend fun deleteReview(reviewId: String): Result<Unit>
    suspend fun getReviewSummary(targetId: String, type: ReviewType): ReviewSummary
    suspend fun markReviewAsHelpful(reviewId: String): Result<Unit>
    suspend fun canUserReview(targetId: String, type: ReviewType, userId: String): Boolean
    
    // Métodos para avaliações do usuário
    fun observeUserReviewsAsReviewer(userId: String): Flow<List<Review>> // Avaliações que o usuário fez
    fun observeUserReviewsAsTarget(userId: String): Flow<List<Review>> // Avaliações sobre o usuário (quando é prestador/vendedor)
    suspend fun getUserReviewSummaryAsTarget(userId: String): ReviewSummary // Resumo das avaliações sobre o usuário
}

