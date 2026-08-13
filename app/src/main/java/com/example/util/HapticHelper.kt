package com.example.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object HapticHelper {
    
    // Light tick for slider drag or button interaction
    fun triggerTick(context: Context, mode: String = "standard") {
        if (mode == "off") return
        val amp = when (mode) {
            "gentle" -> 25
            "strong" -> 100
            else -> 50
        }
        vibrate(context, 12L, amp)
    }

    // Medium bump for interval milestones (e.g. 5 min haptic)
    fun triggerIntervalBump(context: Context, mode: String = "standard") {
        if (mode == "off") return
        val amp = when (mode) {
            "gentle" -> 60
            "strong" -> 200
            else -> 120
        }
        vibrate(context, 40L, amp)
    }

    // Rich pulsating vibration for completion depending on pattern preference
    fun triggerCompletionVibe(context: Context, mode: String = "standard") {
        if (mode == "off") return
        when (mode) {
            "gentle" -> {
                vibratePattern(
                    context,
                    timings = longArrayOf(0, 100, 100, 150),
                    amplitudes = intArrayOf(0, 60, 0, 80)
                )
            }
            "strong" -> {
                vibratePattern(
                    context,
                    timings = longArrayOf(0, 200, 100, 250, 100, 400),
                    amplitudes = intArrayOf(0, 180, 0, 220, 0, 255)
                )
            }
            else -> {
                // Standard
                vibratePattern(
                    context,
                    timings = longArrayOf(0, 150, 100, 150, 100, 300),
                    amplitudes = intArrayOf(0, 100, 0, 150, 0, 200)
                )
            }
        }
    }

    private fun vibrate(context: Context, durationMs: Long, amplitude: Int) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(durationMs)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun vibratePattern(context: Context, timings: LongArray, amplitudes: IntArray) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(timings, -1)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
