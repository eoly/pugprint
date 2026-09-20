package com.example.pugprint.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors =
    lightColorScheme(
        primary = Color(0xFF8C5A2B),
        onPrimary = Color.White,
        secondary = Color(0xFFE0A458),
        background = Color(0xFFFFF8EE),
        surface = Color(0xFFFFF8EE),
    )

/** Warm, high-contrast palette; a single light scheme keeps the kid-facing UI predictable. */
@Composable
fun PugPrintTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, content = content)
}
