package com.example.pugprint.ui.home

import android.content.Intent
import android.content.IntentSender
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pugprint.printer.OfflineReason
import com.example.pugprint.printer.PairingStart
import com.example.pugprint.printer.PrinterManager
import com.example.pugprint.printer.PrinterPairing
import com.example.pugprint.printer.PrinterState
import com.example.pugprint.printer.printTestPage
import com.example.pugprint.printer.transport.PrintFailure
import com.example.pugprint.printer.transport.PrintResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Printer connection as seen by the home screen. */
enum class PrinterStatus {
    NoPrinter,
    Connecting,
    Connected,
    Printing,
    Offline,
}

/** One-shot feedback shown as a snackbar; dismissed via [HomeViewModel.onMessageShown]. */
enum class HomeMessage {
    PrintDone,
    PrintNoPaper,
    PrintPaperOrLid,
    PrintDisconnected,
    PrintFailed,
    PairingCancelled,
    PairingUnavailable,
    PermissionDenied,
}

data class HomeUiState(
    val printerStatus: PrinterStatus = PrinterStatus.NoPrinter,
    val printerName: String? = null,
    val offlineReason: OfflineReason? = null,
    /** 0–100, or `null` when unknown. */
    val batteryPercent: Int? = null,
    val batteryLow: Boolean = false,
    /** Lid open or out of paper (the printer reports both the same way). */
    val paperOrLidProblem: Boolean = false,
    /** 0.0–1.0 while [PrinterStatus.Printing]. */
    val printProgress: Float? = null,
    val pairingInProgress: Boolean = false,
    val message: HomeMessage? = null,
) {
    val canPrint: Boolean get() = printerStatus == PrinterStatus.Connected && !paperOrLidProblem
    val hasPrinter: Boolean get() = printerStatus != PrinterStatus.NoPrinter
}

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val printer: PrinterManager,
        private val pairing: PrinterPairing,
    ) : ViewModel() {
        private val message = MutableStateFlow<HomeMessage?>(null)
        private val pairingInProgress = MutableStateFlow(false)
        private val mutablePairingLaunch = MutableStateFlow<IntentSender?>(null)

        /** A system pairing picker the UI must launch; call [onPairingLaunched] once done. */
        val pairingLaunch: StateFlow<IntentSender?> = mutablePairingLaunch.asStateFlow()

        val uiState: StateFlow<HomeUiState> =
            combine(
                printer.state,
                printer.lastPrintResult,
                message,
                pairingInProgress,
            ) { state, print, msg, pairingNow ->
                state.toUiState().copy(
                    pairingInProgress = pairingNow,
                    message = msg ?: print?.toMessage(),
                )
            }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeUiState())

        init {
            printer.start()
        }

        /** "Connect a printer": runs the platform's pairing flow. Requires the Bluetooth permission first. */
        fun onConnectClicked() {
            if (pairingInProgress.value) return
            pairingInProgress.value = true
            viewModelScope.launch {
                when (val start = pairing.begin()) {
                    is PairingStart.Launch -> mutablePairingLaunch.value = start.intentSender
                    is PairingStart.Paired -> {
                        pairingInProgress.value = false
                        printer.connect(start.device)
                    }
                    is PairingStart.Unavailable -> {
                        pairingInProgress.value = false
                        message.value = HomeMessage.PairingUnavailable
                    }
                }
            }
        }

        fun onPairingLaunched() {
            mutablePairingLaunch.value = null
        }

        /** Result of the picker launched from [pairingLaunch]; `null` data means the user backed out. */
        fun onPairingResult(data: Intent?) {
            pairingInProgress.value = false
            val device = pairing.deviceFrom(data)
            if (device == null) message.value = HomeMessage.PairingCancelled else printer.connect(device)
        }

        fun onPermissionDenied() {
            pairingInProgress.value = false
            message.value = HomeMessage.PermissionDenied
        }

        /** Permission was just granted (or the user asked to try again). */
        fun onRetryClicked() = printer.retry()

        fun onPrintTestPageClicked() = printer.printTestPage()

        fun onForgetClicked() = printer.forget()

        fun onMessageShown() {
            message.value = null
            printer.dismissPrintResult()
        }

        private fun PrinterState.toUiState(): HomeUiState =
            when (this) {
                PrinterState.NoPrinter -> HomeUiState()
                is PrinterState.Offline ->
                    HomeUiState(PrinterStatus.Offline, device.displayName, offlineReason = reason)
                is PrinterState.Connecting -> HomeUiState(PrinterStatus.Connecting, device.displayName)
                is PrinterState.Connected ->
                    HomeUiState(
                        printerStatus = PrinterStatus.Connected,
                        printerName = device.displayName,
                        batteryPercent = identity.batteryMillivolts?.let(::batteryPercent),
                        batteryLow = identity.batteryLow,
                        paperOrLidProblem = paperOrLidProblem,
                    )
                is PrinterState.Printing ->
                    HomeUiState(
                        printerStatus = PrinterStatus.Printing,
                        printerName = device.displayName,
                        batteryPercent = identity.batteryMillivolts?.let(::batteryPercent),
                        batteryLow = identity.batteryLow,
                        printProgress = progress.fraction,
                    )
            }

        private fun PrintResult.toMessage(): HomeMessage =
            when (this) {
                PrintResult.Success -> HomeMessage.PrintDone
                is PrintResult.Failure ->
                    when (reason) {
                        PrintFailure.NO_PAPER -> HomeMessage.PrintNoPaper
                        PrintFailure.LID_OR_PAPER -> HomeMessage.PrintPaperOrLid
                        PrintFailure.DISCONNECTED -> HomeMessage.PrintDisconnected
                        PrintFailure.PRINTER_ERROR, PrintFailure.WRITE_FAILED -> HomeMessage.PrintFailed
                    }
            }

        companion object {
            /** Two-cell Li-ion: 6.0 V empty, 8.4 V full. Linear approximation for a rough gauge. */
            private const val EMPTY_MILLIVOLTS = 6_000
            private const val FULL_MILLIVOLTS = 8_400
            private const val PERCENT = 100

            fun batteryPercent(millivolts: Int): Int =
                ((millivolts - EMPTY_MILLIVOLTS) * PERCENT / (FULL_MILLIVOLTS - EMPTY_MILLIVOLTS)).coerceIn(0, PERCENT)
        }
    }
