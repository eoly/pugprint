package com.example.pugprint.ui.draw

import com.example.pugprint.imaging.BrushSize
import com.example.pugprint.imaging.DrawPoint

/** What the draw screen can ask for; wired to the ViewModel by `DrawRoute`. */
data class DrawActions(
    val onBack: () -> Unit = {},
    val onStrokeStart: (DrawPoint) -> Unit = {},
    val onStrokeMove: (DrawPoint) -> Unit = {},
    val onStrokeEnd: () -> Unit = {},
    val onBrush: (BrushSize) -> Unit = {},
    val onTool: (DrawTool) -> Unit = {},
    val onUndo: () -> Unit = {},
    val onClear: () -> Unit = {},
    val onNext: () -> Unit = {},
)
