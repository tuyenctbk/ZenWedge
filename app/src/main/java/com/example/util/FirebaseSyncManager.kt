package com.example.util

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class FocusSessionLog(
    val durationSeconds: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val themeName: String,
    val completed: Boolean = true
)

object FirebaseSyncManager {
    private const val TAG = "FirebaseSyncManager"
    private const val COLLECTION_FOCUS_SESSIONS = "focus_sessions"

    private fun isFirebaseAvailable(context: Context): Boolean {
        return try {
            FirebaseApp.getApps(context).isNotEmpty()
        } catch (e: Exception) {
            Log.d(TAG, "Firebase not initialized or configured: ${e.message}")
            false
        }
    }

    suspend fun logSession(context: Context, durationSeconds: Int, themeName: String) {
        withContext(Dispatchers.IO) {
            if (!isFirebaseAvailable(context)) {
                Log.d(TAG, "Firebase unavailable, session kept in local memory")
                return@withContext
            }

            try {
                val db = FirebaseFirestore.getInstance()
                val sessionData = hashMapOf(
                    "durationSeconds" to durationSeconds,
                    "timestamp" to System.currentTimeMillis(),
                    "themeName" to themeName,
                    "completed" to true,
                    "device" to "Android"
                )

                db.collection(COLLECTION_FOCUS_SESSIONS)
                    .add(sessionData)
                    .addOnSuccessListener { documentReference ->
                        Log.d(TAG, "Focus session synced to Firebase Firestore with ID: ${documentReference.id}")
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Error adding focus session to Firestore", e)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to communicate with Firebase Firestore: ${e.message}")
            }
        }
    }
}
