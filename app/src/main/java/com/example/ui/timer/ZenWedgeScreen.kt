package com.example.ui.timer

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.AnalyticsTracker
import com.example.util.AppPreferences
import com.example.util.HapticHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZenWedgeScreen(
    viewModel: TimerViewModel,
    modifier: Modifier = Modifier
) {
        
        val context = LocalContext.current
        val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsState()
        val remainingSeconds by viewModel.remainingSeconds.collectAsState()
        val totalSeconds by viewModel.totalDurationSeconds.collectAsState()
        val timerState by viewModel.timerState.collectAsState()
        val theme by viewModel.currentTheme.collectAsState()
        val isKeepScreenOn by viewModel.isKeepScreenOn.collectAsState()
        val hapticMode by viewModel.hapticMode.collectAsState()
        val dynamicColorShiftEnabled by viewModel.dynamicColorShiftEnabled.collectAsState()
        val roomSessions by viewModel.roomSessions.collectAsState()
        val intervalHapticMinutes by viewModel.intervalHapticMinutes.collectAsState()
        val playChime by viewModel.playChime.collectAsState()
        val selectedSoundId by viewModel.selectedSoundId.collectAsState()
        val aiRecommendation by viewModel.aiRecommendation.collectAsState()
        val isAnalyzingPatterns by viewModel.isAnalyzingPatterns.collectAsState()
        val showRatingDialog by viewModel.showRatingDialog.collectAsState()
        val showShareDialog by viewModel.showShareDialog.collectAsState()
        val showUpdateDialog by viewModel.showUpdateDialog.collectAsState()
        val isDarkMode by viewModel.isDarkMode.collectAsState()
        val sessionMode by viewModel.sessionMode.collectAsState()
        val pomodoroCycle by viewModel.pomodoroCycle.collectAsState()
        val quickPresets by viewModel.quickPresets.collectAsState()

        var showSettingsSheet by remember { mutableStateOf(false) }
        var showSessionsSheet by remember { mutableStateOf(false) }
        var editingPresetIndex by remember { mutableStateOf<Int?>(null) }

        val animatedBgColor by animateColorAsState(
            targetValue = theme.getBgColor(isDarkMode),
            animationSpec = tween(durationMillis = 600),
            label = "backgroundColorFade"
        )
        val textColor = theme.getTextColor(isDarkMode)
        val secondaryTextColor = theme.getSecondaryTextColor(isDarkMode)

        LaunchedEffect(Unit) {
            viewModel.initPreferences(context)
            AnalyticsTracker.trackScreen(context, "ZenWedge_Timer_Screen")
        }

        if (!isOnboardingCompleted) {
            OnboardingScreen(
                onFinished = {
                    viewModel.setOnboardingCompleted(true, context)
                },
                isDarkMode = isDarkMode,
                theme = theme,
                modifier = Modifier.fillMaxSize()
            )
            return
        }

        // Screen tracking for settings sheet
        LaunchedEffect(showSettingsSheet) {
            if (showSettingsSheet) {
                AnalyticsTracker.trackScreen(context, "Settings_Preferences_Sheet")
            }
        }

        // Keep screen on management via DisposableEffect to guarantee cleanup
        DisposableEffect(isKeepScreenOn, timerState) {
            val activity = context as? Activity
            val shouldKeepOn = isKeepScreenOn && timerState == TimerState.RUNNING
            if (activity != null && shouldKeepOn) {
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            onDispose {
                (context as? Activity)?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }

        // Handle side-effect haptic events from the ViewModel
        LaunchedEffect(hapticMode) {
            viewModel.hapticEvent.collect { event ->
                when (event) {
                    is TimerViewModel.HapticType.SlideTick, is TimerViewModel.HapticType.ButtonClick -> {
                        HapticHelper.triggerTick(context, hapticMode)
                    }
                    is TimerViewModel.HapticType.IntervalBump -> {
                        HapticHelper.triggerIntervalBump(context, hapticMode)
                    }
                    is TimerViewModel.HapticType.Completion -> {
                        HapticHelper.triggerCompletionVibe(context, hapticMode)
                    }
                }
            }
        }

        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = animatedBgColor,
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(theme.wedgeColor, CircleShape)
                            )
                            Text(
                                text = stringResource(R.string.appName),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    letterSpacing = (-0.5).sp
                                )
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showSessionsSheet = true },
                            modifier = Modifier
                                .testTag("sessions_button")
                                .background(if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = "Sessions Stats",
                                tint = textColor
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = { showSettingsSheet = true },
                            modifier = Modifier
                                .testTag("settings_button")
                                .background(if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = textColor
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Session Mode Selector Bar (Focus, Pomodoro, Short Break, Long Break)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 8.dp)
                ) {
                    SessionMode.entries.forEach { mode ->
                        val isSelected = (sessionMode == mode)
                        val badgeText = if (mode == SessionMode.POMODORO && isSelected) "Pomo #$pomodoroCycle" else mode.displayName
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) theme.wedgeColor else if (isDarkMode) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.05f)
                                )
                                .border(
                                    width = if (isSelected) 1.dp else 0.dp,
                                    color = if (isSelected) Color.White.copy(alpha = 0.6f) else Color.Transparent,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    viewModel.emitButtonClickHaptic()
                                    viewModel.setSessionMode(mode)
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.sp,
                                    color = if (isSelected) Color.White else secondaryTextColor
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }

                // Main Timer Wedge Canvas Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    WedgeDial(
                        remainingSeconds = remainingSeconds,
                        totalSeconds = totalSeconds,
                        timerState = timerState,
                        theme = theme,
                        isDarkMode = isDarkMode,
                        isDynamicColorShift = dynamicColorShiftEnabled,
                        onAngleChanged = { angle ->
                            if (timerState == TimerState.IDLE || timerState == TimerState.COMPLETED) {
                                val minutes = (angle / 360f * 60f).roundToInt().coerceIn(1, 60)
                                viewModel.setDuration(minutes * 60, isUserDrag = true)
                            }
                        }
                    )
                }

                // Quick-select Duration Presets with Long-Press Customization
                if (timerState == TimerState.IDLE || timerState == TimerState.COMPLETED) {
                    val currentMins = (remainingSeconds / 60).coerceAtLeast(1)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        ) {
                            itemsIndexed(quickPresets) { index, mins ->
                                val isSelected = (currentMins == mins)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(
                                            if (isSelected) theme.wedgeColor else if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
                                        )
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) textColor.copy(alpha = 0.8f) else secondaryTextColor.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(20.dp)
                                        )
                                        .pointerInput(mins) {
                                            detectTapGestures(
                                                onTap = {
                                                    viewModel.emitButtonClickHaptic()
                                                    viewModel.setDuration(mins * 60, isUserDrag = false)
                                                },
                                                onLongPress = {
                                                    viewModel.emitButtonClickHaptic()
                                                    editingPresetIndex = index
                                                }
                                            )
                                        }
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                        .testTag("preset_${mins}m"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${mins}m",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else textColor
                                        )
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Tap preset to select • Long-press button to edit duration",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                color = secondaryTextColor.copy(alpha = 0.8f),
                                fontStyle = FontStyle.Italic
                            ),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.customDuration),
                                    style = MaterialTheme.typography.bodySmall.copy(color = secondaryTextColor)
                                )
                                Text(
                                    text = "$currentMins min${if (currentMins > 1) "s" else ""}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = theme.wedgeColor, fontWeight = FontWeight.Bold)
                                )
                            }
                            Slider(
                                value = currentMins.toFloat(),
                                onValueChange = { newValue ->
                                    val newMins = newValue.toInt()
                                    if (newMins != currentMins) {
                                        viewModel.setDuration(newMins * 60, isUserDrag = false)
                                    }
                                },
                                valueRange = 1f..90f,
                                steps = 89,
                                colors = SliderDefaults.colors(
                                    thumbColor = theme.wedgeColor,
                                    activeTrackColor = theme.wedgeColor,
                                    inactiveTrackColor = if (isDarkMode) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f)
                                ),
                                modifier = Modifier.testTag("custom_duration_slider")
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Footer / Control Area
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Color theme preset selector
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 32.dp)
                    ) {
                        WedgeTheme.entries.forEach { wedgeTheme ->
                            val isSelected = theme == wedgeTheme
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(wedgeTheme.wedgeColor)
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = if (isSelected) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        viewModel.emitButtonClickHaptic()
                                        viewModel.selectTheme(wedgeTheme, context)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Main control action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Start / Pause primary action button
                        Button(
                            onClick = {
                                if (timerState == TimerState.RUNNING) {
                                    viewModel.pause(context)
                                } else {
                                    viewModel.start(context)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = theme.wedgeColor,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(28.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .testTag("action_button"),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Text(
                                text = when (timerState) {
                                    TimerState.RUNNING -> stringResource(R.string.pauseFocus)
                                    TimerState.PAUSED -> stringResource(R.string.resumeFocus)
                                    else -> stringResource(R.string.startFocus)
                                },
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp
                                )
                            )
                        }

                        // Reset button
                        IconButton(
                            onClick = { viewModel.reset(context) },
                            modifier = Modifier
                                .size(56.dp)
                                .border(1.dp, secondaryTextColor.copy(alpha = 0.2f), RoundedCornerShape(28.dp))
                                .testTag("reset_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset Timer",
                                tint = textColor
                            )
                        }
                    }

                    // Info / Status footer bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        if (intervalHapticMinutes > 0) theme.wedgeColor else secondaryTextColor.copy(alpha = 0.3f),
                                        CircleShape
                                    )
                            )
                            Text(
                                text = if (intervalHapticMinutes > 0) stringResource(R.string.hapticsEnabled) else stringResource(R.string.hapticsMuted),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = secondaryTextColor,
                                    letterSpacing = 1.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        // Estimated completion time
                        val endsAtPrefix = stringResource(R.string.endsAtPrefix)
                        val minSessionSuffix = stringResource(R.string.minSessionSuffix)
                        val endTimeText = remember(remainingSeconds, timerState) {
                            if (timerState == TimerState.RUNNING) {
                                val cal = Calendar.getInstance()
                                cal.add(Calendar.SECOND, remainingSeconds)
                                val format = SimpleDateFormat("h:mm a", Locale.getDefault())
                                "${endsAtPrefix} ${format.format(cal.time)}"
                            } else {
                                val mins = remainingSeconds / 60
                                "$mins ${minSessionSuffix}"
                            }
                        }

                        Text(
                            text = endTimeText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = secondaryTextColor,
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            // Modal Settings Sheet
            if (showSettingsSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showSettingsSheet = false },
                    containerColor = if (isDarkMode) Color(0xFF1E1F22) else Color(0xFFF8FAFC),
                    contentColor = textColor
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(24.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.timerPreferences),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            ),
                            modifier = Modifier.padding(bottom = 20.dp)
                        )

                        // Premium Dark/Light Mode Theme Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    text = stringResource(R.string.darkThemeMode),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = textColor
                                    )
                                )
                                Text(
                                    text = stringResource(R.string.darkThemeModeDesc),
                                    style = MaterialTheme.typography.bodySmall.copy(color = secondaryTextColor)
                                )
                            }
                            Switch(
                                checked = isDarkMode,
                                onCheckedChange = {
                                    viewModel.emitButtonClickHaptic()
                                    viewModel.toggleDarkMode(context)
                                    AnalyticsTracker.logThemeChanged(context, if (isDarkMode) "Light" else "Dark")
                                },
                                modifier = Modifier.testTag("theme_toggle_button"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = theme.wedgeColor,
                                    checkedTrackColor = theme.wedgeColor.copy(alpha = 0.4f),
                                    uncheckedThumbColor = secondaryTextColor,
                                    uncheckedTrackColor = secondaryTextColor.copy(alpha = 0.2f)
                                )
                            )
                        }

                        HorizontalDivider(
                            color = if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f),
                            modifier = Modifier.padding(vertical = 6.dp)
                        )

                        // Keep screen on config
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    text = stringResource(R.string.keepScreenOn),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = textColor
                                    )
                                )
                                Text(
                                    text = stringResource(R.string.keepScreenOnDesc),
                                    style = MaterialTheme.typography.bodySmall.copy(color = secondaryTextColor)
                                )
                            }
                            Switch(
                                checked = isKeepScreenOn,
                                onCheckedChange = { viewModel.setKeepScreenOn(it, context) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = theme.wedgeColor,
                                    checkedTrackColor = theme.wedgeColor.copy(alpha = 0.4f),
                                    uncheckedThumbColor = secondaryTextColor,
                                    uncheckedTrackColor = secondaryTextColor.copy(alpha = 0.2f)
                                )
                            )
                        }

                        HorizontalDivider(
                            color = if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f),
                            modifier = Modifier.padding(vertical = 6.dp)
                        )

                        // Dynamic Green-to-Red Color Shift Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    text = stringResource(R.string.dynamicColorShift),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = textColor
                                    )
                                )
                                Text(
                                    text = stringResource(R.string.dynamicColorShiftDesc),
                                    style = MaterialTheme.typography.bodySmall.copy(color = secondaryTextColor)
                                )
                            }
                            Switch(
                                checked = dynamicColorShiftEnabled,
                                onCheckedChange = { viewModel.setDynamicColorShift(it, context) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = theme.wedgeColor,
                                    checkedTrackColor = theme.wedgeColor.copy(alpha = 0.4f),
                                    uncheckedThumbColor = secondaryTextColor,
                                    uncheckedTrackColor = secondaryTextColor.copy(alpha = 0.2f)
                                )
                            )
                        }

                        HorizontalDivider(
                            color = if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f),
                            modifier = Modifier.padding(vertical = 6.dp)
                        )

                        // Haptic Intensity Pattern Selector
                        Column(modifier = Modifier.padding(vertical = 10.dp)) {
                            Text(
                                text = stringResource(R.string.completionHaptic),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = textColor
                                )
                            )
                            Text(
                                text = stringResource(R.string.completionHapticDesc),
                                style = MaterialTheme.typography.bodySmall.copy(color = secondaryTextColor),
                                modifier = Modifier.padding(bottom = 10.dp)
                            )

                            val hapticOptions = listOf(
                                "off" to stringResource(R.string.hapticMutedOption),
                                "gentle" to stringResource(R.string.hapticGentleOption),
                                "standard" to stringResource(R.string.hapticStandardOption),
                                "strong" to stringResource(R.string.hapticStrongOption)
                            )

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(hapticOptions) { (key, label) ->
                                    val isSelected = hapticMode == key
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            viewModel.setHapticMode(key, context)
                                            HapticHelper.triggerCompletionVibe(context, key)
                                        },
                                        label = { Text(label) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = theme.wedgeColor,
                                            selectedLabelColor = if (isDarkMode) Color.Black else Color.White,
                                            containerColor = if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f),
                                            labelColor = textColor
                                        )
                                    )
                                }
                            }
                        }

                        HorizontalDivider(
                            color = if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f),
                            modifier = Modifier.padding(vertical = 6.dp)
                        )

                        // Theme Selector
                        Column(modifier = Modifier.padding(vertical = 12.dp)) {
                            Text(
                                text = stringResource(R.string.themeSelectorTitle),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = textColor
                                )
                            )
                            Text(
                                text = stringResource(R.string.chooseWedgePalette),
                                style = MaterialTheme.typography.bodySmall.copy(color = secondaryTextColor),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(WedgeTheme.entries) { t ->
                                    val isSelected = (theme == t)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) t.wedgeColor else (if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f)))
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) textColor else (if (isDarkMode) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.15f)),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable {
                                                viewModel.selectTheme(t, context)
                                            }
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                            .testTag("theme_${t.name.lowercase()}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .background(t.wedgeColor, CircleShape)
                                            )
                                            Text(
                                                text = t.themeName,
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) (if (isDarkMode) Color.Black else Color.White) else textColor
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            color = if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        // Singing bowl chime toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    text = stringResource(R.string.chimeTitle),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Medium,
                                        color = textColor
                                    )
                                )
                                Text(
                                    text = stringResource(R.string.chimeDesc),
                                    style = MaterialTheme.typography.bodySmall.copy(color = secondaryTextColor)
                                )
                            }
                            Switch(
                                checked = playChime,
                                onCheckedChange = { viewModel.setPlayChime(it, context) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = theme.wedgeColor,
                                    checkedTrackColor = theme.wedgeColor.copy(alpha = 0.4f),
                                    uncheckedThumbColor = secondaryTextColor,
                                    uncheckedTrackColor = secondaryTextColor.copy(alpha = 0.2f)
                                )
                            )
                        }

                        if (playChime) {
                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                Text(
                                    text = stringResource(R.string.completionChime),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = textColor
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                val soundOptions = listOf(
                                    "system_alarm" to stringResource(R.string.soundSystemAlarm),
                                    "tibetan_bowl" to stringResource(R.string.soundTibetanBowl),
                                    "crystal_bowl" to stringResource(R.string.soundCrystalBowl),
                                    "soft_gong" to stringResource(R.string.soundSoftGong),
                                    "zen_bell" to stringResource(R.string.soundZenBell),
                                    "raindrop_chime" to stringResource(R.string.soundRaindropChime)
                                )

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(soundOptions) { (id, name) ->
                                        val isSelected = selectedSoundId == id
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                viewModel.emitButtonClickHaptic()
                                                viewModel.selectSound(id, context)
                                                viewModel.previewSound(id, context)
                                            },
                                            label = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(name)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                        contentDescription = "Preview sound",
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = theme.wedgeColor,
                                                selectedLabelColor = if (isDarkMode) Color.Black else Color.White,
                                                containerColor = if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f),
                                                labelColor = textColor
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Gemini AI Sound Assistant Card
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f)
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = theme.wedgeColor,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = stringResource(R.string.aiFocusAdvisor),
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = textColor
                                                )
                                            }

                                            Button(
                                                onClick = { viewModel.analyzeFocusPatternsWithGemini(context) },
                                                enabled = !isAnalyzingPatterns,
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = theme.wedgeColor,
                                                    contentColor = if (isDarkMode) Color.Black else Color.White
                                                ),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                if (isAnalyzingPatterns) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(12.dp),
                                                        color = if (isDarkMode) Color.Black else Color.White,
                                                        strokeWidth = 2.dp
                                                    )
                                                } else {
                                                    Text(stringResource(R.string.analyze), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = stringResource(R.string.aiFocusAdvisorDesc),
                                            style = MaterialTheme.typography.bodySmall.copy(color = secondaryTextColor, fontSize = 11.sp)
                                        )

                                        aiRecommendation?.let { rec ->
                                            Spacer(modifier = Modifier.height(10.dp))
                                            HorizontalDivider(color = if (isDarkMode) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f))
                                            Spacer(modifier = Modifier.height(10.dp))

                                            Text(
                                                text = stringResource(R.string.patternInsights),
                                                style = MaterialTheme.typography.labelSmall.copy(color = theme.wedgeColor, fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = rec.patternAnalysis,
                                                style = MaterialTheme.typography.bodySmall.copy(color = textColor)
                                            )

                                            Spacer(modifier = Modifier.height(6.dp))

                                            Text(
                                                text = stringResource(R.string.recommendedFormat, rec.soundName),
                                                style = MaterialTheme.typography.labelSmall.copy(color = theme.wedgeColor, fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = rec.rationale,
                                                style = MaterialTheme.typography.bodySmall.copy(color = secondaryTextColor)
                                            )

                                            Spacer(modifier = Modifier.height(10.dp))

                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Button(
                                                    onClick = {
                                                        viewModel.emitButtonClickHaptic()
                                                        viewModel.selectSound(rec.recommendedSoundId, context)
                                                        viewModel.previewSound(rec.recommendedSoundId, context)
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (selectedSoundId == rec.recommendedSoundId) (if (isDarkMode) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.2f)) else theme.wedgeColor,
                                                        contentColor = if (selectedSoundId == rec.recommendedSoundId) textColor else (if (isDarkMode) Color.Black else Color.White)
                                                    ),
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(vertical = 6.dp)
                                                ) {
                                                    Text(
                                                        if (selectedSoundId == rec.recommendedSoundId) stringResource(R.string.selected) else stringResource(R.string.applySound),
                                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                                    )
                                                }

                                                IconButton(
                                                    onClick = {
                                                        viewModel.emitButtonClickHaptic()
                                                        viewModel.previewSound(rec.recommendedSoundId, context)
                                                    },
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .background(if (isDarkMode) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f), CircleShape)
                                                ) {
                                                    Icon(
                                                        Icons.AutoMirrored.Filled.VolumeUp,
                                                        contentDescription = stringResource(R.string.previewRecommendedSound),
                                                        tint = textColor,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            color = if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        // Interval haptic configurations
                        Column(modifier = Modifier.padding(vertical = 12.dp)) {
                            Text(
                                text = stringResource(R.string.hapticAlerts),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = textColor
                                )
                            )
                            Text(
                                text = stringResource(R.string.hapticAlertsDesc),
                                style = MaterialTheme.typography.bodySmall.copy(color = secondaryTextColor),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                             ) {
                                listOf(
                                    0 to stringResource(R.string.hapticOff),
                                    5 to stringResource(R.string.haptic5m),
                                    10 to stringResource(R.string.haptic10m)
                                ).forEach { (mins, label) ->
                                    val isSelected = intervalHapticMinutes == mins
                                    Button(
                                        onClick = { viewModel.setIntervalHaptic(mins, context) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) theme.wedgeColor else (if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f)),
                                            contentColor = if (isSelected) (if (isDarkMode) Color.Black else Color.White) else textColor
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                    }
                                }
                            }
                        }

                    }
                }
            }
        }

            // Quick Preset Edit Dialog
            editingPresetIndex?.let { index ->
                if (index in quickPresets.indices) {
                    EditPresetDialog(
                        initialMins = quickPresets[index],
                        theme = theme,
                        isDarkMode = isDarkMode,
                        onDismiss = { editingPresetIndex = null },
                        onSave = { newMins ->
                            viewModel.updatePreset(index, newMins, context)
                        }
                    )
                }
            }

            // Modal Sessions View Sheet with Summary Dashboard
            if (showSessionsSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showSessionsSheet = false },
                    containerColor = if (isDarkMode) Color(0xFF1E1F22) else Color(0xFFF8FAFC),
                    contentColor = textColor
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        item {
                            SummaryDashboard(
                                sessions = roomSessions,
                                theme = theme,
                                isDarkMode = isDarkMode,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

        if (showShareDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissShareDialog() },
                title = { Text(stringResource(R.string.shareDialogTitle)) },
                text = { Text(stringResource(R.string.shareDialogBody)) },
                confirmButton = {
                    TextButton(onClick = {
                        try {
                            val sendIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(android.content.Intent.EXTRA_TEXT, "I use ZenWedge to stay focused! Try it out.")
                                type = "text/plain"
                            }
                            val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                            context.startActivity(shareIntent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        viewModel.dismissShareDialog(context, true)
                    }) {
                        Text(stringResource(R.string.shareButton), color = theme.wedgeColor)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissShareDialog() }) {
                        Text(stringResource(R.string.maybeLater), color = secondaryTextColor)
                    }
                },
                containerColor = if (isDarkMode) Color(0xFF1E1F22) else Color(0xFFF8FAFC),
                titleContentColor = textColor,
                textContentColor = secondaryTextColor
            )
        }
        
        if (showRatingDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissRatingDialog() },
                title = { Text(stringResource(R.string.rateDialogTitle)) },
                text = { Text(stringResource(R.string.rateDialogBody)) },
                confirmButton = {
                    TextButton(onClick = {
                        try {
                            context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=${context.packageName}")))
                        } catch (e: Exception) {
                            context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")))
                        }
                        viewModel.dismissRatingDialog(context, true)
                    }) {
                        Text(stringResource(R.string.rateButton), color = theme.wedgeColor)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissRatingDialog() }) {
                        Text(stringResource(R.string.maybeLater), color = secondaryTextColor)
                    }
                },
                containerColor = if (isDarkMode) Color(0xFF1E1F22) else Color(0xFFF8FAFC),
                titleContentColor = textColor,
                textContentColor = secondaryTextColor
            )
        }
        
        if (showUpdateDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissUpdateDialog() },
                title = { Text(stringResource(R.string.updateDialogTitle)) },
                text = { Text(stringResource(R.string.updateDialogBody)) },
                confirmButton = {
                    TextButton(onClick = {
                        try {
                            context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=${context.packageName}")))
                        } catch (e: Exception) {
                            context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")))
                        }
                        viewModel.dismissUpdateDialog()
                    }) {
                        Text(stringResource(R.string.updateButton), color = theme.wedgeColor)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissUpdateDialog() }) {
                        Text(stringResource(R.string.maybeLater), color = secondaryTextColor)
                    }
                },
                containerColor = if (isDarkMode) Color(0xFF1E1F22) else Color(0xFFF8FAFC),
                titleContentColor = textColor,
                textContentColor = secondaryTextColor
            )
        }
}

