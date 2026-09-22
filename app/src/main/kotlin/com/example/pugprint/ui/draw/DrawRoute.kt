package com.example.pugprint.ui.draw

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Wires [DrawViewModel] to the screen; [onDrawingReady] opens the editor on the hand-off drawing. */
@Composable
fun DrawRoute(
    onClose: () -> Unit,
    onHome: () -> Unit,
    onDrawingReady: () -> Unit,
    viewModel: DrawViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val finished by viewModel.finished.collectAsStateWithLifecycle()

    LaunchedEffect(finished) {
        if (finished) {
            viewModel.onFinishedHandled()
            onDrawingReady()
        }
    }

    DrawScreen(
        state = state,
        actions =
            DrawActions(
                onBack = onClose,
                onHome = onHome,
                onStrokeStart = viewModel::onStrokeStarted,
                onStrokeMove = viewModel::onStrokeMoved,
                onStrokeEnd = viewModel::onStrokeEnded,
                onBrush = viewModel::onBrushSelected,
                onTool = viewModel::onToolSelected,
                onUndo = viewModel::onUndoClicked,
                onClear = viewModel::onClearClicked,
                onNext = viewModel::onNextClicked,
            ),
    )
}
