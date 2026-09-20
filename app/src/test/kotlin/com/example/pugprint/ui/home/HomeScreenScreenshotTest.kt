package com.example.pugprint.ui.home

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.pugprint.ui.theme.PugPrintTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Roborazzi golden: `./gradlew recordRoborazziDebug` to (re)record, `verifyRoborazziDebug` to check. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class HomeScreenScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun homeScreen_notConnected() {
        compose.setContent {
            PugPrintTheme { HomeScreen(state = HomeUiState(printerStatus = PrinterStatus.NotConnected)) }
        }
        compose.onRoot().captureRoboImage()
    }
}
