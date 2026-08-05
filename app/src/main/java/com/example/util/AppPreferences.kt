package com.example.util

import android.content.Context
import java.util.Calendar

object AppPreferences {
    private const val PREFS_NAME = "zenwedge_prefs"
    private const val KEY_SESSION_COUNT = "session_count"
    private const val KEY_TOTAL_MINUTES = "total_focus_minutes"
    private const val KEY_LAST_SESSION_MINS = "last_session_minutes"
    private const val KEY_RECENT_SESSIONS = "recent_sessions_history" // comma separated list e.g. "25m@Afternoon,15m@Morning"
    private const val KEY_SELECTED_SOUND = "selected_chime_sound"

    fun incrementSessionCount(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val count = prefs.getInt(KEY_SESSION_COUNT, 0) + 1
        prefs.edit().putInt(KEY_SESSION_COUNT, count).apply()
        return count
    }

    fun getSessionCount(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_SESSION_COUNT, 0)
    }

    fun recordCompletedSession(context: Context, durationMinutes: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val count = prefs.getInt(KEY_SESSION_COUNT, 0) + 1
        val totalMins = prefs.getInt(KEY_TOTAL_MINUTES, 0) + durationMinutes
        val tod = getTimeOfDay()

        val recentHistory = prefs.getString(KEY_RECENT_SESSIONS, "") ?: ""
        val newEntry = "${durationMinutes}m@$tod"
        val updatedHistory = if (recentHistory.isEmpty()) newEntry else "$newEntry, $recentHistory".split(", ").take(5).joinToString(", ")

        prefs.edit()
            .putInt(KEY_SESSION_COUNT, count)
            .putInt(KEY_TOTAL_MINUTES, totalMins)
            .putInt(KEY_LAST_SESSION_MINS, durationMinutes)
            .putString(KEY_RECENT_SESSIONS, updatedHistory)
            .apply()
    }

    fun getTotalFocusMinutes(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_TOTAL_MINUTES, 0)
    }

    fun getLastSessionMinutes(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_LAST_SESSION_MINS, 25)
    }

    fun getRecentSessionsSummary(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val history = prefs.getString(KEY_RECENT_SESSIONS, "")
        return if (!history.isNullOrEmpty()) history else "25m@${getTimeOfDay()}"
    }

    fun getSelectedSound(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SELECTED_SOUND, "tibetan_bowl") ?: "tibetan_bowl"
    }

    fun setSelectedSound(context: Context, soundId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SELECTED_SOUND, soundId).apply()
    }

    fun getTimeOfDay(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "Morning"
            in 12..16 -> "Afternoon"
            in 17..21 -> "Evening"
            else -> "Late Night"
        }
    }

    fun hasRatedApp(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("has_rated", false)
    }

    fun setHasRatedApp(context: Context, rated: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("has_rated", rated).apply()
    }
    
    fun hasSharedApp(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("has_shared", false)
    }

    fun setHasSharedApp(context: Context, shared: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("has_shared", shared).apply()
    }
}
