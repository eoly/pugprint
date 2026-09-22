package com.example.pugprint.imaging

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Carries a finished drawing from the draw screen to the editor, which only knows how to open
 * a picture by URI: the draw screen puts the [image] here and opens the editor at [URI].
 */
@Singleton
class DrawingHandoff
    @Inject
    constructor() {
        @Volatile
        var image: GrayImage? = null

        companion object {
            const val URI = "pugprint://drawing"
        }
    }

/** Serves the hand-off drawing for [DrawingHandoff.URI] and everything else from [pictures]. */
class HandoffPhotoSource(
    private val handoff: DrawingHandoff,
    private val pictures: PhotoSource,
) : PhotoSource {
    override suspend fun load(uri: String): GrayImage =
        if (uri == DrawingHandoff.URI) {
            handoff.image ?: throw PhotoLoadException("No drawing to open")
        } else {
            pictures.load(uri)
        }
}
