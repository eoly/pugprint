package com.example.pugprint.imaging

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StickerRendererTest {
    private val photo = SyntheticPhoto.render()

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
}
