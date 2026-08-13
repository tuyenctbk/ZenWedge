package com.example.ui.timer

import androidx.compose.ui.graphics.Color

enum class WedgeTheme(
    val themeName: String,
    val wedgeColor: Color,
    val darkBackgroundColor: Color,
    val lightBackgroundColor: Color,
    val accentColor: Color,
    val darkDialBackground: Color,
    val lightDialBackground: Color
) {
    CALM_TEAL(
        themeName = "Calm Teal",
        wedgeColor = Color(0xFF0D9488),
        darkBackgroundColor = Color(0xFF121316),
        lightBackgroundColor = Color(0xFFF8FAFC),
        accentColor = Color(0xFF2DD4BF),
        darkDialBackground = Color(0xFF1A1C20),
        lightDialBackground = Color(0xFFE2E8F0)
    ),
    CRIMSON_RED(
        themeName = "Crimson Red",
        wedgeColor = Color(0xFFDC2626),
        darkBackgroundColor = Color(0xFF121316),
        lightBackgroundColor = Color(0xFFFFF1F2),
        accentColor = Color(0xFFF87171),
        darkDialBackground = Color(0xFF1D1414),
        lightDialBackground = Color(0xFFFFE4E6)
    ),
    WARM_AMBER(
        themeName = "Warm Amber",
        wedgeColor = Color(0xFFD97706),
        darkBackgroundColor = Color(0xFF121316),
        lightBackgroundColor = Color(0xFFFFFBEB),
        accentColor = Color(0xFFFBBF24),
        darkDialBackground = Color(0xFF1E1911),
        lightDialBackground = Color(0xFFFEF3C7)
    ),
    ZEN_PURPLE(
        themeName = "Zen Purple",
        wedgeColor = Color(0xFF7C3AED),
        darkBackgroundColor = Color(0xFF0F0E17),
        lightBackgroundColor = Color(0xFFF5F3FF),
        accentColor = Color(0xFFA78BFA),
        darkDialBackground = Color(0xFF181628),
        lightDialBackground = Color(0xFFEDE9FE)
    );

    fun getBgColor(isDark: Boolean): Color = if (isDark) darkBackgroundColor else lightBackgroundColor
    fun getDialBg(isDark: Boolean): Color = if (isDark) darkDialBackground else lightDialBackground
    fun getTextColor(isDark: Boolean): Color = if (isDark) Color.White else Color(0xFF0F172A)
    fun getSecondaryTextColor(isDark: Boolean): Color = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF475569)
    fun getCardBg(isDark: Boolean): Color = if (isDark) Color.White.copy(alpha = 0.06f) else Color(0xFFFFFFFF)
    fun getBorderColor(isDark: Boolean): Color = if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFCBD5E1)
}

