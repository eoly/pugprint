package com.example.pugprint.imaging

import kotlin.math.ceil
import kotlin.math.sqrt

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

    /**
     * On a round sticker the words may take at most this much of the width, so the band stays a
     * cap at the edge instead of climbing to the middle where the circle is widest.
     */
    public const val CIRCLE_CAPTION_WIDTH: Float = 0.7f

    /**
     * ...and the cap (curve inset, letters, padding) at most this much of the height: the letters
     * shrink a step at a time until it fits, so a long caption stays a cap rather than a half.
     */
    public const val CIRCLE_CAPTION_HEIGHT: Float = 0.4f

    /**
     * @param width dots across the sticker; @param maxRows tallest it may be (see [ImagePipeline.render]).
     * @param shape the sticker's outline: a [LabelShape.CIRCLE] keeps the caption inside the curve and
     * clears everything outside it, so what comes back is exactly what lands on the label.
     */
    public fun render(
        sticker: Sticker,
        width: Int = ImagePipeline.PRINT_WIDTH,
        maxRows: Int = ImagePipeline.MAX_ROWS,
        shape: LabelShape = LabelShape.RECTANGLE,
    ): MonoBitmap {
        val picture = ImagePipeline.render(sticker.image, sticker.crop, sticker.mode, width, maxRows)
        val caption = sticker.caption?.takeUnless { it.isBlank }
        if (caption == null && sticker.stamps.isEmpty() && shape == LabelShape.RECTANGLE) return picture
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
        if (caption != null) drawCaption(canvas, caption, shape)
        if (shape == LabelShape.CIRCLE) canvas.clearOutsideEllipse()
        return canvas.toMonoBitmap()
    }

    private fun drawCaption(
        canvas: BitCanvas,
        caption: Caption,
        shape: LabelShape,
    ) {
        val font = FontCatalog.byId(caption.fontId)
        val maxWidth =
            when (shape) {
                LabelShape.RECTANGLE -> canvas.width
                LabelShape.CIRCLE -> (canvas.width * CIRCLE_CAPTION_WIDTH).toInt()
            } - 2 * CAPTION_PADDING
        // The letters, and how far in from the edge they start: the padding on a rectangle; on a
        // circle, far enough that the chord under the letters' outer row is wider than the letters.
        val (block, inset) =
            when (shape) {
                LabelShape.RECTANGLE ->
                    (TextRasterizer.renderBlock(caption.text, font, maxWidth) ?: return) to CAPTION_PADDING
                LabelShape.CIRCLE -> circleBlock(canvas, caption.text, font, maxWidth) ?: return
            }
        val bandHeight = (inset + block.height + CAPTION_PADDING).coerceAtMost(canvas.height)
        val top = caption.placement == CaptionPlacement.TOP
        val bandTop = if (top) 0 else canvas.height - bandHeight
        canvas.fillRect(0, bandTop, canvas.width, bandHeight, isBlack = false)
        val textTop = if (top) inset else canvas.height - inset - block.height
        canvas.drawBlack(block, (canvas.width - block.width) / 2, textTop)
    }

    /** The largest letters whose cap fits [CIRCLE_CAPTION_HEIGHT], with their inset; the smallest if none does. */
    private fun circleBlock(
        canvas: BitCanvas,
        text: String,
        font: PixelFont,
        maxWidth: Int,
    ): Pair<BitCanvas, Int>? {
        val capLimit = canvas.height * CIRCLE_CAPTION_HEIGHT
        var fit: Pair<BitCanvas, Int>? = null
        for (scale in TextRasterizer.MAX_SCALE downTo TextRasterizer.MIN_SCALE) {
            val block = TextRasterizer.renderBlock(text, font, maxWidth, maxScale = scale) ?: return null
            fit = block to circleInset(canvas, block.width)
            if (fit.second + block.height + CAPTION_PADDING <= capLimit) break
        }
        return fit
    }

    /**
     * Rows in from the top or bottom of the inscribed ellipse at which a chord is [CAPTION_PADDING]
     * wider on each side than [textWidth]; never less than the padding.
     */
    private fun circleInset(
        canvas: BitCanvas,
        textWidth: Int,
    ): Int {
        val rx = canvas.width / 2.0
        val ry = canvas.height / 2.0
        val halfChord = ((textWidth / 2.0 + CAPTION_PADDING) / rx).coerceAtMost(1.0)
        val inset = ry * (1.0 - sqrt(1.0 - halfChord * halfChord))
        return ceil(inset).toInt().coerceAtLeast(CAPTION_PADDING)
    }
}
