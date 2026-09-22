package com.example.pugprint.imaging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DitherTest {
    private fun MonoBitmap.blackCount(): Int =
        (0 until height).sumOf { y -> (0 until width).count { x -> isBlack(x, y) } }

    @Test
    fun `threshold splits at 128 by default`() {
        val image = GrayImage.generate(4, 1) { x, _ -> intArrayOf(0, 127, 128, 255)[x] }
        val mono = Dither.threshold(image)
        assertTrue(mono.isBlack(0, 0))
        assertTrue(mono.isBlack(1, 0))
        assertFalse(mono.isBlack(2, 0))
        assertFalse(mono.isBlack(3, 0))
        assertEquals(1, Dither.threshold(image, level = 100).blackCount())
    }

    @Test
    fun `floyd-steinberg keeps pure black and white as they are`() {
        val white = GrayImage.generate(40, 10) { _, _ -> 255 }
        val black = GrayImage.generate(40, 10) { _, _ -> 0 }
        assertEquals(0, Dither.floydSteinberg(white).blackCount())
        assertEquals(400, Dither.floydSteinberg(black).blackCount())
    }

    @Test
    fun `floyd-steinberg turns mid-grey into about half dots`() {
        val grey = GrayImage.generate(64, 64) { _, _ -> 128 }
        val fraction = Dither.floydSteinberg(grey).blackCount() / 4096.0
        assertTrue(fraction in 0.45..0.55, "black fraction $fraction")
    }

    @Test
    fun `floyd-steinberg preserves tone across a ramp`() {
        val ramp = GrayImage.generate(256, 32) { x, _ -> x }
        val mono = Dither.floydSteinberg(ramp)

        fun blackFraction(from: Int): Double =
            (0 until 32).sumOf { y -> (from until from + 64).count { x -> mono.isBlack(x, y) } } / (64.0 * 32)
        assertTrue(blackFraction(0) > 0.8, "dark end ${blackFraction(0)}")
        assertTrue(blackFraction(192) < 0.2, "light end ${blackFraction(192)}")
        assertTrue(blackFraction(0) > blackFraction(64) && blackFraction(64) > blackFraction(128))
    }

    @Test
    fun `apply picks the mode`() {
        val image = GrayImage.generate(8, 8) { _, _ -> 200 }
        assertEquals(0, Dither.apply(image, DitherMode.DRAWING).blackCount())
        val photoDots = Dither.apply(image, DitherMode.PHOTO).blackCount()
        assertTrue(photoDots in 8..20, "expected ~22% dots for luma 200, got $photoDots")
    }

    @Test
    fun `photo mode matches the golden PBM`() =
        Pbm.assertMatchesGolden("dither_photo", Dither.apply(SyntheticPhoto.render(), DitherMode.PHOTO))

    @Test
    fun `drawing mode matches the golden PBM`() =
        Pbm.assertMatchesGolden("dither_drawing", Dither.apply(SyntheticPhoto.render(), DitherMode.DRAWING))
}
