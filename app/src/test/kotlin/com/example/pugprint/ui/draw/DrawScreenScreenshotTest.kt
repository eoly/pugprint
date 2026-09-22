package com.example.pugprint.ui.draw

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.pugprint.design.theme.PugPrintTheme
import com.example.pugprint.imaging.BrushSize
import com.example.pugprint.imaging.DrawPoint
import com.example.pugprint.imaging.Drawing
import com.example.pugprint.imaging.Stroke
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
class DrawScreenScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private fun snap(state: DrawUiState) {
        compose.setContent { PugPrintTheme { DrawScreen(state = state) } }
        compose.onRoot().captureRoboImage(roborazziOptions = Screenshots.options)
    }

    @Test
    fun drawScreen_empty() = snap(DrawUiState())

    @Test
    fun drawScreen_doodle() =
        snap(
            DrawUiState(
                drawing =
                    Drawing()
                        .plus(
                            Stroke(
                                listOf(DrawPoint(0.2f, 0.3f), DrawPoint(0.5f, 0.15f), DrawPoint(0.8f, 0.3f)),
                                BrushSize.FAT,
                            ),
                        ).plus(
                            Stroke(
                                listOf(DrawPoint(0.3f, 0.6f), DrawPoint(0.5f, 0.8f), DrawPoint(0.7f, 0.6f)),
                                BrushSize.MEDIUM,
                            ),
                        ).plus(Stroke(listOf(DrawPoint(0.5f, 0.45f)), BrushSize.FAT))
                        .plus(Stroke(listOf(DrawPoint(0.5f, 0.45f)), BrushSize.THIN, erase = true)),
                current = Stroke(listOf(DrawPoint(0.1f, 0.9f), DrawPoint(0.9f, 0.9f)), BrushSize.THIN),
                brush = BrushSize.THIN,
                tool = DrawTool.ERASER,
            ),
        )
}
