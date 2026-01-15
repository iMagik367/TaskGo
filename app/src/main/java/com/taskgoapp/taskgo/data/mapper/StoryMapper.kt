package com.taskgoapp.taskgo.data.mapper

import com.google.firebase.Timestamp
import com.taskgoapp.taskgo.core.model.Story
import com.taskgoapp.taskgo.core.model.StoryLocation
import com.taskgoapp.taskgo.data.firestore.models.StoryFirestore
import com.taskgoapp.taskgo.data.firestore.models.StoryLocationFirestore
import java.util.Date

/**
 * Mapper para converter entre Story (modelo de domínio) e StoryFirestore
 */

/**
 * Mapper para converter entre Story (modelo de domínio) e StoryFirestore
 */
object StoryMapper {
    
    /**
     * Converte StoryFirestore para Story (modelo de domínio)
     */
    fun StoryFirestore.toModel(isViewed: Boolean = false): Story {
        return Story(
            id = this.id,
            userId = this.userId,
            userName = this.userName,
            userAvatarUrl = this.userAvatarUrl,
            mediaUrl = this.mediaUrl,
            mediaType = this.mediaType,
            thumbnailUrl = this.thumbnailUrl,
            caption = this.caption,
            createdAt = this.createdAt?.toDate() ?: Date(),
            expiresAt = this.expiresAt?.toDate() ?: Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000),
            viewsCount = this.viewsCount,
            isViewed = isViewed,
            location = this.location?.toModel()
        )
    }
    
    /**
     * Converte Story (modelo de domínio) para StoryFirestore
     */
    fun Story.toFirestore(): StoryFirestore {
        return StoryFirestore(
            id = this.id,
            userId = this.userId,
            userName = this.userName,
            userAvatarUrl = this.userAvatarUrl,
            mediaUrl = this.mediaUrl,
            mediaType = this.mediaType,
            thumbnailUrl = this.thumbnailUrl,
            caption = this.caption,
            createdAt = this.createdAt?.let { Timestamp(it) },
            expiresAt = this.expiresAt?.let { Timestamp(it) },
            viewsCount = this.viewsCount,
            location = this.location?.toFirestore()
        )
    }
    
    /**
     * Converte StoryLocationFirestore para StoryLocation
     */
    fun StoryLocationFirestore.toModel(): StoryLocation {
        return StoryLocation(
            city = this.city,
            state = this.state,
            latitude = this.latitude,
            longitude = this.longitude
        )
    }
    
    /**
     * Converte StoryLocation para StoryLocationFirestore
     */
    fun StoryLocation.toFirestore(): StoryLocationFirestore {
        return StoryLocationFirestore(
            city = this.city,
            state = this.state,
            latitude = this.latitude,
            longitude = this.longitude
        )
    }
}

