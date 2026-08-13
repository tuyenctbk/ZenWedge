package com.example.ui.timer

import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.db.FocusSessionEntity
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SummaryDashboard(
    sessions: List<FocusSessionEntity>,
    theme: WedgeTheme,
    isDarkMode: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isWeeklyMode by remember { mutableStateOf(false) }

    val textColor = theme.getTextColor(isDarkMode)
    val secondaryText = theme.getSecondaryTextColor(isDarkMode)
    val cardBg = theme.getCardBg(isDarkMode)
    val borderCol = theme.getBorderColor(isDarkMode)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("summary_dashboard")
    ) {
        // Dashboard Header with Mode Selector (Daily vs Weekly)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    tint = theme.wedgeColor,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = stringResource(R.string.focusSummaryDashboard),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                )
            }

            // Segmented Toggle Button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f))
                    .padding(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (!isWeeklyMode) theme.wedgeColor else Color.Transparent)
                        .clickable { isWeeklyMode = false }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.daily),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (!isWeeklyMode) Color.White else secondaryText
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isWeeklyMode) theme.wedgeColor else Color.Transparent)
                        .clickable { isWeeklyMode = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.weekly),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isWeeklyMode) Color.White else secondaryText
                        )
                    )
                }
            }
        }

        // Analytics KPI Summary Cards
        val totalMins = sessions.sumOf { it.durationMinutes }
        val sessionCount = sessions.size
        val avgMins = if (sessionCount > 0) totalMins / sessionCount else 0

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        ) {
            KPICard(
                title = stringResource(R.string.kpiTotalTime),
                value = formatMinutesToHours(totalMins),
                icon = Icons.Default.Schedule,
                theme = theme,
                isDarkMode = isDarkMode,
                modifier = Modifier.weight(1f)
            )
            KPICard(
                title = stringResource(R.string.kpiSessions),
                value = stringResource(R.string.completedSuffixFormat, sessionCount),
                icon = Icons.Default.CheckCircle,
                theme = theme,
                isDarkMode = isDarkMode,
                modifier = Modifier.weight(1f)
            )
            KPICard(
                title = stringResource(R.string.kpiAvgSession),
                value = "${avgMins}m",
                icon = Icons.Default.AutoAwesome,
                theme = theme,
                isDarkMode = isDarkMode,
                modifier = Modifier.weight(1f)
            )
        }

        // Bar Chart Container
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBg),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, borderCol),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isWeeklyMode) stringResource(R.string.past4Weeks) else stringResource(R.string.dailyFocusBreakdown),
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = secondaryText,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (isWeeklyMode) {
                    WeeklyBarChart(sessions = sessions, theme = theme, isDarkMode = isDarkMode)
                } else {
                    DailyBarChart(sessions = sessions, theme = theme, isDarkMode = isDarkMode)
                }
            }
        }

        // Room Database Logged Session History List Header + Export CSV Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.roomDbSessionLogs, sessions.size),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            )

            if (sessions.isNotEmpty()) {
                OutlinedButton(
                    onClick = { exportSessionsToCSV(context, sessions) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = theme.wedgeColor
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, theme.wedgeColor.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("export_csv_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = stringResource(R.string.exportCsv),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.exportCsv),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.noSessionsLogged),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = secondaryText,
                        textAlign = TextAlign.Center
                    )
                )
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                sessions.take(15).forEach { session ->
                    SessionLogRow(session = session, theme = theme, isDarkMode = isDarkMode)
                }
            }
        }
    }
}

@Composable
fun KPICard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    theme: WedgeTheme,
    isDarkMode: Boolean = true,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = theme.getCardBg(isDarkMode)),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.getBorderColor(isDarkMode)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = theme.wedgeColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = theme.getSecondaryTextColor(isDarkMode),
                    fontSize = 10.sp
                )
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = theme.getTextColor(isDarkMode)
                )
            )
        }
    }
}

