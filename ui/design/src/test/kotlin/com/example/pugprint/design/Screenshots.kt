package com.example.pugprint.design

import com.github.takahirom.roborazzi.RoborazziOptions

/** Shared Roborazzi settings; see `app/src/test/.../Screenshots.kt` for why 0.05 %. */
object Screenshots {
    private const val CHANGE_THRESHOLD = 0.0005f

    val options: RoborazziOptions =
        RoborazziOptions(compareOptions = RoborazziOptions.CompareOptions(changeThreshold = CHANGE_THRESHOLD))
}
