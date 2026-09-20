package com.example.pugprint

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
            PugPrintTheme {
                val viewModel: HomeViewModel = hiltViewModel()
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                HomeScreen(state = state)
            }
        }
    }
}
