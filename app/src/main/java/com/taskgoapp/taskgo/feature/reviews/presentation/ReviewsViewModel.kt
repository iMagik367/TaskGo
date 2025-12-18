package com.taskgoapp.taskgo.feature.reviews.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.taskgoapp.taskgo.core.model.Review
import com.taskgoapp.taskgo.core.model.ReviewType
import com.taskgoapp.taskgo.domain.repository.ReviewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReviewsUiState(
    val isLoading: Boolean = false,
    val reviews: List<Review> = emptyList(),
    val summary: com.taskgoapp.taskgo.core.model.ReviewSummary = com.taskgoapp.taskgo.core.model.ReviewSummary(0.0, 0),
    val canReview: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ReviewsViewModel @Inject constructor(
    private val reviewsRepository: ReviewsRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ReviewsUiState())
    val uiState: StateFlow<ReviewsUiState> = _uiState.asStateFlow()
    
    fun loadReviews(targetId: String, type: ReviewType) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val userId = firebaseAuth.currentUser?.uid
                val canReview = userId != null && reviewsRepository.canUserReview(targetId, type, userId)
                
                reviewsRepository.observeReviews(targetId, type).collect { reviews ->
                    val summary = reviewsRepository.getReviewSummary(targetId, type)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        reviews = reviews,
                        summary = summary,
                        canReview = canReview
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Erro ao carregar avaliações"
                )
            }
        }
    }
    
    fun markAsHelpful(reviewId: String) {
        viewModelScope.launch {
            reviewsRepository.markReviewAsHelpful(reviewId)
        }
    }
}

data class CreateReviewUiState(
    val isLoading: Boolean = false,
    val reviewCreated: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CreateReviewViewModel @Inject constructor(
    private val reviewsRepository: ReviewsRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CreateReviewUiState())
    val uiState: StateFlow<CreateReviewUiState> = _uiState.asStateFlow()
    
    private var currentTargetId: String = ""
    private var currentType: ReviewType = ReviewType.PRODUCT
    private var currentOrderId: String? = null
    
    fun initialize(targetId: String, type: ReviewType, orderId: String?) {
        currentTargetId = targetId
        currentType = type
        currentOrderId = orderId
    }
    
    fun createReview(rating: Int, comment: String?) {
        viewModelScope.launch {
            val user = firebaseAuth.currentUser
            if (user == null) {
                _uiState.value = _uiState.value.copy(
                    error = "Você precisa estar logado para avaliar"
                )
                return@launch
            }
            
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val review = Review(
                id = "",
                type = currentType,
                targetId = currentTargetId,
                reviewerId = user.uid,
                reviewerName = user.displayName ?: "Usuário",
                reviewerAvatarUri = user.photoUrl?.toString(),
                rating = rating,
                comment = comment,
                photoUrls = emptyList(),
                createdAt = System.currentTimeMillis(),
                updatedAt = null,
                orderId = currentOrderId,
                helpfulCount = 0,
                verifiedPurchase = currentOrderId != null
            )
            
            when (val result = reviewsRepository.createReview(review)) {
                is com.taskgoapp.taskgo.core.model.Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        reviewCreated = true
                    )
                }
                is com.taskgoapp.taskgo.core.model.Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.exception.message ?: "Erro ao criar avaliação"
                    )
                }
                else -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Erro desconhecido"
                    )
                }
            }
        }
    }
}

data class UserReviewsUiState(
    val isLoading: Boolean = false,
    val reviewsAsReviewer: List<Review> = emptyList(), // Avaliações que o usuário fez
    val reviewsAsTarget: List<Review> = emptyList(), // Avaliações sobre o usuário
    val summaryAsTarget: com.taskgoapp.taskgo.core.model.ReviewSummary = com.taskgoapp.taskgo.core.model.ReviewSummary(0.0, 0),
    val error: String? = null
)

@HiltViewModel
class UserReviewsViewModel @Inject constructor(
    private val reviewsRepository: ReviewsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(UserReviewsUiState())
    val uiState: StateFlow<UserReviewsUiState> = _uiState.asStateFlow()
    
    fun loadUserReviews(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Usar combine para combinar os dois flows
                kotlinx.coroutines.flow.combine(
                    reviewsRepository.observeUserReviewsAsReviewer(userId),
                    reviewsRepository.observeUserReviewsAsTarget(userId)
                ) { reviewsAsReviewer, reviewsAsTarget ->
                    // Calcular summary a partir das avaliações recebidas
                    val summary = if (reviewsAsTarget.isNotEmpty()) {
                        val ratings = reviewsAsTarget.map { it.rating }
                        val averageRating = ratings.average()
                        val ratingDistribution = ratings.groupingBy { it }.eachCount()
                        com.taskgoapp.taskgo.core.model.ReviewSummary(
                            averageRating = averageRating,
                            totalReviews = reviewsAsTarget.size,
                            ratingDistribution = ratingDistribution
                        )
                    } else {
                        com.taskgoapp.taskgo.core.model.ReviewSummary(0.0, 0)
                    }
                    
                    UserReviewsUiState(
                        isLoading = false,
                        reviewsAsReviewer = reviewsAsReviewer,
                        reviewsAsTarget = reviewsAsTarget,
                        summaryAsTarget = summary
                    )
                }.collect { newState ->
                    _uiState.value = newState
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Erro ao carregar avaliações"
                )
            }
        }
    }
}

