package com.example.pugprint.ui.coloring

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.pugprint.design.theme.PugPrintTheme
import com.example.pugprint.imaging.LabelShape
import com.example.pugprint.ui.Screenshots
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class ColoringScreenScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private fun snap(
        state: ColoringUiState,
        bigText: Boolean = false,
    ) {
        compose.setContent {
            PugPrintTheme {
                if (bigText) Screenshots.BigText { ColoringScreen(state = state) } else ColoringScreen(state = state)
            }
        }
        compose.onRoot().captureRoboImage(roborazziOptions = Screenshots.options)
    }

    @Test
    fun coloringScreen_pages_bigText() = snap(ColoringUiState(), bigText = true)

    @Test
    fun coloringScreen_pages() = snap(ColoringUiState())

    @Test
    fun coloringScreen_roundRoll() = snap(ColoringUiState(labelShape = LabelShape.CIRCLE))
}
