package com.example.taskgoapp.core.data.mapper

import com.example.taskgoapp.core.data.local.entity.TaskEntity
import com.example.taskgoapp.core.model.Task
import com.example.taskgoapp.core.model.Priority

object TaskMapper {
    
    fun TaskEntity.toTask(): Task {
        return Task(
            id = id,
            title = title,
            description = description,
            dueAt = dueAt?.toEpochMilli(),
            priority = priority,
            done = done,
            tags = tags
        )
    }
    
    fun Task.toTaskEntity(): TaskEntity {
        return TaskEntity(
            id = id,
            title = title,
            description = description,
            dueAt = dueAt?.let { java.time.Instant.ofEpochMilli(it) },
            priority = priority,
            done = done,
            tags = tags,
            createdAt = if (id == 0L) java.time.Instant.now() else java.time.Instant.now(),
            updatedAt = java.time.Instant.now()
        )
    }
    
    fun List<TaskEntity>.toTaskList(): List<Task> {
        return map { it.toTask() }
    }
    
    fun List<Task>.toTaskEntityList(): List<TaskEntity> {
        return map { it.toTaskEntity() }
    }
}
