package com.example.pugprint.imaging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import kotlin.math.hypot

/** The rules every page in [ColoringPageCatalog.all] must follow; a new page is checked automatically. */
class ColoringPageCatalogTest {
    @Test
    fun `ids are unique, lowercase and never blank`() {
        val ids = ColoringPageCatalog.all.map { it.id }
        assertEquals(ids.toSet().size, ids.size, "duplicate page ids in $ids")
        ids.forEach { assertTrue(it.isNotBlank() && it == it.lowercase() && ' ' !in it, "bad page id '$it'") }
    }

    @Test
    fun `lookup by id, and a removed page is simply absent`() {
        ColoringPageCatalog.all.forEach { assertSame(it, ColoringPageCatalog.byId(it.id)) }
        assertNull(ColoringPageCatalog.byId("dragon"))
    }

    @TestFactory
    fun `every page has a name and lines, is mostly white, and keeps its ink inside the safe circle`() =
        ColoringPageCatalog.all.map { page ->
            DynamicTest.dynamicTest(page.id) {
                assertTrue(page.displayName.isNotBlank(), "${page.id} has no name")
                assertTrue(page.drawing.strokes.isNotEmpty(), "${page.id} has no lines")
                assertTrue(page.drawing.strokes.none { it.erase }, "${page.id} uses the eraser")
                val image = page.render()
                assertEquals(StrokeRasterizer.SIZE, image.width)
                assertEquals(StrokeRasterizer.SIZE, image.height)
                var ink = 0
                for (y in 0 until image.height) {
                    for (x in 0 until image.width) {
                        if (image[x, y] >= Dither.DEFAULT_THRESHOLD) continue
                        ink++
                        val dx = (x + 0.5f) / image.width - 0.5f
                        val dy = (y + 0.5f) / image.height - 0.5f
                        assertTrue(
                            hypot(dx, dy) <= ColoringPage.SAFE_RADIUS,
                            "${page.id} has ink outside the safe circle at ($x,$y)",
                        )
                    }
                }
                val fraction = ink.toFloat() / (image.width * image.height)
                assertTrue(fraction in MIN_INK..MAX_INK, "${page.id} is ${"%.1f".format(fraction * 100)}% ink")
            }
        }

    @TestFactory
    fun `every page matches its golden`() =
        ColoringPageCatalog.all.map { page ->
            DynamicTest.dynamicTest(page.id) {
                Pbm.assertMatchesGolden("coloring_${page.id}", Dither.apply(page.render(), DitherMode.DRAWING))
            }
        }

    @TestFactory
    fun `every page prints whole on the round roll`() =
        ColoringPageCatalog.all.map { page ->
            DynamicTest.dynamicTest(page.id) {
                val roll = StickerRollCatalog.CircleStandard
                val round = roll.render(Sticker(page.render(), mode = DitherMode.DRAWING))
                val square = ImagePipeline.render(page.render(), mode = DitherMode.DRAWING, width = roll.contentWidth)
                assertEquals(square.count(), round.count(), "${page.id} loses ink to the circle")
            }
        }

    @Test
    fun `outline shapes close, sweep and point the way they say`() {
        val drawing =
            Outline.build {
                circle(cx = 0.5f, cy = 0.5f, r = 0.2f)
                arc(cx = 0.5f, cy = 0.5f, rx = 0.2f, ry = 0.1f, fromDegrees = 0f, toDegrees = 90f)
                star(cx = 0.5f, cy = 0.5f, outer = 0.4f, inner = 0.2f)
                loop(BrushSize.THIN, 0f to 0f, 1f to 0f, 1f to 1f)
                dot(x = 0.3f, y = 0.3f, size = BrushSize.THIN)
            }
        val (circle, arc, star) = drawing.strokes
        val loop = drawing.strokes[3]
        val dot = drawing.strokes[4]
        assertEquals(49, circle.points.size)
        assertEquals(circle.points.first().x, circle.points.last().x, 1e-6f)
        assertEquals(circle.points.first().y, circle.points.last().y, 1e-6f)
        assertEquals(DrawPoint(0.7f, 0.5f), arc.points.first())
        assertEquals(0.5f, arc.points.last().x, 1e-6f)
        assertEquals(0.6f, arc.points.last().y, 1e-6f)
        assertEquals(11, star.points.size)
        assertEquals(DrawPoint(0.5f, 0.1f), star.points.first().rounded())
        assertEquals(star.points.first(), star.points.last())
        assertEquals(listOf(DrawPoint(0f, 0f), DrawPoint(1f, 0f), DrawPoint(1f, 1f), DrawPoint(0f, 0f)), loop.points)
        assertEquals(BrushSize.THIN, dot.size)
        assertFalse(dot.erase)
    }

    private fun DrawPoint.rounded() = DrawPoint(Math.round(x * 1000) / 1000f, Math.round(y * 1000) / 1000f)

    private fun MonoBitmap.count(): Int {
        var ink = 0
        for (y in 0 until height) for (x in 0 until width) if (isBlack(x, y)) ink++
        return ink
    }

    private companion object {
        /** Outlines, not a stamp: enough to see, plenty left to colour. */
        const val MIN_INK = 0.03f
        const val MAX_INK = 0.30f
    }
}
