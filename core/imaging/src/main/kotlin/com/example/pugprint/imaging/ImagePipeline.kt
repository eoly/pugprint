package com.example.pugprint.imaging

/** Crop → scale to the head width → dither: the whole journey from a picture to printable rows. */
public object ImagePipeline {
    /** Dots across the Hello Blink head. */
    public const val PRINT_WIDTH: Int = 384

    /** Safety cap on print length (about 14 cm of paper); taller results are trimmed evenly top and bottom. */
    public const val MAX_ROWS: Int = PRINT_WIDTH * 3

    /**
     * @param width dots across the result; the head by default, less for a roll that keeps a margin.
     * @param maxRows tallest result; taller pictures are trimmed evenly top and bottom.
     */
    public fun render(
        image: GrayImage,
        crop: CropRect? = null,
        mode: DitherMode = DitherMode.PHOTO,
        width: Int = PRINT_WIDTH,
        maxRows: Int = MAX_ROWS,
    ): MonoBitmap {
        require(width in 1..PRINT_WIDTH) { "width must be 1..$PRINT_WIDTH, got $width" }
        require(maxRows > 0) { "maxRows must be positive" }
        val cropped = crop?.let(image::cropped) ?: image
        val scaled = cropped.scaledToWidth(width)
        val limited =
            if (scaled.height > maxRows) {
                scaled.cropped(CropRect(0, (scaled.height - maxRows) / 2, width, maxRows))
            } else {
                scaled
            }
        return Dither.apply(limited, mode)
    }
}
