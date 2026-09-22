package com.example.pugprint.imaging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StrokeRasterizerTest {
    private fun GrayImage.isBlack(
        x: Int,
        y: Int,
    ) = this[x, y] < Dither.DEFAULT_THRESHOLD

    @Test
    fun `an empty drawing is all white at head size`() {
        val image = StrokeRasterizer.render(Drawing())
        assertEquals(384, image.width)
        assertEquals(384, image.height)
        assertTrue(image.luma.all { (it.toInt() and 0xFF) == GrayImage.WHITE })
    }

    @Test
    fun `a tap is a filled disc of the brush size`() {
        val image = StrokeRasterizer.render(Drawing().plus(Stroke(listOf(DrawPoint(0.5f, 0.5f)), BrushSize.FAT)))
        val c = 192
        val r = BrushSize.FAT.dots / 2
        assertTrue(image.isBlack(c, c))
        assertTrue(image.isBlack(c + r - 1, c))
        assertTrue(image.isBlack(c, c - r + 1))
        assertTrue(!image.isBlack(c + r + 2, c))
        assertTrue(!image.isBlack(c + r - 1, c + r - 1), "corner of the bounding box is outside the disc")
    }

    @Test
    fun `a line is solid between its points and clipped at the edge`() {
        val stroke = Stroke(listOf(DrawPoint(0.1f, 0.5f), DrawPoint(1.2f, 0.5f)), BrushSize.THIN)
        val image = StrokeRasterizer.render(Drawing().plus(stroke))
        for (x in 40 until 384 step 7) assertTrue(image.isBlack(x, 192), "gap at x=$x")
        assertTrue(!image.isBlack(200, 192 + BrushSize.THIN.dots))
    }

    @Test
    fun `the eraser paints white over earlier strokes`() {
        val drawing =
            Drawing()
                .plus(Stroke(listOf(DrawPoint(0.5f, 0.5f)), BrushSize.FAT))
                .plus(Stroke(listOf(DrawPoint(0.5f, 0.5f)), BrushSize.THIN, erase = true))
        val image = StrokeRasterizer.render(drawing)
        assertTrue(!image.isBlack(192, 192))
        assertTrue(image.isBlack(192 + BrushSize.FAT.dots / 2 - 1, 192))
    }

    @Test
    fun `undo drops the newest stroke and is a no-op when empty`() {
        val empty = Drawing()
        assertSame(empty, empty.undo())
        val one = empty.plus(Stroke(listOf(DrawPoint(0f, 0f))))
        assertEquals(empty, one.undo())
    }

    @Test
    fun `a doodle matches the golden`() {
        val face =
            Drawing()
                .plus(Stroke(circle(0.5f, 0.5f, 0.4f), BrushSize.MEDIUM))
                .plus(Stroke(listOf(DrawPoint(0.35f, 0.4f)), BrushSize.FAT))
                .plus(Stroke(listOf(DrawPoint(0.65f, 0.4f)), BrushSize.FAT))
                .plus(
                    Stroke(
                        listOf(DrawPoint(0.3f, 0.65f), DrawPoint(0.5f, 0.78f), DrawPoint(0.7f, 0.65f)),
                        BrushSize.THIN,
                    ),
                ).plus(Stroke(listOf(DrawPoint(0.5f, 0.78f)), BrushSize.MEDIUM, erase = true))
        val dots = Dither.apply(StrokeRasterizer.render(face), DitherMode.DRAWING)
        Pbm.assertMatchesGolden("drawing_face", dots)
    }

    private fun circle(
        cx: Float,
        cy: Float,
        r: Float,
    ): List<DrawPoint> =
        (0..36).map { i ->
            val a = i * 2 * Math.PI / 36
            DrawPoint(cx + (r * Math.cos(a)).toFloat(), cy + (r * Math.sin(a)).toFloat())
        }
}
