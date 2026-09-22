package com.example.pugprint.ui.editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.pugprint.R
import com.example.pugprint.design.components.BannerKind
import com.example.pugprint.design.components.BigButton
import com.example.pugprint.design.components.BigTextField
import com.example.pugprint.design.components.ButtonEmphasis
import com.example.pugprint.design.components.ChoiceRow
import com.example.pugprint.design.components.KidScreen
import com.example.pugprint.design.components.StatusBanner
import com.example.pugprint.design.theme.PugSpacing
import com.example.pugprint.imaging.CaptionPlacement
import com.example.pugprint.imaging.CropShape
import com.example.pugprint.imaging.DitherMode
import com.example.pugprint.imaging.StampSize
import com.example.pugprint.printer.DensityLevel
import com.example.pugprint.ui.home.PrinterStatus
import com.example.pugprint.ui.home.printerStatusLabel
import com.example.pugprint.ui.imaging.toImageBitmap

@Composable
fun EditorScreen(
    state: EditorUiState,
    actions: EditorActions = EditorActions(),
    modifier: Modifier = Modifier,
) {
    KidScreen(
        modifier = modifier,
        title =
            stringResource(
                when (state.step) {
                    EditorStep.Loading -> R.string.editor_title_loading
                    EditorStep.Failed -> R.string.editor_title_failed
                    EditorStep.Crop -> R.string.editor_title_crop
                    EditorStep.Words -> R.string.editor_title_words
                    EditorStep.Stamps -> R.string.editor_title_stamps
                    EditorStep.Preview -> R.string.editor_title_preview
                },
            ),
        onBack = actions.onBack,
        onHome = actions.onHome,
    ) {
        when (state.step) {
            EditorStep.Loading -> Centered { CircularProgressIndicator() }
            EditorStep.Failed -> Failed(actions.onBack)
            EditorStep.Crop -> CropStep(state, actions)
            EditorStep.Words -> WordsStep(state, actions)
            EditorStep.Stamps -> StampsStep(state, actions)
            EditorStep.Preview -> PreviewStep(state, actions)
        }
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
    BigButton(text = stringResource(R.string.editor_back), onClick = onBack)
}

@Composable
private fun ColumnScope.CropStep(
    state: EditorUiState,
    actions: EditorActions,
) {
    val image = state.image ?: return
    val window = state.window ?: return
    val bitmap = remember(image) { image.toImageBitmap() }
    Spacer(Modifier.height(PugSpacing.small))
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
    Spacer(Modifier.height(PugSpacing.small))
    Text(
        text = stringResource(R.string.editor_crop_hint),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(PugSpacing.medium))
    ChoiceRow(
        options = CropShape.entries,
        selected = window.shape,
        onSelect = actions.onShape,
        label = { stringResource(shapeLabel(it)) },
    )
    Spacer(Modifier.height(PugSpacing.small))
    BigButton(
        text = stringResource(R.string.editor_rotate),
        onClick = actions.onRotate,
        emphasis = ButtonEmphasis.Secondary,
    )
    Spacer(Modifier.height(PugSpacing.small))
    BigButton(text = stringResource(R.string.editor_next), onClick = actions.onNext)
}

/**
 * The dots that will print, or a spinner while they are being worked out. With [onDrag], a finger
 * on the picture reports its movement in fractions of the picture's size.
 */
@Composable
private fun ColumnScope.Dots(
    state: EditorUiState,
    onDrag: ((dx: Float, dy: Float) -> Unit)? = null,
) {
    Spacer(Modifier.height(PugSpacing.small))
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
                        .border(1.dp, MaterialTheme.colorScheme.outline)
                        .then(
                            if (onDrag == null) {
                                Modifier
                            } else {
                                Modifier.pointerInput(Unit) {
                                    detectDragGestures { change, drag ->
                                        change.consume()
                                        onDrag(drag.x / size.width, drag.y / size.height)
                                    }
                                }
                            },
                        ),
            )
        }
    }
}

