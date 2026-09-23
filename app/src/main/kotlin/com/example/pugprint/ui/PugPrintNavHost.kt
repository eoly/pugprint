package com.example.pugprint.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.pugprint.imaging.DrawingHandoff
import com.example.pugprint.ui.coloring.ColoringRoute
import com.example.pugprint.ui.draw.DrawRoute
import com.example.pugprint.ui.editor.EditorRoute
import com.example.pugprint.ui.gallery.GalleryRoute
import com.example.pugprint.ui.home.HomeRoute

private const val HOME = "home"
private const val DRAW = "draw"
private const val COLORING = "coloring"
private const val GALLERY = "gallery"
private const val EDITOR = "editor/{photo}"
private const val PHOTO_ARG = "photo"

/**
 * Home (printer + pick a photo / draw / color), the draw sheet, the coloring-page picker, and the
 * editor for a picked photo, a finished drawing or a coloring page (the last two reach it
 * through [DrawingHandoff]).
 */
@Composable
fun PugPrintNavHost() {
    val navController = rememberNavController()

    fun openEditor(uri: String) = navController.navigate("editor/${Uri.encode(uri)}")

    // Home is where a kid lands after printing (progress, "Print it again") and where the Home button goes.
    fun goHome() = navController.popBackStack(HOME, inclusive = false)
    NavHost(navController = navController, startDestination = HOME) {
        composable(HOME) {
            HomeRoute(
                onPhotoPicked = { uri -> openEditor(uri.toString()) },
                onDraw = { navController.navigate(DRAW) },
                onColor = { navController.navigate(COLORING) },
                onDesignGallery = { navController.navigate(GALLERY) },
            )
        }
        composable(GALLERY) {
            GalleryRoute(onClose = { navController.popBackStack() })
        }
        composable(COLORING) {
            ColoringRoute(
                onClose = { navController.popBackStack() },
                onHome = { goHome() },
                onPageReady = { openEditor(DrawingHandoff.URI) },
            )
        }
        composable(DRAW) {
            DrawRoute(
                onClose = { navController.popBackStack() },
                onHome = { goHome() },
                onDrawingReady = { openEditor(DrawingHandoff.URI) },
            )
        }
        composable(EDITOR, arguments = listOf(navArgument(PHOTO_ARG) { type = NavType.StringType })) { entry ->
            EditorRoute(
                photoUri = entry.arguments?.getString(PHOTO_ARG).orEmpty(),
                onClose = { navController.popBackStack() },
                onHome = { goHome() },
            )
        }
    }
}
