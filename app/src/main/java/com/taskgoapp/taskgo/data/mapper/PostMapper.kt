package com.taskgoapp.taskgo.data.mapper

import com.taskgoapp.taskgo.data.firestore.models.PostFirestore
import com.taskgoapp.taskgo.data.firestore.models.PostLocation as PostLocationFirestore
import com.taskgoapp.taskgo.core.model.Post
import com.taskgoapp.taskgo.core.model.PostLocation
import java.util.Date

object PostMapper {
    
    /**
     * Converte PostFirestore para Post, verificando se o usuário atual curtiu
     */
    fun PostFirestore.toModel(currentUserId: String?): Post {
        return Post(
            id = this.id,
            userId = this.userId,
            userName = this.userName,
            userAvatarUrl = this.userAvatarUrl,
            text = this.text,
            mediaUrls = this.mediaUrls,
            mediaTypes = this.mediaTypes,
            location = this.location?.toModel(),
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
            likesCount = this.likesCount,
            commentsCount = this.commentsCount,
            isLiked = currentUserId?.let { this.likedBy.contains(it) } ?: false,
            tags = this.tags
        )
    }
    
    /**
     * Converte Post para PostFirestore
     */
    fun Post.toFirestore(): PostFirestore {
        return PostFirestore(
            id = this.id,
            userId = this.userId,
            userName = this.userName,
            userAvatarUrl = this.userAvatarUrl,
            text = this.text,
            mediaUrls = this.mediaUrls,
            mediaTypes = this.mediaTypes,
            location = this.location?.toFirestore(),
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
            likesCount = this.likesCount,
            commentsCount = this.commentsCount,
            likedBy = emptyList(), // Não incluir likedBy no toFirestore, será atualizado separadamente
            tags = this.tags
        )
    }
    
    /**
     * Converte PostLocation do Firestore para modelo de domínio
     */
    fun PostLocationFirestore.toModel(): PostLocation {
        return PostLocation(
            city = this.city,
            state = this.state,
            latitude = this.latitude,
            longitude = this.longitude
        )
    }
    
    /**
     * Converte PostLocation do modelo de domínio para Firestore
     */
    fun PostLocation.toFirestore(): PostLocationFirestore {
        return PostLocationFirestore(
            city = this.city,
            state = this.state,
            latitude = this.latitude,
            longitude = this.longitude
        )
    }
}
