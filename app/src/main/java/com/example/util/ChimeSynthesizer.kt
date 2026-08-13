package com.example.util

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object ChimeSynthesizer {
    private const val SAMPLE_RATE = 22050

    @Volatile
    private var currentTrack: AudioTrack? = null

    fun stop() {
        try {
            currentTrack?.let { track ->
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.stop()
                }
                track.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            currentTrack = null
        }
    }

    suspend fun playTibetanSingingBowl() = playSoundById("tibetan_bowl")

    suspend fun playSoundById(soundId: String) = withContext(Dispatchers.Default) {
        try {
            stop()

            val durationSeconds = when (soundId) {
                "soft_gong" -> 4.5f
                "crystal_bowl" -> 4.0f
                "raindrop_chime" -> 3.5f
                "zen_bell" -> 3.5f
                else -> 4.0f // tibetan_bowl
            }

            val numSamples = (SAMPLE_RATE * durationSeconds).toInt()
            val samples = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                val sampleVal = when (soundId) {
                    "crystal_bowl" -> synthesizeCrystalBowl(t)
                    "soft_gong" -> synthesizeSoftGong(t)
                    "zen_bell" -> synthesizeZenBell(t)
                    "raindrop_chime" -> synthesizeRaindropChime(t)
                    else -> synthesizeTibetanBowl(t)
                }

                val scaledSample = (sampleVal * 12000.0).coerceIn(-32767.0, 32767.0).toInt().toShort()
                samples[i] = scaledSample
            }

            val bufferSize = samples.size * 2

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            currentTrack = audioTrack
            audioTrack.write(samples, 0, samples.size)
            audioTrack.play()

            val sleepTimeMs = (durationSeconds * 1000).toLong()
            kotlinx.coroutines.delay(sleepTimeMs)

            if (currentTrack == audioTrack) {
                stop()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun synthesizeTibetanBowl(t: Double): Double {
        val fundamental = 261.63 // C4
        val overtones = listOf(
            Overtone(1.0, 1.0f),
            Overtone(1.51, 0.45f),
            Overtone(2.22, 0.35f),
            Overtone(3.01, 0.22f),
            Overtone(4.15, 0.12f),
            Overtone(5.53, 0.05f)
        )
        var sum = 0.0
        for (ov in overtones) {
            val freq = fundamental * ov.factor
            sum += ov.amplitude * sin(2.0 * PI * freq * t)
        }
        val tremolo = 1.0 + 0.18 * sin(2.0 * PI * 4.5 * t)
        return sum * tremolo * exp(-1.0 * t)
    }

    private fun synthesizeCrystalBowl(t: Double): Double {
        val fundamental = 528.0 // Solfeggio 528Hz
        val overtones = listOf(
            Overtone(1.0, 1.0f),
            Overtone(2.0, 0.25f),
            Overtone(3.0, 0.10f)
        )
        var sum = 0.0
        for (ov in overtones) {
            val freq = fundamental * ov.factor
            sum += ov.amplitude * sin(2.0 * PI * freq * t)
        }
        val shimmer = 1.0 + 0.12 * sin(2.0 * PI * 2.0 * t)
        // Attack envelope
        val attack = (t / 0.1).coerceAtMost(1.0)
        return sum * shimmer * attack * exp(-0.8 * t)
    }

    private fun synthesizeSoftGong(t: Double): Double {
        val fundamental = 130.81 // C3 low deep tone
        val overtones = listOf(
            Overtone(1.0, 1.0f),
            Overtone(1.33, 0.60f),
            Overtone(1.85, 0.40f),
            Overtone(2.40, 0.25f),
            Overtone(3.10, 0.10f)
        )
        var sum = 0.0
        for (ov in overtones) {
            val freq = fundamental * ov.factor
            sum += ov.amplitude * sin(2.0 * PI * freq * t)
        }
        val swell = (t / 0.25).coerceAtMost(1.0)
        val tremolo = 1.0 + 0.2 * sin(2.0 * PI * 1.5 * t)
        return sum * swell * tremolo * exp(-0.6 * t)
    }

    private fun synthesizeZenBell(t: Double): Double {
        val fundamental = 440.0 // A4
        val overtones = listOf(
            Overtone(1.0, 1.0f),
            Overtone(2.76, 0.35f),
            Overtone(5.40, 0.15f)
        )
        var sum = 0.0
        for (ov in overtones) {
            val freq = fundamental * ov.factor
            sum += ov.amplitude * sin(2.0 * PI * freq * t)
        }
        val attack = (t / 0.01).coerceAtMost(1.0)
        return sum * attack * exp(-1.2 * t)
    }

    private fun synthesizeRaindropChime(t: Double): Double {
        // 3 cascading notes: E5 (659.25Hz at t=0s), G5 (783.99Hz at t=0.22s), A5 (880Hz at t=0.44s)
        var sample = 0.0

        if (t >= 0.0) {
            val dt0 = t - 0.0
            sample += sin(2.0 * PI * 659.25 * dt0) * exp(-1.8 * dt0) * 0.8
        }
        if (t >= 0.22) {
            val dt1 = t - 0.22
            sample += sin(2.0 * PI * 783.99 * dt1) * exp(-1.8 * dt1) * 0.9
        }
        if (t >= 0.44) {
            val dt2 = t - 0.44
            sample += sin(2.0 * PI * 880.0 * dt2) * exp(-1.8 * dt2) * 1.0
        }
        return sample
    }

    private data class Overtone(val factor: Double, val amplitude: Float)
}
