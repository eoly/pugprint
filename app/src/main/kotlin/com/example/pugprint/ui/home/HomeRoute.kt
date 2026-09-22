package com.example.pugprint.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pugprint.BuildConfig
import com.example.pugprint.bluetooth.BluetoothPermissions

/**
 * Glue between [HomeViewModel] and the platform pieces a ViewModel cannot own: the runtime
 * Bluetooth permission prompt, the Companion Device Manager picker (an IntentSender) and the
 * system Photo Picker (no storage permission needed).
 */
@Composable
fun HomeRoute(
    onPhotoPicked: (Uri) -> Unit,
    onDraw: () -> Unit,
    onDesignGallery: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pairingLaunch by viewModel.pairingLaunch.collectAsStateWithLifecycle()

    val pairingLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            viewModel.onPairingResult(result.data)
        }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            if (grants.values.all { it }) viewModel.onConnectClicked() else viewModel.onPermissionDenied()
        }
    val retryPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            if (grants.values.all { it }) viewModel.onRetryClicked() else viewModel.onPermissionDenied()
        }
    val photoPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) onPhotoPicked(uri)
        }

    LaunchedEffect(pairingLaunch) {
        pairingLaunch?.let { sender ->
            viewModel.onPairingLaunched()
            pairingLauncher.launch(IntentSenderRequest.Builder(sender).build())
        }
    }

    HomeScreen(
        state = state,
        showDesignGallery = BuildConfig.DEBUG,
        actions =
            HomeActions(
                onConnect = {
                    val missing = BluetoothPermissions.missing(context)
                    if (missing.isEmpty()) {
                        viewModel.onConnectClicked()
                    } else {
                        permissionLauncher.launch(
                            missing.toTypedArray(),
                        )
                    }
                },
                onPickPhoto = {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onDraw = onDraw,
                onDesignGallery = onDesignGallery,
                onPrintTestPage = viewModel::onPrintTestPageClicked,
                onPrintAgain = viewModel::onPrintAgainClicked,
                onRetry = {
                    val missing = BluetoothPermissions.missing(context)
                    if (missing.isEmpty()) {
                        viewModel.onRetryClicked()
                    } else {
                        retryPermissionLauncher.launch(
                            missing.toTypedArray(),
                        )
                    }
                },
                onForget = viewModel::onForgetClicked,
                onMessageShown = viewModel::onMessageShown,
                onThemeSelected = viewModel::onThemeSelected,
                onRollSelected = viewModel::onRollSelected,
            ),
    )
}
