package com.example.pugprint.ui.editor

import com.example.pugprint.imaging.CropRect
import com.example.pugprint.imaging.CropShape
import com.example.pugprint.imaging.DitherMode
import com.example.pugprint.imaging.GrayImage
import com.example.pugprint.imaging.MonoBitmap
import com.example.pugprint.imaging.PhotoLoadException
import com.example.pugprint.imaging.PhotoSource
import com.example.pugprint.printer.DensityLevel
import com.example.pugprint.printer.InMemoryPairedPrinterStore
import com.example.pugprint.printer.PrinterManager
import com.example.pugprint.printer.transport.FakePrinterTransport
import com.example.pugprint.printer.transport.PrinterClient
import com.example.pugprint.printer.transport.PrinterDevice
import com.example.pugprint.settings.InMemorySettingsStore
import com.example.pugprint.ui.home.PrinterStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelTest {
    private val device = PrinterDevice("AA:BB:CC:DD:EE:FF", "HB-1234")
    private val transport = FakePrinterTransport()
    private val settings = InMemorySettingsStore()

    /** 200×100, dark on the left half, light on the right. */
    private val landscape = GrayImage.generate(200, 100) { x, _ -> if (x < 100) 20 else 235 }
    private val photos = HashMap<String, GrayImage>().apply { put("content://photo/1", landscape) }
    private val source =
        PhotoSource { uri -> photos[uri] ?: throw PhotoLoadException("no such photo $uri") }

    @BeforeEach
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.printer() =
        PrinterManager(PrinterClient(transport), InMemoryPairedPrinterStore(), { true }, backgroundScope) { 1_000L }

    private fun TestScope.viewModel(printer: PrinterManager = printer()) =
        EditorViewModel(source, printer, settings, UnconfinedTestDispatcher(testScheduler))

    private fun TestScope.opened(): EditorViewModel =
        viewModel().also {
            it.open("content://photo/1")
            runCurrent()
        }

    @Test
    fun `opens the picture into the crop step with a fresh square window`() =
        runTest {
            val viewModel = viewModel()
            assertEquals(EditorStep.Loading, viewModel.uiState.value.step)

            viewModel.open("content://photo/1")
            runCurrent()

            val state = viewModel.uiState.value
            assertEquals(EditorStep.Crop, state.step)
            assertEquals(200, state.image?.width)
            assertEquals(CropShape.SQUARE, state.window?.shape)
            assertEquals(CropRect(50, 0, 100, 100), state.window?.cropRect())
            assertFalse(state.canPrint)
        }

    @Test
    fun `a picture that cannot be read goes to Failed`() =
        runTest {
            val viewModel = viewModel()
            viewModel.open("content://photo/missing")
            runCurrent()
            assertEquals(EditorStep.Failed, viewModel.uiState.value.step)
        }

    @Test
    fun `opening the same picture again is a no-op`() =
        runTest {
            val viewModel = opened()
            viewModel.onShapeSelected(CropShape.WIDE)
            viewModel.open("content://photo/1")
            runCurrent()
            assertEquals(
                CropShape.WIDE,
                viewModel.uiState.value.window
                    ?.shape,
            )
        }

    @Test
    fun `rotate turns the picture and resets the window to the new size`() =
        runTest {
            val viewModel = opened()
            viewModel.onTransform(zoomBy = 2f, panDx = 0f, panDy = 0f, focalX = 0f, focalY = 0f)

            viewModel.onRotateClicked()
            runCurrent()

            val state = viewModel.uiState.value
            assertEquals(100, state.image?.width)
            assertEquals(200, state.image?.height)
            assertEquals(1f, state.window?.zoom)
            assertEquals(CropRect(0, 50, 100, 100), state.window?.cropRect())
            // The dark half is now on top.
            assertEquals(20, state.image?.get(50, 10))
        }

    @Test
    fun `shape and gestures move the window`() =
        runTest {
            val viewModel = opened()
            viewModel.onShapeSelected(CropShape.WHOLE)
            assertEquals(
                CropRect(0, 0, 200, 100),
                viewModel.uiState.value.window
                    ?.cropRect(),
            )

            viewModel.onShapeSelected(CropShape.SQUARE)
            viewModel.onTransform(zoomBy = 2f, panDx = 0.25f, panDy = 0f, focalX = 0f, focalY = 0f)
            // Zoomed twice the square is 50 px; a quarter-frame pan right slides it 12.5 px left of centre.
            assertEquals(
                CropRect(63, 25, 50, 50),
                viewModel.uiState.value.window
                    ?.cropRect(),
            )
        }

    @Test
    fun `next renders a head-width preview and back returns to cropping`() =
        runTest {
            val viewModel = opened()

            viewModel.onNextClicked()
            runCurrent()

            val state = viewModel.uiState.value
            assertEquals(EditorStep.Preview, state.step)
            assertFalse(state.rendering)
            val preview = checkNotNull(state.preview)
            assertEquals(384, preview.width)
            assertEquals(384, preview.height)

            viewModel.onBackToCropClicked()
            assertEquals(EditorStep.Crop, viewModel.uiState.value.step)
            assertNull(viewModel.uiState.value.preview)
        }

    @Test
    fun `changing the style re-renders the preview`() =
        runTest {
            val viewModel = opened()
            viewModel.onShapeSelected(CropShape.WHOLE)
            viewModel.onNextClicked()
            runCurrent()
            val photo = checkNotNull(viewModel.uiState.value.preview)

            viewModel.onModeSelected(DitherMode.DRAWING)
            runCurrent()

            val drawing = checkNotNull(viewModel.uiState.value.preview)
            assertEquals(DitherMode.DRAWING, viewModel.uiState.value.mode)

            // Drawing mode: the light half is pure white; photo mode has dots there.
            fun MonoBitmap.lightHalfHasDots() = (0 until height).any { y -> (192 until 384).any { x -> isBlack(x, y) } }
            assertFalse(drawing.lightHalfHasDots())
            assertTrue(photo.lightHalfHasDots())
        }

    @Test
    fun `print needs a connected printer, then hands the dots over and asks to close`() =
        runTest {
            val printer = printer()
            val viewModel = viewModel(printer)
            viewModel.open("content://photo/1")
            runCurrent()
            viewModel.onNextClicked()
            runCurrent()
            assertFalse(viewModel.uiState.value.canPrint)
            viewModel.onPrintClicked()
            assertFalse(viewModel.printRequested.value)

            printer.connect(device)
            runCurrent()
            assertEquals(PrinterStatus.Connected, viewModel.uiState.value.printerStatus)
            assertEquals("HB-1234", viewModel.uiState.value.printerName)
            assertTrue(viewModel.uiState.value.canPrint)

            viewModel.onPrintClicked()
            runCurrent()
            assertTrue(viewModel.printRequested.value)
            viewModel.onPrintHandled()
            assertFalse(viewModel.printRequested.value)

            advanceTimeBy(60_000)
            assertEquals(384, transport.emulator.rows.size)
            assertEquals(emptyList<String>(), transport.emulator.violations)
        }

    @Test
    fun `darkness is remembered and used for the print`() =
        runTest {
            val printer = printer()
            printer.connect(device)
            val viewModel = viewModel(printer)
            viewModel.open("content://photo/1")
            runCurrent()
            assertEquals(DensityLevel.MEDIUM, viewModel.uiState.value.density)

            viewModel.onDensitySelected(DensityLevel.LIGHT)
            viewModel.onNextClicked()
            runCurrent()
            assertEquals(DensityLevel.LIGHT, viewModel.uiState.value.density)
            assertEquals(DensityLevel.LIGHT, settings.settings.value.density)

            viewModel.onPrintClicked()
            advanceTimeBy(60_000)

            assertEquals(20, transport.emulator.density) // public-factory light
            assertEquals(emptyList<String>(), transport.emulator.violations)
        }
}
