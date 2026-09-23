package com.example.pugprint.ui.draw

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pugprint.imaging.BrushSize
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
    /** The stroke under the finger right now, drawn on top of [drawing]. */
    val current: Stroke? = null,
    val brush: BrushSize = BrushSize.MEDIUM,
    val tool: DrawTool = DrawTool.PEN,
    val rendering: Boolean = false,
    /** The sticker's outline: on a round roll the sheet shows the circle the drawing must stay in. */
    val labelShape: LabelShape = LabelShape.RECTANGLE,
) {
    val canUndo: Boolean get() = !drawing.isEmpty

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
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(DrawUiState())
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

        fun onUndoClicked() = mutableState.update { it.copy(drawing = it.drawing.undo()) }

        fun onClearClicked() = mutableState.update { it.copy(drawing = Drawing(), current = null) }

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
    }
