package com.example.taskgoapp.core.model

import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class Task(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val dueAt: Long? = null, // Usando Long em vez de Instant para evitar problemas de serialização
    val priority: Priority = Priority.MEDIUM,
    val done: Boolean = false,
    val tags: List<String> = emptyList()
)

enum class Priority {
    LOW, MEDIUM, HIGH
}
