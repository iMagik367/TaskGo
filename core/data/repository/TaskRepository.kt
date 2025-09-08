package com.example.taskgoapp.core.data.repository

import com.example.taskgoapp.core.data.local.dao.TaskDao
import com.example.taskgoapp.core.data.local.entity.TaskEntity
import com.example.taskgoapp.core.data.mapper.TaskMapper.toTask
import com.example.taskgoapp.core.data.mapper.TaskMapper.toTaskEntity
import com.example.taskgoapp.core.data.mapper.TaskMapper.toTaskList
import com.example.taskgoapp.core.model.Priority
import com.example.taskgoapp.core.model.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

class TaskRepository(
    private val taskDao: TaskDao
) {
    
    // Streams com Flow
    fun getAllTasks(): Flow<List<Task>> {
        return taskDao.getAllTasks().map { it.toTaskList() }
    }
    
    fun getPendingTasks(): Flow<List<Task>> {
        return taskDao.getPendingTasks().map { it.toTaskList() }
    }
    
    fun getCompletedTasks(): Flow<List<Task>> {
        return taskDao.getCompletedTasks().map { it.toTaskList() }
    }
    
    fun searchTasks(query: String): Flow<List<Task>> {
        return taskDao.searchTasks(query).map { it.toTaskList() }
    }
    
    fun getTasksByPriority(priority: Priority): Flow<List<Task>> {
        return taskDao.getTasksByPriority(priority).map { it.toTaskList() }
    }
    
    fun getTasksByDateRange(startDate: Instant, endDate: Instant): Flow<List<Task>> {
        return taskDao.getTasksByDateRange(startDate, endDate).map { it.toTaskList() }
    }
    
    fun getPendingTasksCount(): Flow<Int> {
        return taskDao.getPendingTasksCount()
    }
    
    fun getCompletedTasksCount(): Flow<Int> {
        return taskDao.getCompletedTasksCount()
    }
    
    // Operações CRUD
    suspend fun getTaskById(taskId: Long): Task? {
        return taskDao.getTaskById(taskId)?.toTask()
    }
    
    suspend fun insertTask(task: Task): Long {
        val taskEntity = task.toTaskEntity()
        return taskDao.insertTask(taskEntity)
    }
    
    suspend fun updateTask(task: Task) {
        val taskEntity = task.toTaskEntity()
        taskDao.updateTask(taskEntity)
    }
    
    suspend fun toggleTaskDone(taskId: Long, done: Boolean) {
        taskDao.updateTaskDone(taskId, done)
    }
    
    suspend fun deleteTask(task: Task) {
        val taskEntity = task.toTaskEntity()
        taskDao.deleteTask(taskEntity)
    }
    
    suspend fun deleteTaskById(taskId: Long) {
        taskDao.deleteTaskById(taskId)
    }
    
    suspend fun deleteCompletedTasks() {
        taskDao.deleteCompletedTasks()
    }
    
    // Operações de conveniência
    suspend fun createTask(
        title: String,
        description: String = "",
        dueAt: Instant? = null,
        priority: Priority = Priority.MEDIUM,
        tags: List<String> = emptyList()
    ): Long {
        val task = Task(
            title = title,
            description = description,
            dueAt = dueAt?.toEpochMilli(),
            priority = priority,
            tags = tags
        )
        return insertTask(task)
    }
    
    suspend fun markTaskAsDone(taskId: Long) {
        toggleTaskDone(taskId, true)
    }
    
    suspend fun markTaskAsPending(taskId: Long) {
        toggleTaskDone(taskId, false)
    }
    
    suspend fun updateTaskPriority(taskId: Long, priority: Priority) {
        val task = getTaskById(taskId)
        task?.let {
            val updatedTask = it.copy(priority = priority)
            updateTask(updatedTask)
        }
    }
    
    suspend fun updateTaskDueDate(taskId: Long, dueAt: Instant?) {
        val task = getTaskById(taskId)
        task?.let {
            val updatedTask = it.copy(dueAt = dueAt?.toEpochMilli())
            updateTask(updatedTask)
        }
    }
    
    suspend fun addTagToTask(taskId: Long, tag: String) {
        val task = getTaskById(taskId)
        task?.let {
            val updatedTags = it.tags.toMutableList().apply { add(tag) }
            val updatedTask = it.copy(tags = updatedTags)
            updateTask(updatedTask)
        }
    }
    
    suspend fun removeTagFromTask(taskId: Long, tag: String) {
        val task = getTaskById(taskId)
        task?.let {
            val updatedTags = it.tags.filter { existingTag -> existingTag != tag }
            val updatedTask = it.copy(tags = updatedTags)
            updateTask(updatedTask)
        }
    }
}
