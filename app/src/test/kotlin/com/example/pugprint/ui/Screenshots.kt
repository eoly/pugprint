package com.example.pugprint.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.RoborazziOptions

/** Shared Roborazzi settings for the screen goldens. */
object Screenshots {
    /**
     * Skia on macOS and Linux differ by a few dozen pixels in anti-aliased text and in
     * nearest-neighbour image sampling at fractional scales (82 px seen on CI for a disabled
     * button plus a hard-edged preview). 0.05 % of a 1233×2673 capture is about 1 600 px, so a
     * changed label, chip state or layout still fails.
     */
    private const val CHANGE_THRESHOLD = 0.0005f

    val options: RoborazziOptions =
        RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = CHANGE_THRESHOLD))

    /** Android's "Largest" font size; goldens taken at this scale catch clipped and truncated labels. */
    const val BIG_FONT_SCALE = 1.5f

    /** Renders [content] as a user who set the system font to [BIG_FONT_SCALE] would see it. */
    @Composable
    fun BigText(content: @Composable () -> Unit) {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, BIG_FONT_SCALE), content = content)
    }
}
