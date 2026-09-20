package com.example.pugprint.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.pugprint.R
import com.example.pugprint.ui.theme.PugPrintTheme

@Composable
fun HomeScreen(
    state: HomeUiState,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
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
                text = statusLabel(state.printerStatus),
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = {},
                enabled = state.canPrint,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(64.dp),
            ) {
                Text(
                    text = stringResource(R.string.home_print),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
    }
}

@Composable
private fun statusLabel(status: PrinterStatus): String =
    when (status) {
        PrinterStatus.NotConnected -> stringResource(R.string.home_status_not_connected)
    }

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    PugPrintTheme { HomeScreen(state = HomeUiState()) }
}
