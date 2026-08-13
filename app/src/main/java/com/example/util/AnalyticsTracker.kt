package com.example.util

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace

/**
 * Universal Tracker providing Screen Tracking, Event Tracking, Performance Tracing, and Crashlytics Error Reporting.
 * Uses Firebase free-tier services (100% no-cost unlimited Analytics & Crashlytics).
 */
object AnalyticsTracker {
    private const val TAG = "AnalyticsTracker"
    private val activeTraces = HashMap<String, Trace>()

    fun startTrace(traceName: String) {
        Log.d(TAG, "[Performance Trace Start] $traceName")
        try {
            val trace = FirebasePerformance.getInstance().newTrace(traceName)
            trace.start()
            activeTraces[traceName] = trace
        } catch (e: Exception) {
            Log.w(TAG, "Failed to start performance trace: $traceName", e)
        }
    }

    fun stopTrace(traceName: String) {
        Log.d(TAG, "[Performance Trace Stop] $traceName")
        try {
            activeTraces.remove(traceName)?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to stop performance trace: $traceName", e)
        }
    }

    private fun isFirebaseAvailable(context: Context): Boolean {
        return try {
            FirebaseApp.getApps(context).isNotEmpty()
        } catch (e: Exception) {
            Log.d(TAG, "Firebase unavailable: ${e.message}")
            false
        }
    }

    /**
     * Log custom screen view
     */
    fun trackScreen(context: Context, screenName: String, screenClass: String = "ZenWedgeActivity") {
        Log.d(TAG, "[ScreenView] $screenName ($screenClass)")
        if (!isFirebaseAvailable(context)) return

        try {
            val analytics = FirebaseAnalytics.getInstance(context)
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
            }
            analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
            
            // Also log key for Crashlytics context
            FirebaseCrashlytics.getInstance().setCustomKey("current_screen", screenName)
            FirebaseCrashlytics.getInstance().log("Screen navigated to: $screenName")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to track screen view in Firebase", e)
        }
    }

    /**
     * Log custom analytics event
     */
    fun logEvent(context: Context, eventName: String, params: Bundle = Bundle()) {
        Log.d(TAG, "[Analytics Event] $eventName | Params: $params")
        if (!isFirebaseAvailable(context)) return

        try {
            FirebaseAnalytics.getInstance(context).logEvent(eventName, params)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to log event '$eventName' in Firebase", e)
        }
    }

    fun logFocusStart(context: Context, durationSeconds: Int, themeName: String) {
        val bundle = Bundle().apply {
            putInt("duration_seconds", durationSeconds)
            putInt("duration_minutes", durationSeconds / 60)
            putString("theme_name", themeName)
        }
        logEvent(context, "focus_session_start", bundle)
        setCrashlyticsKey("last_focus_duration_mins", durationSeconds / 60)
    }

    fun logFocusPause(context: Context, remainingSeconds: Int) {
        val bundle = Bundle().apply {
            putInt("remaining_seconds", remainingSeconds)
        }
        logEvent(context, "focus_session_pause", bundle)
    }

    fun logFocusReset(context: Context) {
        logEvent(context, "focus_session_reset")
    }

    fun logFocusComplete(context: Context, durationSeconds: Int, themeName: String) {
        val bundle = Bundle().apply {
            putInt("duration_seconds", durationSeconds)
            putString("theme_name", themeName)
        }
        logEvent(context, "focus_session_complete", bundle)
        logCrashlytics("Completed focus session of ${durationSeconds / 60} minutes")
    }

    fun logThemeChanged(context: Context, themeName: String) {
        val bundle = Bundle().apply {
            putString("theme_name", themeName)
        }
        logEvent(context, "theme_changed", bundle)
        setCrashlyticsKey("active_theme", themeName)
    }

    fun logSettingChanged(context: Context, settingName: String, enabled: Boolean) {
        val bundle = Bundle().apply {
            putString("setting_name", settingName)
            putBoolean("enabled", enabled)
        }
        logEvent(context, "setting_changed", bundle)
    }

    fun logSettingChanged(context: Context, settingName: String, value: String) {
        val bundle = Bundle().apply {
            putString("setting_name", settingName)
            putString("value", value)
        }
        logEvent(context, "setting_changed", bundle)
    }

    /**
     * Crashlytics Custom Logging & Exception Recording
     */
    fun logCrashlytics(message: String) {
        Log.d(TAG, "[Crashlytics Log] $message")
        try {
            FirebaseCrashlytics.getInstance().log(message)
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    fun recordException(throwable: Throwable) {
        Log.e(TAG, "[Crashlytics Exception]", throwable)
        try {
            FirebaseCrashlytics.getInstance().recordException(throwable)
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    fun setCrashlyticsKey(key: String, value: Any) {
        try {
            when (value) {
                is String -> FirebaseCrashlytics.getInstance().setCustomKey(key, value)
                is Boolean -> FirebaseCrashlytics.getInstance().setCustomKey(key, value)
                is Int -> FirebaseCrashlytics.getInstance().setCustomKey(key, value)
                is Long -> FirebaseCrashlytics.getInstance().setCustomKey(key, value)
                is Float -> FirebaseCrashlytics.getInstance().setCustomKey(key, value)
                is Double -> FirebaseCrashlytics.getInstance().setCustomKey(key, value)
            }
        } catch (e: Exception) {
            // Safe fallback
        }
    }
}
