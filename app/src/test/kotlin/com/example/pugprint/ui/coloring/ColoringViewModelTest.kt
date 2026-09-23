package com.example.pugprint.ui.coloring

import com.example.pugprint.imaging.ColoringPageCatalog
import com.example.pugprint.imaging.Dither
import com.example.pugprint.imaging.DrawingHandoff
import com.example.pugprint.imaging.LabelShape
import com.example.pugprint.settings.AppSettings
import com.example.pugprint.settings.InMemorySettingsStore
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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ColoringViewModelTest {
    private val handoff = DrawingHandoff()
    private val settings = InMemorySettingsStore()

    @BeforeEach
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel() = ColoringViewModel(handoff, settings, UnconfinedTestDispatcher(testScheduler))

    @Test
    fun `the picker shows every page in catalog order`() =
        runTest {
            assertEquals(ColoringPageCatalog.all, viewModel().uiState.value.pages)
        }

    @Test
    fun `picking a page renders it into the hand-off and flags finished once`() =
        runTest {
            val viewModel = viewModel()
            assertNull(handoff.image)

            viewModel.onPagePicked("pug")
            runCurrent()

            val image = handoff.image!!
            assertEquals(384, image.width)
            assertEquals(384, image.height)
            val ink = image.luma.count { (it.toInt() and 0xFF) < Dither.DEFAULT_THRESHOLD }
            assertTrue(ink > 0, "the page should have outlines")
            assertTrue(ink < image.luma.size / 2, "the page should be mostly white to color in")
            assertFalse(viewModel.uiState.value.opening)
            assertTrue(viewModel.finished.value)
            viewModel.onFinishedHandled()
            assertFalse(viewModel.finished.value)
        }

    @Test
    fun `a page that is not in the catalog does nothing`() =
        runTest {
            val viewModel = viewModel()
            viewModel.onPagePicked("dragon")
            runCurrent()
            assertNull(handoff.image)
            assertFalse(viewModel.finished.value)
        }

    @Test
    fun `the previews are round on the round roll and square otherwise`() =
        runTest {
            val viewModel = viewModel()
            assertEquals(LabelShape.RECTANGLE, viewModel.uiState.value.labelShape)
            settings.update { AppSettings(rollId = "circle-49") }
            assertEquals(LabelShape.CIRCLE, viewModel.uiState.value.labelShape)
        }
}
