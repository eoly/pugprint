package com.example.pugprint.imaging

/** Where a caption sits on the sticker. */
public enum class CaptionPlacement { TOP, BOTTOM }

/**
 * Words on a sticker: a white band with black letters, so they read over any picture.
 *
 * @property fontId a [FontCatalog] id; unknown ids fall back to the default font.
 */
public data class Caption(
    val text: String,
    val placement: CaptionPlacement = CaptionPlacement.BOTTOM,
    val fontId: String = FontCatalog.default.id,
) {
    val isBlank: Boolean get() = text.isBlank()
}

/**
 * One stamp on the sticker. [centerX] / [centerY] are fractions of the sticker's width and
 * height (0.0 = left/top edge, 1.0 = right/bottom), so a placement survives any crop or scale.
 *
 * @property stampId a [StampCatalog] id; a placement whose stamp was removed draws nothing.
 */
public data class StampPlacement(
    val stampId: String,
    val centerX: Float = CENTRE,
    val centerY: Float = CENTRE,
    val size: StampSize = StampSize.MEDIUM,
) {
    /** The same stamp nudged by ([dx], [dy]) fractions, kept on the sticker. */
    public fun movedBy(
        dx: Float,
        dy: Float,
    ): StampPlacement = copy(centerX = (centerX + dx).coerceIn(0f, 1f), centerY = (centerY + dy).coerceIn(0f, 1f))

    public companion object {
        public const val CENTRE: Float = 0.5f
    }
}

/**
 * Everything that makes one sticker: the picture (cropped and dithered by [ImagePipeline])
 * plus the layers drawn on top, bottom to top: [stamps] then the [caption] band. New kid
 * features are new layers here; nothing below [StickerRenderer] changes.
 */
public data class Sticker(
    val image: GrayImage,
    val crop: CropRect? = null,
    val mode: DitherMode = DitherMode.PHOTO,
    val caption: Caption? = null,
    val stamps: List<StampPlacement> = emptyList(),
)

/** Turns a [Sticker] into the dots that print. Deterministic; covered by golden PBMs. */
public object StickerRenderer {
    /** White space around the letters inside the caption band. */
    public const val CAPTION_PADDING: Int = 12

    public fun render(sticker: Sticker): MonoBitmap {
        val picture = ImagePipeline.render(sticker.image, sticker.crop, sticker.mode)
        val caption = sticker.caption?.takeUnless { it.isBlank }
        if (caption == null && sticker.stamps.isEmpty()) return picture
        val canvas = BitCanvas.from(picture)
        sticker.stamps.forEach { placement ->
            val stamp = StampCatalog.byId(placement.stampId) ?: return@forEach
            StampRasterizer.draw(
                canvas,
                stamp,
                placement.size,
                centerX = (placement.centerX * canvas.width).toInt(),
                centerY = (placement.centerY * canvas.height).toInt(),
            )
        }
        if (caption != null) drawCaption(canvas, caption)
        return canvas.toMonoBitmap()
    }

    private fun drawCaption(
        canvas: BitCanvas,
        caption: Caption,
    ) {
        val font = FontCatalog.byId(caption.fontId)
        val block = TextRasterizer.renderBlock(caption.text, font, canvas.width - 2 * CAPTION_PADDING) ?: return
        val bandHeight = (block.height + 2 * CAPTION_PADDING).coerceAtMost(canvas.height)
        val bandTop = if (caption.placement == CaptionPlacement.TOP) 0 else canvas.height - bandHeight
        canvas.fillRect(0, bandTop, canvas.width, bandHeight, isBlack = false)
        canvas.drawBlack(block, (canvas.width - block.width) / 2, bandTop + (bandHeight - block.height) / 2)
    }
}
