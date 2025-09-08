package com.example.taskgoapp.core.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.TypeConverters
import com.example.taskgoapp.core.data.local.converter.InstantConverter
import com.example.taskgoapp.core.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
@TypeConverters(InstantConverter::class)
interface TaskDao {
    
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>
    
    @Query("SELECT * FROM tasks WHERE done = 0 ORDER BY dueAt ASC, priority DESC")
    fun getPendingTasks(): Flow<List<TaskEntity>>
    
    @Query("SELECT * FROM tasks WHERE done = 1 ORDER BY updatedAt DESC")
    fun getCompletedTasks(): Flow<List<TaskEntity>>
    
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Long): TaskEntity?
    
    @Query("SELECT * FROM tasks WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    fun searchTasks(query: String): Flow<List<TaskEntity>>
    
    @Query("SELECT * FROM tasks WHERE priority = :priority ORDER BY dueAt ASC")
    fun getTasksByPriority(priority: com.example.taskgoapp.core.model.Priority): Flow<List<TaskEntity>>
    
    @Query("SELECT * FROM tasks WHERE dueAt BETWEEN :startDate AND :endDate ORDER BY dueAt ASC")
    fun getTasksByDateRange(startDate: java.time.Instant, endDate: java.time.Instant): Flow<List<TaskEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long
    
    @Update
    suspend fun updateTask(task: TaskEntity)
    
    @Query("UPDATE tasks SET done = :done, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun updateTaskDone(taskId: Long, done: Boolean, updatedAt: java.time.Instant = java.time.Instant.now())
    
    @Delete
    suspend fun deleteTask(task: TaskEntity)
    
    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Long)
    
    @Query("DELETE FROM tasks WHERE done = 1")
    suspend fun deleteCompletedTasks()
    
    @Query("SELECT COUNT(*) FROM tasks WHERE done = 0")
    fun getPendingTasksCount(): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM tasks WHERE done = 1")
    fun getCompletedTasksCount(): Flow<Int>
}
