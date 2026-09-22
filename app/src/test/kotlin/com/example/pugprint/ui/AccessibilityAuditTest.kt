package com.example.pugprint.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.pugprint.design.theme.PugPrintTheme
import com.example.pugprint.imaging.BrushSize
import com.example.pugprint.imaging.Caption
import com.example.pugprint.imaging.CropWindow
import com.example.pugprint.imaging.DrawPoint
import com.example.pugprint.imaging.Drawing
import com.example.pugprint.imaging.GrayImage
import com.example.pugprint.imaging.StampPlacement
import com.example.pugprint.imaging.Sticker
import com.example.pugprint.imaging.StickerRenderer
import com.example.pugprint.imaging.Stroke
import com.example.pugprint.ui.draw.DrawScreen
import com.example.pugprint.ui.draw.DrawUiState
import com.example.pugprint.ui.editor.EditorScreen
import com.example.pugprint.ui.editor.EditorStep
import com.example.pugprint.ui.editor.EditorUiState
import com.example.pugprint.ui.home.HomeMessage
import com.example.pugprint.ui.home.HomeScreen
import com.example.pugprint.ui.home.HomeUiState
import com.example.pugprint.ui.home.PrinterStatus
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Walks every screen's semantics tree: everything a kid can tap must say what it is (text or a
 * content description) and be at least [MIN_TARGET] on both sides. Compose has no lint for
 * this, so this test is the rule.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class AccessibilityAuditTest {
    @get:Rule
    val compose = createComposeRule()

    private val photo = GrayImage.generate(320, 240) { x, y -> (x + y) * 255 / 560 }
    private val window = CropWindow(photo.width, photo.height)

    private fun audit(
        screen: String,
        content: @androidx.compose.runtime.Composable () -> Unit,
    ) {
        compose.setContent { PugPrintTheme { content() } }
        val tappable =
            compose
                .onAllNodes(SemanticsMatcher("tappable") { it.config.contains(SemanticsActions.OnClick) })
                .fetchSemanticsNodes()
        assertTrue("$screen: nothing tappable found — is the matcher broken?", tappable.isNotEmpty())
        val minPx = with(compose.density) { MIN_TARGET.toPx() }
        tappable.forEach { node ->
            val text =
                node.config
                    .getOrNull(SemanticsProperties.Text)
                    ?.joinToString { it.text }
                    .orEmpty()
            val description =
                node.config
                    .getOrNull(SemanticsProperties.ContentDescription)
                    ?.joinToString()
                    .orEmpty()
            val label = (text + description).trim()
            assertTrue(
                "$screen: a tappable thing at ${node.boundsInRoot} has no words for TalkBack",
                label.isNotEmpty(),
            )
            val bounds = node.boundsInRoot
            val widthDp = bounds.width / compose.density.density
            val heightDp = bounds.height / compose.density.density
            assertTrue(
                "$screen: '$label' is ${widthDp}x$heightDp dp, smaller than $MIN_TARGET",
                bounds.width >= minPx - TOLERANCE_PX && bounds.height >= minPx - TOLERANCE_PX,
            )
        }
    }

    @Test
    fun home_noPrinter() = audit("home") { HomeScreen(HomeUiState()) }

    @Test
    fun home_connected_withEverything() =
        audit("home") {
            HomeScreen(
                HomeUiState(
                    PrinterStatus.Connected,
                    printerName = "HB-1234",
                    batteryPercent = 64,
                    hasLastPrint = true,
                    message = HomeMessage.PrintDone,
                ),
            )
        }

    @Test
    fun home_debug_withGallery() = audit("home debug") { HomeScreen(HomeUiState(), showDesignGallery = true) }

    @Test
    fun home_offline() =
        audit("home") { HomeScreen(HomeUiState(PrinterStatus.Offline, printerName = "HB-1234", hasLastPrint = true)) }

    @Test
    fun editor_crop() =
        audit("editor crop") { EditorScreen(EditorUiState(step = EditorStep.Crop, image = photo, window = window)) }

    @Test
    fun editor_preview() =
        audit("editor preview") {
            val stamps = listOf(StampPlacement("heart"))
            EditorScreen(
                EditorUiState(
                    step = EditorStep.Preview,
                    image = photo,
                    window = window,
                    caption = "Hi",
                    stamps = stamps,
                    preview =
                        StickerRenderer.render(
                            Sticker(photo, window.cropRect(), caption = Caption("Hi"), stamps = stamps),
                        ),
                    printerStatus = PrinterStatus.Connected,
                    printerName = "HB-1234",
                ),
            )
        }

    @Test
    fun editor_words() =
        audit("editor words") {
            EditorScreen(
                EditorUiState(
                    step = EditorStep.Words,
                    image = photo,
                    window = window,
                    preview = StickerRenderer.render(Sticker(photo, window.cropRect())),
                ),
            )
        }

    @Test
    fun editor_stamps() =
        audit("editor stamps") {
            EditorScreen(
                EditorUiState(
                    step = EditorStep.Stamps,
                    image = photo,
                    window = window,
                    stamps = listOf(StampPlacement("star")),
                    preview = StickerRenderer.render(Sticker(photo, window.cropRect())),
                ),
            )
        }

    @Test
    fun editor_failed() = audit("editor failed") { EditorScreen(EditorUiState(step = EditorStep.Failed)) }

    @Test
    fun draw() =
        audit("draw") {
            DrawScreen(DrawUiState(drawing = Drawing().plus(Stroke(listOf(DrawPoint(0.5f, 0.5f)), BrushSize.FAT))))
        }

    private companion object {
        val MIN_TARGET = 48.dp

        /** Layout rounding. */
        const val TOLERANCE_PX = 1f
    }
}
