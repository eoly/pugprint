package com.example.pugprint

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.splashscreen.SplashScreenViewProvider
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
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setOnExitAnimationListener(::dismissSplashAfterAnimation)
        enableEdgeToEdge()
        setContent {
            val current by settings.settings.collectAsStateWithLifecycle()
            PugPrintTheme(ThemeCatalog.byId(current.themeId)) { PugPrintNavHost() }
        }
    }

    /**
     * Compose is usually ready before the pug has finished printing its sticker, so let the icon
     * animation (Android 12+) play to the end before the splash comes down. Older versions show a
     * still frame and report no animation, so the splash goes straight away.
     */
    private fun dismissSplashAfterAnimation(provider: SplashScreenViewProvider) {
        val remaining = splashRemainingMillis(provider.iconAnimationStartMillis, provider.iconAnimationDurationMillis)
        if (remaining <= 0) {
            provider.remove()
        } else {
            provider.view.postDelayed(provider::remove, remaining)
        }
    }

    private fun splashRemainingMillis(
        animationStartMillis: Long,
        animationDurationMillis: Long,
    ): Long =
        if (animationStartMillis <= 0 || animationDurationMillis <= 0) {
            0
        } else {
            val endsAt = animationStartMillis + animationDurationMillis
            (endsAt - SystemClock.uptimeMillis()).coerceIn(0, MAX_SPLASH_HOLD_MILLIS)
        }

    private companion object {
        /** Never keep a kid waiting on the splash longer than this, whatever the clocks say. */
        const val MAX_SPLASH_HOLD_MILLIS = 1_200L
    }
}
