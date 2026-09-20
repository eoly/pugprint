package com.example.pugprint.imaging

/**
 * A 1-bit-per-pixel bitmap packed the way `GS v 0` expects: rows top-to-bottom, pixels
 * left-to-right, most-significant bit first, `1` = black. Each row is padded to a whole byte.
 */
public class MonoBitmap(
    public val width: Int,
    public val height: Int,
    /** `height * bytesPerRow` packed bytes. */
    public val packed: ByteArray,
) {
    public val bytesPerRow: Int = bytesPerRow(width)

    init {
        require(width > 0 && height > 0) { "Dimensions must be positive: ${width}x$height" }
        require(packed.size == height * bytesPerRow) {
            "Expected ${height * bytesPerRow} packed bytes for ${width}x$height, got ${packed.size}"
        }
    }

    /** Packed bytes for row [y] (a copy). */
    public fun row(y: Int): ByteArray = packed.copyOfRange(y * bytesPerRow, (y + 1) * bytesPerRow)

    /** True if pixel ([x], [y]) is black. */
    public fun isBlack(
        x: Int,
        y: Int,
    ): Boolean {
        require(x in 0 until width && y in 0 until height) { "($x,$y) outside ${width}x$height" }
        val byte = packed[y * bytesPerRow + x / BITS_PER_BYTE].toInt()
        return (byte and (MSB_MASK ushr (x % BITS_PER_BYTE))) != 0
    }

    public companion object {
        private const val BITS_PER_BYTE = 8
        private const val MSB_MASK = 0x80

        /** Bytes needed to hold [width] pixels, rounded up to a whole byte. */
        public fun bytesPerRow(width: Int): Int = (width + BITS_PER_BYTE - 1) / BITS_PER_BYTE

        /** Packs [black] (row-major, `width * height` entries, `true` = black) into a [MonoBitmap]. */
        public fun fromPixels(
            width: Int,
            height: Int,
            black: BooleanArray,
        ): MonoBitmap {
            require(black.size == width * height) {
                "Expected ${width * height} pixels for ${width}x$height, got ${black.size}"
            }
            val stride = bytesPerRow(width)
            val packed = ByteArray(height * stride)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    if (black[y * width + x]) {
                        val index = y * stride + x / BITS_PER_BYTE
                        packed[index] = (packed[index].toInt() or (MSB_MASK ushr (x % BITS_PER_BYTE))).toByte()
                    }
                }
            }
            return MonoBitmap(width, height, packed)
        }
    }
}
