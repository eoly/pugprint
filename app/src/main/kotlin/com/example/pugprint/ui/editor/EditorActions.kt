package com.example.pugprint.ui.editor

import com.example.pugprint.imaging.CaptionPlacement
import com.example.pugprint.imaging.CropShape
import com.example.pugprint.imaging.DitherMode
import com.example.pugprint.imaging.StampSize
import com.example.pugprint.printer.DensityLevel

/** What the editor screen can ask for; wired to the ViewModel by `EditorRoute`. */
data class EditorActions(
    val onBack: () -> Unit = {},
    val onRotate: () -> Unit = {},
    val onShape: (CropShape) -> Unit = {},
    /** Pinch/drag step in frame widths: zoom factor, pan, and the focal point relative to the frame centre. */
    val onTransform: (
        zoomBy: Float,
        panDx: Float,
        panDy: Float,
        focalX: Float,
        focalY: Float,
    ) -> Unit = { _, _, _, _, _ -> },
    val onNext: () -> Unit = {},
    val onMode: (DitherMode) -> Unit = {},
    val onDensity: (DensityLevel) -> Unit = {},
    val onAddWords: () -> Unit = {},
    val onCaption: (String) -> Unit = {},
    val onCaptionPlacement: (CaptionPlacement) -> Unit = {},
    val onWordsDone: () -> Unit = {},
    val onAddStamps: () -> Unit = {},
    val onStampPicked: (stampId: String) -> Unit = {},
    /** Drag on the dots, in fractions of the sticker's width and height. */
    val onStampDragged: (dx: Float, dy: Float) -> Unit = { _, _ -> },
    val onStampSize: (StampSize) -> Unit = {},
    val onUndoStamp: () -> Unit = {},
    val onStampsDone: () -> Unit = {},
    val onPrint: () -> Unit = {},
)
