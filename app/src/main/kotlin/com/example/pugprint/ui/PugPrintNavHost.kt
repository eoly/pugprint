package com.example.pugprint.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.pugprint.ui.editor.EditorRoute
import com.example.pugprint.ui.home.HomeRoute

private const val HOME = "home"
private const val EDITOR = "editor/{photo}"
private const val PHOTO_ARG = "photo"

/** Two screens: home (printer + pick a photo) and the editor for the picked photo. */
@Composable
fun PugPrintNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = HOME) {
        composable(HOME) {
            HomeRoute(onPhotoPicked = { uri -> navController.navigate("editor/${Uri.encode(uri.toString())}") })
        }
        composable(EDITOR, arguments = listOf(navArgument(PHOTO_ARG) { type = NavType.StringType })) { entry ->
            EditorRoute(
                photoUri = entry.arguments?.getString(PHOTO_ARG).orEmpty(),
                onClose = { navController.popBackStack() },
            )
        }
    }
}
