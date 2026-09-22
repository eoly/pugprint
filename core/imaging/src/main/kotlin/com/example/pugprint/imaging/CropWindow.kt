package com.example.pugprint.imaging

/** The sticker outline the picture is fitted into; [aspect] is frame height ÷ width. */
public enum class CropShape {
    SQUARE,
    TALL,
    WIDE,

    /** The whole picture, at its own aspect (capped so a panorama or a screenshot stays printable). */
    WHOLE,
    ;

    public val aspect: Float?
        get() =
            when (this) {
                SQUARE -> 1f
                TALL -> TALL_ASPECT
                WIDE -> WIDE_ASPECT
                WHOLE -> null
            }

    private companion object {
        const val TALL_ASPECT = 4f / 3f
        const val WIDE_ASPECT = 3f / 4f
    }
}

/**
 * The pan/zoom crop model behind the editor: an image of [imageWidth] × [imageHeight] is
 * scaled to *cover* a frame of [shape], then zoomed by [zoom] (≥ 1) and shifted by
 * ([panX], [panY]). All frame-space lengths are in frame widths, so the same window works
 * whatever the on-screen size of the frame. Pure maths — the Compose layer only converts
 * pixels to frame widths. [cropRect] is the frame mapped back onto image pixels.
 */
public data class CropWindow(
    val imageWidth: Int,
    val imageHeight: Int,
    val shape: CropShape = CropShape.SQUARE,
    val zoom: Float = 1f,
    val panX: Float = 0f,
    val panY: Float = 0f,
) {
    init {
        require(imageWidth > 0 && imageHeight > 0) { "Image must be non-empty: ${imageWidth}x$imageHeight" }
    }

    /** Frame height in frame widths. */
    public val frameAspect: Float =
        shape.aspect ?: (imageHeight.toFloat() / imageWidth).coerceIn(MIN_WHOLE_ASPECT, MAX_WHOLE_ASPECT)

    /** Frame widths per image pixel at the current zoom. */
    public val scale: Float get() = zoom * maxOf(1f / imageWidth, frameAspect / imageHeight)

    /** Largest |panX| that keeps the frame covered. */
    public val maxPanX: Float get() = ((imageWidth * scale - 1f) / 2f).coerceAtLeast(0f)

    /** Largest |panY| that keeps the frame covered. */
    public val maxPanY: Float get() = ((imageHeight * scale - frameAspect) / 2f).coerceAtLeast(0f)

    /** Where the image's top-left corner lands, in frame widths from the frame's top-left. */
    public val imageLeft: Float get() = HALF - imageWidth * scale / 2f + panX
    public val imageTop: Float get() = frameAspect / 2f - imageHeight * scale / 2f + panY

    /** Same window with zoom and pans brought back into range. */
    public fun normalized(): CropWindow {
        val clampedZoom = zoom.coerceIn(1f, MAX_ZOOM)
        val base = copy(zoom = clampedZoom)
        return base.copy(
            panX = panX.coerceIn(-base.maxPanX, base.maxPanX),
            panY = panY.coerceIn(-base.maxPanY, base.maxPanY),
        )
    }

    /** A new shape starts fresh: no zoom, centred. */
    public fun withShape(shape: CropShape): CropWindow = copy(shape = shape, zoom = 1f, panX = 0f, panY = 0f)

    /** The window for the image turned by one quarter turn clockwise (the crop starts fresh). */
    public fun rotated(): CropWindow = CropWindow(imageHeight, imageWidth, shape)

    /**
     * Applies a pinch/drag step: zoom by [zoomBy] about [focalX]/[focalY] (frame widths from the
     * frame centre), then shift by [panDx]/[panDy]. The result is normalized.
     */
    public fun transformed(
        zoomBy: Float,
        panDx: Float,
        panDy: Float,
        focalX: Float = 0f,
        focalY: Float = 0f,
    ): CropWindow {
        val newZoom = (zoom * zoomBy).coerceIn(1f, MAX_ZOOM)
        val k = newZoom / zoom
        // Keep the image point under the fingers still while the scale changes around it.
        val zoomedPanX = focalX - (focalX - panX) * k
        val zoomedPanY = focalY - (focalY - panY) * k
        return copy(zoom = newZoom, panX = zoomedPanX + panDx, panY = zoomedPanY + panDy).normalized()
    }

    /** The frame, mapped back onto image pixels (clamped inside the image, at least 1×1). */
    public fun cropRect(): CropRect {
        val window = normalized()
        val s = window.scale
        val x = Math.round(-window.imageLeft / s).coerceIn(0, imageWidth - 1)
        val y = Math.round(-window.imageTop / s).coerceIn(0, imageHeight - 1)
        val width = Math.round(1f / s).coerceIn(1, imageWidth - x)
        val height = Math.round(window.frameAspect / s).coerceIn(1, imageHeight - y)
        return CropRect(x, y, width, height)
    }

    public companion object {
        public const val MAX_ZOOM: Float = 4f

        /** [CropShape.WHOLE] never gets wider than 4:1… */
        public const val MIN_WHOLE_ASPECT: Float = 0.25f

        /** …or taller than 1:3 (1152 rows, about 14 cm of paper). */
        public const val MAX_WHOLE_ASPECT: Float = 3f
        private const val HALF = 0.5f
    }
}