@Composable
fun WedgeDial(
    remainingSeconds: Int,
    totalSeconds: Int,
    timerState: TimerState,
    theme: WedgeTheme,
    isDarkMode: Boolean = true,
    isDynamicColorShift: Boolean = true,
    onAngleChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val displayMin = remainingSeconds / 60
    val displaySec = remainingSeconds % 60

    val targetFraction = if (totalSeconds > 0) {
        remainingSeconds.toFloat() / totalSeconds.toFloat()
    } else {
        0f
    }

    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        label = "wedgeFractionAnimation"
    )

    // Calculate dynamic wedge color interpolating smoothly from Green -> Yellow -> Red as remaining fraction drops
    val rawWedgeColor = if (isDynamicColorShift && (timerState == TimerState.RUNNING || timerState == TimerState.PAUSED)) {
        val green = Color(0xFF4CAF50)
        val yellow = Color(0xFFFFC107)
        val red = Color(0xFFE53935)
        if (animatedFraction >= 0.5f) {
            val t = (1.0f - animatedFraction) * 2f
            androidx.compose.ui.graphics.lerp(green, yellow, t.coerceIn(0f, 1f))
        } else {
            val t = (0.5f - animatedFraction) * 2f
            androidx.compose.ui.graphics.lerp(yellow, red, t.coerceIn(0f, 1f))
        }
    } else {
        theme.wedgeColor
    }

    val activeWedgeColor by animateColorAsState(
        targetValue = rawWedgeColor,
        animationSpec = tween(durationMillis = 600),
        label = "wedgeColorFade"
    )

    val dialBgColor by animateColorAsState(
        targetValue = theme.getDialBg(isDarkMode),
        animationSpec = tween(600),
        label = "dialBgFade"
    )

    val hubBgColor by animateColorAsState(
        targetValue = theme.getBgColor(isDarkMode),
        animationSpec = tween(600),
        label = "hubBgFade"
    )

    val textColor = theme.getTextColor(isDarkMode)
    val secondaryTextColor = theme.getSecondaryTextColor(isDarkMode)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .fillMaxWidth(0.82f)
            .testTag("wedge_dial_container"),
        contentAlignment = Alignment.Center
    ) {
        // Decorative background elements & swipe controller
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(timerState) {
                    // Only support setting/changing time if timer is idle, completed
                    if (timerState == TimerState.IDLE || timerState == TimerState.COMPLETED) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val dragPosition = change.position
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val angleRad = atan2(dragPosition.y - center.y, dragPosition.x - center.x)
                            var angleDeg = Math.toDegrees(angleRad.toDouble()).toFloat() + 90f
                            if (angleDeg < 0) angleDeg += 360f
                            onAngleChanged(angleDeg)
                        }
                    }
                }
                .pointerInput(timerState) {
                    if (timerState == TimerState.IDLE || timerState == TimerState.COMPLETED) {
                        detectTapGestures { tapOffset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val angleRad = atan2(tapOffset.y - center.y, tapOffset.x - center.x)
                            var angleDeg = Math.toDegrees(angleRad.toDouble()).toFloat() + 90f
                            if (angleDeg < 0) angleDeg += 360f
                            onAngleChanged(angleDeg)
                        }
                    }
                }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f

            // Outer ring border decoration
            drawCircle(
                color = if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f),
                radius = radius,
                style = Stroke(width = 1.dp.toPx())
            )

            // Draw circular dial background
            drawCircle(
                color = dialBgColor,
                radius = radius * 0.98f
            )

            // Draw full wedge / dynamic remaining colored arc
            val sweepAngle = animatedFraction * 360f
            drawArc(
                color = activeWedgeColor.copy(alpha = 0.88f),
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = true,
                size = Size(radius * 2 * 0.9f, radius * 2 * 0.9f),
                topLeft = Offset(center.x - radius * 0.9f, center.y - radius * 0.9f)
            )

            // Minimalist dial markers (ticks) at 12, 3, 6, 9 o'clock
            val tickLength = 10.dp.toPx()
            val tickStroke = 2.dp.toPx()
            val tickColor = if (isDarkMode) Color.White.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.3f)

            // 12 o'clock
            drawLine(
                color = tickColor,
                start = Offset(center.x, center.y - radius + 8.dp.toPx()),
                end = Offset(center.x, center.y - radius + 8.dp.toPx() + tickLength),
                strokeWidth = tickStroke
            )
            // 3 o'clock
            drawLine(
                color = tickColor.copy(alpha = 0.15f),
                start = Offset(center.x + radius - 8.dp.toPx() - tickLength, center.y),
                end = Offset(center.x + radius - 8.dp.toPx(), center.y),
                strokeWidth = tickStroke
            )
            // 6 o'clock
            drawLine(
                color = tickColor.copy(alpha = 0.15f),
                start = Offset(center.x, center.y + radius - 8.dp.toPx() - tickLength),
                end = Offset(center.x, center.y + radius - 8.dp.toPx()),
                strokeWidth = tickStroke
            )
            // 9 o'clock
            drawLine(
                color = tickColor.copy(alpha = 0.15f),
                start = Offset(center.x - radius + 8.dp.toPx(), center.y),
                end = Offset(center.x - radius + 8.dp.toPx() + tickLength, center.y),
                strokeWidth = tickStroke
            )
        }

        // Inner Hub cutout centering the remaining time nicely
        Box(
            modifier = Modifier
                .fillMaxSize(0.72f)
                .clip(CircleShape)
                .background(hubBgColor)
                .shadow(elevation = 12.dp, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (timerState == TimerState.RUNNING || timerState == TimerState.PAUSED) {
                    // Running/Paused detailed countdown
                    Text(
                        text = String.format(Locale.getDefault(), "%02d:%02d", displayMin, displaySec),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Light,
                            color = textColor,
                            letterSpacing = (-1.5).sp
                        )
                    )
                    Text(
                        text = if (timerState == TimerState.RUNNING) stringResource(R.string.focusState) else stringResource(R.string.pausedState),
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = theme.accentColor,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    )
                } else if (timerState == TimerState.COMPLETED) {
                    Text(
                        text = stringResource(R.string.completedTitle),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Normal,
                            color = theme.wedgeColor,
                            letterSpacing = (-0.5).sp
                        )
                    )
                    Text(
                        text = stringResource(R.string.completedSubtitle),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = secondaryTextColor,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp
                        )
                    )
                } else {
                    // Idle state showing set duration
                    Text(
                        text = displayMin.toString(),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Light,
                            color = textColor,
                            letterSpacing = (-2).sp
                        )
                    )
                    Text(
                        text = if (displayMin == 1) stringResource(R.string.minuteSingular) else stringResource(R.string.minutePlural),
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = secondaryTextColor,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun EditPresetDialog(
    initialMins: Int,
    theme: WedgeTheme,
    isDarkMode: Boolean,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var textValue by remember { mutableStateOf(initialMins.toString()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val enterValueErrorStr = stringResource(R.string.enterValueBetween)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.editQuickPreset),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.enterDurationMinutes),
                    style = MaterialTheme.typography.bodySmall,
                    color = theme.getSecondaryTextColor(isDarkMode),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { input ->
                        textValue = input.filter { it.isDigit() }
                        errorMessage = null
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = {
                        errorMessage?.let { msg ->
                            Text(text = msg, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_preset_textfield")
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsed = textValue.toIntOrNull()
                    if (parsed != null && parsed in 1..180) {
                        onSave(parsed)
                        onDismiss()
                    } else {
                        errorMessage = enterValueErrorStr
                    }
                }
            ) {
                Text(stringResource(R.string.save), color = theme.wedgeColor, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = theme.getSecondaryTextColor(isDarkMode))
            }
        },
        containerColor = if (isDarkMode) Color(0xFF1E1F22) else Color(0xFFF8FAFC),
        titleContentColor = theme.getTextColor(isDarkMode),
        textContentColor = theme.getTextColor(isDarkMode)
    )
}
