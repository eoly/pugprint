package com.example.pugprint.imaging

import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * A picture to print and colour in with crayons: thick black outlines, white inside. It is a
 * [Drawing] (ADR 0008), so it prints through the same path as a kid's own drawing.
 *
 * @property id stable key; never rename once shipped.
 */
public class ColoringPage(
    public val id: String,
    public val displayName: String,
    public val drawing: Drawing,
) {
    /** The page as a picture, black on white, [StrokeRasterizer.SIZE] dots square. */
    public fun render(): GrayImage = StrokeRasterizer.render(drawing)

    public companion object {
        /**
         * Every page keeps its ink within this fraction of the page's width from the centre, so it
         * prints whole on square and round labels alike; `ColoringPageCatalogTest` checks.
         */
        public const val SAFE_RADIUS: Float = 0.47f
    }
}

/**
 * Builds a [Drawing] out of shapes given in page fractions (0.0 = left/top edge, 1.0 =
 * right/bottom), each a [Stroke] of the chosen [BrushSize]. Angles are degrees, clockwise on
 * screen: 0 = right, 90 = down, 180 = left, 270 = up.
 */
public class Outline private constructor() {
    private val strokes = mutableListOf<Stroke>()

    /** A single round dot. */
    public fun dot(
        x: Float,
        y: Float,
        size: BrushSize = BrushSize.MEDIUM,
    ) {
        strokes += Stroke(listOf(DrawPoint(x, y)), size)
    }

    /** A straight line. */
    public fun line(
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        size: BrushSize = BrushSize.MEDIUM,
    ) {
        strokes += Stroke(listOf(DrawPoint(x0, y0), DrawPoint(x1, y1)), size)
    }

    /** Straight segments through [points] (`x to y` pairs), in order. */
    public fun path(
        size: BrushSize,
        vararg points: Pair<Float, Float>,
    ) {
        require(points.size >= 2) { "A path needs at least two points" }
        strokes += Stroke(points.map { (x, y) -> DrawPoint(x, y) }, size)
    }

    /** A [path] joined back to its first point. */
    public fun loop(
        size: BrushSize,
        vararg points: Pair<Float, Float>,
    ): Unit = loop(size, points.toList())

    private fun loop(
        size: BrushSize,
        points: List<Pair<Float, Float>>,
    ) {
        require(points.size >= 2) { "A loop needs at least two points" }
        strokes += Stroke((points + points.first()).map { (x, y) -> DrawPoint(x, y) }, size)
    }

    public fun circle(
        cx: Float,
        cy: Float,
        r: Float,
        size: BrushSize = BrushSize.MEDIUM,
    ): Unit = ellipse(cx, cy, r, r, size)

    public fun ellipse(
        cx: Float,
        cy: Float,
        rx: Float,
        ry: Float,
        size: BrushSize = BrushSize.MEDIUM,
    ): Unit = arc(cx, cy, rx, ry, fromDegrees = 0f, toDegrees = FULL_TURN, size = size)

    /** The part of an ellipse from [fromDegrees] to [toDegrees]. */
    @Suppress("LongParameterList") // a shape is its geometry
    public fun arc(
        cx: Float,
        cy: Float,
        rx: Float,
        ry: Float,
        fromDegrees: Float,
        toDegrees: Float,
        size: BrushSize = BrushSize.MEDIUM,
    ) {
        val sweep = toDegrees - fromDegrees
        val steps = (sweep / DEGREES_PER_SEGMENT).roundToInt().coerceAtLeast(1)
        val points =
            (0..steps).map { i ->
                val a = Math.toRadians((fromDegrees + sweep * i / steps).toDouble())
                DrawPoint(cx + (rx * cos(a)).toFloat(), cy + (ry * sin(a)).toFloat())
            }
        strokes += Stroke(points, size)
    }

    /** A [points]-pointed star: tips on [outer], corners on [inner], the first tip straight up. */
    @Suppress("LongParameterList")
    public fun star(
        cx: Float,
        cy: Float,
        outer: Float,
        inner: Float,
        points: Int = STAR_POINTS,
        size: BrushSize = BrushSize.MEDIUM,
    ) {
        val corners =
            (0 until 2 * points).map { i ->
                val r = if (i % 2 == 0) outer else inner
                val a = Math.toRadians(UP_DEGREES + HALF_TURN * i / points.toDouble())
                cx + (r * cos(a)).toFloat() to cy + (r * sin(a)).toFloat()
            }
        loop(size, corners)
    }

    public companion object {
        private const val FULL_TURN = 360f
        private const val HALF_TURN = 180.0
        private const val UP_DEGREES = -90.0
        private const val STAR_POINTS = 5

        /** A full circle is 48 segments: at sticker size a 14-dot line hides the corners. */
        private const val DEGREES_PER_SEGMENT = 7.5f

        public fun build(block: Outline.() -> Unit): Drawing = Drawing(Outline().apply(block).strokes.toList())
    }
}
