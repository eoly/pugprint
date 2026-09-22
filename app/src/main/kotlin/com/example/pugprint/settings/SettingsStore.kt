package com.example.pugprint.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.pugprint.design.theme.ThemeCatalog
import com.example.pugprint.printer.DensityLevel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import javax.inject.Inject
import javax.inject.Singleton

/** The few things the kid can change about the app. Everything has a sensible default. */
data class AppSettings(
    /** A [ThemeCatalog] id; unknown ids fall back to the default theme. */
    val themeId: String = ThemeCatalog.default.id,
    /** How hard the printer burns: the "How dark?" choice on the preview step. */
    val density: DensityLevel = DensityLevel.MEDIUM,
)

/** Where [AppSettings] live; observed by the theme, the home screen and the editor. */
interface SettingsStore {
    val settings: StateFlow<AppSettings>

    fun update(transform: (AppSettings) -> AppSettings)
}

fun SettingsStore.setTheme(themeId: String) = update { it.copy(themeId = themeId) }

fun SettingsStore.setDensity(density: DensityLevel) = update { it.copy(density = density) }

/** App-private preferences; nothing here identifies the user or leaves the device. */
@Singleton
class PreferencesSettingsStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : SettingsStore {
        private val prefs: SharedPreferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        private val mutableSettings = MutableStateFlow(load())

        override val settings: StateFlow<AppSettings> = mutableSettings.asStateFlow()

        override fun update(transform: (AppSettings) -> AppSettings) {
            val next = mutableSettings.updateAndGet(transform)
            prefs.edit {
                putString(KEY_THEME, next.themeId)
                putString(KEY_DENSITY, next.density.name)
            }
        }

        private fun load(): AppSettings {
            val defaults = AppSettings()
            return AppSettings(
                themeId = ThemeCatalog.byId(prefs.getString(KEY_THEME, null)).id,
                density =
                    prefs.getString(KEY_DENSITY, null)?.let { name ->
                        DensityLevel.entries.firstOrNull { it.name == name }
                    } ?: defaults.density,
            )
        }

        private companion object {
            const val FILE = "settings"
            const val KEY_THEME = "theme"
            const val KEY_DENSITY = "density"
        }
    }

/** For tests. */
class InMemorySettingsStore(
    initial: AppSettings = AppSettings(),
) : SettingsStore {
    private val mutableSettings = MutableStateFlow(initial)

    override val settings: StateFlow<AppSettings> = mutableSettings.asStateFlow()

    override fun update(transform: (AppSettings) -> AppSettings) = mutableSettings.update(transform)
}
