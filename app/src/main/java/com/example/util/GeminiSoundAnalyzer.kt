package com.example.util

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class SoundRecommendation(
    val recommendedSoundId: String,
    val soundName: String,
    val patternAnalysis: String,
    val rationale: String
)

object GeminiSoundAnalyzer {
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun analyzeFocusPatternsAndSuggestSound(
        totalSessions: Int,
        totalMinutes: Int,
        averageDurationMinutes: Int,
        lastSessionMinutes: Int,
        timeOfDay: String,
        recentSessionsSummary: String
    ): Result<SoundRecommendation> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            val fallback = getFallbackRecommendation(timeOfDay, lastSessionMinutes, totalSessions)
            return@withContext Result.success(fallback)
        }

        val prompt = """
            You are a mindful focus coach and acoustic designer. Analyze the user's focus patterns:
            - Total completed focus sessions: $totalSessions
            - Total focus time: $totalMinutes min
            - Average session duration: $averageDurationMinutes min
            - Last session duration: $lastSessionMinutes min
            - Time of day: $timeOfDay
            - Recent focus activity: $recentSessionsSummary

            Available ambient chime presets:
            1. "tibetan_bowl" (Tibetan Singing Bowl): Deep grounding C4 metallic bowl, best for sustained meditation and long focus blocks.
            2. "crystal_bowl" (528Hz Crystal Bowl): High pure Solfeggio clarity tone, best for quick sprints and sharp mental focus.
            3. "soft_gong" (Warm Soft Gong): Low-frequency warm swell, best for late evening, long sessions, or fatigue recovery.
            4. "zen_bell" (Zen Temple Bell): Crisp single bell chime, best for mid-day work and smooth productivity transitions.
            5. "raindrop_chime" (Gentle Rain Chime): Cascading ambient tones, best for creative sessions or gentle morning starts.

            Instructions:
            1. Analyze the user's focus session history and time of day.
            2. Pick the optimal sound ID ("tibetan_bowl", "crystal_bowl", "soft_gong", "zen_bell", or "raindrop_chime").
            3. Provide a concise 1-sentence analysis of their focus pattern.
            4. Provide a 1-sentence reasoning for why this chime suits their current focus state.

            Return JSON ONLY with this exact schema:
            {
              "recommendedSoundId": "soft_gong",
              "soundName": "Warm Soft Gong",
              "patternAnalysis": "You frequently engage in extended focus blocks during $timeOfDay.",
              "rationale": "A warm soft gong gently grounds your mind after deep cognitive effort."
            }
        """.trimIndent()

        try {
            val jsonPayload = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.3)
                })
            }

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""

            if (!response.isSuccessful || bodyString.isEmpty()) {
                val fallback = getFallbackRecommendation(timeOfDay, lastSessionMinutes, totalSessions)
                return@withContext Result.success(fallback)
            }

            val jsonResponse = JSONObject(bodyString)
            val candidates = jsonResponse.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val responseText = parts?.optJSONObject(0)?.optString("text") ?: ""

            val cleanedText = responseText
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val parsedJson = JSONObject(cleanedText)
            val soundId = parsedJson.optString("recommendedSoundId", "tibetan_bowl")
            val soundName = parsedJson.optString("soundName", "Tibetan Singing Bowl")
            val patternAnalysis = parsedJson.optString("patternAnalysis", "Based on your recent focus session patterns.")
            val rationale = parsedJson.optString("rationale", "This resonant sound provides a soothing transition.")

            Result.success(
                SoundRecommendation(
                    recommendedSoundId = soundId,
                    soundName = soundName,
                    patternAnalysis = patternAnalysis,
                    rationale = rationale
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.success(getFallbackRecommendation(timeOfDay, lastSessionMinutes, totalSessions))
        }
    }

    private fun getFallbackRecommendation(timeOfDay: String, lastSessionMinutes: Int, totalSessions: Int): SoundRecommendation {
        return when {
            timeOfDay.contains("Night", ignoreCase = true) || timeOfDay.contains("Evening", ignoreCase = true) -> SoundRecommendation(
                recommendedSoundId = "soft_gong",
                soundName = "Warm Soft Gong",
                patternAnalysis = "You are focusing during $timeOfDay hours across $totalSessions completed sessions.",
                rationale = "The warm soft gong provides a soothing, low-frequency transition for peaceful evening focus."
            )
            lastSessionMinutes <= 15 -> SoundRecommendation(
                recommendedSoundId = "crystal_bowl",
                soundName = "528Hz Crystal Bowl",
                patternAnalysis = "You completed a quick $lastSessionMinutes-minute focus sprint.",
                rationale = "The 528Hz crystal tone offers crisp mental clarity for energetic sprints."
            )
            lastSessionMinutes >= 40 -> SoundRecommendation(
                recommendedSoundId = "tibetan_bowl",
                soundName = "Tibetan Singing Bowl",
                patternAnalysis = "You completed an extended $lastSessionMinutes-minute deep focus block.",
                rationale = "The deep Tibetan singing bowl grounds the mind after sustained concentration."
            )
            else -> SoundRecommendation(
                recommendedSoundId = "zen_bell",
                soundName = "Zen Temple Bell",
                patternAnalysis = "Your focus rhythm shows steady $lastSessionMinutes-minute sessions during $timeOfDay.",
                rationale = "A mindful Zen temple bell chime marks a balanced work-rest boundary."
            )
        }
    }
}
