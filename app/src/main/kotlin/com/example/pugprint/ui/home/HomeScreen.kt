package com.example.pugprint.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.pugprint.R
import com.example.pugprint.design.components.BannerKind
import com.example.pugprint.design.components.BigButton
import com.example.pugprint.design.components.ButtonEmphasis
import com.example.pugprint.design.components.HeroTitle
import com.example.pugprint.design.components.KidScreen
import com.example.pugprint.design.components.StatusBanner
import com.example.pugprint.design.components.ThemePicker
import com.example.pugprint.design.theme.PugPrintTheme
import com.example.pugprint.design.theme.PugSpacing
import com.example.pugprint.design.theme.ThemeCatalog

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
    KidScreen(modifier = modifier, snackbarHost = { SnackbarHost(snackbar) }) {
        Spacer(Modifier.height(PugSpacing.huge))
        HeroTitle(stringResource(R.string.home_title))
        Spacer(Modifier.height(PugSpacing.large))
        PrinterStatus(state)
        Spacer(Modifier.height(PugSpacing.huge))
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(PugSpacing.medium)) {
            if (state.hasPrinter) {
                BigButton(text = stringResource(R.string.home_print_photo), onClick = actions.onPickPhoto)
                BigButton(
                    text = stringResource(R.string.home_print_test),
                    onClick = actions.onPrintTestPage,
                    enabled = state.canPrint,
                    emphasis = ButtonEmphasis.Secondary,
                )
                if (state.printerStatus == PrinterStatus.Offline) {
                    BigButton(
                        text = stringResource(R.string.home_retry),
                        onClick = actions.onRetry,
                        emphasis = ButtonEmphasis.Secondary,
                    )
                }
                BigButton(
                    text = stringResource(R.string.home_forget),
                    onClick = actions.onForget,
                    emphasis = ButtonEmphasis.Quiet,
                )
            } else {
                BigButton(
                    text = stringResource(R.string.home_connect),
                    onClick = actions.onConnect,
                    enabled = !state.pairingInProgress,
                )
            }
        }
        Spacer(Modifier.height(PugSpacing.huge))
        Text(
            text = stringResource(R.string.home_pick_look),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(PugSpacing.small))
        ThemePicker(
            themes = ThemeCatalog.all,
            selectedId = state.themeId,
            onSelect = { actions.onThemeSelected(it.id) },
        )
    }
}

/** Where the printer is at, plus anything the kid needs to fix, as banners. */
@Composable
private fun PrinterStatus(state: HomeUiState) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(PugSpacing.small)) {
        StatusBanner(
            kind = state.printerStatus.bannerKind(),
            text = printerStatusLabel(state.printerStatus, state.printerName, state.offlineReason),
            hint =
                state.batteryPercent?.takeUnless { state.batteryLow }?.let {
                    stringResource(
                        R.string.home_battery,
                        it,
                    )
                },
            progress = state.printProgress,
        )
        if (state.batteryLow) {
            StatusBanner(kind = BannerKind.Problem, text = stringResource(R.string.home_battery_low))
        }
        if (state.paperOrLidProblem) {
            StatusBanner(kind = BannerKind.Problem, text = stringResource(R.string.home_paper_or_lid))
        }
    }
}

private fun PrinterStatus.bannerKind(): BannerKind =
    when (this) {
        PrinterStatus.NoPrinter -> BannerKind.Info
        PrinterStatus.Connecting, PrinterStatus.Printing -> BannerKind.Working
        PrinterStatus.Connected -> BannerKind.Success
        PrinterStatus.Offline -> BannerKind.Problem
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
