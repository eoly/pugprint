package com.example.pugprint.ui.editor

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.pugprint.design.theme.PugPrintTheme
import com.example.pugprint.imaging.Caption
import com.example.pugprint.imaging.CaptionPlacement
import com.example.pugprint.imaging.CropShape
import com.example.pugprint.imaging.CropWindow
import com.example.pugprint.imaging.DitherMode
import com.example.pugprint.imaging.GrayImage
import com.example.pugprint.imaging.ImagePipeline
import com.example.pugprint.imaging.StampPlacement
import com.example.pugprint.imaging.StampSize
import com.example.pugprint.imaging.Sticker
import com.example.pugprint.imaging.StickerRenderer
import com.example.pugprint.printer.DensityLevel
import com.example.pugprint.printer.OfflineReason
import com.example.pugprint.ui.Screenshots
import com.example.pugprint.ui.home.PrinterStatus
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.hypot

/** Roborazzi goldens: `./gradlew recordRoborazziDebug` to (re)record, `verifyRoborazziDebug` to check. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class EditorScreenScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    /** A landscape "photo": diagonal ramp with a dark disc, so cropping and dithering are visible. */
    private val photo =
        GrayImage.generate(320, 240) { x, y ->
            val ramp = 40 + (x + y) * 200 / 560
            val disc = hypot(x - 110.0, y - 120.0)
            if (disc < 60) (disc * 2).toInt() else ramp
        }

    private fun snap(state: EditorUiState) {
        compose.setContent { PugPrintTheme { EditorScreen(state = state) } }
        compose.onRoot().captureRoboImage(roborazziOptions = Screenshots.options)
    }

    @Test
    fun editorScreen_loading() = snap(EditorUiState())

    @Test
    fun editorScreen_failed() = snap(EditorUiState(step = EditorStep.Failed))

    @Test
    fun editorScreen_crop_square() =
        snap(EditorUiState(step = EditorStep.Crop, image = photo, window = CropWindow(photo.width, photo.height)))

    @Test
    fun editorScreen_crop_tall_zoomed() =
        snap(
            EditorUiState(
                step = EditorStep.Crop,
                image = photo,
                window =
                    CropWindow(photo.width, photo.height, CropShape.TALL)
                        .transformed(zoomBy = 1.8f, panDx = 0.2f, panDy = 0.1f),
            ),
        )

    @Test
    fun editorScreen_preview_ready() {
        val window = CropWindow(photo.width, photo.height, CropShape.WHOLE)
        snap(
            EditorUiState(
                step = EditorStep.Preview,
                image = photo,
                window = window,
                mode = DitherMode.PHOTO,
                preview = ImagePipeline.render(photo, window.cropRect(), DitherMode.PHOTO),
                printerStatus = PrinterStatus.Connected,
                printerName = "HB-1234",
            ),
        )
    }

    @Test
    fun editorScreen_preview_drawing_offline() {
        val window = CropWindow(photo.width, photo.height)
        snap(
            EditorUiState(
                step = EditorStep.Preview,
                image = photo,
                window = window,
                mode = DitherMode.DRAWING,
                isDrawing = true, // a drawing: no Photo / Drawing row
                preview = ImagePipeline.render(photo, window.cropRect(), DitherMode.DRAWING),
                density = DensityLevel.DARK,
                printerStatus = PrinterStatus.Offline,
                printerName = "HB-1234",
                offlineReason = OfflineReason.LOST,
            ),
        )
    }

    @Test
    fun editorScreen_preview_rendering() =
        snap(
            EditorUiState(
                step = EditorStep.Preview,
                image = photo,
                window = CropWindow(photo.width, photo.height),
                rendering = true,
                printerStatus = PrinterStatus.Connected,
                printerName = "HB-1234",
            ),
        )

    @Test
    fun editorScreen_words() {
        val window = CropWindow(photo.width, photo.height)
        snap(
            EditorUiState(
                step = EditorStep.Words,
                image = photo,
                window = window,
                caption = "Best dog",
                captionPlacement = CaptionPlacement.TOP,
                preview =
                    StickerRenderer.render(
                        Sticker(photo, window.cropRect(), DitherMode.PHOTO, Caption("Best dog", CaptionPlacement.TOP)),
                    ),
                printerStatus = PrinterStatus.Connected,
                printerName = "HB-1234",
            ),
        )
    }

    @Test
    fun editorScreen_stamps() {
        val window = CropWindow(photo.width, photo.height)
        val stamps = listOf(StampPlacement("heart", 0.25f, 0.3f, StampSize.BIG), StampPlacement("star", 0.7f, 0.7f))
        snap(
            EditorUiState(
                step = EditorStep.Stamps,
                image = photo,
                window = window,
                stamps = stamps,
                preview = StickerRenderer.render(Sticker(photo, window.cropRect(), DitherMode.PHOTO, stamps = stamps)),
                printerStatus = PrinterStatus.Connected,
                printerName = "HB-1234",
            ),
        )
    }

    @Test
    fun editorScreen_preview_caption() {
        val window = CropWindow(photo.width, photo.height)
        val stamps = listOf(StampPlacement("paw", 0.8f, 0.2f))
        snap(
            EditorUiState(
                step = EditorStep.Preview,
                image = photo,
                window = window,
                caption = "Best dog",
                stamps = stamps,
                preview =
                    StickerRenderer.render(
                        Sticker(photo, window.cropRect(), DitherMode.PHOTO, Caption("Best dog"), stamps),
                    ),
                printerStatus = PrinterStatus.Connected,
                printerName = "HB-1234",
            ),
        )
    }
}
