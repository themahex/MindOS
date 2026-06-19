package com.example.data.repository

import com.example.data.local.MemoryDao
import com.example.data.model.Memory
import kotlinx.coroutines.flow.Flow

class MemoryRepository(private val memoryDao: MemoryDao) {
    val allMemories: Flow<List<Memory>> = memoryDao.getAllMemoriesFlow()
    val allTasks: Flow<List<Memory>> = memoryDao.getAllTasksFlow()

    fun searchMemories(query: String): Flow<List<Memory>> {
        return memoryDao.searchMemoriesFlow(query)
    }

    fun getTasksFiltered(completed: Boolean): Flow<List<Memory>> {
        return memoryDao.getTasksFilteredFlow(completed)
    }

    suspend fun getMemoryById(id: Long): Memory? {
        return memoryDao.getMemoryById(id)
    }

    suspend fun insertMemory(memory: Memory): Long {
        return memoryDao.insertMemory(memory)
    }

    suspend fun updateMemory(memory: Memory) {
        memoryDao.updateMemory(memory)
    }

    suspend fun deleteMemory(memory: Memory) {
        memoryDao.deleteMemory(memory)
    }

    suspend fun clearAll() {
        memoryDao.clearAll()
    }
}
