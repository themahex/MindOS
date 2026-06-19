package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "memories")
data class Memory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val processedText: String = "",
    val category: String = "General",
    val timestamp: Long = System.currentTimeMillis(),
    val isTask: Boolean = false,
    val isCompleted: Boolean = false,
    val dueDateStr: String? = null,
    val dueDateMs: Long? = null,
    val audioDurationSeconds: Int = 0,
    val isStarred: Boolean = false
) : Serializable
