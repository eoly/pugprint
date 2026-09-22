package com.example.pugprint.imaging

/** Quarter-turn rotations, applied clockwise. */
public enum class Rotation {
    NONE,
    CLOCKWISE_90,
    HALF,
    COUNTER_CLOCKWISE_90,
    ;

    /** One more quarter turn clockwise. */
    public fun plusQuarterTurn(): Rotation = entries[(ordinal + 1) % entries.size]

    /** True when the rotation swaps width and height. */
    public val swapsAxes: Boolean get() = this == CLOCKWISE_90 || this == COUNTER_CLOCKWISE_90
}

/** An axis-aligned window in image pixels. */
public data class CropRect(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
) {
    init {
        require(x >= 0 && y >= 0) { "Origin must not be negative: ($x,$y)" }
        require(width > 0 && height > 0) { "Size must be positive: ${width}x$height" }
    }

    val right: Int get() = x + width
    val bottom: Int get() = y + height
}

/**
 * An 8-bit greyscale raster: row-major, one byte per pixel, `0` = black, `255` = white.
 * This is the platform-neutral form every image takes before it is dithered; the Android
 * side converts a decoded bitmap into one with [lumaOf].
 */
public class GrayImage(
    public val width: Int,
    public val height: Int,
    public val luma: ByteArray,
) {
    init {
        require(width > 0 && height > 0) { "Dimensions must be positive: ${width}x$height" }
        require(luma.size == width * height) {
            "Expected ${width * height} luma bytes for ${width}x$height, got ${luma.size}"
        }
    }

    /** Luminance 0–255 of pixel ([x], [y]). */
    public operator fun get(
        x: Int,
        y: Int,
    ): Int {
        require(x in 0 until width && y in 0 until height) { "($x,$y) outside ${width}x$height" }
        return luma[y * width + x].toInt() and BYTE_MASK
    }

    /** A copy turned by [rotation]. */
    public fun rotated(rotation: Rotation): GrayImage {
        if (rotation == Rotation.NONE) return this
        val outWidth = if (rotation.swapsAxes) height else width
        val outHeight = if (rotation.swapsAxes) width else height
        val out = ByteArray(luma.size)
        for (y in 0 until outHeight) {
            for (x in 0 until outWidth) {
                out[y * outWidth + x] = luma[sourceIndex(rotation, x, y)]
            }
        }
        return GrayImage(outWidth, outHeight, out)
    }

    /** Index into [luma] of the source pixel that lands at ([x], [y]) after [rotation]. */
    private fun sourceIndex(
        rotation: Rotation,
        x: Int,
        y: Int,
    ): Int =
        when (rotation) {
            Rotation.NONE -> y * width + x
            Rotation.CLOCKWISE_90 -> (height - 1 - x) * width + y
            Rotation.HALF -> (height - 1 - y) * width + (width - 1 - x)
            Rotation.COUNTER_CLOCKWISE_90 -> x * width + (width - 1 - y)
        }

    /** The pixels inside [rect], which must lie within the image. */
    public fun cropped(rect: CropRect): GrayImage {
        require(rect.right <= width && rect.bottom <= height) { "$rect outside ${width}x$height" }
        val whole = rect.x == 0 && rect.y == 0 && rect.width == width && rect.height == height
        if (whole) return this
        val out = ByteArray(rect.width * rect.height)
        for (y in 0 until rect.height) {
            luma.copyInto(out, y * rect.width, (rect.y + y) * width + rect.x, (rect.y + y) * width + rect.right)
        }
        return GrayImage(rect.width, rect.height, out)
    }

    /** Resamples to [targetWidth] keeping the aspect ratio (height rounded, at least 1). */
    public fun scaledToWidth(targetWidth: Int): GrayImage {
        val targetHeight = (height.toLong() * targetWidth + width / 2) / width
        return scaled(targetWidth, targetHeight.toInt().coerceAtLeast(1))
    }

    /** Box-filter resample to exactly [targetWidth] × [targetHeight]. */
    public fun scaled(
        targetWidth: Int,
        targetHeight: Int,
    ): GrayImage {
        require(targetWidth > 0 && targetHeight > 0) { "Target must be positive: ${targetWidth}x$targetHeight" }
        if (targetWidth == width && targetHeight == height) return this
        return BoxResampler.resample(this, targetWidth, targetHeight)
    }

    public companion object {
        public const val BLACK: Int = 0
        public const val WHITE: Int = 255
        private const val BYTE_MASK = 0xFF
        private const val LUMA_R = 38
        private const val LUMA_G = 75
        private const val LUMA_B = 15
        private const val LUMA_SHIFT = 7

        /**
         * Luminance of an 8-bit RGB triple, `(38R + 75G + 15B) >> 7` — the integer Rec. 601
         * weighting the vendor app uses (docs/PRINTER_PROTOCOL.md § Image pipeline).
         */
        public fun lumaOf(
            r: Int,
            g: Int,
            b: Int,
        ): Int = (LUMA_R * r + LUMA_G * g + LUMA_B * b) shr LUMA_SHIFT

        /** Builds an image from a per-pixel luminance function (values are clamped to 0–255). */
        public fun generate(
            width: Int,
            height: Int,
            luma: (x: Int, y: Int) -> Int,
        ): GrayImage {
            val out = ByteArray(width * height)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    out[y * width + x] = luma(x, y).coerceIn(BLACK, WHITE).toByte()
                }
            }
            return GrayImage(width, height, out)
        }
    }
}
