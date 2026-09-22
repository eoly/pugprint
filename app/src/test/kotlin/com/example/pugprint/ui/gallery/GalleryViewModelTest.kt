package com.example.pugprint.ui.gallery

import com.example.pugprint.design.theme.ThemeCatalog
import com.example.pugprint.settings.AppSettings
import com.example.pugprint.settings.InMemorySettingsStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class GalleryViewModelTest {
    @Test
    fun `starts on the app's theme and previews without saving`() {
        val settings = InMemorySettingsStore(AppSettings(themeId = "ocean"))
        val viewModel = GalleryViewModel(settings)
        assertSame(ThemeCatalog.Ocean, viewModel.theme.value)

        viewModel.onThemeSelected(ThemeCatalog.Bubblegum)

        assertSame(ThemeCatalog.Bubblegum, viewModel.theme.value)
        assertEquals("ocean", settings.settings.value.themeId)
    }
}
