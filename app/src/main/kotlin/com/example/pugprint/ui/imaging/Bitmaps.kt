package com.example.pugprint.ui.imaging

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.pugprint.imaging.GrayImage
import com.example.pugprint.imaging.MonoBitmap

private const val OPAQUE = 0xFF shl 24
private const val BYTE_MASK = 0xFF
private const val BITS_PER_BYTE = 8
private const val MSB = 0x80
private const val RED_SHIFT = 16
private const val GREEN_SHIFT = 8
private const val WHITE = OPAQUE or 0xFFFFFF

/** Greyscale pixels as an opaque ARGB image for display. */
fun GrayImage.toImageBitmap(): ImageBitmap {
    val pixels =
        IntArray(width * height) { i ->
            val l = luma[i].toInt() and BYTE_MASK
            OPAQUE or (l shl RED_SHIFT) or (l shl GREEN_SHIFT) or l
        }
    return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888).asImageBitmap()
}

/** Print dots as an opaque black-on-white image, one screen pixel per dot. */
fun MonoBitmap.toImageBitmap(): ImageBitmap {
    val pixels = IntArray(width * height)
    for (y in 0 until height) {
        for (x in 0 until width) {
            val byte = packed[y * bytesPerRow + x / BITS_PER_BYTE].toInt()
            val black = (byte and (MSB ushr (x % BITS_PER_BYTE))) != 0
            pixels[y * width + x] = if (black) OPAQUE else WHITE
        }
    }
    return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888).asImageBitmap()
}
