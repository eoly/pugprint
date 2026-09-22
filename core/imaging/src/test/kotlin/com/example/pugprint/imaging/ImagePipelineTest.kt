package com.example.pugprint.imaging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ImagePipelineTest {
    @Test
    fun `output is head-width with the source aspect`() {
        val printed = ImagePipeline.render(SyntheticPhoto.render())
        assertEquals(384, printed.width)
        assertEquals(288, printed.height)
    }

    @Test
    fun `a crop is applied before scaling`() {
        val image = GrayImage.generate(200, 200) { x, _ -> if (x < 100) 0 else 255 }
        val leftHalf = ImagePipeline.render(image, CropRect(0, 0, 100, 200), DitherMode.DRAWING)
        assertEquals(384, leftHalf.width)
        assertEquals(768, leftHalf.height)
        assertTrue(leftHalf.isBlack(383, 0))
        val rightHalf = ImagePipeline.render(image, CropRect(100, 0, 100, 200), DitherMode.DRAWING)
        assertFalse(rightHalf.isBlack(0, 0))
    }

    @Test
    fun `very tall pictures are trimmed to the row cap`() {
        val strip = GrayImage.generate(10, 100) { _, _ -> 0 }
        val printed = ImagePipeline.render(strip)
        assertEquals(ImagePipeline.MAX_ROWS, printed.height)
    }

    @Test
    fun `mode selects the dither`() {
        val grey = GrayImage.generate(384, 16) { _, _ -> 200 }
        val drawing = ImagePipeline.render(grey, mode = DitherMode.DRAWING)
        val photo = ImagePipeline.render(grey, mode = DitherMode.PHOTO)

        fun MonoBitmap.anyBlack() = (0 until height).any { y -> (0 until width).any { x -> isBlack(x, y) } }
        assertFalse(drawing.anyBlack())
        assertTrue(photo.anyBlack())
    }
}
