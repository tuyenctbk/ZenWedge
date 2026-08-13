package com.example.ui.timer

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    isDarkMode: Boolean = true,
    theme: WedgeTheme = WedgeTheme.CALM_TEAL,
    modifier: Modifier = Modifier
) {
    var currentPage by remember { mutableIntStateOf(0) }
    val totalPages = 3

    val bgColor = theme.getBgColor(isDarkMode)
    val textColor = theme.getTextColor(isDarkMode)
    val secondaryText = theme.getSecondaryTextColor(isDarkMode)

    Scaffold(
        modifier = modifier.testTag("onboarding_scaffold"),
        containerColor = bgColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = theme.wedgeColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ZenWedge",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    )
                }

                Text(
                    text = "${currentPage + 1} / $totalPages",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = secondaryText,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            // Central Dynamic Art Visualizer
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                when (currentPage) {
                    0 -> WedgeAnimationVisual(theme = theme, isDarkMode = isDarkMode)
                    1 -> SoundWaveVisual(theme = theme, isDarkMode = isDarkMode)
                    2 -> PerformanceStatsVisual(theme = theme, isDarkMode = isDarkMode)
                }
            }

            // Description and Title Text
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val titleRes = when (currentPage) {
                    0 -> R.string.onboardingWelcomeTitle
                    1 -> R.string.onboardingSoundsTitle
                    else -> R.string.onboardingStatsTitle
                }
                val descRes = when (currentPage) {
                    0 -> R.string.onboardingWelcomeDesc
                    1 -> R.string.onboardingSoundsDesc
                    else -> R.string.onboardingStatsDesc
                }

                Text(
                    text = stringResource(titleRes),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = textColor,
                        textAlign = TextAlign.Center,
                        lineHeight = 34.sp
                    ),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Text(
                    text = stringResource(descRes),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = secondaryText,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(bottom = 24.dp)
                )

                // Highlighting Specific App Sub-Features
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(textColor.copy(alpha = 0.04f))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val featureIcon = when (currentPage) {
                        0 -> Icons.Default.HourglassTop
                        1 -> Icons.Default.AutoAwesome
                        else -> Icons.Default.Leaderboard
                    }
                    val featureTitle = when (currentPage) {
                        0 -> R.string.onboardingSlide1Feature
                        1 -> R.string.onboardingSlide2Feature
                        else -> R.string.onboardingSlide3Feature
                    }
                    val featureDesc = when (currentPage) {
                        0 -> R.string.onboardingSlide1FeatureDesc
                        1 -> R.string.onboardingSlide2FeatureDesc
                        else -> R.string.onboardingSlide3FeatureDesc
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(theme.wedgeColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = featureIcon,
                            contentDescription = null,
                            tint = theme.wedgeColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(featureTitle),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(featureDesc),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = secondaryText,
                                lineHeight = 16.sp
                            )
                        )
                    }
                }
            }

            // Bottom Navigation Indicators & Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button or spacer
                if (currentPage > 0) {
                    TextButton(
                        onClick = { currentPage-- },
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("onboarding_back_button"),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.onboardingBack),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = secondaryText
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(80.dp))
                }

                // Page Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    repeat(totalPages) { index ->
                        val active = index == currentPage
                        val width by animateDpAsState(
                            targetValue = if (active) 20.dp else 8.dp,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "dot_width"
                        )
                        val color by animateColorAsState(
                            targetValue = if (active) theme.wedgeColor else textColor.copy(alpha = 0.2f),
                            label = "dot_color"
                        )

                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .height(8.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }

                // Next or Get Started Button
                val isLastPage = currentPage == totalPages - 1
                Button(
                    onClick = {
                        if (isLastPage) {
                            onFinished()
                        } else {
                            currentPage++
                        }
                    },
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("onboarding_next_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = theme.wedgeColor,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp)
                ) {
                    if (isLastPage) {
                        Text(
                            text = stringResource(R.string.onboardingGetStarted),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.onboardingNext),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WedgeAnimationVisual(
    theme: WedgeTheme,
    isDarkMode: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wedge_rotation")
    val angle by infiniteTransition.animateFloat(
        initialValue = -35f,
        targetValue = 35f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "angle_anim"
    )

    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale_pulse"
    )

    val ringColor = if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
    val tickColor = if (isDarkMode) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.2f)

    Canvas(
        modifier = Modifier
            .size(240.dp)
            .aspectRatio(1f)
    ) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = (size.width / 2) * scalePulse

        // Draw background dial tracks
        drawCircle(
            color = ringColor,
            radius = radius,
            center = center,
            style = Stroke(width = 4.dp.toPx())
        )

        drawCircle(
            color = ringColor,
            radius = radius - 16.dp.toPx(),
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )

        // Draw ticks
        val tickCount = 12
        for (i in 0 until tickCount) {
            val tickAngle = (i * 360f / tickCount) * (Math.PI / 180f)
            val outerPoint = Offset(
                center.x + radius * cos(tickAngle).toFloat(),
                center.y + radius * sin(tickAngle).toFloat()
            )
            val innerPoint = Offset(
                center.x + (radius - 10.dp.toPx()) * cos(tickAngle).toFloat(),
                center.y + (radius - 10.dp.toPx()) * sin(tickAngle).toFloat()
            )
            drawLine(
                color = tickColor,
                start = innerPoint,
                end = outerPoint,
                strokeWidth = 2.dp.toPx()
            )
        }

        // Draw rotating sensory focus wedge
        rotate(degrees = angle, pivot = center) {
            // Draw a wedge slice representing our elegant focus segment
            drawArc(
                brush = Brush.radialGradient(
                    colors = listOf(theme.wedgeColor, theme.wedgeColor.copy(alpha = 0.4f)),
                    center = center,
                    radius = radius
                ),
                startAngle = -115f,
                sweepAngle = 50f,
                useCenter = true,
                size = Size(radius * 2, radius * 2),
                topLeft = Offset(center.x - radius, center.y - radius)
            )

            // Wedge spine highlight line
            val activeAngleRad = (-90f) * (Math.PI / 180f)
            val spineTip = Offset(
                center.x + radius * cos(activeAngleRad).toFloat(),
                center.y + radius * sin(activeAngleRad).toFloat()
            )
            drawLine(
                color = theme.accentColor,
                start = center,
                end = spineTip,
                strokeWidth = 3.dp.toPx()
            )
        }

        // Elegant inner cap
        drawCircle(
            color = if (isDarkMode) Color(0xFF1E1F22) else Color.White,
            radius = 32.dp.toPx(),
            center = center
        )

        drawCircle(
            color = theme.wedgeColor,
            radius = 16.dp.toPx(),
            center = center
        )
    }
}

