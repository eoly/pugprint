package com.example.pugprint.ui.coloring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pugprint.imaging.ColoringPage
import com.example.pugprint.imaging.ColoringPageCatalog
import com.example.pugprint.imaging.DrawingHandoff
import com.example.pugprint.imaging.ImagingDispatcher
import com.example.pugprint.imaging.LabelShape
import com.example.pugprint.imaging.StickerRollCatalog
import com.example.pugprint.settings.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ColoringUiState(
    /** Every page, in picker order. */
    val pages: List<ColoringPage> = ColoringPageCatalog.all,
    /** The sticker's outline: on a round roll each preview shows the circle the page prints in. */
    val labelShape: LabelShape = LabelShape.RECTANGLE,
    /** A page is being turned into a picture; the tiles wait so a double tap opens one editor. */
    val opening: Boolean = false,
)

/**
 * The "Color a picture" picker. A tapped page is rendered into [DrawingHandoff] and the editor
 * opens it like a finished drawing (preview step, Drawing style), so words, stamps and the roll
 * all work on it unchanged (ADR 0008).
 */
@HiltViewModel
class ColoringViewModel
    @Inject
    constructor(
        private val handoff: DrawingHandoff,
        settings: SettingsStore,
        @ImagingDispatcher private val dispatcher: CoroutineDispatcher,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(ColoringUiState())
        private val mutableFinished = MutableStateFlow(false)

        val uiState: StateFlow<ColoringUiState> =
            combine(mutableState, settings.settings) { state, prefs ->
                state.copy(labelShape = StickerRollCatalog.byId(prefs.rollId).shape)
            }.stateIn(viewModelScope, SharingStarted.Eagerly, ColoringUiState())

        /** Set once the page is in [DrawingHandoff]; the route opens the editor and calls [onFinishedHandled]. */
        val finished: StateFlow<Boolean> = mutableFinished.asStateFlow()

        /** Renders the page under [pageId] and hands it to the editor; an unknown id does nothing. */
        fun onPagePicked(pageId: String) {
            val page = ColoringPageCatalog.byId(pageId) ?: return
            if (mutableState.value.opening) return
            mutableState.update { it.copy(opening = true) }
            viewModelScope.launch {
                handoff.image = withContext(dispatcher) { page.render() }
                mutableState.update { it.copy(opening = false) }
                mutableFinished.value = true
            }
        }

        fun onFinishedHandled() {
            mutableFinished.value = false
        }
    }
