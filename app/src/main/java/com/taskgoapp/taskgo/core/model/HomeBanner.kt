package com.taskgoapp.taskgo.core.model

data class HomeBanner(
    val id: String,
    val title: String,
    val subtitle: String,
    val actionLabel: String,
    val imageUrl: String? = null,
    val audience: Audience = Audience.TODOS,
    val actionRoute: String? = null,
    val priority: Int = 0
) {
    enum class Audience {
        CLIENTE,
        PRESTADOR,
        TODOS
    }
}

