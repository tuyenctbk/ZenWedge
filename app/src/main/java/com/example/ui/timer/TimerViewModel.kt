package com.example.ui.timer

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.util.AnalyticsTracker
import com.example.util.AppPreferences
import com.example.util.ChimeSynthesizer
import com.example.util.FirebaseSyncManager
import com.example.util.GeminiSoundAnalyzer
import com.example.util.HapticHelper
import com.example.util.SoundRecommendation
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class TimerState {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED
}

class TimerViewModel : ViewModel() {

    private val _remainingSeconds = MutableStateFlow(25 * 60)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _totalDurationSeconds = MutableStateFlow(25 * 60)
    val totalDurationSeconds: StateFlow<Int> = _totalDurationSeconds.asStateFlow()

    private val _timerState = MutableStateFlow(TimerState.IDLE)
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private val _currentTheme = MutableStateFlow(WedgeTheme.CALM_TEAL)
    val currentTheme: StateFlow<WedgeTheme> = _currentTheme.asStateFlow()

    private val _isKeepScreenOn = MutableStateFlow(true)
    val isKeepScreenOn: StateFlow<Boolean> = _isKeepScreenOn.asStateFlow()

    private val _intervalHapticMinutes = MutableStateFlow(5)
    val intervalHapticMinutes: StateFlow<Int> = _intervalHapticMinutes.asStateFlow()

    private val _playChime = MutableStateFlow(true)
    val playChime: StateFlow<Boolean> = _playChime.asStateFlow()

    private val _selectedSoundId = MutableStateFlow("tibetan_bowl")
    val selectedSoundId: StateFlow<String> = _selectedSoundId.asStateFlow()

    private val _aiRecommendation = MutableStateFlow<SoundRecommendation?>(null)
    val aiRecommendation: StateFlow<SoundRecommendation?> = _aiRecommendation.asStateFlow()

    private val _isAnalyzingPatterns = MutableStateFlow(false)
    val isAnalyzingPatterns: StateFlow<Boolean> = _isAnalyzingPatterns.asStateFlow()

    private val _showRatingDialog = MutableStateFlow(false)
    val showRatingDialog: StateFlow<Boolean> = _showRatingDialog.asStateFlow()

    private val _showShareDialog = MutableStateFlow(false)
    val showShareDialog: StateFlow<Boolean> = _showShareDialog.asStateFlow()

    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog: StateFlow<Boolean> = _showUpdateDialog.asStateFlow()

    private val _hapticEvent = MutableSharedFlow<HapticType>()
    val hapticEvent: SharedFlow<HapticType> = _hapticEvent.asSharedFlow()

    private var timerJob: Job? = null
    private var lastTickTime: Long = 0L

    sealed class HapticType {
        object SlideTick : HapticType()
        object IntervalBump : HapticType()
        object Completion : HapticType()
    }

    fun initPreferences(context: Context) {
        _selectedSoundId.value = AppPreferences.getSelectedSound(context)
    }

    fun setDuration(seconds: Int, isUserDrag: Boolean = false) {
        val sanitizedSeconds = seconds.coerceIn(60, 3600)
        
        if (isUserDrag && (sanitizedSeconds / 60) != (_remainingSeconds.value / 60)) {
            viewModelScope.launch {
                _hapticEvent.emit(HapticType.SlideTick)
            }
        }
        
        _remainingSeconds.value = sanitizedSeconds
        _totalDurationSeconds.value = sanitizedSeconds
        if (_timerState.value == TimerState.COMPLETED) {
            _timerState.value = TimerState.IDLE
        }
    }

    fun start(context: Context? = null) {
        if (_timerState.value == TimerState.RUNNING) return
        _timerState.value = TimerState.RUNNING
        lastTickTime = System.currentTimeMillis()
        
        context?.let { ctx ->
            AnalyticsTracker.logFocusStart(
                context = ctx.applicationContext,
                durationSeconds = _remainingSeconds.value,
                themeName = _currentTheme.value.name
            )
        }
        
        timerJob = viewModelScope.launch {
            while (_timerState.value == TimerState.RUNNING) {
                delay(200)
                val now = System.currentTimeMillis()
                val deltaSec = ((now - lastTickTime) / 1000).toInt()
                if (deltaSec >= 1) {
                    val prevSeconds = _remainingSeconds.value
                    val nextSeconds = (prevSeconds - deltaSec).coerceAtLeast(0)
                    _remainingSeconds.value = nextSeconds
                    lastTickTime = now

                    val hapticInterval = _intervalHapticMinutes.value
                    if (hapticInterval > 0) {
                        val prevMins = prevSeconds / 60
                        val nextMins = nextSeconds / 60
                        if (nextSeconds > 0 && prevMins != nextMins && nextMins % hapticInterval == 0) {
                            _hapticEvent.emit(HapticType.IntervalBump)
                        }
                    }

                    if (nextSeconds <= 0) {
                        completeTimer(context)
                    }
                }
            }
        }
    }

    fun pause(context: Context? = null) {
        if (_timerState.value != TimerState.RUNNING) return
        _timerState.value = TimerState.PAUSED
        timerJob?.cancel()
        
        context?.let { ctx ->
            AnalyticsTracker.logFocusPause(
                context = ctx.applicationContext,
                remainingSeconds = _remainingSeconds.value
            )
        }
    }

