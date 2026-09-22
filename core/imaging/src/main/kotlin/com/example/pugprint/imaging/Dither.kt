package com.example.pugprint.imaging

/** How greys become dots. */
public enum class DitherMode {
    /** Floyd–Steinberg error diffusion: photos keep their tones. */
    PHOTO,

    /** Fixed threshold: line art and drawings stay crisp. */
    DRAWING,
}

/** Converts a [GrayImage] into the 1-bit [MonoBitmap] the print head needs. */
public object Dither {
    /** Luminance below this is black. Mid-grey, as the vendor app uses. */
    public const val DEFAULT_THRESHOLD: Int = 128

    private const val WHITE = GrayImage.WHITE
    private const val ERROR_SCALE = 16
    private const val WEIGHT_RIGHT = 7
    private const val WEIGHT_DOWN_LEFT = 3
    private const val WEIGHT_DOWN = 5
    private const val WEIGHT_DOWN_RIGHT = 1

    public fun apply(
        image: GrayImage,
        mode: DitherMode,
    ): MonoBitmap =
        when (mode) {
            DitherMode.PHOTO -> floydSteinberg(image)
            DitherMode.DRAWING -> threshold(image)
        }

    /** Black wherever luminance is below [level]. */
    public fun threshold(
        image: GrayImage,
        level: Int = DEFAULT_THRESHOLD,
    ): MonoBitmap {
        val black = BooleanArray(image.luma.size) { (image.luma[it].toInt() and BYTE_MASK) < level }
        return MonoBitmap.fromPixels(image.width, image.height, black)
    }

    /**
     * Classic Floyd–Steinberg, scanning left to right on every row, errors kept as integers in
     * sixteenths (7/16 right, 3/16 down-left, 5/16 down, 1/16 down-right). Deterministic, so
     * the output is covered by golden tests.
     */
    public fun floydSteinberg(image: GrayImage): MonoBitmap {
        val width = image.width
        val black = BooleanArray(image.luma.size)
        var errorCurrent = IntArray(width + 2)
        var errorNext = IntArray(width + 2)
        for (y in 0 until image.height) {
            val rowOffset = y * width
            for (x in 0 until width) {
                // Error arrays are offset by one so x-1 and x+1 never fall outside.
                val old = (image.luma[rowOffset + x].toInt() and BYTE_MASK) + errorCurrent[x + 1] / ERROR_SCALE
                val isBlack = old < DEFAULT_THRESHOLD
                black[rowOffset + x] = isBlack
                val error = old - (if (isBlack) GrayImage.BLACK else WHITE)
                errorCurrent[x + 2] += error * WEIGHT_RIGHT
                errorNext[x] += error * WEIGHT_DOWN_LEFT
                errorNext[x + 1] += error * WEIGHT_DOWN
                errorNext[x + 2] += error * WEIGHT_DOWN_RIGHT
            }
            val recycled = errorCurrent
            errorCurrent = errorNext
            errorNext = recycled
            errorNext.fill(0)
        }
        return MonoBitmap.fromPixels(width, image.height, black)
    }

    private const val BYTE_MASK = 0xFF
}
