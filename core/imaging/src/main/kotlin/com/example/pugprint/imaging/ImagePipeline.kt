package com.example.pugprint.imaging

/** Crop → scale to the head width → dither: the whole journey from a picture to printable rows. */
public object ImagePipeline {
    /** Dots across the Hello Blink head. */
    public const val PRINT_WIDTH: Int = 384

    /** Safety cap on print length (about 14 cm of paper); taller results are trimmed evenly top and bottom. */
    public const val MAX_ROWS: Int = PRINT_WIDTH * 3

    public fun render(
        image: GrayImage,
        crop: CropRect? = null,
        mode: DitherMode = DitherMode.PHOTO,
    ): MonoBitmap {
        val cropped = crop?.let(image::cropped) ?: image
        val scaled = cropped.scaledToWidth(PRINT_WIDTH)
        val limited =
            if (scaled.height > MAX_ROWS) {
                scaled.cropped(CropRect(0, (scaled.height - MAX_ROWS) / 2, PRINT_WIDTH, MAX_ROWS))
            } else {
                scaled
            }
        return Dither.apply(limited, mode)
    }
}
