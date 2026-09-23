package com.example.pugprint.imaging

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StickerRendererTest {
    private val photo = SyntheticPhoto.render()

    /** The synthetic photo is 128 × 96; round stickers want a square. */
    private val squareCrop = CropRect(0, 0, 96, 96)

    @Test
    fun `without a caption the sticker is the plain pipeline output`() {
        val plain = ImagePipeline.render(photo, null, DitherMode.PHOTO)
        val sticker = StickerRenderer.render(Sticker(photo))
        assertArrayEquals(plain.packed, sticker.packed)
    }

    @Test
    fun `a blank caption changes nothing`() {
        val plain = ImagePipeline.render(photo)
        val sticker = StickerRenderer.render(Sticker(photo, caption = Caption("   ")))
        assertArrayEquals(plain.packed, sticker.packed)
    }

    @Test
    fun `a bottom caption paints a white band with black letters at the bottom`() {
        val sticker = StickerRenderer.render(Sticker(photo, caption = Caption("WOOF")))
        val bandTop = sticker.height - (7 * TextRasterizer.MAX_SCALE + 2 * StickerRenderer.CAPTION_PADDING)
        // Band corners are white even though the photo is dark there.
        assertFalse(sticker.isBlack(0, sticker.height - 1))
        assertFalse(sticker.isBlack(sticker.width - 1, bandTop))
        // Letters: something is black in the middle rows of the band.
        val middle = bandTop + StickerRenderer.CAPTION_PADDING + 7 * TextRasterizer.MAX_SCALE / 2
        assertTrue((0 until sticker.width).any { sticker.isBlack(it, middle) })
        Pbm.assertMatchesGolden("sticker_caption_bottom", sticker)
    }

    @Test
    fun `a top caption sits at the top`() {
        val sticker = StickerRenderer.render(Sticker(photo, caption = Caption("Hello!", CaptionPlacement.TOP)))
        assertFalse(sticker.isBlack(0, 0))
        assertFalse(sticker.isBlack(sticker.width - 1, 0))
        Pbm.assertMatchesGolden("sticker_caption_top", sticker)
    }

    @Test
    fun `stamps are drawn with a white halo, under the caption, and clipped at the edge`() {
        val sticker =
            StickerRenderer.render(
                Sticker(
                    photo,
                    caption = Caption("Woof"),
                    stamps =
                        listOf(
                            StampPlacement("heart", 0.25f, 0.3f, StampSize.BIG),
                            StampPlacement("star", 1f, 0f, StampSize.MEDIUM), // hangs off the top-right corner
                            StampPlacement("dragon"), // not in the catalog: draws nothing, does not crash
                        ),
                ),
            )
        // The heart's halo: the art row above the first dot row is white across the heart's width.
        val heartLeft = (0.25f * sticker.width).toInt() - 16 * 8 / 2
        val heartTop = (0.3f * sticker.height).toInt() - 16 * 8 / 2
        for (x in heartLeft + 2 * 8 until heartLeft + 6 * 8) {
            assertFalse(
                sticker.isBlack(x, heartTop + 0 * 8 + 4),
                "halo at $x",
            )
        }
        // A dot inside the heart is black.
        assertTrue(sticker.isBlack(heartLeft + 8 * 8, heartTop + 5 * 8))
        Pbm.assertMatchesGolden("sticker_stamps", sticker)
    }

    @Test
    fun `a round sticker is white outside the inscribed circle and unchanged inside`() {
        val square = StickerRenderer.render(Sticker(photo, crop = squareCrop))
        val round = StickerRenderer.render(Sticker(photo, crop = squareCrop), shape = LabelShape.CIRCLE)
        assertEquals(square.width, round.width)
        assertEquals(square.width, round.height)
        val r = square.width / 2.0
        var kept = 0
        for (y in 0 until square.height) {
            for (x in 0 until square.width) {
                val inside = (x + 0.5 - r).let { it * it } + (y + 0.5 - r).let { it * it } <= r * r
                if (inside) {
                    assertEquals(square.isBlack(x, y), round.isBlack(x, y), "dot $x,$y should be untouched")
                    kept++
                } else {
                    assertFalse(round.isBlack(x, y), "dot $x,$y is outside the circle")
                }
            }
        }
        // About π/4 of the square survives.
        assertTrue(kept in (0.78 * square.width * square.height).toInt()..(0.79 * square.width * square.height).toInt())
    }

    @Test
    fun `on a round sticker the caption is a white cap whose letters fit inside the curve`() {
        val sticker =
            StickerRenderer.render(
                Sticker(
                    photo,
                    crop = squareCrop,
                    caption = Caption("WOOF WOOF"),
                    stamps = listOf(StampPlacement("heart", 0.5f, 0.3f)),
                ),
                width = 365,
                maxRows = 365,
                shape = LabelShape.CIRCLE,
            )
        assertEquals(365, sticker.width)
        assertEquals(365, sticker.height)
        // Every black dot in the caption region lies inside the circle, and none touches the last row.
        val r = 365 / 2.0
        assertTrue((0 until 365).none { sticker.isBlack(it, 364) })
        for (y in 300 until 365) {
            for (x in 0 until 365) {
                if (sticker.isBlack(x, y)) {
                    assertTrue((x + 0.5 - r).let { it * it } + (y + 0.5 - r).let { it * it } <= r * r, "$x,$y")
                }
            }
        }
        // There are letters: something is black in the bottom third but above the white cap's edge.
        assertTrue((300 until 365).any { y -> (100 until 265).any { x -> sticker.isBlack(x, y) } })
        Pbm.assertMatchesGolden("sticker_circle_caption", sticker)
    }

    @Test
    fun `on a round sticker a top caption is a cap at the top`() {
        val sticker =
            StickerRenderer.render(
                Sticker(photo, crop = squareCrop, caption = Caption("Hi", CaptionPlacement.TOP)),
                width = 365,
                maxRows = 365,
                shape = LabelShape.CIRCLE,
            )
        assertTrue((0 until 365).none { sticker.isBlack(it, 0) })
        // The first rows inside the circle are white (the cap), then letters, then picture.
        val firstBlackRow = (0 until 365).first { y -> (0 until 365).any { x -> sticker.isBlack(x, y) } }
        assertTrue(firstBlackRow >= StickerRenderer.CAPTION_PADDING, "cap starts at row $firstBlackRow")
        assertTrue(firstBlackRow < 100, "letters should sit near the top, first black row is $firstBlackRow")
    }

    @Test
    fun `a placement nudge stays on the sticker`() {
        val moved = StampPlacement("heart", 0.9f, 0.1f).movedBy(0.5f, -0.5f)
        assertEquals(1f, moved.centerX)
        assertEquals(0f, moved.centerY)
    }
}
