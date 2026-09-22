package com.example.pugprint.ui.editor

import com.example.pugprint.R
import com.example.pugprint.imaging.CaptionPlacement
import com.example.pugprint.imaging.CropShape
import com.example.pugprint.imaging.DitherMode
import com.example.pugprint.imaging.StampSize
import com.example.pugprint.printer.DensityLevel

// The words on the editor's choice tiles.

internal fun shapeLabel(shape: CropShape): Int =
    when (shape) {
        CropShape.SQUARE -> R.string.editor_shape_square
        CropShape.TALL -> R.string.editor_shape_tall
        CropShape.WIDE -> R.string.editor_shape_wide
        CropShape.WHOLE -> R.string.editor_shape_whole
    }

internal fun modeLabel(mode: DitherMode): Int =
    when (mode) {
        DitherMode.PHOTO -> R.string.editor_style_photo
        DitherMode.DRAWING -> R.string.editor_style_drawing
    }

internal fun placementLabel(placement: CaptionPlacement): Int =
    when (placement) {
        CaptionPlacement.TOP -> R.string.editor_words_top
        CaptionPlacement.BOTTOM -> R.string.editor_words_bottom
    }

internal fun densityLabel(level: DensityLevel): Int =
    when (level) {
        DensityLevel.LIGHT -> R.string.editor_density_light
        DensityLevel.MEDIUM -> R.string.editor_density_medium
        DensityLevel.DARK -> R.string.editor_density_dark
    }

internal fun stampSizeLabel(size: StampSize): Int =
    when (size) {
        StampSize.SMALL -> R.string.editor_stamp_small
        StampSize.MEDIUM -> R.string.editor_stamp_medium
        StampSize.BIG -> R.string.editor_stamp_big
    }
