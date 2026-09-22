package com.example.pugprint.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.pugprint.R
import com.example.pugprint.printer.OfflineReason

/** One line saying where the printer is at, shared by the home and editor screens. */
@Composable
fun printerStatusLabel(
    status: PrinterStatus,
    printerName: String?,
    offlineReason: OfflineReason?,
): String {
    val name = printerName.orEmpty()
    return when (status) {
        PrinterStatus.NoPrinter -> stringResource(R.string.home_status_no_printer)
        PrinterStatus.Connecting -> stringResource(R.string.home_status_connecting, name)
        PrinterStatus.Connected -> stringResource(R.string.home_status_connected, name)
        PrinterStatus.Printing -> stringResource(R.string.home_status_printing)
        PrinterStatus.Offline ->
            when (offlineReason) {
                OfflineReason.LOST -> stringResource(R.string.home_status_offline_lost, name)
                OfflineReason.UNREACHABLE -> stringResource(R.string.home_status_offline_unreachable, name)
                OfflineReason.NO_PERMISSION -> stringResource(R.string.home_status_offline_no_permission, name)
                OfflineReason.IDLE, null -> stringResource(R.string.home_status_offline_idle, name)
            }
    }
}

/** A second, smaller line under [printerStatusLabel] saying what to do, or `null` when nothing is needed. */
@Composable
fun printerStatusHint(
    status: PrinterStatus,
    offlineReason: OfflineReason?,
    printProgress: Float?,
): String? =
    when (status) {
        PrinterStatus.NoPrinter -> stringResource(R.string.home_status_no_printer_hint)
        PrinterStatus.Connecting -> null
        PrinterStatus.Connected -> null
        PrinterStatus.Printing ->
            printProgress?.let { stringResource(R.string.home_status_printing_hint, (it * PERCENT).toInt()) }
        PrinterStatus.Offline ->
            when (offlineReason) {
                OfflineReason.LOST -> stringResource(R.string.home_status_offline_lost_hint)
                OfflineReason.UNREACHABLE -> stringResource(R.string.home_status_offline_unreachable_hint)
                OfflineReason.NO_PERMISSION -> stringResource(R.string.home_status_offline_no_permission_hint)
                OfflineReason.IDLE, null -> stringResource(R.string.home_status_offline_idle_hint)
            }
    }

private const val PERCENT = 100
