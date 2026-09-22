package com.example.pugprint.ui

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
}
