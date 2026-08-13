package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {
    @Query("SELECT * FROM focus_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<FocusSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSessionEntity)

    @Query("SELECT * FROM focus_sessions WHERE timestamp >= :startTime ORDER BY timestamp ASC")
    fun getSessionsSince(startTime: Long): Flow<List<FocusSessionEntity>>

    @Query("DELETE FROM focus_sessions")
    suspend fun clearAll()
}
