package com.example.pugprint

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pugprint.design.theme.PugPrintTheme
import com.example.pugprint.design.theme.ThemeCatalog
import com.example.pugprint.settings.SettingsStore
import com.example.pugprint.ui.PugPrintNavHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var settings: SettingsStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val current by settings.settings.collectAsStateWithLifecycle()
            PugPrintTheme(ThemeCatalog.byId(current.themeId)) { PugPrintNavHost() }
        }
    }
}
