package com.example.pugprint.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.pugprint.design.theme.ThemeCatalog
import com.example.pugprint.printer.DensityLevel
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Round-trips through real SharedPreferences (Robolectric), including bad stored values. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class PreferencesSettingsStoreTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun defaultsWhenNothingIsStored() {
        assertEquals(AppSettings(), PreferencesSettingsStore(context).settings.value)
    }

    @Test
    fun changesSurviveANewStore() {
        PreferencesSettingsStore(context).apply {
            setTheme("bubblegum")
            setDensity(DensityLevel.LIGHT)
        }

        assertEquals(
            AppSettings(themeId = "bubblegum", density = DensityLevel.LIGHT),
            PreferencesSettingsStore(context).settings.value,
        )
    }

    @Test
    fun unknownStoredValuesFallBackToDefaults() {
        context
            .getSharedPreferences("settings", Context.MODE_PRIVATE)
            .edit()
            .putString("theme", "retired-theme")
            .putString("density", "EXTRA_CRISPY")
            .commit()

        val loaded = PreferencesSettingsStore(context).settings.value
        assertEquals(ThemeCatalog.default.id, loaded.themeId)
        assertEquals(DensityLevel.MEDIUM, loaded.density)
    }
}
