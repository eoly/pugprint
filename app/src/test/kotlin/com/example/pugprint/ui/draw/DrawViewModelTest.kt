package com.example.pugprint.ui.draw

import com.example.pugprint.imaging.BrushSize
import com.example.pugprint.imaging.Dither
import com.example.pugprint.imaging.DrawPoint
import com.example.pugprint.imaging.DrawingHandoff
import com.example.pugprint.imaging.Stroke
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DrawViewModelTest {
    private val handoff = DrawingHandoff()

    @BeforeEach
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel() = DrawViewModel(handoff, UnconfinedTestDispatcher(testScheduler))

    @Test
    fun `a stroke is the finger's path with the brush and tool at the time`() =
        runTest {
            val viewModel = viewModel()
            viewModel.onBrushSelected(BrushSize.FAT)
            viewModel.onStrokeStarted(DrawPoint(0.1f, 0.1f))
            viewModel.onStrokeMoved(DrawPoint(0.2f, 0.2f))
            assertEquals(
                Stroke(listOf(DrawPoint(0.1f, 0.1f), DrawPoint(0.2f, 0.2f)), BrushSize.FAT),
                viewModel.uiState.value.current,
            )

            viewModel.onStrokeEnded()
            viewModel.onToolSelected(DrawTool.ERASER)
            viewModel.onStrokeStarted(DrawPoint(0.5f, 0.5f))
            viewModel.onStrokeEnded()
            val strokes = viewModel.uiState.value.drawing.strokes
            assertEquals(2, strokes.size)
            assertFalse(strokes[0].erase)
            assertTrue(strokes[1].erase)
            assertTrue(viewModel.uiState.value.canFinish)
        }

    @Test
    fun `undo and start over`() =
        runTest {
            val viewModel = viewModel()
            repeat(3) {
                viewModel.onStrokeStarted(DrawPoint(0.5f, 0.5f))
                viewModel.onStrokeEnded()
            }
            viewModel.onUndoClicked()
            assertEquals(2, viewModel.uiState.value.drawing.strokes.size)
            viewModel.onClearClicked()
            assertTrue(viewModel.uiState.value.drawing.isEmpty)
            assertFalse(viewModel.uiState.value.canUndo)
            viewModel.onUndoClicked() // no-op when empty
            assertTrue(viewModel.uiState.value.drawing.isEmpty)
        }

    @Test
    fun `a blank sheet can go to the editor too, for words and stamps only`() =
        runTest {
            val viewModel = viewModel()
            assertTrue(viewModel.uiState.value.canFinish)
            viewModel.onNextClicked()
            runCurrent()

            val image = handoff.image!!
            assertTrue(image.luma.all { (it.toInt() and 0xFF) == 255 }, "blank sheet should be all white")
            assertTrue(viewModel.finished.value)
        }

    @Test
    fun `next renders the drawing into the hand-off and flags finished once`() =
        runTest {
            val viewModel = viewModel()
            assertNull(handoff.image)
            assertFalse(viewModel.finished.value)

            viewModel.onStrokeStarted(DrawPoint(0.5f, 0.5f))
            viewModel.onStrokeEnded()
            viewModel.onNextClicked()
            runCurrent()

            assertNotNull(handoff.image)
            val image = handoff.image!!
            assertEquals(384, image.width)
            assertTrue(image[192, 192] < Dither.DEFAULT_THRESHOLD)
            assertTrue(viewModel.finished.value)
            viewModel.onFinishedHandled()
            assertFalse(viewModel.finished.value)
        }
}
