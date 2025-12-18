package com.taskgoapp.taskgo.data.mapper

import com.taskgoapp.taskgo.core.model.Review
import com.taskgoapp.taskgo.core.model.ReviewType
import com.taskgoapp.taskgo.data.firestore.models.ReviewFirestore

object ReviewMapper {
    
    fun ReviewFirestore.toModel(): Review {
        return Review(
            id = this.id,
            type = when (this.type) {
                "PRODUCT" -> ReviewType.PRODUCT
                "SERVICE" -> ReviewType.SERVICE
                "PROVIDER" -> ReviewType.PROVIDER
                else -> ReviewType.PRODUCT
            },
            targetId = this.targetId,
            reviewerId = this.reviewerId,
            reviewerName = this.reviewerName,
            reviewerAvatarUri = this.reviewerAvatarUri,
            rating = this.rating,
            comment = this.comment,
            photoUrls = this.photoUrls,
            createdAt = this.createdAt?.time ?: System.currentTimeMillis(),
            updatedAt = this.updatedAt?.time,
            orderId = this.orderId,
            helpfulCount = this.helpfulCount,
            verifiedPurchase = this.verifiedPurchase
        )
    }
    
    fun Review.toFirestore(): ReviewFirestore {
        return ReviewFirestore(
            id = this.id,
            type = when (this.type) {
                ReviewType.PRODUCT -> "PRODUCT"
                ReviewType.SERVICE -> "SERVICE"
                ReviewType.PROVIDER -> "PROVIDER"
            },
            targetId = this.targetId,
            reviewerId = this.reviewerId,
            reviewerName = this.reviewerName,
            reviewerAvatarUri = this.reviewerAvatarUri,
            rating = this.rating,
            comment = this.comment,
            photoUrls = this.photoUrls,
            createdAt = java.util.Date(this.createdAt),
            updatedAt = this.updatedAt?.let { java.util.Date(it) },
            orderId = this.orderId,
            helpfulCount = this.helpfulCount,
            verifiedPurchase = this.verifiedPurchase
        )
    }
}

