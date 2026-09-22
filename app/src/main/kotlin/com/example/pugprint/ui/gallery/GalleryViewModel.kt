package com.example.pugprint.ui.gallery

import androidx.lifecycle.ViewModel
import com.example.pugprint.design.theme.PugTheme
import com.example.pugprint.design.theme.ThemeCatalog
import com.example.pugprint.settings.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * The design gallery's own theme choice: starts as the app's, and switching it here is a
 * preview only — nothing is saved, so a designer can flip through looks without changing the kid's.
 */
@HiltViewModel
class GalleryViewModel
    @Inject
    constructor(
        settings: SettingsStore,
    ) : ViewModel() {
        private val mutableTheme = MutableStateFlow(ThemeCatalog.byId(settings.settings.value.themeId))

        val theme: StateFlow<PugTheme> = mutableTheme.asStateFlow()

        fun onThemeSelected(theme: PugTheme) {
            mutableTheme.value = theme
        }
    }
