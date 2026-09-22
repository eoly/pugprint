package com.example.pugprint.imaging

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

/**
 * Decodes a picked picture through the content resolver, already downsized so the editor works
 * on at most [MAX_SOURCE_SIDE] px a side, and converts it to luminance. Android 9+ uses
 * `ImageDecoder` (which applies EXIF orientation itself); older devices fall back to
 * `BitmapFactory` plus a manual EXIF rotation.
 */
class ContentResolverPhotoSource
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : PhotoSource {
        override suspend fun load(uri: String): GrayImage =
            withContext(Dispatchers.IO) {
                val parsed = uri.toUri()
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) decodeModern(parsed) else decodeLegacy(parsed)
                } catch (e: IOException) {
                    throw PhotoLoadException("Could not read $uri", e)
                } catch (e: SecurityException) {
                    throw PhotoLoadException("No longer allowed to read $uri", e)
                } catch (e: IllegalArgumentException) {
                    throw PhotoLoadException("Not a picture: $uri", e)
                }
            }

        @RequiresApi(Build.VERSION_CODES.P)
        private fun decodeModern(uri: Uri): GrayImage {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            val bitmap =
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    val (width, height) = fitWithin(info.size.width, info.size.height)
                    decoder.setTargetSize(width, height)
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = false
                }
            return bitmap.toGrayImage()
        }

        private fun decodeLegacy(uri: Uri): GrayImage {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            open(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw PhotoLoadException("Not a picture: $uri")
            val options = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight) }
            val bitmap =
                open(uri).use { BitmapFactory.decodeStream(it, null, options) }
                    ?: throw PhotoLoadException("Could not decode $uri")
            val (width, height) = fitWithin(bitmap.width, bitmap.height)
            val orientation = open(uri).use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1) }
            return bitmap.toGrayImage().scaled(width, height).rotated(rotationFor(orientation))
        }

        private fun open(uri: Uri) =
            context.contentResolver.openInputStream(uri) ?: throw IOException("no stream for $uri")

        /** Reads the pixels one row at a time so a large decode never needs a second full-size buffer. */
        private fun Bitmap.toGrayImage(): GrayImage {
            val luma = ByteArray(width * height)
            val row = IntArray(width)
            for (y in 0 until height) {
                getPixels(row, 0, width, 0, y, width, 1)
                for (x in 0 until width) {
                    val argb = row[x]
                    luma[y * width + x] =
                        GrayImage
                            .lumaOf(
                                (argb shr RED_SHIFT) and CHANNEL_MASK,
                                (argb shr GREEN_SHIFT) and CHANNEL_MASK,
                                argb and CHANNEL_MASK,
                            ).toByte()
                }
            }
            recycle()
            return GrayImage(width, height, luma)
        }

        companion object {
            /** Long side of the editing copy. Plenty for a 384-dot print, small enough to rotate instantly. */
            const val MAX_SOURCE_SIDE = 1600
            private const val RED_SHIFT = 16
            private const val GREEN_SHIFT = 8
            private const val CHANNEL_MASK = 0xFF

            /** The largest size with the same aspect that fits in [MAX_SOURCE_SIDE] square. */
            fun fitWithin(
                width: Int,
                height: Int,
            ): Pair<Int, Int> {
                val longest = maxOf(width, height)
                if (longest <= MAX_SOURCE_SIDE) return width to height
                val scale = MAX_SOURCE_SIDE.toDouble() / longest
                return maxOf(1, Math.round(width * scale).toInt()) to maxOf(1, Math.round(height * scale).toInt())
            }

            /** Largest power-of-two subsampling that keeps the long side at or above [MAX_SOURCE_SIDE]. */
            fun sampleSize(
                width: Int,
                height: Int,
            ): Int {
                var sample = 1
                while (maxOf(width, height) / (sample * 2) >= MAX_SOURCE_SIDE) sample *= 2
                return sample
            }

            fun rotationFor(exifOrientation: Int): Rotation =
                when (exifOrientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> Rotation.CLOCKWISE_90
                    ExifInterface.ORIENTATION_ROTATE_180 -> Rotation.HALF
                    ExifInterface.ORIENTATION_ROTATE_270 -> Rotation.COUNTER_CLOCKWISE_90
                    else -> Rotation.NONE
                }
        }
    }
