package com.example.pugprint.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.pugprint.imaging.CropWindow
import kotlin.math.roundToInt

/**
 * The sticker outline with the picture inside it. Pinch and drag move the picture; the maths
 * lives in [CropWindow], this only converts screen pixels to frame widths and draws.
 */
@Composable
fun CropFrame(
    image: ImageBitmap,
    window: CropWindow,
    onTransform: (zoomBy: Float, panDx: Float, panDy: Float, focalX: Float, focalY: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val transform by rememberUpdatedState(onTransform)
    val shape = RoundedCornerShape(12.dp)
    Canvas(
        modifier =
            modifier
                .aspectRatio(1f / window.frameAspect)
                .clip(shape)
                .border(3.dp, MaterialTheme.colorScheme.primary, shape)
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val width = size.width.toFloat()
                        if (width > 0f) {
                            transform(
                                zoom,
                                pan.x / width,
                                pan.y / width,
                                (centroid.x - width / 2f) / width,
                                (centroid.y - size.height / 2f) / width,
                            )
                        }
                    }
                },
    ) {
        val frameWidth = size.width
        val pixel = window.scale * frameWidth
        drawImage(
            image = image,
            dstOffset =
                IntOffset(
                    (window.imageLeft * frameWidth).roundToInt(),
                    (window.imageTop * frameWidth).roundToInt(),
                ),
            dstSize = IntSize((image.width * pixel).roundToInt(), (image.height * pixel).roundToInt()),
            filterQuality = FilterQuality.Medium,
        )
    }
}
