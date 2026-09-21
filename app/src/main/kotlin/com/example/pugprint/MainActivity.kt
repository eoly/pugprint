package com.example.pugprint

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pugprint.bluetooth.BluetoothPermissions
import com.example.pugprint.ui.home.HomeActions
import com.example.pugprint.ui.home.HomeScreen
import com.example.pugprint.ui.home.HomeViewModel
import com.example.pugprint.ui.theme.PugPrintTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PugPrintTheme { HomeRoute() }
        }
    }
}

/**
 * Glue between [HomeViewModel] and the platform pieces a ViewModel cannot own: the runtime
 * Bluetooth permission prompt and the Companion Device Manager picker (an IntentSender).
 */
@Composable
private fun HomeRoute(viewModel: HomeViewModel = hiltViewModel()) {
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

    LaunchedEffect(pairingLaunch) {
        pairingLaunch?.let { sender ->
            viewModel.onPairingLaunched()
            pairingLauncher.launch(IntentSenderRequest.Builder(sender).build())
        }
    }

    HomeScreen(
        state = state,
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
                onPrintTestPage = viewModel::onPrintTestPageClicked,
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
            ),
    )
}
