package com.example.pugprint.imaging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CropWindowTest {
    private val landscape = CropWindow(imageWidth = 400, imageHeight = 200)

    @Test
    fun `a square frame on a landscape picture takes the centred full-height square`() {
        assertEquals(CropRect(100, 0, 200, 200), landscape.cropRect())
        assertEquals(CropRect(0, 100, 200, 200), CropWindow(200, 400).cropRect())
    }

    @Test
    fun `whole keeps the picture's own aspect, capped`() {
        assertEquals(CropRect(0, 0, 400, 200), landscape.withShape(CropShape.WHOLE).cropRect())
        assertEquals(0.5f, landscape.withShape(CropShape.WHOLE).frameAspect)
        assertEquals(CropWindow.MAX_WHOLE_ASPECT, CropWindow(100, 1000, CropShape.WHOLE).frameAspect)
        assertEquals(CropWindow.MIN_WHOLE_ASPECT, CropWindow(1000, 100, CropShape.WHOLE).frameAspect)
        // A capped frame still fills: 100 px wide, 300 tall out of 1000.
        assertEquals(CropRect(0, 350, 100, 300), CropWindow(100, 1000, CropShape.WHOLE).cropRect())
    }

    @Test
    fun `tall and wide frames`() {
        assertEquals(CropRect(125, 0, 150, 200), landscape.withShape(CropShape.TALL).cropRect())
        assertEquals(CropRect(67, 0, 267, 200), landscape.withShape(CropShape.WIDE).cropRect())
    }

    @Test
    fun `zooming halves the window and panning slides it within the picture`() {
        val zoomed = landscape.transformed(zoomBy = 2f, panDx = 0f, panDy = 0f)
        assertEquals(CropRect(150, 50, 100, 100), zoomed.cropRect())

        val panned = zoomed.transformed(zoomBy = 1f, panDx = 0.25f, panDy = -0.1f)
        // +0.25 frame widths of pan moves the window left by a quarter of its width.
        assertEquals(CropRect(125, 60, 100, 100), panned.cropRect())

        val slammed = zoomed.transformed(zoomBy = 1f, panDx = -99f, panDy = 99f)
        assertEquals(CropRect(300, 0, 100, 100), slammed.cropRect())
        assertEquals(-slammed.maxPanX, slammed.panX)
    }

    @Test
    fun `zoom is clamped between 1 and the maximum`() {
        assertEquals(1f, landscape.transformed(zoomBy = 0.1f, panDx = 0f, panDy = 0f).zoom)
        assertEquals(CropWindow.MAX_ZOOM, landscape.transformed(zoomBy = 100f, panDx = 0f, panDy = 0f).zoom)
        // Zoom back to 1 first, then the pan is clamped for that zoom (a 2:1 picture in a square frame can pan ±0.5).
        assertEquals(landscape.copy(panX = 0.5f), landscape.copy(zoom = 0.5f, panX = 5f).normalized())
    }

    @Test
    fun `zooming about a focal point keeps that picture point under the finger`() {
        // Focal point: a quarter frame to the right of centre.
        val focalX = 0.25f
        val before = landscape
        val imagePointBefore = (focalX - before.panX) / before.scale
        val after = before.transformed(zoomBy = 1.5f, panDx = 0f, panDy = 0f, focalX = focalX, focalY = 0f)
        val imagePointAfter = (focalX - after.panX) / after.scale
        assertEquals(imagePointBefore, imagePointAfter, 0.01f)
    }

    @Test
    fun `changing shape or rotating starts the crop fresh`() {
        val moved = landscape.transformed(zoomBy = 2f, panDx = 0.1f, panDy = 0.1f)
        assertEquals(CropWindow(400, 200, CropShape.WIDE), moved.withShape(CropShape.WIDE))
        assertEquals(CropWindow(200, 400, CropShape.SQUARE), moved.rotated())
    }

    @Test
    fun `crop rect never leaves the picture even for odd sizes`() {
        for (shape in CropShape.entries) {
            val window = CropWindow(37, 91, shape).transformed(zoomBy = 3.3f, panDx = 5f, panDy = -5f)
            val rect = window.cropRect()
            assertTrue(rect.right <= 37 && rect.bottom <= 91, "$shape → $rect")
        }
    }
}
