package com.example.pugprint.imaging

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class HandoffPhotoSourceTest {
    private val picture = GrayImage.generate(4, 4) { _, _ -> 200 }
    private val drawing = GrayImage.generate(2, 2) { _, _ -> 0 }
    private val handoff = DrawingHandoff()
    private val source =
        HandoffPhotoSource(handoff) { uri ->
            if (uri ==
                "content://p"
            ) {
                picture
            } else {
                throw PhotoLoadException(uri)
            }
        }

    @Test
    fun `pictures still come from the real source`() =
        runTest {
            assertSame(picture, source.load("content://p"))
        }

    @Test
    fun `the drawing URI serves the hand-off image, or fails when there is none`() =
        runTest {
            assertThrows(
                PhotoLoadException::class.java,
            ) { kotlinx.coroutines.runBlocking { source.load(DrawingHandoff.URI) } }
            handoff.image = drawing
            assertSame(drawing, source.load(DrawingHandoff.URI))
        }
}
