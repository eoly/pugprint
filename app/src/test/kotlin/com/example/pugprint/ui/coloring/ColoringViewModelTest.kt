package com.example.pugprint.ui.coloring

import com.example.pugprint.imaging.ColoringPageCatalog
import com.example.pugprint.imaging.LabelShape
import com.example.pugprint.settings.AppSettings
import com.example.pugprint.settings.InMemorySettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ColoringViewModelTest {
    private val settings = InMemorySettingsStore()

    @BeforeEach
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `the picker shows every page in catalog order`() =
        runTest {
            assertEquals(ColoringPageCatalog.all, ColoringViewModel(settings).uiState.value.pages)
        }

    @Test
    fun `the previews are round on the round roll and square otherwise`() =
        runTest {
            val viewModel = ColoringViewModel(settings)
            assertEquals(LabelShape.RECTANGLE, viewModel.uiState.value.labelShape)
            settings.update { AppSettings(rollId = "circle-49") }
            assertEquals(LabelShape.CIRCLE, viewModel.uiState.value.labelShape)
        }
}
