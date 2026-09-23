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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.pugprint.R
import com.example.pugprint.imaging.CropWindow
import com.example.pugprint.imaging.LabelShape
import com.example.pugprint.ui.imaging.drawRoundStickerGuide
import kotlin.math.roundToInt

/**
 * The sticker outline with the picture inside it. Pinch and drag move the picture; the maths
 * lives in [CropWindow], this only converts screen pixels to frame widths and draws. On a round
 * sticker ([labelShape]) the corners outside the circle are dimmed: they will not print.
 */
@Composable
fun CropFrame(
    image: ImageBitmap,
    window: CropWindow,
    onTransform: (zoomBy: Float, panDx: Float, panDy: Float, focalX: Float, focalY: Float) -> Unit,
    modifier: Modifier = Modifier,
    labelShape: LabelShape = LabelShape.RECTANGLE,
) {
    val transform by rememberUpdatedState(onTransform)
    val shape = RoundedCornerShape(12.dp)
    val description =
        stringResource(
            if (labelShape == LabelShape.CIRCLE) {
                R.string.editor_crop_round_description
            } else {
                R.string.editor_crop_description
            },
        )
    val outlineColour = MaterialTheme.colorScheme.primary
    Canvas(
        modifier =
            modifier
                .semantics { contentDescription = description }
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
        if (labelShape == LabelShape.CIRCLE) drawRoundStickerGuide(outlineColour)
    }
}
