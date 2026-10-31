/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import android.app.Activity
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import com.metrolist.music.constants.DarkModeKey
import com.metrolist.music.constants.PureBlackKey
import com.metrolist.music.ui.screens.artist.ArtistAlbumsScreen
import com.metrolist.music.ui.screens.assistant.assistantRoutes
import com.metrolist.music.ui.screens.features.featureRoutes
import com.metrolist.music.ui.screens.onboarding.onboardingRoutes
import com.metrolist.music.ui.screens.artist.ArtistItemsScreen
import com.metrolist.music.ui.screens.artist.ArtistScreen
import com.metrolist.music.ui.screens.artist.ArtistSongsScreen
import com.metrolist.music.ui.screens.equalizer.EqScreen
import com.metrolist.music.ui.screens.equalizer.wizard.WizardScreen
import com.metrolist.music.ui.screens.library.LibraryScreen
import com.metrolist.music.ui.screens.playlist.AutoPlaylistScreen
import com.metrolist.music.ui.screens.playlist.CachePlaylistScreen
import com.metrolist.music.ui.screens.playlist.LocalPlaylistScreen
import com.metrolist.music.ui.screens.playlist.OnlinePlaylistScreen
import com.metrolist.music.ui.screens.playlist.TopPlaylistScreen
import com.metrolist.music.ui.screens.podcast.OnlinePodcastScreen
import com.metrolist.music.ui.screens.recognition.RecognitionHistoryScreen
import com.metrolist.music.ui.screens.recognition.RecognitionScreen
import com.metrolist.music.ui.screens.search.OnlineSearchResult
import com.metrolist.music.ui.screens.search.SearchScreen
import com.metrolist.music.ui.screens.settings.AboutScreen
import com.metrolist.music.ui.screens.settings.AndroidAutoSettings
import com.metrolist.music.ui.screens.settings.AppearanceSettings
import com.metrolist.music.ui.screens.settings.BackupAndRestore
import com.metrolist.music.ui.screens.settings.ContentSettings
import com.metrolist.music.ui.screens.settings.DarkMode
import com.metrolist.music.ui.screens.settings.PlaybackSettings
import com.metrolist.music.ui.screens.settings.PlayerPageSettings
import com.metrolist.music.ui.screens.settings.PlayerSettings
import com.metrolist.music.ui.screens.settings.QueueSettings
import com.metrolist.music.ui.screens.settings.TimersSettings
import com.metrolist.music.ui.screens.settings.PrivacySettings
import com.metrolist.music.ui.screens.settings.RomanizationSettings
import com.metrolist.music.ui.screens.settings.SettingsScreen
import com.metrolist.music.ui.screens.settings.StorageSettings
import com.metrolist.music.ui.screens.settings.UpdaterScreen
import com.metrolist.music.ui.screens.settings.ListenTogetherSettings

