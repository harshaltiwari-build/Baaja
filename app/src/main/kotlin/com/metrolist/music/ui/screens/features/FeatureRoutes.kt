/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.features

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable

const val LOCAL_MUSIC_ROUTE = "local_music"
const val IMPORT_SPOTIFY_ROUTE = "import_spotify"
const val IMPORT_YOUTUBE_ROUTE = "import_youtube"

/** Routes for audio output, Spotify import and local music screens. */
fun NavGraphBuilder.featureRoutes(navController: NavHostController) {
    composable(LOCAL_MUSIC_ROUTE) {
        LocalMusicScreen(navController)
    }
    composable(IMPORT_SPOTIFY_ROUTE) {
        SpotifyImportScreen(navController)
    }
    composable(IMPORT_YOUTUBE_ROUTE) {
        YouTubeImportScreen(navController)
    }
}
