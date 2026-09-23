package com.example.pugprint.ui.coloring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pugprint.imaging.ColoringPage
import com.example.pugprint.imaging.ColoringPageCatalog
import com.example.pugprint.imaging.LabelShape
import com.example.pugprint.imaging.StickerRollCatalog
import com.example.pugprint.settings.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ColoringUiState(
    /** Every page, in picker order. */
    val pages: List<ColoringPage> = ColoringPageCatalog.all,
    /** The sticker's outline: on a round roll each preview shows the circle the page prints in. */
    val labelShape: LabelShape = LabelShape.RECTANGLE,
)

/**
 * The "Color a picture" picker. A tapped page opens on the draw sheet with its outline already
 * there (`draw?page=<id>`); Next there renders it like any drawing, so words, stamps and the roll
 * all work on it unchanged (ADR 0008).
 */
@HiltViewModel
class ColoringViewModel
    @Inject
    constructor(
        settings: SettingsStore,
    ) : ViewModel() {
        val uiState: StateFlow<ColoringUiState> =
            settings.settings
                .map { prefs -> ColoringUiState(labelShape = StickerRollCatalog.byId(prefs.rollId).shape) }
                .stateIn(viewModelScope, SharingStarted.Eagerly, ColoringUiState())
    }
