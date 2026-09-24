package com.example.pugprint

import android.graphics.drawable.Animatable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.content.res.ResourcesCompat
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

    /** Wall-clock time this activity came up; stands in for the splash's start when the system reports none. */
    private var createdAtMillis = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        createdAtMillis = System.currentTimeMillis()
        splash.setOnExitAnimationListener(::holdSplash)
        enableEdgeToEdge()
        setContent {
            val current by settings.settings.collectAsStateWithLifecycle()
            PugPrintTheme(ThemeCatalog.byId(current.themeId)) { PugPrintNavHost() }
        }
    }

    /**
     * Compose is ready long before [SPLASH_MILLIS] are up, so keep the splash on screen. On
     * Android 12+ the system plays the icon animation once, on a surface the app cannot restart,
     * so when that run ends the same animated vector is laid over the icon and replayed back to
     * back: the pug keeps printing stickers instead of freezing on the last frame. Older versions
     * show the still frame for the same length of time.
     */
    private fun holdSplash(provider: SplashScreenViewProvider) {
        val shownAt = provider.iconAnimationStartMillis.takeIf { it > 0 } ?: createdAtMillis
        val endsAt = shownAt + SPLASH_MILLIS
        val runMillis = provider.iconAnimationDurationMillis
        val view = provider.view
        var replay: Animatable? = null

        fun tick() {
            val now = System.currentTimeMillis()
            when {
                now >= endsAt -> provider.remove()
                runMillis > 0 && now + runMillis <= endsAt -> {
                    val animation = replay ?: replayIconOver(provider).also { replay = it }
                    animation?.start()
                    view.postDelayed(::tick, runMillis)
                }
                else -> view.postDelayed(::tick, endsAt - now)
            }
        }
        // Let the run the system started finish before doing anything. (Both times are wall clock.)
        val currentRunEndsAt = provider.iconAnimationStartMillis + runMillis
        view.postDelayed(::tick, (currentRunEndsAt - System.currentTimeMillis()).coerceAtLeast(0))
    }

    /**
     * Covers the system's icon with an [ImageView] of the same drawable, drawn the way the system
     * draws it: the 288 dp canvas scaled up so its 192 dp safe circle fills the icon view.
     */
    private fun replayIconOver(provider: SplashScreenViewProvider): Animatable? {
        val container = provider.view as? ViewGroup
        val drawable = ResourcesCompat.getDrawable(resources, R.drawable.avd_pug_splash, theme)
        if (container == null || drawable == null) return null
        val icon = provider.iconView
        val size = (icon.width * SPLASH_ICON_CANVAS / SPLASH_ICON_SAFE_CIRCLE).toInt()
        val cover =
            ImageView(this).apply {
                setImageDrawable(drawable)
                scaleType = ImageView.ScaleType.FIT_XY
                x = icon.x - (size - icon.width) / 2f
                y = icon.y - (size - icon.height) / 2f
            }
        container.clipChildren = false
        container.addView(cover, ViewGroup.LayoutParams(size, size))
        icon.visibility = View.INVISIBLE
        return drawable as? Animatable
    }

    private companion object {
        /** How long the splash stays up, counted from when it appeared. */
        const val SPLASH_MILLIS = 3_000L

        /** Splash icons are drawn on a 288 dp canvas whose centred 192 dp circle is what shows. */
        const val SPLASH_ICON_CANVAS = 288f
        const val SPLASH_ICON_SAFE_CIRCLE = 192f
    }
}
