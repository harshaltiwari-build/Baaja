/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import com.metrolist.music.ui.menu.YouTubeSongMenu
import com.metrolist.music.ui.component.home.BaajaYouTubeCard
import com.metrolist.music.ui.component.home.BaajaHomeSectionTitle
import com.metrolist.music.ui.component.home.BaajaHomeGutter
import com.metrolist.music.ui.component.home.BaajaChartPlaylistRow
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.innertube.models.WatchEndpoint
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.getValue
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import androidx.compose.foundation.lazy.grid.GridItemSpan
import com.metrolist.music.constants.AppBarHeight
import com.metrolist.music.ui.component.home.BaajaErrorState
import com.metrolist.music.ui.component.home.BaajaLoadingLabel
import com.metrolist.music.constants.GridItemSize
import com.metrolist.music.constants.GridItemsSizeKey
import com.metrolist.music.constants.GridThumbnailHeight
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.YouTubeGridItem
import com.metrolist.music.ui.component.shimmer.GridItemPlaceHolder
import com.metrolist.music.ui.component.shimmer.ShimmerHost
import com.metrolist.music.ui.menu.YouTubeAlbumMenu
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.ui.component.home.BaajaBrowseTopBar
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.viewmodels.NewReleaseViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NewReleaseScreen(
    navController: NavController,
    viewModel: NewReleaseViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()

    val newReleaseAlbums by viewModel.newReleaseAlbums.collectAsStateWithLifecycle()
    val featured by viewModel.featured.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val hasError by viewModel.hasError.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)

    if (newReleaseAlbums.isEmpty() && !isLoading && hasError) {
        BaajaErrorState(
            title = stringResource(R.string.baaja_new_releases_error_title),
            body = stringResource(R.string.baaja_charts_error_body),
            onRetry = viewModel::load,
            modifier = Modifier.padding(top = AppBarHeight),
        )
    } else LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = GridThumbnailHeight + if (gridItemSize == GridItemSize.BIG) 24.dp else (-24).dp),
        contentPadding = LocalPlayerAwareWindowInsets.current.add(WindowInsets(left = 4.dp, right = 4.dp)).asPaddingValues(),
    ) {
        // Same order as YouTube Music's New releases (music only): featured playlists, then albums & singles.
        if (featured.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "newrelease_featured") {
                BaajaChartPlaylistRow(
                    playlists = featured,
                    onOpen = { navController.navigate("online_playlist/${it.id}") },
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }, key = "newrelease_albums_title") {
                BaajaHomeSectionTitle(title = stringResource(R.string.baaja_new_releases_albums))
            }
        }
        items(
            items = newReleaseAlbums.distinctBy { it.id },
            key = { "newrelease_${it.id}" },
        ) { album ->
            YouTubeGridItem(
                item = album,
                isActive = mediaMetadata?.album?.id == album.id,
                isPlaying = isPlaying,
                fillMaxWidth = true,
                coroutineScope = coroutineScope,
                modifier =
                    Modifier
                        .combinedClickable(
                            onClick = {
                                navController.navigate("album/${album.id}")
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuState.show {
                                    YouTubeAlbumMenu(
                                        albumItem = album,
                                        onDismiss = menuState::dismiss,
                                    )
                                }
                            },
                        ),
            )
        }

        if (newReleaseAlbums.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "newrelease_loading_label") {
                BaajaLoadingLabel()
            }
            items(8) {
                ShimmerHost {
                    GridItemPlaceHolder(fillMaxWidth = true)
                }
            }
        }
    }

    BaajaBrowseTopBar(
        title = stringResource(R.string.baaja_home_new_releases),
        onBack = navController::navigateUp,
        onBackLongClick = navController::backToMain,
    )
}
