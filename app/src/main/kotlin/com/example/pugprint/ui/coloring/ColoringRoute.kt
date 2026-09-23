package com.example.pugprint.ui.coloring

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Wires [ColoringViewModel] to the screen; [onPageReady] opens the editor on the hand-off picture. */
@Composable
fun ColoringRoute(
    onClose: () -> Unit,
    onHome: () -> Unit,
    onPageReady: () -> Unit,
    viewModel: ColoringViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val finished by viewModel.finished.collectAsStateWithLifecycle()

    LaunchedEffect(finished) {
        if (finished) {
            viewModel.onFinishedHandled()
            onPageReady()
        }
    }

    ColoringScreen(
        state = state,
        actions = ColoringActions(onBack = onClose, onHome = onHome, onPick = viewModel::onPagePicked),
    )
}
