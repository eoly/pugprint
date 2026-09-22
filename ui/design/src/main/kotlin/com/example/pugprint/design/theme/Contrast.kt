package com.example.pugprint.design.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.pow

/** WCAG 2 contrast maths, so every theme is checked for readability before it ships. */
object Contrast {
    /** Normal-size text (WCAG AA). */
    const val AA_TEXT: Double = 4.5

    /** Large text and UI outlines (WCAG AA). */
    const val AA_LARGE: Double = 3.0

    private const val LINEAR_CUTOFF = 0.03928
    private const val LINEAR_DIVISOR = 12.92
    private const val GAMMA_OFFSET = 0.055
    private const val GAMMA_DIVISOR = 1.055
    private const val GAMMA = 2.4
    private const val RED_WEIGHT = 0.2126
    private const val GREEN_WEIGHT = 0.7152
    private const val BLUE_WEIGHT = 0.0722
    private const val VIEWING_FLARE = 0.05

    /** Contrast ratio between two opaque colours, 1.0 (same) to 21.0 (black on white). */
    fun ratio(
        foreground: Color,
        background: Color,
    ): Double {
        val lighter = maxOf(relativeLuminance(foreground), relativeLuminance(background))
        val darker = minOf(relativeLuminance(foreground), relativeLuminance(background))
        return (lighter + VIEWING_FLARE) / (darker + VIEWING_FLARE)
    }

    /** Relative luminance of an sRGB colour (0.0 black to 1.0 white). Alpha is ignored. */
    fun relativeLuminance(color: Color): Double =
        RED_WEIGHT * linear(color.red) + GREEN_WEIGHT * linear(color.green) + BLUE_WEIGHT * linear(color.blue)

    private fun linear(channel: Float): Double {
        val c = channel.toDouble()
        return if (c <= LINEAR_CUTOFF) c / LINEAR_DIVISOR else ((c + GAMMA_OFFSET) / GAMMA_DIVISOR).pow(GAMMA)
    }
}
