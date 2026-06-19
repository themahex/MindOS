package com.example.data.local

import androidx.room.*
import com.example.data.model.Memory
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories ORDER BY timestamp DESC")
    fun getAllMemoriesFlow(): Flow<List<Memory>>

    @Query("SELECT * FROM memories WHERE id = :id")
    suspend fun getMemoryById(id: Long): Memory?

    @Query("SELECT * FROM memories WHERE text LIKE '%' || :query || '%' OR processedText LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%'")
    fun searchMemoriesFlow(query: String): Flow<List<Memory>>

    @Query("SELECT * FROM memories WHERE isTask = 1 ORDER BY isCompleted ASC, timestamp DESC")
    fun getAllTasksFlow(): Flow<List<Memory>>

    @Query("SELECT * FROM memories WHERE isTask = 1 AND isCompleted = :completed ORDER BY timestamp DESC")
    fun getTasksFilteredFlow(completed: Boolean): Flow<List<Memory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: Memory): Long

    @Update
    suspend fun updateMemory(memory: Memory)

    @Delete
    suspend fun deleteMemory(memory: Memory)

    @Query("DELETE FROM memories")
    suspend fun clearAll()
}
