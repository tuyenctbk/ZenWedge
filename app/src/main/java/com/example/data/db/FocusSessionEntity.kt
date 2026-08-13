package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val durationSeconds: Int,
    val durationMinutes: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val timeOfDay: String,
    val themeName: String,
    val soundId: String
)
