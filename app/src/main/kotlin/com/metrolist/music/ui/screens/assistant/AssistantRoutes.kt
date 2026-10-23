/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.assistant

import android.net.Uri
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

/** Route for Behind the Song / Music DNA of [videoId]. */
fun behindTheSongRoute(videoId: String): String = "behind_the_song/${Uri.encode(videoId)}"

/** Route for the Behind the Song screen. */
fun NavGraphBuilder.assistantRoutes(navController: NavHostController) {
    composable(
        route = "behind_the_song/{videoId}",
        arguments = listOf(navArgument("videoId") { type = NavType.StringType }),
    ) {
        BehindTheSongScreen(navController)
    }
}
