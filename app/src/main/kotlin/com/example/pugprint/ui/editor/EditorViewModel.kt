package com.example.pugprint.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pugprint.imaging.Caption
import com.example.pugprint.imaging.CaptionPlacement
import com.example.pugprint.imaging.CropShape
import com.example.pugprint.imaging.CropWindow
import com.example.pugprint.imaging.DitherMode
import com.example.pugprint.imaging.GrayImage
import com.example.pugprint.imaging.ImagingDispatcher
import com.example.pugprint.imaging.MonoBitmap
import com.example.pugprint.imaging.PhotoLoadException
import com.example.pugprint.imaging.PhotoSource
import com.example.pugprint.imaging.Rotation
import com.example.pugprint.imaging.Sticker
import com.example.pugprint.imaging.StickerRenderer
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
    Preview,
    Failed,
}

data class EditorUiState(
    val step: EditorStep = EditorStep.Loading,
    /** The picture being edited, already turned as the user asked. */
    val image: GrayImage? = null,
    val window: CropWindow? = null,
    val mode: DitherMode = DitherMode.PHOTO,
    /** Words on the sticker; blank means none. */
    val caption: String = "",
    val captionPlacement: CaptionPlacement = CaptionPlacement.BOTTOM,
    /** The dots that will print, once [EditorStep.Preview] (or [EditorStep.Words]) has rendered them. */
    val preview: MonoBitmap? = null,
    val rendering: Boolean = false,
    /** "How dark?" — remembered across stickers. */
    val density: DensityLevel = DensityLevel.MEDIUM,
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
    val caption: String = "",
    val captionPlacement: CaptionPlacement = CaptionPlacement.BOTTOM,
    val preview: MonoBitmap? = null,
    val rendering: Boolean = false,
) {
    val showsDots: Boolean get() = step == EditorStep.Preview || step == EditorStep.Words
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
                    caption = edit.caption,
                    captionPlacement = edit.captionPlacement,
                    preview = edit.preview,
                    rendering = edit.rendering,
                    density = prefs.density,
                    printerStatus = printerState.status(),
                    printerName = printerState.deviceName(),
                    offlineReason = (printerState as? PrinterState.Offline)?.reason,
                    paperOrLidProblem = (printerState as? PrinterState.Connected)?.paperOrLidProblem ?: false,
                )
            }.stateIn(viewModelScope, SharingStarted.Eagerly, EditorUiState())

        /** Loads the picture at [uri]; a repeat call for the same picture is a no-op (configuration change). */
        fun open(uri: String) {
            if (openedUri == uri) return
            openedUri = uri
            renderJob?.cancel()
            edit.value = EditState()
            viewModelScope.launch {
                try {
                    val image = withContext(dispatcher) { photos.load(uri) }
                    edit.update {
                        it.copy(
                            step = EditorStep.Crop,
                            image = image,
                            window = CropWindow(image.width, image.height),
                        )
                    }
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

        fun onShapeSelected(shape: CropShape) = edit.update { it.copy(window = it.window?.withShape(shape)) }

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

        fun onBackToCropClicked() {
            renderJob?.cancel()
            edit.update { it.copy(step = EditorStep.Crop, preview = null, rendering = false) }
        }

        fun onDensitySelected(density: DensityLevel) = settings.setDensity(density)

        fun onPrintClicked() {
            val state = uiState.value.takeIf { it.canPrint } ?: return
            val preview = state.preview ?: return
            printer.printImage(preview, state.density)
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
                )
            // Keep the old dots on screen while re-rendering a caption edit, so the picture doesn't blink.
            edit.update { it.copy(rendering = true, preview = if (it.step == EditorStep.Words) it.preview else null) }
            renderJob =
                viewModelScope.launch {
                    val dots = withContext(dispatcher) { StickerRenderer.render(sticker) }
                    edit.update { it.copy(preview = dots, rendering = false) }
                }
        }
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
