package com.example.pugprint.design.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Gaps between things. Screens use these instead of raw `dp` so spacing changes in one place. */
object PugSpacing {
    val tiny: Dp = 4.dp
    val small: Dp = 8.dp
    val medium: Dp = 16.dp
    val large: Dp = 24.dp
    val huge: Dp = 32.dp
}

/** How big things a kid taps must be. Nothing tappable in the app is smaller than [minimum]. */
object PugTouch {
    /** The one big thing to do on a screen. */
    val primary: Dp = 64.dp

    /** Other things to do. */
    val secondary: Dp = 56.dp

    /** Small links and back buttons; Android's floor is 48 dp. */
    val minimum: Dp = 48.dp
}

/** Screen-level sizes. */
object PugLayout {
    /** Content stays this wide on tablets so buttons keep a hand-sized shape. */
    val maxContentWidth: Dp = 480.dp

    /** Space between the content and the screen edge. */
    val screenPadding: Dp = PugSpacing.large

    /** The pug logo above a hero title. */
    val heroLogo: Dp = 128.dp

    /** The pug logo tucked into a header or a row. */
    val smallLogo: Dp = 56.dp
}
