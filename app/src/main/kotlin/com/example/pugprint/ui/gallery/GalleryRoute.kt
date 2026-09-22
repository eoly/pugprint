package com.example.pugprint.ui.gallery

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pugprint.design.gallery.DesignGallery
import com.example.pugprint.design.theme.PugPrintTheme

/** Debug builds only: every kit component in any theme, reached from the home screen. */
@Composable
fun GalleryRoute(
    onClose: () -> Unit,
    viewModel: GalleryViewModel = hiltViewModel(),
) {
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    PugPrintTheme(theme) {
        DesignGallery(onBack = onClose, onHome = onClose, onThemeSelected = viewModel::onThemeSelected)
    }
}
