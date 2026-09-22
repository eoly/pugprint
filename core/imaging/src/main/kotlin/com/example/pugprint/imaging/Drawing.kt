package com.example.pugprint.imaging

import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** A point on the drawing, as fractions of its width and height (0.0 = left/top, 1.0 = right/bottom). */
public data class DrawPoint(
    val x: Float,
    val y: Float,
)

private const val THIN_DOTS = 6
private const val MEDIUM_DOTS = 14
private const val FAT_DOTS = 28

/** How wide a stroke prints, in dots across a [StrokeRasterizer.SIZE]-dot drawing. */
public enum class BrushSize(
    public val dots: Int,
) {
    THIN(THIN_DOTS),
    MEDIUM(MEDIUM_DOTS),
    FAT(FAT_DOTS),
}

/** One finger-down-to-finger-up mark: a round-capped polyline, black or (for the eraser) white. */
public data class Stroke(
    val points: List<DrawPoint>,
    val size: BrushSize = BrushSize.MEDIUM,
    val erase: Boolean = false,
) {
    init {
        require(points.isNotEmpty()) { "A stroke needs at least one point" }
    }

    public fun plus(point: DrawPoint): Stroke = copy(points = points + point)
}

/** A kid's drawing: strokes in the order they were made. Immutable; every edit is a new value. */
public data class Drawing(
    val strokes: List<Stroke> = emptyList(),
) {
    val isEmpty: Boolean get() = strokes.isEmpty()

    public fun plus(stroke: Stroke): Drawing = copy(strokes = strokes + stroke)

    /** Without the newest stroke; the same drawing when there is none. */
    public fun undo(): Drawing = if (strokes.isEmpty()) this else copy(strokes = strokes.dropLast(1))
}

/** Turns a [Drawing] into a black-on-white [GrayImage] the sticker pipeline treats like any picture. */
public object StrokeRasterizer {
    /** Drawings are square and head-width, so they print one sticker with no scaling. */
    public const val SIZE: Int = ImagePipeline.PRINT_WIDTH

    public fun render(
        drawing: Drawing,
        width: Int = SIZE,
        height: Int = SIZE,
    ): GrayImage {
        val sheet = Sheet(width, height)
        drawing.strokes.forEach(sheet::paint)
        return sheet.toImage()
    }

    /** The paper being drawn on: white until a stroke touches it. */
    private class Sheet(
        val width: Int,
        val height: Int,
    ) {
        private val luma = ByteArray(width * height) { GrayImage.WHITE.toByte() }

        fun toImage() = GrayImage(width, height, luma)

        /** Discs at every point and along every segment, close enough together to look like a line. */
        fun paint(stroke: Stroke) {
            val radius = stroke.size.dots / 2f
            val colour = (if (stroke.erase) GrayImage.WHITE else GrayImage.BLACK).toByte()
            val dots = stroke.points.map { it.x * width to it.y * height }
            dots.forEach { (x, y) -> disc(x, y, radius, colour) }
            dots.zipWithNext { (x0, y0), (x1, y1) ->
                val length = hypot(x1 - x0, y1 - y0)
                val steps = ceil(length / (radius / 2f).coerceAtLeast(1f)).toInt()
                for (i in 1 until steps) {
                    val t = i.toFloat() / steps
                    disc(x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, radius, colour)
                }
            }
        }

        /** A filled circle, clipped to the sheet. */
        private fun disc(
            cx: Float,
            cy: Float,
            radius: Float,
            colour: Byte,
        ) {
            val top = (cy - radius).roundToInt().coerceAtLeast(0)
            val bottom = (cy + radius).roundToInt().coerceAtMost(height - 1)
            for (y in top..bottom) {
                val dy = y - cy
                val half = sqrt((radius * radius - dy * dy).coerceAtLeast(0f))
                val left = (cx - half).roundToInt().coerceAtLeast(0)
                val right = (cx + half).roundToInt().coerceAtMost(width - 1)
                for (x in left..right) luma[y * width + x] = colour
            }
        }
    }
}
