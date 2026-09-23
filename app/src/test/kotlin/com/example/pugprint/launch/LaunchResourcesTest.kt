package com.example.pugprint.launch

import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.AnimatedVectorDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.VectorDrawable
import androidx.core.content.res.ResourcesCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.pugprint.MainActivity
import com.example.pugprint.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import com.example.pugprint.design.R as DesignR

/**
 * The launcher icon, the splash animation and the in-app logo are one piece of art in three
 * resources. A broken vector only shows up on a device, so inflate each one here.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class LaunchResourcesTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `splash icon is an animated vector on PugPrint blue`() {
        val icon = ResourcesCompat.getDrawable(context.resources, R.drawable.avd_pug_splash, context.theme)
        assertTrue("expected AnimatedVectorDrawable, got $icon", icon is AnimatedVectorDrawable)
        assertEquals(PUG_BLUE, context.getColor(R.color.splash_bg))
    }

    @Test
    fun `launcher icon is adaptive with the pug foreground and a monochrome layer`() {
        val launcher = ResourcesCompat.getDrawable(context.resources, R.mipmap.ic_launcher, context.theme)
        assertTrue("expected AdaptiveIconDrawable, got $launcher", launcher is AdaptiveIconDrawable)
        launcher as AdaptiveIconDrawable
        assertEquals(PUG_BLUE, (launcher.background as ColorDrawable).color)
        assertTrue(launcher.foreground is VectorDrawable)
        assertTrue(launcher.monochrome is VectorDrawable)
    }

    @Test
    fun `in-app logo is the pug cropped to its 192-unit safe circle`() {
        val logo = ResourcesCompat.getDrawable(context.resources, DesignR.drawable.pug_logo, context.theme)
        assertTrue("expected VectorDrawable, got $logo", logo is VectorDrawable)
        val density = context.resources.displayMetrics.density
        assertEquals(LOGO_CANVAS_DP, (logo!!.intrinsicWidth / density).toInt())
        assertEquals(LOGO_CANVAS_DP, (logo.intrinsicHeight / density).toInt())
    }

    @Test
    fun `launcher activity starts in the splash theme and lands in the app theme`() {
        val info = context.packageManager.getActivityInfo(ComponentName(context, MainActivity::class.java), 0)
        assertEquals(R.style.Theme_PugPrint_Starting, info.themeResource)
        assertEquals(R.style.Theme_PugPrint, context.applicationInfo.theme)
    }

    private companion object {
        const val PUG_BLUE = 0xFF2D4CC8.toInt()
        const val LOGO_CANVAS_DP = 192
    }
}
