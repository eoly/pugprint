package com.example.pugprint.ui.editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.pugprint.R
import com.example.pugprint.imaging.CropShape
import com.example.pugprint.imaging.DitherMode
import com.example.pugprint.ui.home.printerStatusLabel
import com.example.pugprint.ui.imaging.toImageBitmap

@Composable
fun EditorScreen(
    state: EditorUiState,
    actions: EditorActions = EditorActions(),
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Header(state.step, actions.onBack)
            when (state.step) {
                EditorStep.Loading -> Centered { CircularProgressIndicator() }
                EditorStep.Failed -> Failed(actions.onBack)
                EditorStep.Crop -> CropStep(state, actions)
                EditorStep.Preview -> PreviewStep(state, actions)
            }
        }
    }
}

@Composable
private fun Header(
    step: EditorStep,
    onBack: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onBack) {
            Text(stringResource(R.string.editor_back), style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text =
                stringResource(
                    when (step) {
                        EditorStep.Loading -> R.string.editor_title_loading
                        EditorStep.Failed -> R.string.editor_title_failed
                        EditorStep.Crop -> R.string.editor_title_crop
                        EditorStep.Preview -> R.string.editor_title_preview
                    },
                ),
            style = MaterialTheme.typography.headlineSmall,
        )
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(320.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun Failed(onBack: () -> Unit) {
    Centered {
        Text(
            text = stringResource(R.string.editor_failed_body),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }
    BigButton(text = stringResource(R.string.editor_back), enabled = true, onClick = onBack)
}

@Composable
private fun CropStep(
    state: EditorUiState,
    actions: EditorActions,
) {
    val image = state.image ?: return
    val window = state.window ?: return
    val bitmap = remember(image) { image.toImageBitmap() }
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(8.dp))
        // The frame takes whatever height the controls leave, so they never scroll off screen.
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            // Loose constraints: aspectRatio picks the largest frame that fits both width and height.
            CropFrame(image = bitmap, window = window, onTransform = actions.onTransform)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.editor_crop_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CropShape.entries.forEach { shape ->
                FilterChip(
                    selected = window.shape == shape,
                    onClick = { actions.onShape(shape) },
                    label = { Text(stringResource(shapeLabel(shape))) },
                )
            }
        }
        TextButton(onClick = actions.onRotate) {
            Text(stringResource(R.string.editor_rotate), style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(8.dp))
        BigButton(text = stringResource(R.string.editor_next), enabled = true, onClick = actions.onNext)
    }
}

@Composable
private fun PreviewStep(
    state: EditorUiState,
    actions: EditorActions,
) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(8.dp))
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            val preview = state.preview
            if (preview == null) {
                CircularProgressIndicator()
            } else {
                val bitmap = remember(preview) { preview.toImageBitmap() }
                Image(
                    bitmap = bitmap,
                    contentDescription = stringResource(R.string.editor_preview_description),
                    contentScale = ContentScale.Fit,
                    filterQuality = FilterQuality.None,
                    modifier =
                        Modifier
                            .padding(horizontal = PREVIEW_INSET)
                            .aspectRatio(preview.width.toFloat() / preview.height)
                            .background(Color.White)
                            .border(1.dp, MaterialTheme.colorScheme.outline),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DitherMode.entries.forEach { mode ->
                FilterChip(
                    selected = state.mode == mode,
                    onClick = { actions.onMode(mode) },
                    label = { Text(stringResource(modeLabel(mode))) },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = printerStatusLabel(state.printerStatus, state.printerName, state.offlineReason),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        if (state.paperOrLidProblem) {
            Text(
                text = stringResource(R.string.home_paper_or_lid),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Spacer(Modifier.height(12.dp))
        BigButton(text = stringResource(R.string.editor_print), enabled = state.canPrint, onClick = actions.onPrint)
    }
}

@Composable
private fun BigButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(64.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.titleLarge)
    }
}

private fun shapeLabel(shape: CropShape): Int =
    when (shape) {
        CropShape.SQUARE -> R.string.editor_shape_square
        CropShape.TALL -> R.string.editor_shape_tall
        CropShape.WIDE -> R.string.editor_shape_wide
        CropShape.WHOLE -> R.string.editor_shape_whole
    }

private fun modeLabel(mode: DitherMode): Int =
    when (mode) {
        DitherMode.PHOTO -> R.string.editor_style_photo
        DitherMode.DRAWING -> R.string.editor_style_drawing
    }

/** The preview sits a little in from the edges so it reads as a sticker, not a full-bleed picture. */
private val PREVIEW_INSET = 32.dp
