package com.example.data.db

import kotlinx.coroutines.flow.Flow

class FocusSessionRepository(private val dao: FocusSessionDao) {
    val allSessions: Flow<List<FocusSessionEntity>> = dao.getAllSessions()

    suspend fun insert(session: FocusSessionEntity) = dao.insertSession(session)

    fun getSessionsSince(startTime: Long): Flow<List<FocusSessionEntity>> = dao.getSessionsSince(startTime)

    suspend fun clearAll() = dao.clearAll()
}
