package com.example.pugprint.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Wires [EditorViewModel] to the screen; [onClose] pops back to the home screen. */
@Composable
fun EditorRoute(
    photoUri: String,
    onClose: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val printRequested by viewModel.printRequested.collectAsStateWithLifecycle()

    LaunchedEffect(photoUri) { viewModel.open(photoUri) }
    LaunchedEffect(printRequested) {
        if (printRequested) {
            viewModel.onPrintHandled()
            onClose()
        }
    }
    val inPreview = state.step == EditorStep.Preview
    BackHandler(enabled = inPreview) { viewModel.onBackToCropClicked() }

    EditorScreen(
        state = state,
        actions =
            EditorActions(
                onBack = { if (inPreview) viewModel.onBackToCropClicked() else onClose() },
                onRotate = viewModel::onRotateClicked,
                onShape = viewModel::onShapeSelected,
                onTransform = viewModel::onTransform,
                onNext = viewModel::onNextClicked,
                onMode = viewModel::onModeSelected,
                onPrint = viewModel::onPrintClicked,
            ),
    )
}
