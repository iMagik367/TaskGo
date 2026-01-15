package com.taskgoapp.taskgo.core.model

import java.util.Date

/**
 * Modelo de usuário bloqueado
 */
data class BlockedUser(
    val id: String,
    val blockerId: String, // ID do usuário que bloqueou
    val blockedId: String, // ID do usuário bloqueado
    val blockedName: String?,
    val blockedAvatarUrl: String?,
    val createdAt: Date
)