import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.navigationBuilder(
    navController: NavHostController,
    scrollBehavior: TopAppBarScrollBehavior,
    latestVersionName: String,
    activity: Activity,
    snackbarHostState: SnackbarHostState,
) {
    composable(Screens.Home.route) {
        HomeScreen(snackbarHostState = snackbarHostState)
    }

    composable(Screens.Search.route) { backStackEntry ->
        val pureBlackEnabled by rememberPreference(PureBlackKey, defaultValue = false)
        val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
        val isSystemInDarkTheme = isSystemInDarkTheme()
        val useDarkTheme =
            remember(darkTheme, isSystemInDarkTheme) {
                if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
            }
        val pureBlack =
            remember(pureBlackEnabled, useDarkTheme) {
                pureBlackEnabled && useDarkTheme
            }
        SearchScreen(
            pureBlack = pureBlack,
            savedStateHandle = backStackEntry.savedStateHandle
        )
    }

    composable(Screens.Library.route) {
        LibraryScreen()
    }

    composable(Screens.ListenTogether.route) {
        ListenTogetherScreen(navController)
    }

    composable("history") {
        HistoryScreen(navController)
    }

    composable("stats") {
        StatsScreen(navController)
    }

    composable("mood_and_genres") {
        MoodAndGenresScreen(navController)
    }

    composable("account") {
        AccountScreen(navController)
    }

    composable("new_release") {
        NewReleaseScreen(navController)
    }

    composable(
        route = "chart_songs/{playlistId}?title={title}",
        arguments =
            listOf(
                navArgument("playlistId") { type = NavType.StringType },
                navArgument("title") {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
    ) { backStackEntry ->
        ChartSongsScreen(navController, title = backStackEntry.arguments?.getString("title").orEmpty())
    }

    composable("charts_screen") {
        ChartsScreen(navController)
    }

    composable(
        route = "browse/{browseId}",
        arguments =
            listOf(
                navArgument("browseId") {
                    type = NavType.StringType
                },
            ),
    ) {
        BrowseScreen(
            navController,
            it.arguments?.getString("browseId"),
        )
    }

    composable(
        route = "search/{query}",
        arguments =
            listOf(
                navArgument("query") {
                    type = NavType.StringType
                },
            ),
        enterTransition = {
            fadeIn(tween(250))
        },
        exitTransition = {
            if (targetState.destination.route?.startsWith("search/") == true) {
                fadeOut(tween(200))
            } else {
                fadeOut(tween(200)) + slideOutHorizontally { -it / 2 }
            }
        },
        popEnterTransition = {
            if (initialState.destination.route?.startsWith("search/") == true) {
                fadeIn(tween(250))
            } else {
                fadeIn(tween(250)) + slideInHorizontally { -it / 2 }
            }
        },
        popExitTransition = {
            fadeOut(tween(200))
        },
    ) { backStackEntry ->
        OnlineSearchResult(
            savedStateHandle = backStackEntry.savedStateHandle
        )

    }

    composable(
        route = "album/{albumId}",
        arguments =
            listOf(
                navArgument("albumId") {
                    type = NavType.StringType
                },
            ),
    ) {
        AlbumScreen(navController)
    }

    composable(
        route = "artist/{artistId}?isPodcastChannel={isPodcastChannel}",
        arguments =
            listOf(
                navArgument("artistId") {
                    type = NavType.StringType
                },
                navArgument("isPodcastChannel") {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
    ) {
        ArtistScreen(navController)
    }

    composable(
        route = "artist/{artistId}/songs",
        arguments =
            listOf(
                navArgument("artistId") {
                    type = NavType.StringType
                },
            ),
    ) {
        ArtistSongsScreen(navController)
    }

    composable(
        route = "artist/{artistId}/albums",
        arguments =
            listOf(
                navArgument("artistId") {
                    type = NavType.StringType
                },
            ),
    ) {
        ArtistAlbumsScreen(navController, scrollBehavior)
    }

    composable(
        route = "artist/{artistId}/items?browseId={browseId}?params={params}",
        arguments =
            listOf(
                navArgument("artistId") {
                    type = NavType.StringType
                },
                navArgument("browseId") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("params") {
                    type = NavType.StringType
                    nullable = true
                },
            ),
    ) {
        ArtistItemsScreen(navController)
    }

    composable(
        route = "online_playlist/{playlistId}",
        arguments =
            listOf(
                navArgument("playlistId") {
                    type = NavType.StringType
                },
            ),
    ) {
        OnlinePlaylistScreen(navController)
    }

    composable(
        route = "online_podcast/{podcastId}",
        arguments =
            listOf(
                navArgument("podcastId") {
                    type = NavType.StringType
                },
            ),
    ) {
        OnlinePodcastScreen(navController, scrollBehavior)
    }

    composable(
        route = "local_playlist/{playlistId}",
        arguments =
            listOf(
                navArgument("playlistId") {
                    type = NavType.StringType
                },
            ),
    ) {
        LocalPlaylistScreen(navController)
    }

    composable(
        route = "auto_playlist/{playlist}",
        arguments =
            listOf(
                navArgument("playlist") {
                    type = NavType.StringType
                },
            ),
    ) {
        AutoPlaylistScreen(navController)
    }

    composable(
        route = "cache_playlist/{playlist}",
        arguments =
            listOf(
                navArgument("playlist") {
                    type = NavType.StringType
                },
            ),
    ) {
        CachePlaylistScreen(navController)
    }

    composable(
        route = "top_playlist/{top}",
        arguments =
            listOf(
                navArgument("top") {
                    type = NavType.StringType
                },
            ),
    ) {
        TopPlaylistScreen(navController)
    }

    composable(
        route = "youtube_browse/{browseId}?params={params}",
        arguments =
            listOf(
                navArgument("browseId") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("params") {
                    type = NavType.StringType
                    nullable = true
                },
            ),
    ) {
        YouTubeBrowseScreen(navController)
    }

    composable("settings") {
        SettingsScreen(navController)
    }

    composable("settings/appearance") {
        AppearanceSettings(navController, snackbarHostState)
    }

    composable("settings/appearance/lyrics") {
        AppearanceSettings(navController, snackbarHostState, lyricsPage = true)
    }


    composable("settings/content") {
        ContentSettings(navController)
    }

    composable("settings/content/romanization") {
        RomanizationSettings(navController)
    }

    composable("settings/player") {
        PlayerSettings(navController)
    }

    composable("settings/player/playback") {
        PlaybackSettings(navController)
    }

    composable("settings/player/player") {
        PlayerPageSettings(navController)
    }

    composable("settings/player/queue") {
        QueueSettings(navController)
    }

    composable("settings/player/timers") {
        TimersSettings(navController)
    }

    composable("settings/storage") {
        StorageSettings(navController)
    }

    composable("settings/privacy") {
        PrivacySettings(navController)
    }

    composable("settings/backup_restore") {
        BackupAndRestore(navController)
    }

    composable(route = "settings/listen_together") {
        ListenTogetherSettings(navController)
    }

    composable("settings/updater") {
        UpdaterScreen(navController)
    }

    composable("settings/about") {
        AboutScreen(navController)
    }

    composable("login") {
        LoginScreen(navController)
    }

    composable("login_direct") {
        LoginScreen(navController = navController, skipIntro = true)
    }

    composable("switch_channel") {
        LoginScreen(
            navController = navController,
            isSwitchingChannel = true,
        )
    }

    composable("equalizer") {
        EqScreen()
    }

    composable("eq_wizard") {
        WizardScreen(onNavigateBack = {
            navController.popBackStack()
        })
    }

    composable(
        route = "recognition?autoStart={autoStart}",
        arguments =
            listOf(
                navArgument("autoStart") {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
    ) {
        RecognitionScreen(navController, it.arguments?.getBoolean("autoStart") ?: false)
    }

    composable("recognition_history") {
        RecognitionHistoryScreen(navController)
    }
    composable("settings/android_auto") {
        AndroidAutoSettings(navController)
    }

    onboardingRoutes(navController)
    assistantRoutes(navController)
    featureRoutes(navController)
}
