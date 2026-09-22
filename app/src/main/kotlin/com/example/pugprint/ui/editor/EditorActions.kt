package com.example.pugprint.ui.editor

import com.example.pugprint.imaging.CropShape
import com.example.pugprint.imaging.DitherMode

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
    val onPrint: () -> Unit = {},
)
