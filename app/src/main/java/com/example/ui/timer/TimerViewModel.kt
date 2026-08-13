package com.example.ui.timer

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.FocusSessionEntity
import com.example.data.db.FocusSessionRepository
import com.example.data.db.ZenWedgeDatabase
import com.example.util.AlarmPlayer
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

enum class SessionMode(val displayName: String, val defaultMinutes: Int) {
    FOCUS("Focus", 25),
    POMODORO("Pomodoro Work", 25),
    SHORT_BREAK("Short Break", 5),
    LONG_BREAK("Long Break", 15)
}

class TimerViewModel : ViewModel() {

    private var repository: FocusSessionRepository? = null

    private val _roomSessions = MutableStateFlow<List<FocusSessionEntity>>(emptyList())
    val roomSessions: StateFlow<List<FocusSessionEntity>> = _roomSessions.asStateFlow()

    private val _sessionMode = MutableStateFlow(SessionMode.FOCUS)
    val sessionMode: StateFlow<SessionMode> = _sessionMode.asStateFlow()

    private val _pomodoroCycle = MutableStateFlow(1)
    val pomodoroCycle: StateFlow<Int> = _pomodoroCycle.asStateFlow()

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _quickPresets = MutableStateFlow(listOf(5, 10, 15, 25, 45, 60))
    val quickPresets: StateFlow<List<Int>> = _quickPresets.asStateFlow()

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

    private val _hapticMode = MutableStateFlow("standard")
    val hapticMode: StateFlow<String> = _hapticMode.asStateFlow()

    private val _dynamicColorShiftEnabled = MutableStateFlow(true)
    val dynamicColorShiftEnabled: StateFlow<Boolean> = _dynamicColorShiftEnabled.asStateFlow()

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

    private val _isOnboardingCompleted = MutableStateFlow(false)
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

    private val _hapticEvent = MutableSharedFlow<HapticType>()
    val hapticEvent: SharedFlow<HapticType> = _hapticEvent.asSharedFlow()

    private var timerJob: Job? = null
    private var lastTickTime: Long = 0L

    sealed class HapticType {
        object SlideTick : HapticType()
        object ButtonClick : HapticType()
        object IntervalBump : HapticType()
        object Completion : HapticType()
    }

    fun initPreferences(context: Context) {
        _selectedSoundId.value = AppPreferences.getSelectedSound(context)
        _hapticMode.value = AppPreferences.getHapticMode(context)
        _dynamicColorShiftEnabled.value = AppPreferences.getDynamicColorShiftEnabled(context)
        _isDarkMode.value = AppPreferences.isDarkMode(context)
        _quickPresets.value = AppPreferences.getQuickPresets(context)
        _isOnboardingCompleted.value = AppPreferences.isOnboardingCompleted(context)

        if (repository == null) {
            val db = ZenWedgeDatabase.getDatabase(context)
            val repo = FocusSessionRepository(db.focusSessionDao())
            repository = repo
            viewModelScope.launch {
                repo.allSessions.collect { sessions ->
                    _roomSessions.value = sessions
                }
            }
        }
    }

    fun setOnboardingCompleted(completed: Boolean, context: Context) {
        _isOnboardingCompleted.value = completed
        AppPreferences.setOnboardingCompleted(context, completed)
    }

    fun toggleDarkMode(context: Context) {
        val nextMode = !_isDarkMode.value
        _isDarkMode.value = nextMode
        AppPreferences.setDarkMode(context, nextMode)
    }

    fun updatePreset(index: Int, newMinutes: Int, context: Context) {
        val currentList = _quickPresets.value.toMutableList()
        if (index in currentList.indices) {
            currentList[index] = newMinutes.coerceIn(1, 180)
            _quickPresets.value = currentList
            AppPreferences.setQuickPresets(context, currentList)
        }
    }

    fun setSessionMode(mode: SessionMode) {
        _sessionMode.value = mode
        setDuration(mode.defaultMinutes * 60)
    }

    fun setHapticMode(mode: String, context: Context) {
        _hapticMode.value = mode
        AppPreferences.setHapticMode(context, mode)
    }

    fun setDynamicColorShift(enabled: Boolean, context: Context) {
        _dynamicColorShiftEnabled.value = enabled
        AppPreferences.setDynamicColorShiftEnabled(context, enabled)
    }

    fun setDuration(seconds: Int, isUserDrag: Boolean = false) {
        val sanitizedSeconds = seconds.coerceIn(60, 10800)
        
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

    fun previewSound(soundId: String, context: Context? = null) {
        viewModelScope.launch {
            if (soundId == "system_alarm" && context != null) {
                AlarmPlayer.playCompletionAlarm(context, 2500L)
            } else {
                ChimeSynthesizer.playSoundById(soundId)
            }
        }
    }

    fun emitButtonClickHaptic() {
        viewModelScope.launch {
            _hapticEvent.emit(HapticType.ButtonClick)
        }
    }

    fun analyzeFocusPatternsWithGemini(context: Context) {
        viewModelScope.launch {
            _isAnalyzingPatterns.value = true
            AnalyticsTracker.startTrace("analyze_patterns_gemini")
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
            AnalyticsTracker.stopTrace("analyze_patterns_gemini")
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
                if (soundToPlay == "system_alarm" && context != null) {
                    AlarmPlayer.playCompletionAlarm(context, 4000L)
                } else {
                    ChimeSynthesizer.playSoundById(soundToPlay)
                }
            }
            context?.let { ctx ->
                val durationMins = (_totalDurationSeconds.value / 60).coerceAtLeast(1)
                AppPreferences.recordCompletedSession(ctx, durationMins)
                val sessionCount = AppPreferences.getSessionCount(ctx)
                val totalFocusMins = AppPreferences.getTotalFocusMinutes(ctx)
                
                val entity = FocusSessionEntity(
                    durationSeconds = _totalDurationSeconds.value,
                    durationMinutes = durationMins,
                    timeOfDay = AppPreferences.getTimeOfDay(),
                    themeName = "${_currentTheme.value.name} (${_sessionMode.value.displayName})",
                    soundId = _selectedSoundId.value
                )
                repository?.insert(entity)

                // Pomodoro Auto-Chaining transition logic
                when (_sessionMode.value) {
                    SessionMode.POMODORO -> {
                        val currentCycle = _pomodoroCycle.value
                        if (currentCycle >= 4) {
                            _sessionMode.value = SessionMode.LONG_BREAK
                            setDuration(15 * 60)
                            _pomodoroCycle.value = 1
                        } else {
                            _sessionMode.value = SessionMode.SHORT_BREAK
                            setDuration(5 * 60)
                            _pomodoroCycle.value = currentCycle + 1
                        }
                    }
                    SessionMode.SHORT_BREAK, SessionMode.LONG_BREAK -> {
                        _sessionMode.value = SessionMode.POMODORO
                        setDuration(25 * 60)
                    }
                    else -> {}
                }

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
                
                // Smart Calculation Suggestions based on high-satisfaction user engagement metrics
                val avgDurationMins = if (sessionCount > 0) totalFocusMins / sessionCount else 0
                val isHighlySatisfied = (sessionCount >= 3 && avgDurationMins >= 15) || totalFocusMins >= 100

                if (isHighlySatisfied && !AppPreferences.hasRatedApp(ctx)) {
                    _showRatingDialog.value = true
                } else if ((sessionCount == 5 || sessionCount % 15 == 0) && !AppPreferences.hasSharedApp(ctx)) {
                    _showShareDialog.value = true
                } else if (sessionCount % 8 == 0) {
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