@Composable
fun SoundWaveVisual(
    theme: WedgeTheme,
    isDarkMode: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sound_waves")
    
    val wave1Progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave1"
    )

    val wave2Progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave2"
    )

    // Offset wave 2 starts
    val delayedWave2 = (wave2Progress + 0.5f) % 1.0f

    val bowlScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bowl_scale"
    )

    Canvas(
        modifier = Modifier
            .size(240.dp)
            .aspectRatio(1f)
    ) {
        val center = Offset(size.width / 2, size.height / 2)
        val maxWaveRadius = size.width / 2

        // Wave 1 ring
        drawCircle(
            color = theme.wedgeColor.copy(alpha = (1f - wave1Progress) * 0.4f),
            radius = maxWaveRadius * wave1Progress,
            center = center,
            style = Stroke(width = 3.dp.toPx() * (1f - wave1Progress))
        )

        // Wave 2 ring
        drawCircle(
            color = theme.accentColor.copy(alpha = (1f - delayedWave2) * 0.4f),
            radius = maxWaveRadius * delayedWave2,
            center = center,
            style = Stroke(width = 3.dp.toPx() * (1f - delayedWave2))
        )

        // Center visual sound bowl or chime node
        val bowlRadius = 42.dp.toPx() * bowlScale
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    theme.wedgeColor,
                    theme.wedgeColor.copy(alpha = 0.8f),
                    Color.Transparent
                ),
                center = center,
                radius = bowlRadius * 1.5f
            ),
            radius = bowlRadius * 1.5f,
            center = center
        )

        // Drawn bowl ring
        drawCircle(
            color = theme.accentColor,
            radius = bowlRadius,
            center = center,
            style = Stroke(width = 5.dp.toPx())
        )

        drawCircle(
            color = if (isDarkMode) Color(0xFF1E1F22) else Color.White,
            radius = bowlRadius - 6.dp.toPx(),
            center = center
        )

        // Center resonance core
        drawCircle(
            color = theme.wedgeColor,
            radius = bowlRadius / 2,
            center = center
        )
    }
}

@Composable
fun PerformanceStatsVisual(
    theme: WedgeTheme,
    isDarkMode: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "stats_bars")
    val scale1 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )

    val scale2 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )

    val scale3 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )

    val scale4 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar4"
    )

    val gridColor = if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f)
    val secondaryText = theme.getSecondaryTextColor(isDarkMode)

    Canvas(
        modifier = Modifier
            .size(240.dp)
            .aspectRatio(1f)
    ) {
        val width = size.width
        val height = size.height

        // Draw beautiful grid guidelines
        val linesCount = 4
        for (i in 0..linesCount) {
            val y = (height / linesCount) * i
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Draw 4 aesthetic bar charts
        val barSpacing = 20.dp.toPx()
        val totalSpacing = barSpacing * 5
        val barWidth = (width - totalSpacing) / 4

        val barHeights = listOf(scale1, scale2, scale3, scale4)
        val colors = listOf(
            theme.wedgeColor,
            theme.accentColor,
            theme.wedgeColor.copy(alpha = 0.7f),
            theme.accentColor.copy(alpha = 0.8f)
        )

        for (i in 0..3) {
            val left = barSpacing + (barWidth + barSpacing) * i
            val barMaxHeight = height * 0.75f
            val currentBarHeight = barMaxHeight * barHeights[i]
            val top = height - currentBarHeight

            // Draw shadow or glow behind each bar
            drawRoundRect(
                color = colors[i].copy(alpha = 0.12f),
                topLeft = Offset(left - 4.dp.toPx(), top - 4.dp.toPx()),
                size = Size(barWidth + 8.dp.toPx(), currentBarHeight + 4.dp.toPx()),
                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
            )

            // Draw main bar
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(colors[i], colors[i].copy(alpha = 0.5f))
                ),
                topLeft = Offset(left, top),
                size = Size(barWidth, currentBarHeight),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
            )

            // Draw a cute dot highlight on top of the bar
            drawCircle(
                color = Color.White.copy(alpha = 0.8f),
                radius = 3.dp.toPx(),
                center = Offset(left + barWidth / 2, top + 6.dp.toPx())
            )
        }
    }
}
