package com.example.pugprint.imaging

/**
 * Separable box-filter resampling: every destination pixel is the area-weighted mean of the
 * source pixels it covers. Downscaling (the normal case, camera photo → 384 dots) averages
 * cleanly with no aliasing; upscaling degrades to nearest-neighbour with blended seams.
 */
internal object BoxResampler {
    /** Source indices [start, start + weights.size) and their coverage weights, summing to 1. */
    private class Span(
        val start: Int,
        val weights: FloatArray,
    )

    fun resample(
        source: GrayImage,
        targetWidth: Int,
        targetHeight: Int,
    ): GrayImage {
        val columns = spans(source.width, targetWidth)
        val rows = spans(source.height, targetHeight)

        // Pass 1: horizontal, keeping full precision per source row.
        val horizontal = FloatArray(targetWidth * source.height)
        for (y in 0 until source.height) {
            val rowOffset = y * source.width
            for (x in 0 until targetWidth) {
                val span = columns[x]
                var sum = 0f
                for (i in span.weights.indices) {
                    sum += (source.luma[rowOffset + span.start + i].toInt() and BYTE_MASK) * span.weights[i]
                }
                horizontal[y * targetWidth + x] = sum
            }
        }

        // Pass 2: vertical, rounding to bytes.
        val out = ByteArray(targetWidth * targetHeight)
        for (y in 0 until targetHeight) {
            val span = rows[y]
            for (x in 0 until targetWidth) {
                var sum = 0f
                for (i in span.weights.indices) {
                    sum += horizontal[(span.start + i) * targetWidth + x] * span.weights[i]
                }
                out[y * targetWidth + x] = (sum + HALF).toInt().coerceIn(GrayImage.BLACK, GrayImage.WHITE).toByte()
            }
        }
        return GrayImage(targetWidth, targetHeight, out)
    }

    /** For each destination index, which source indices it covers and by how much. */
    private fun spans(
        sourceLength: Int,
        targetLength: Int,
    ): Array<Span> {
        val ratio = sourceLength.toDouble() / targetLength
        return Array(targetLength) { i ->
            val from = i * ratio
            val to = (i + 1) * ratio
            val first = from.toInt()
            val last = (Math.ceil(to).toInt() - 1).coerceIn(first, sourceLength - 1)
            val weights =
                FloatArray(last - first + 1) { k ->
                    val s = first + k
                    val overlap = minOf(to, s + 1.0) - maxOf(from, s.toDouble())
                    (overlap / ratio).toFloat()
                }
            Span(first, weights)
        }
    }

    private const val BYTE_MASK = 0xFF
    private const val HALF = 0.5f
}
