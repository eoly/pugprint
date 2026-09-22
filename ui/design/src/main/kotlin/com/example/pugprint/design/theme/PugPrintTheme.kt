package com.example.pugprint.design.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp

/** The [PugTheme] in force, for kit components that need more than Material exposes. */
val LocalPugTheme = staticCompositionLocalOf { ThemeCatalog.default }

/**
 * Dresses [content] in [theme]: a single light Material scheme built from the palette (no dark
 * mode — one predictable look for a kid), the theme's roundness and typeface.
 */
@Composable
fun PugPrintTheme(
    theme: PugTheme = ThemeCatalog.default,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalPugTheme provides theme) {
        MaterialTheme(
            colorScheme = theme.palette.toColorScheme(),
            shapes = shapesFor(theme.roundness),
            typography = typographyFor(theme.fontFamily),
            content = content,
        )
    }
}

private fun PugPalette.toColorScheme() =
    lightColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primarySoft,
        onPrimaryContainer = text,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = card,
        onSecondaryContainer = text,
        background = background,
        onBackground = text,
        surface = background,
        onSurface = text,
        surfaceVariant = card,
        onSurfaceVariant = textSoft,
        surfaceContainer = card,
        surfaceContainerHigh = card,
        surfaceContainerHighest = card,
        outline = outline,
        outlineVariant = outline,
        error = error,
        onError = onPrimary,
        errorContainer = errorSoft,
        onErrorContainer = text,
    )

private const val QUARTER = 0.25f
private const val HALF = 0.5f
private const val THREE_QUARTERS = 0.75f
private const val ONE_AND_A_HALF = 1.5f

/** Small things a little less round than big things, all driven by one number. */
private fun shapesFor(roundness: Dp) =
    Shapes(
        extraSmall = RoundedCornerShape(roundness * QUARTER),
        small = RoundedCornerShape(roundness * HALF),
        medium = RoundedCornerShape(roundness * THREE_QUARTERS),
        large = RoundedCornerShape(roundness),
        extraLarge = RoundedCornerShape(roundness * ONE_AND_A_HALF),
    )

/** Material's sizes, in the theme's typeface, with bolder titles so headings read from across a table. */
private fun typographyFor(fontFamily: FontFamily): Typography {
    val base = Typography()

    fun TextStyle.themed(weight: FontWeight? = null) =
        copy(
            fontFamily = fontFamily,
            fontWeight =
                weight ?: this.fontWeight,
        )
    return Typography(
        displayLarge = base.displayLarge.themed(FontWeight.Bold),
        displayMedium = base.displayMedium.themed(FontWeight.Bold),
        displaySmall = base.displaySmall.themed(FontWeight.Bold),
        headlineLarge = base.headlineLarge.themed(FontWeight.Bold),
        headlineMedium = base.headlineMedium.themed(FontWeight.Bold),
        headlineSmall = base.headlineSmall.themed(FontWeight.Bold),
        titleLarge = base.titleLarge.themed(FontWeight.SemiBold),
        titleMedium = base.titleMedium.themed(FontWeight.SemiBold),
        titleSmall = base.titleSmall.themed(),
        bodyLarge = base.bodyLarge.themed(),
        bodyMedium = base.bodyMedium.themed(),
        bodySmall = base.bodySmall.themed(),
        labelLarge = base.labelLarge.themed(FontWeight.SemiBold),
        labelMedium = base.labelMedium.themed(),
        labelSmall = base.labelSmall.themed(),
    )
}
