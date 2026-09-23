package com.example.pugprint.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pugprint.imaging.Caption
import com.example.pugprint.imaging.CaptionPlacement
import com.example.pugprint.imaging.CropShape
import com.example.pugprint.imaging.CropWindow
import com.example.pugprint.imaging.DitherMode
import com.example.pugprint.imaging.DrawingHandoff
import com.example.pugprint.imaging.GrayImage
import com.example.pugprint.imaging.ImagingDispatcher
import com.example.pugprint.imaging.LabelShape
import com.example.pugprint.imaging.MonoBitmap
import com.example.pugprint.imaging.PhotoLoadException
import com.example.pugprint.imaging.PhotoSource
import com.example.pugprint.imaging.Rotation
import com.example.pugprint.imaging.StampPlacement
import com.example.pugprint.imaging.StampSize
import com.example.pugprint.imaging.Sticker
import com.example.pugprint.imaging.StickerRoll
import com.example.pugprint.imaging.StickerRollCatalog
import com.example.pugprint.printer.DensityLevel
import com.example.pugprint.printer.OfflineReason
import com.example.pugprint.printer.PrinterManager
import com.example.pugprint.printer.PrinterState
import com.example.pugprint.settings.SettingsStore
import com.example.pugprint.settings.setDensity
import com.example.pugprint.ui.home.PrinterStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
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

/** Where the kid is in the edit: open the picture, fit it into the sticker, look at the dots, print. */
enum class EditorStep {
    Loading,
    Crop,

    /** Optional detour from [Preview]: type a caption and pick where it goes, watching the dots update. */
    Words,

    /** Optional detour from [Preview]: tap stamps onto the sticker and drag the newest one around. */
    Stamps,
    Preview,
    Failed,
}

data class EditorUiState(
    val step: EditorStep = EditorStep.Loading,
    /** The picture being edited, already turned as the user asked. */
    val image: GrayImage? = null,
    val window: CropWindow? = null,
    val mode: DitherMode = DitherMode.PHOTO,
    /** A drawing is already black-and-white, so Photo / Drawing style makes no difference and is hidden. */
    val isDrawing: Boolean = false,
    /** Words on the sticker; blank means none. */
    val caption: String = "",
    val captionPlacement: CaptionPlacement = CaptionPlacement.BOTTOM,
    /** Stamps on the sticker, oldest first; the last one is the one a drag moves. */
    val stamps: List<StampPlacement> = emptyList(),
    /** Size for the next stamp (and the newest one). */
    val stampSize: StampSize = StampSize.MEDIUM,
    /** The dots that will print, once a dots-showing step has rendered them. */
    val preview: MonoBitmap? = null,
    val rendering: Boolean = false,
    /** "How dark?" — remembered across stickers. */
    val density: DensityLevel = DensityLevel.MEDIUM,
    /** A die-cut label is one shape, so the Square / Tall / Wide / Whole row is hidden. */
    val shapeLocked: Boolean = false,
    /** The sticker's outline: the crop frame and the dots are shown round on a round roll. */
    val labelShape: LabelShape = LabelShape.RECTANGLE,
    val printerStatus: PrinterStatus = PrinterStatus.NoPrinter,
    val printerName: String? = null,
    val offlineReason: OfflineReason? = null,
    val paperOrLidProblem: Boolean = false,
) {
    val canPrint: Boolean
        get() =
            step == EditorStep.Preview &&
                preview != null &&
                printerStatus == PrinterStatus.Connected &&
                !paperOrLidProblem
}

/** The editing session, kept apart from printer state so the two combine cleanly. */
private data class EditState(
    val step: EditorStep = EditorStep.Loading,
    val image: GrayImage? = null,
    val window: CropWindow? = null,
    val mode: DitherMode = DitherMode.PHOTO,
    val isDrawing: Boolean = false,
    val caption: String = "",
    val captionPlacement: CaptionPlacement = CaptionPlacement.BOTTOM,
    val stamps: List<StampPlacement> = emptyList(),
    val stampSize: StampSize = StampSize.MEDIUM,
    val preview: MonoBitmap? = null,
    val rendering: Boolean = false,
) {
    val showsDots: Boolean get() = step == EditorStep.Preview || step == EditorStep.Words || step == EditorStep.Stamps

    /** Detours keep the old dots on screen while re-rendering so the picture doesn't blink. */
    val isDetour: Boolean get() = step == EditorStep.Words || step == EditorStep.Stamps
}