@Composable
fun DailyBarChart(
    sessions: List<FocusSessionEntity>,
    theme: WedgeTheme,
    isDarkMode: Boolean = true
) {
    val dayLabels = listOf(
        stringResource(R.string.mon),
        stringResource(R.string.tue),
        stringResource(R.string.wed),
        stringResource(R.string.thu),
        stringResource(R.string.fri),
        stringResource(R.string.sat),
        stringResource(R.string.sun)
    )
    val dailyMinutes = remember(sessions) {
        val cal = Calendar.getInstance()
        val totals = MutableList(7) { 0 }
        
        sessions.forEach { s ->
            cal.timeInMillis = s.timestamp
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            val index = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - 2
            if (index in 0..6) {
                totals[index] += s.durationMinutes
            }
        }
        totals
    }

    val maxVal = (dailyMinutes.maxOrNull() ?: 60).coerceAtLeast(30)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        dayLabels.forEachIndexed { index, label ->
            val mins = dailyMinutes[index]
            val heightRatio = if (maxVal > 0) mins.toFloat() / maxVal.toFloat() else 0f
            val animatedHeight by animateFloatAsState(
                targetValue = heightRatio,
                animationSpec = tween(durationMillis = 600),
                label = "barHeight_$index"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Text(
                    text = if (mins > 0) "${mins}m" else "",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        color = theme.wedgeColor,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .fillMaxHeight(0.75f * animatedHeight.coerceAtLeast(0.04f))
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(
                            if (mins == maxVal && mins > 0) {
                                Brush.verticalGradient(
                                    colors = listOf(theme.wedgeColor, theme.accentColor)
                                )
                            } else {
                                Brush.verticalGradient(
                                    colors = listOf(
                                        theme.wedgeColor.copy(alpha = if (mins > 0) 0.85f else 0.2f),
                                        theme.wedgeColor.copy(alpha = if (mins > 0) 0.45f else 0.08f)
                                    )
                                )
                            }
                        )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        color = theme.getSecondaryTextColor(isDarkMode),
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

@Composable
fun WeeklyBarChart(
    sessions: List<FocusSessionEntity>,
    theme: WedgeTheme,
    isDarkMode: Boolean = true
) {
    val weekLabels = listOf(
        stringResource(R.string.wk1),
        stringResource(R.string.wk2),
        stringResource(R.string.wk3),
        stringResource(R.string.thisWk)
    )
    val weeklyMinutes = remember(sessions) {
        val totals = MutableList(4) { 0 }
        val now = System.currentTimeMillis()
        val weekMs = 7 * 24 * 60 * 60 * 1000L

        sessions.forEach { s ->
            val diff = now - s.timestamp
            val weekIndex = 3 - (diff / weekMs).toInt()
            if (weekIndex in 0..3) {
                totals[weekIndex] += s.durationMinutes
            }
        }
        totals
    }

    val maxVal = (weeklyMinutes.maxOrNull() ?: 120).coerceAtLeast(60)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        weekLabels.forEachIndexed { index, label ->
            val mins = weeklyMinutes[index]
            val heightRatio = if (maxVal > 0) mins.toFloat() / maxVal.toFloat() else 0f
            val animatedHeight by animateFloatAsState(
                targetValue = heightRatio,
                animationSpec = tween(durationMillis = 600),
                label = "weeklyBarHeight_$index"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Text(
                    text = if (mins > 0) "${mins}m" else "",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        color = theme.wedgeColor,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .fillMaxHeight(0.75f * animatedHeight.coerceAtLeast(0.04f))
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    theme.wedgeColor.copy(alpha = if (mins > 0) 0.9f else 0.2f),
                                    theme.wedgeColor.copy(alpha = if (mins > 0) 0.4f else 0.08f)
                                )
                            )
                        )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        color = theme.getSecondaryTextColor(isDarkMode),
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

@Composable
fun SessionLogRow(
    session: FocusSessionEntity,
    theme: WedgeTheme,
    isDarkMode: Boolean = true
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    val formattedDate = remember(session.timestamp) { dateFormat.format(Date(session.timestamp)) }

    Card(
        colors = CardDefaults.cardColors(containerColor = theme.getCardBg(isDarkMode)),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, theme.getBorderColor(isDarkMode)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(theme.wedgeColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${session.durationMinutes}m",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = theme.wedgeColor
                        )
                    )
                }

                Column {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = theme.getTextColor(isDarkMode)
                        )
                    )
                    Text(
                        text = "${session.timeOfDay} • ${session.soundId.replace('_', ' ').replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = theme.getSecondaryTextColor(isDarkMode),
                            fontSize = 10.sp
                        )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(theme.wedgeColor.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = session.themeName,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        color = theme.wedgeColor,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

private fun exportSessionsToCSV(context: Context, sessions: List<FocusSessionEntity>) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val csvBuilder = StringBuilder()
    csvBuilder.append("ID,Duration_Minutes,Duration_Seconds,TimeOfDay,Timestamp,Date,Theme,Sound\n")

    sessions.forEach { s ->
        val dateStr = dateFormat.format(Date(s.timestamp))
        csvBuilder.append("${s.id},${s.durationMinutes},${s.durationSeconds},\"${s.timeOfDay}\",${s.timestamp},\"$dateStr\",\"${s.themeName}\",\"${s.soundId}\"\n")
    }

    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, csvBuilder.toString())
        putExtra(Intent.EXTRA_SUBJECT, "ZenWedge Focus Sessions History.csv")
        type = "text/plain"
    }
    try {
        val chooser = Intent.createChooser(shareIntent, "Share or Export Focus CSV")
        context.startActivity(chooser)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun formatMinutesToHours(minutes: Int): String {
    if (minutes < 60) return "${minutes}m"
    val hours = minutes / 60
    val remMins = minutes % 60
    return if (remMins == 0) "${hours}h" else "${hours}h ${remMins}m"
}

