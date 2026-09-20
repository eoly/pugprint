package com.example.pugprint.ui.home

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/** Printer connection as seen by the home screen. Real states arrive with the BLE transport (Phase 3). */
enum class PrinterStatus {
    NotConnected,
}

data class HomeUiState(
    val printerStatus: PrinterStatus = PrinterStatus.NotConnected,
) {
    val canPrint: Boolean get() = printerStatus != PrinterStatus.NotConnected
}

@HiltViewModel
class HomeViewModel
    @Inject
    constructor() : ViewModel() {
        private val mutableUiState = MutableStateFlow(HomeUiState())
        val uiState: StateFlow<HomeUiState> = mutableUiState.asStateFlow()
    }