@Composable
private fun ColumnScope.StampsStep(
    state: EditorUiState,
    actions: EditorActions,
) {
    Dots(state, onDrag = actions.onStampDragged)
    Spacer(Modifier.height(PugSpacing.small))
    Text(
        text = stringResource(R.string.editor_stamps_hint),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(PugSpacing.small))
    StampGrid(onPick = actions.onStampPicked)
    Spacer(Modifier.height(PugSpacing.small))
    ChoiceRow(
        options = StampSize.entries,
        selected = state.stampSize,
        onSelect = actions.onStampSize,
        label = { stringResource(stampSizeLabel(it)) },
    )
    Spacer(Modifier.height(PugSpacing.medium))
    Row(horizontalArrangement = Arrangement.spacedBy(PugSpacing.small)) {
        BigButton(
            text = stringResource(R.string.editor_stamps_undo),
            onClick = actions.onUndoStamp,
            enabled = state.stamps.isNotEmpty(),
            emphasis = ButtonEmphasis.Secondary,
            modifier = Modifier.weight(1f),
        )
        BigButton(
            text = stringResource(R.string.editor_words_done),
            onClick = actions.onStampsDone,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ColumnScope.WordsStep(
    state: EditorUiState,
    actions: EditorActions,
) {
    Dots(state)
    Spacer(Modifier.height(PugSpacing.medium))
    BigTextField(
        value = state.caption,
        onValueChange = actions.onCaption,
        placeholder = stringResource(R.string.editor_words_placeholder),
        onDone = actions.onWordsDone,
    )
    Spacer(Modifier.height(PugSpacing.small))
    ChoiceRow(
        options = CaptionPlacement.entries,
        selected = state.captionPlacement,
        onSelect = actions.onCaptionPlacement,
        label = { stringResource(placementLabel(it)) },
    )
    Spacer(Modifier.height(PugSpacing.medium))
    BigButton(text = stringResource(R.string.editor_words_done), onClick = actions.onWordsDone)
}

@Composable
private fun ColumnScope.PreviewStep(
    state: EditorUiState,
    actions: EditorActions,
) {
    Dots(state)
    Spacer(Modifier.height(PugSpacing.medium))
    ChoiceRow(
        options = DitherMode.entries,
        selected = state.mode,
        onSelect = actions.onMode,
        label = { stringResource(modeLabel(it)) },
    )
    Spacer(Modifier.height(PugSpacing.small))
    Text(
        text = stringResource(R.string.editor_darkness),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(PugSpacing.tiny))
    ChoiceRow(
        options = DensityLevel.entries,
        selected = state.density,
        onSelect = actions.onDensity,
        label = { stringResource(densityLabel(it)) },
    )
    Spacer(Modifier.height(PugSpacing.small))
    Row(horizontalArrangement = Arrangement.spacedBy(PugSpacing.small)) {
        BigButton(
            text =
                stringResource(
                    if (state.caption.isBlank()) R.string.editor_add_words else R.string.editor_change_words,
                ),
            onClick = actions.onAddWords,
            emphasis = ButtonEmphasis.Secondary,
            modifier = Modifier.weight(1f),
        )
        BigButton(
            text =
                stringResource(
                    if (state.stamps.isEmpty()) R.string.editor_add_stamps else R.string.editor_change_stamps,
                ),
            onClick = actions.onAddStamps,
            emphasis = ButtonEmphasis.Secondary,
            modifier = Modifier.weight(1f),
        )
    }
    Spacer(Modifier.height(PugSpacing.medium))
    StatusBanner(
        kind = if (state.paperOrLidProblem) BannerKind.Problem else state.printerStatus.bannerKind(),
        text = printerStatusLabel(state.printerStatus, state.printerName, state.offlineReason),
        hint = if (state.paperOrLidProblem) stringResource(R.string.home_paper_or_lid) else null,
    )
    Spacer(Modifier.height(PugSpacing.medium))
    BigButton(text = stringResource(R.string.editor_print), onClick = actions.onPrint, enabled = state.canPrint)
}

private fun PrinterStatus.bannerKind(): BannerKind =
    when (this) {
        PrinterStatus.NoPrinter, PrinterStatus.Offline -> BannerKind.Problem
        PrinterStatus.Connecting, PrinterStatus.Printing -> BannerKind.Working
        PrinterStatus.Connected -> BannerKind.Success
    }

/** The preview sits a little in from the edges so it reads as a sticker, not a full-bleed picture. */
private val PREVIEW_INSET = 32.dp
