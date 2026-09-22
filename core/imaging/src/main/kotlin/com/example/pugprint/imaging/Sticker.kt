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
 * Everything that makes one sticker: the picture (cropped and dithered by [ImagePipeline])
 * plus the layers drawn on top. New kid features (stamps, drawings) are new layers here;
 * nothing below [StickerRenderer] changes.
 */
public data class Sticker(
    val image: GrayImage,
    val crop: CropRect? = null,
    val mode: DitherMode = DitherMode.PHOTO,
    val caption: Caption? = null,
)

/** Turns a [Sticker] into the dots that print. Deterministic; covered by golden PBMs. */
public object StickerRenderer {
    /** White space around the letters inside the caption band. */
    public const val CAPTION_PADDING: Int = 12

    public fun render(sticker: Sticker): MonoBitmap {
        val picture = ImagePipeline.render(sticker.image, sticker.crop, sticker.mode)
        val caption = sticker.caption?.takeUnless { it.isBlank } ?: return picture
        val canvas = BitCanvas.from(picture)
        drawCaption(canvas, caption)
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
