package com.example.pugprint.imaging

/** The picked picture could not be read (gone, unreadable, permission expired, not an image). */
class PhotoLoadException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * Turns a picked picture (a `content:` URI as a string, so ViewModels stay Android-free) into
 * a [GrayImage] small enough to edit. The real one reads the content resolver; tests fake it.
 */
fun interface PhotoSource {
    /** @throws PhotoLoadException when the picture cannot be decoded. */
    suspend fun load(uri: String): GrayImage
}
