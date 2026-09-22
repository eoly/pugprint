package com.example.pugprint.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Wires [EditorViewModel] to the screen. Back ([onClose]) returns to wherever the picture came
 * from (home or the draw sheet); the Home button and a finished print go to home ([onHome]).
 */
@Composable
fun EditorRoute(
    photoUri: String,
    onClose: () -> Unit,
    onHome: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val printRequested by viewModel.printRequested.collectAsStateWithLifecycle()

    LaunchedEffect(photoUri) { viewModel.open(photoUri) }
    LaunchedEffect(printRequested) {
        if (printRequested) {
            viewModel.onPrintHandled()
            onHome()
        }
    }
    val inPreview = state.step == EditorStep.Preview
    val inWords = state.step == EditorStep.Words
    val inStamps = state.step == EditorStep.Stamps
    BackHandler(enabled = inPreview) { viewModel.onBackToCropClicked() }
    BackHandler(enabled = inWords) { viewModel.onWordsDoneClicked() }
    BackHandler(enabled = inStamps) { viewModel.onStampsDoneClicked() }

    EditorScreen(
        state = state,
        actions =
            EditorActions(
                onBack = {
                    when {
                        inWords -> viewModel.onWordsDoneClicked()
                        inStamps -> viewModel.onStampsDoneClicked()
                        inPreview -> viewModel.onBackToCropClicked()
                        else -> onClose()
                    }
                },
                onHome = onHome,
                onRotate = viewModel::onRotateClicked,
                onShape = viewModel::onShapeSelected,
                onTransform = viewModel::onTransform,
                onNext = viewModel::onNextClicked,
                onMode = viewModel::onModeSelected,
                onDensity = viewModel::onDensitySelected,
                onAddWords = viewModel::onAddWordsClicked,
                onCaption = viewModel::onCaptionChanged,
                onCaptionPlacement = viewModel::onCaptionPlacementSelected,
                onWordsDone = viewModel::onWordsDoneClicked,
                onAddStamps = viewModel::onAddStampsClicked,
                onStampPicked = viewModel::onStampPicked,
                onStampDragged = viewModel::onStampDragged,
                onStampSize = viewModel::onStampSizeSelected,
                onUndoStamp = viewModel::onUndoStampClicked,
                onStampsDone = viewModel::onStampsDoneClicked,
                onPrint = viewModel::onPrintClicked,
            ),
    )
}
