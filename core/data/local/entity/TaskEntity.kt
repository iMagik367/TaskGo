package com.example.taskgoapp.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.taskgoapp.core.data.local.converter.InstantConverter
import com.example.taskgoapp.core.data.local.converter.StringListConverter
import com.example.taskgoapp.core.model.Priority

@Entity(tableName = "tasks")
@TypeConverters(InstantConverter::class, StringListConverter::class)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val dueAt: java.time.Instant? = null,
    val priority: Priority = Priority.MEDIUM,
    val done: Boolean = false,
    val tags: List<String> = emptyList(),
    val createdAt: java.time.Instant = java.time.Instant.now(),
    val updatedAt: java.time.Instant = java.time.Instant.now()
)
