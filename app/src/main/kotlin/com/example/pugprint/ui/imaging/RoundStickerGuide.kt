package com.example.pugprint.ui.imaging

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Fades what lies outside the circle inscribed in the canvas and draws the circle's edge, so on
 * a round sticker roll the kid sees where the sticker ends while fitting a photo or drawing.
 */
fun DrawScope.drawRoundStickerGuide(colour: Color) {
    val outside =
        Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(Rect(Offset.Zero, size))
            addOval(Rect(Offset.Zero, size))
        }
    drawPath(outside, Color.White.copy(alpha = OUTSIDE_ALPHA))
    drawCircle(colour, radius = size.minDimension / 2f, style = Stroke(width = GUIDE_STROKE.toPx()))
}

private const val OUTSIDE_ALPHA = 0.7f
private val GUIDE_STROKE = 3.dp
