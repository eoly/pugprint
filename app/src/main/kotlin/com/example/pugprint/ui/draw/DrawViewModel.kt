package com.example.pugprint.ui.draw

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pugprint.imaging.BrushSize
import com.example.pugprint.imaging.ColoringPageCatalog
import com.example.pugprint.imaging.DrawPoint
import com.example.pugprint.imaging.Drawing
import com.example.pugprint.imaging.DrawingHandoff
import com.example.pugprint.imaging.ImagingDispatcher
import com.example.pugprint.imaging.LabelShape
import com.example.pugprint.imaging.StickerRollCatalog
import com.example.pugprint.imaging.Stroke
import com.example.pugprint.imaging.StrokeRasterizer
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

/** Pen draws black, eraser draws white. */
enum class DrawTool { PEN, ERASER }

data class DrawUiState(
    val drawing: Drawing = Drawing(),
    /**
     * What the sheet started with: blank, or a coloring page's outline. Undo and Start over never
     * go past it, so the page stays whatever the kid draws.
     */
    val base: Drawing = Drawing(),
    /** The coloring page's name when the sheet started from one; the screen's title. */
    val pageName: String? = null,
    /** The stroke under the finger right now, drawn on top of [drawing]. */
    val current: Stroke? = null,
    val brush: BrushSize = BrushSize.MEDIUM,
    val tool: DrawTool = DrawTool.PEN,
    val rendering: Boolean = false,
    /** The sticker's outline: on a round roll the sheet shows the circle the drawing must stay in. */
    val labelShape: LabelShape = LabelShape.RECTANGLE,
) {
    /** True once the kid has drawn something on top of [base]. */
    val canUndo: Boolean get() = drawing.strokes.size > base.strokes.size

    /** Nothing but the outline (or nothing at all) on the sheet yet. */
    val isUntouched: Boolean get() = !canUndo

    /** A blank sheet is fine too: words and stamps go on it in the editor. */
    val canFinish: Boolean get() = !rendering
}

@HiltViewModel
class DrawViewModel
    @Inject
    constructor(
        private val handoff: DrawingHandoff,
        private val settings: SettingsStore,
        @ImagingDispatcher private val dispatcher: CoroutineDispatcher,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(initialState(savedStateHandle.get<String>(PAGE_ARG)))
        private val mutableFinished = MutableStateFlow(false)

        val uiState: StateFlow<DrawUiState> =
            combine(mutableState, settings.settings) { state, prefs ->
                state.copy(labelShape = StickerRollCatalog.byId(prefs.rollId).shape)
            }.stateIn(viewModelScope, SharingStarted.Eagerly, DrawUiState())

        /** Set once the drawing is in [DrawingHandoff]; the route opens the editor and calls [onFinishedHandled]. */
        val finished: StateFlow<Boolean> = mutableFinished.asStateFlow()

        fun onStrokeStarted(point: DrawPoint) =
            mutableState.update { state ->
                val erase = state.tool == DrawTool.ERASER
                state.copy(current = Stroke(listOf(point), state.brush, erase = erase))
            }

        fun onStrokeMoved(point: DrawPoint) = mutableState.update { it.copy(current = it.current?.plus(point)) }

        fun onStrokeEnded() =
            mutableState.update { state ->
                val stroke = state.current ?: return@update state
                state.copy(drawing = state.drawing.plus(stroke), current = null)
            }

        fun onBrushSelected(brush: BrushSize) = mutableState.update { it.copy(brush = brush) }

        fun onToolSelected(tool: DrawTool) = mutableState.update { it.copy(tool = tool) }

        /** Takes the newest of the kid's lines off; the outline underneath stays. */
        fun onUndoClicked() = mutableState.update { if (it.canUndo) it.copy(drawing = it.drawing.undo()) else it }

        /** Back to the blank sheet, or to the page's outline. */
        fun onClearClicked() = mutableState.update { it.copy(drawing = it.base, current = null) }

        /** Renders the drawing to a picture and hands it to the editor. */
        fun onNextClicked() {
            val state = mutableState.value
            if (!state.canFinish) return
            mutableState.update { it.copy(rendering = true) }
            viewModelScope.launch {
                handoff.image = withContext(dispatcher) { StrokeRasterizer.render(state.drawing) }
                mutableState.update { it.copy(rendering = false) }
                mutableFinished.value = true
            }
        }

        fun onFinishedHandled() {
            mutableFinished.value = false
        }

        companion object {
            /** Navigation argument: a [ColoringPageCatalog] id to start the sheet from; absent for a blank sheet. */
            const val PAGE_ARG = "page"

            private fun initialState(pageId: String?): DrawUiState {
                val page = pageId?.let(ColoringPageCatalog::byId) ?: return DrawUiState()
                return DrawUiState(drawing = page.drawing, base = page.drawing, pageName = page.displayName)
            }
        }
    }
