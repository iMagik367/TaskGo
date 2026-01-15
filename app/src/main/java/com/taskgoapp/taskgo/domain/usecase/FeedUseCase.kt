package com.taskgoapp.taskgo.domain.usecase

import com.taskgoapp.taskgo.core.model.Post
import com.taskgoapp.taskgo.core.model.PostLocation
import com.taskgoapp.taskgo.core.model.Result
import com.taskgoapp.taskgo.domain.repository.FeedRepository
import kotlinx.coroutines.flow.Flow
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
        return feedRepository.createPost(text, mediaUrls, mediaTypes, location)
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
        return feedRepository.observeFeedPosts(userLatitude, userLongitude, radiusKm)
    }
}

class GetUserPostsUseCase @Inject constructor(
    private val feedRepository: FeedRepository
) {
    operator fun invoke(userId: String): Flow<List<Post>> {
        return feedRepository.observeUserPosts(userId)
    }
}

class LikePostUseCase @Inject constructor(
    private val feedRepository: FeedRepository
) {
    suspend operator fun invoke(postId: String, userId: String): Result<Unit> {
        return feedRepository.likePost(postId, userId)
    }
}

class UnlikePostUseCase @Inject constructor(
    private val feedRepository: FeedRepository
) {
    suspend operator fun invoke(postId: String, userId: String): Result<Unit> {
        return feedRepository.unlikePost(postId, userId)
    }
}

class DeletePostUseCase @Inject constructor(
    private val feedRepository: FeedRepository
) {
    suspend operator fun invoke(postId: String): Result<Unit> {
        return feedRepository.deletePost(postId)
    }
}