@HiltViewModel
class EditorViewModel
    @Inject
    constructor(
        private val photos: PhotoSource,
        private val printer: PrinterManager,
        private val settings: SettingsStore,
        @ImagingDispatcher private val dispatcher: CoroutineDispatcher,
    ) : ViewModel() {
        private val edit = MutableStateFlow(EditState())
        private val mutablePrintRequested = MutableStateFlow(false)
        private var openedUri: String? = null
        private var renderJob: Job? = null

        /** True once a print has been handed to the printer; the route closes the editor and calls [onPrintHandled]. */
        val printRequested: StateFlow<Boolean> = mutablePrintRequested.asStateFlow()

        val uiState: StateFlow<EditorUiState> =
            combine(edit, printer.state, settings.settings) { edit, printerState, prefs ->
                EditorUiState(
                    step = edit.step,
                    image = edit.image,
                    window = edit.window,
                    mode = edit.mode,
                    isDrawing = edit.isDrawing,
                    caption = edit.caption,
                    captionPlacement = edit.captionPlacement,
                    stamps = edit.stamps,
                    stampSize = edit.stampSize,
                    preview = edit.preview,
                    rendering = edit.rendering,
                    density = prefs.density,
                    shapeLocked = StickerRollCatalog.byId(prefs.rollId).isLabel,
                    labelShape = StickerRollCatalog.byId(prefs.rollId).shape,
                    printerStatus = printerState.status(),
                    printerName = printerState.deviceName(),
                    offlineReason = (printerState as? PrinterState.Offline)?.reason,
                    paperOrLidProblem = (printerState as? PrinterState.Connected)?.paperOrLidProblem ?: false,
                )
            }.stateIn(viewModelScope, SharingStarted.Eagerly, EditorUiState())

        /**
         * Loads the picture at [uri]; a repeat call for the same picture is a no-op (configuration
         * change). A drawing ([DrawingHandoff.URI]) is already one sticker, so it skips "Make it fit"
         * and opens on the preview in Drawing style.
         */
        fun open(uri: String) {
            if (openedUri == uri) return
            openedUri = uri
            renderJob?.cancel()
            edit.value = EditState()
            val isDrawing = uri == DrawingHandoff.URI
            edit.update { it.copy(isDrawing = isDrawing) }
            viewModelScope.launch {
                try {
                    val image = withContext(dispatcher) { photos.load(uri) }
                    edit.update {
                        it.copy(
                            step = if (isDrawing) EditorStep.Preview else EditorStep.Crop,
                            image = image,
                            window = CropWindow(image.width, image.height),
                            mode = if (isDrawing) DitherMode.DRAWING else it.mode,
                        )
                    }
                    if (isDrawing) render()
                } catch (_: PhotoLoadException) {
                    edit.update { it.copy(step = EditorStep.Failed) }
                }
            }
        }

        fun onRotateClicked() {
            val image = edit.value.image ?: return
            viewModelScope.launch {
                val turned = withContext(dispatcher) { image.rotated(Rotation.CLOCKWISE_90) }
                edit.update { current ->
                    if (current.image !==
                        image
                    ) {
                        current
                    } else {
                        current.copy(image = turned, window = current.window?.rotated())
                    }
                }
            }
        }

        /** Ignored on a label roll: the label decides the shape. */
        fun onShapeSelected(shape: CropShape) {
            if (roll().isLabel) return
            edit.update { it.copy(window = it.window?.withShape(shape)) }
        }

        /** A pinch/drag step from the crop frame, in frame widths (see [CropWindow.transformed]). */
        fun onTransform(
            zoomBy: Float,
            panDx: Float,
            panDy: Float,
            focalX: Float,
            focalY: Float,
        ) = edit.update { it.copy(window = it.window?.transformed(zoomBy, panDx, panDy, focalX, focalY)) }

        /** Crop is done: show what will print. */
        fun onNextClicked() {
            if (edit.value.step != EditorStep.Crop) return
            edit.update { it.copy(step = EditorStep.Preview) }
            render()
        }

        fun onModeSelected(mode: DitherMode) {
            if (edit.value.mode == mode) return
            edit.update { it.copy(mode = mode) }
            if (edit.value.showsDots) render()
        }

        /** "Add words" from the preview. */
        fun onAddWordsClicked() {
            if (edit.value.step != EditorStep.Preview) return
            edit.update { it.copy(step = EditorStep.Words) }
        }

        fun onCaptionChanged(text: String) {
            if (edit.value.caption == text) return
            edit.update { it.copy(caption = text) }
            if (edit.value.showsDots) render()
        }

        fun onCaptionPlacementSelected(placement: CaptionPlacement) {
            if (edit.value.captionPlacement == placement) return
            edit.update { it.copy(captionPlacement = placement) }
            if (edit.value.showsDots && edit.value.caption.isNotBlank()) render()
        }

        /** Done with the words: back to the preview (the dots are already up to date). */
        fun onWordsDoneClicked() {
            if (edit.value.step != EditorStep.Words) return
            edit.update { it.copy(step = EditorStep.Preview) }
        }

        /** "Add stamps" from the preview. */
        fun onAddStampsClicked() {
            if (edit.value.step != EditorStep.Preview) return
            edit.update { it.copy(step = EditorStep.Stamps) }
        }

        /** A stamp from the picker lands in the middle at the current size; drag it from there. */
        fun onStampPicked(stampId: String) {
            edit.update { it.copy(stamps = it.stamps + StampPlacement(stampId, size = it.stampSize)) }
            render()
        }

        /** Drag on the dots, in fractions of the sticker's width and height; moves the newest stamp. */
        fun onStampDragged(
            dx: Float,
            dy: Float,
        ) {
            val stamps = edit.value.stamps
            if (stamps.isEmpty()) return
            edit.update { it.copy(stamps = stamps.dropLast(1) + stamps.last().movedBy(dx, dy)) }
            render()
        }

        /** Sets the size for the next stamp and resizes the newest one. */
        fun onStampSizeSelected(size: StampSize) {
            if (edit.value.stampSize == size) return
            edit.update { current ->
                val stamps = current.stamps
                val resized = if (stamps.isEmpty()) stamps else stamps.dropLast(1) + stamps.last().copy(size = size)
                current.copy(stampSize = size, stamps = resized)
            }
            if (edit.value.stamps.isNotEmpty()) render()
        }

        /** Takes the newest stamp off again. */
        fun onUndoStampClicked() {
            if (edit.value.stamps.isEmpty()) return
            edit.update { it.copy(stamps = it.stamps.dropLast(1)) }
            render()
        }

        fun onStampsDoneClicked() {
            if (edit.value.step != EditorStep.Stamps) return
            edit.update { it.copy(step = EditorStep.Preview) }
        }

        fun onBackToCropClicked() {
            renderJob?.cancel()
            edit.update { it.copy(step = EditorStep.Crop, preview = null, rendering = false) }
        }

        fun onDensitySelected(density: DensityLevel) = settings.setDensity(density)

        fun onPrintClicked() {
            val state = uiState.value.takeIf { it.canPrint } ?: return
            val preview = state.preview ?: return
            printer.printImage(roll().place(preview), state.density)
            mutablePrintRequested.value = true
        }

        fun onPrintHandled() {
            mutablePrintRequested.value = false
        }

        private fun render() {
            renderJob?.cancel()
            val current = edit.value
            val image = current.image ?: return
            val window = current.window ?: return
            val sticker =
                Sticker(
                    image = image,
                    crop = window.cropRect(),
                    mode = current.mode,
                    caption = Caption(current.caption, current.captionPlacement),
                    stamps = current.stamps,
                )
            val roll = roll()
            edit.update { it.copy(rendering = true, preview = if (it.isDetour) it.preview else null) }
            renderJob =
                viewModelScope.launch {
                    val dots = withContext(dispatcher) { roll.render(sticker) }
                    edit.update { it.copy(preview = dots, rendering = false) }
                }
        }

        /** The paper in the printer, as chosen on the home screen. */
        private fun roll(): StickerRoll = StickerRollCatalog.byId(settings.settings.value.rollId)
    }

private fun PrinterState.status(): PrinterStatus =
    when (this) {
        PrinterState.NoPrinter -> PrinterStatus.NoPrinter
        is PrinterState.Offline -> PrinterStatus.Offline
        is PrinterState.Connecting -> PrinterStatus.Connecting
        is PrinterState.Connected -> PrinterStatus.Connected
        is PrinterState.Printing -> PrinterStatus.Printing
    }

private fun PrinterState.deviceName(): String? =
    when (this) {
        PrinterState.NoPrinter -> null
        is PrinterState.Offline -> device.displayName
        is PrinterState.Connecting -> device.displayName
        is PrinterState.Connected -> device.displayName
        is PrinterState.Printing -> device.displayName
    }
