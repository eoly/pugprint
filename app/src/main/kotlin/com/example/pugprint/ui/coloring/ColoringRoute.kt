package com.example.pugprint.ui.coloring

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Wires [ColoringViewModel] to the screen; [onPagePicked] opens the draw sheet on that page. */
@Composable
fun ColoringRoute(
    onClose: () -> Unit,
    onHome: () -> Unit,
    onPagePicked: (pageId: String) -> Unit,
    viewModel: ColoringViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ColoringScreen(
        state = state,
        actions = ColoringActions(onBack = onClose, onHome = onHome, onPick = onPagePicked),
    )
}
