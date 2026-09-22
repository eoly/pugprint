package com.example.pugprint.design

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.pugprint.design.gallery.DesignGallery
import com.example.pugprint.design.theme.PugPrintTheme
import com.example.pugprint.design.theme.ThemeCatalog
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * One golden per theme in [ThemeCatalog.all], each showing every kit component
 * (`ui/design/screenshots/DesignGallery.<id>.png`). `./gradlew recordRoborazziDebug` to (re)record.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h1100dp-xxhdpi")
class DesignGalleryScreenshotTest(
    private val themeId: String,
) {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun gallery() {
        val theme = ThemeCatalog.byId(themeId)
        compose.setContent { PugPrintTheme(theme) { DesignGallery() } }
        compose
            .onRoot()
            .captureRoboImage("screenshots/DesignGallery.${theme.id}.png", roborazziOptions = Screenshots.options)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun themes(): List<Array<Any>> = ThemeCatalog.all.map { arrayOf<Any>(it.id) }
    }
}
