package com.example.ui.timer

import androidx.compose.ui.graphics.Color

enum class WedgeTheme(
    val themeName: String,
    val wedgeColor: Color,
    val backgroundColor: Color,
    val accentColor: Color,
    val dialBackground: Color
) {
    CALM_TEAL(
        themeName = "Calm Teal",
        wedgeColor = Color(0xFF14B8A6),
        backgroundColor = Color(0xFF121316),
        accentColor = Color(0xFF2DD4BF),
        dialBackground = Color(0xFF1A1C20)
    ),
    CRIMSON_RED(
        themeName = "Crimson Red",
        wedgeColor = Color(0xFFEF4444),
        backgroundColor = Color(0xFF121316),
        accentColor = Color(0xFFF87171),
        dialBackground = Color(0xFF1D1414)
    ),
    WARM_AMBER(
        themeName = "Warm Amber",
        wedgeColor = Color(0xFFF59E0B),
        backgroundColor = Color(0xFF121316),
        accentColor = Color(0xFFFBBF24),
        dialBackground = Color(0xFF1E1911)
    ),
    OLED_DARK(
        themeName = "OLED Dark",
        wedgeColor = Color(0xFFF8FAFC),
        backgroundColor = Color(0xFF000000),
        accentColor = Color(0xFF94A3B8),
        dialBackground = Color(0xFF121212)
    )
}
