package com.example.pugprint.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.pugprint.R
import com.example.pugprint.printer.OfflineReason
import com.example.pugprint.ui.theme.PugPrintTheme

@Composable
fun HomeScreen(
    state: HomeUiState,
    actions: HomeActions = HomeActions(),
    modifier: Modifier = Modifier,
) {
    val snackbar = remember { SnackbarHostState() }
    val messageText = state.message?.let { stringResource(messageRes(it)) }
    LaunchedEffect(messageText) {
        if (messageText != null) {
            snackbar.showSnackbar(messageText)
            actions.onMessageShown()
        }
    }
    Scaffold(modifier = modifier.fillMaxSize(), snackbarHost = { SnackbarHost(snackbar) }) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.home_title),
                style = MaterialTheme.typography.displayMedium,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = statusLabel(state),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            PrinterDetails(state)
            Spacer(Modifier.height(32.dp))
            if (state.hasPrinter) {
                BigButton(
                    text = stringResource(R.string.home_print_test),
                    enabled = state.canPrint,
                    onClick = actions.onPrintTestPage,
                )
                if (state.printerStatus == PrinterStatus.Offline) {
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = actions.onRetry) { Text(stringResource(R.string.home_retry)) }
                }
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = actions.onForget) { Text(stringResource(R.string.home_forget)) }
            } else {
                BigButton(
                    text = stringResource(R.string.home_connect),
                    enabled = !state.pairingInProgress,
                    onClick = actions.onConnect,
                )
            }
        }
    }
}

@Composable
private fun PrinterDetails(state: HomeUiState) {
    state.batteryPercent?.let { percent ->
        Spacer(Modifier.height(8.dp))
        Text(
            text =
                if (state.batteryLow) {
                    stringResource(R.string.home_battery_low)
                } else {
                    stringResource(R.string.home_battery, percent)
                },
            style = MaterialTheme.typography.bodyMedium,
            color = if (state.batteryLow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
    }
    if (state.paperOrLidProblem) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.home_paper_or_lid),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
    state.printProgress?.let { progress ->
        Spacer(Modifier.height(16.dp))
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun BigButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(64.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun statusLabel(state: HomeUiState): String {
    val name = state.printerName.orEmpty()
    return when (state.printerStatus) {
        PrinterStatus.NoPrinter -> stringResource(R.string.home_status_no_printer)
        PrinterStatus.Connecting -> stringResource(R.string.home_status_connecting, name)
        PrinterStatus.Connected -> stringResource(R.string.home_status_connected, name)
        PrinterStatus.Printing -> stringResource(R.string.home_status_printing)
        PrinterStatus.Offline ->
            when (state.offlineReason) {
                OfflineReason.LOST -> stringResource(R.string.home_status_offline_lost, name)
                OfflineReason.UNREACHABLE -> stringResource(R.string.home_status_offline_unreachable, name)
                OfflineReason.NO_PERMISSION -> stringResource(R.string.home_status_offline_no_permission, name)
                OfflineReason.IDLE, null -> stringResource(R.string.home_status_offline_idle, name)
            }
    }
}

private fun messageRes(message: HomeMessage): Int =
    when (message) {
        HomeMessage.PrintDone -> R.string.message_print_done
        HomeMessage.PrintNoPaper -> R.string.message_print_no_paper
        HomeMessage.PrintPaperOrLid -> R.string.message_print_paper_or_lid
        HomeMessage.PrintDisconnected -> R.string.message_print_disconnected
        HomeMessage.PrintFailed -> R.string.message_print_failed
        HomeMessage.PairingCancelled -> R.string.message_pairing_cancelled
        HomeMessage.PairingUnavailable -> R.string.message_pairing_unavailable
        HomeMessage.PermissionDenied -> R.string.message_permission_denied
    }

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    PugPrintTheme { HomeScreen(state = HomeUiState()) }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenConnectedPreview() {
    PugPrintTheme {
        HomeScreen(
            state = HomeUiState(PrinterStatus.Connected, printerName = "HB-1234", batteryPercent = 64),
        )
    }
}