    fun reset(context: Context? = null) {
        timerJob?.cancel()
        _remainingSeconds.value = _totalDurationSeconds.value
        _timerState.value = TimerState.IDLE
        
        context?.let { ctx ->
            AnalyticsTracker.logFocusReset(ctx.applicationContext)
        }
    }

    fun selectTheme(theme: WedgeTheme, context: Context? = null) {
        _currentTheme.value = theme
        context?.let { ctx ->
            AnalyticsTracker.logThemeChanged(ctx.applicationContext, theme.name)
        }
    }

    fun setKeepScreenOn(enabled: Boolean, context: Context? = null) {
        _isKeepScreenOn.value = enabled
        context?.let { ctx ->
            AnalyticsTracker.logSettingChanged(ctx.applicationContext, "keep_screen_on", enabled)
        }
    }

    fun setIntervalHaptic(minutes: Int, context: Context? = null) {
        _intervalHapticMinutes.value = minutes
        context?.let { ctx ->
            AnalyticsTracker.logSettingChanged(ctx.applicationContext, "interval_haptics_mins", minutes > 0)
        }
    }

    fun setPlayChime(enabled: Boolean, context: Context? = null) {
        _playChime.value = enabled
        context?.let { ctx ->
            AnalyticsTracker.logSettingChanged(ctx.applicationContext, "play_chime", enabled)
        }
    }

    fun selectSound(soundId: String, context: Context? = null) {
        _selectedSoundId.value = soundId
        context?.let { ctx ->
            AppPreferences.setSelectedSound(ctx, soundId)
        }
    }

    fun previewSound(soundId: String) {
        viewModelScope.launch {
            ChimeSynthesizer.playSoundById(soundId)
        }
    }

    fun analyzeFocusPatternsWithGemini(context: Context) {
        viewModelScope.launch {
            _isAnalyzingPatterns.value = true
            val count = AppPreferences.getSessionCount(context)
            val totalMins = AppPreferences.getTotalFocusMinutes(context)
            val avgMins = if (count > 0) totalMins / count else 25
            val lastMins = AppPreferences.getLastSessionMinutes(context)
            val tod = AppPreferences.getTimeOfDay()
            val history = AppPreferences.getRecentSessionsSummary(context)

            val result = GeminiSoundAnalyzer.analyzeFocusPatternsAndSuggestSound(
                totalSessions = count,
                totalMinutes = totalMins,
                averageDurationMinutes = avgMins,
                lastSessionMinutes = lastMins,
                timeOfDay = tod,
                recentSessionsSummary = history
            )

            result.onSuccess { recommendation ->
                _aiRecommendation.value = recommendation
            }
            _isAnalyzingPatterns.value = false
        }
    }

    fun dismissRatingDialog(context: Context? = null, rated: Boolean = false) {
        _showRatingDialog.value = false
        if (rated && context != null) {
            AppPreferences.setHasRatedApp(context, true)
        }
    }

    fun dismissShareDialog(context: Context? = null, shared: Boolean = false) {
        _showShareDialog.value = false
        if (shared && context != null) {
            AppPreferences.setHasSharedApp(context, true)
        }
    }
    
    fun dismissUpdateDialog() {
        _showUpdateDialog.value = false
    }
    
    fun checkAppUpdate() {
        _showUpdateDialog.value = true
    }
    
    fun triggerShareApp() {
        _showShareDialog.value = true
    }
    
    fun triggerRateApp() {
        _showRatingDialog.value = true
    }

    private fun completeTimer(context: Context? = null) {
        timerJob?.cancel()
        _timerState.value = TimerState.COMPLETED
        
        viewModelScope.launch {
            _hapticEvent.emit(HapticType.Completion)
            val soundToPlay = _selectedSoundId.value
            if (_playChime.value) {
                ChimeSynthesizer.playSoundById(soundToPlay)
            }
            context?.let { ctx ->
                val durationMins = (_totalDurationSeconds.value / 60).coerceAtLeast(1)
                AppPreferences.recordCompletedSession(ctx, durationMins)
                val sessionCount = AppPreferences.getSessionCount(ctx)
                
                FirebaseSyncManager.logSession(
                    context = ctx.applicationContext,
                    durationSeconds = _totalDurationSeconds.value,
                    themeName = _currentTheme.value.name
                )
                AnalyticsTracker.logFocusComplete(
                    context = ctx.applicationContext,
                    durationSeconds = _totalDurationSeconds.value,
                    themeName = _currentTheme.value.name
                )
                
                if (sessionCount >= 3 && !AppPreferences.hasRatedApp(ctx) && (sessionCount == 3 || sessionCount % 15 == 0)) {
                    _showRatingDialog.value = true
                } else if (sessionCount >= 5 && !AppPreferences.hasSharedApp(ctx) && (sessionCount == 5 || sessionCount % 20 == 0)) {
                    _showShareDialog.value = true
                } else if (sessionCount % 10 == 0) {
                    _showUpdateDialog.value = true
                }

            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
