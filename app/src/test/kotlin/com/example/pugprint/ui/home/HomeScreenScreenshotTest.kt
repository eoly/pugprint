package com.example.pugprint.ui.home

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.pugprint.printer.OfflineReason
import com.example.pugprint.ui.theme.PugPrintTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Roborazzi goldens: `./gradlew recordRoborazziDebug` to (re)record, `verifyRoborazziDebug` to check. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class HomeScreenScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private fun snap(state: HomeUiState) {
        compose.setContent { PugPrintTheme { HomeScreen(state = state) } }
        compose.onRoot().captureRoboImage()
    }

    @Test
    fun homeScreen_noPrinter() = snap(HomeUiState())

    @Test
    fun homeScreen_connecting() = snap(HomeUiState(PrinterStatus.Connecting, printerName = "HB-1234"))

    @Test
    fun homeScreen_connected() =
        snap(HomeUiState(PrinterStatus.Connected, printerName = "HB-1234", batteryPercent = 64))

    @Test
    fun homeScreen_connected_lowBattery_paperOrLid() =
        snap(
            HomeUiState(
                PrinterStatus.Connected,
                printerName = "HB-1234",
                batteryPercent = 12,
                batteryLow = true,
                paperOrLidProblem = true,
            ),
        )

    @Test
    fun homeScreen_printing() =
        snap(HomeUiState(PrinterStatus.Printing, printerName = "HB-1234", batteryPercent = 64, printProgress = 0.4f))

    @Test
    fun homeScreen_offline_lost() =
        snap(HomeUiState(PrinterStatus.Offline, printerName = "HB-1234", offlineReason = OfflineReason.LOST))
}
