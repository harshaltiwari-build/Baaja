/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import com.metrolist.music.ui.theme.baajaEntrance
import com.metrolist.music.ui.theme.rememberBaajaEntranceKey
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import com.metrolist.music.LocalNavController
import com.metrolist.innertube.YouTube.SearchFilter.Companion.FILTER_ALBUM
import com.metrolist.innertube.YouTube.SearchFilter.Companion.FILTER_ARTIST
import com.metrolist.innertube.YouTube.SearchFilter.Companion.FILTER_COMMUNITY_PLAYLIST
import com.metrolist.innertube.YouTube.SearchFilter.Companion.FILTER_EPISODE
import com.metrolist.innertube.YouTube.SearchFilter.Companion.FILTER_FEATURED_PLAYLIST
import com.metrolist.innertube.YouTube.SearchFilter.Companion.FILTER_PODCAST
import com.metrolist.innertube.YouTube.SearchFilter.Companion.FILTER_PROFILE
import com.metrolist.innertube.YouTube.SearchFilter.Companion.FILTER_SONG
import com.metrolist.innertube.YouTube.SearchFilter.Companion.FILTER_VIDEO
import com.metrolist.innertube.YouTubeConstants
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.EpisodeItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.PodcastItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.innertube.models.YTItem
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.rememberKeepPlayingSimilar
import com.metrolist.music.constants.HideVideoSongsKey
import com.metrolist.music.constants.MiniPlayerBottomSpacing
import com.metrolist.music.constants.MiniPlayerHeight
import com.metrolist.music.constants.NavigationBarHeight
import com.metrolist.music.constants.PauseSearchHistoryKey
import com.metrolist.music.db.entities.SearchHistory
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.component.EmptyPlaceholder
import com.metrolist.music.ui.component.HideOnScrollFAB
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.home.BaajaChipsRow
import com.metrolist.music.ui.component.home.BaajaHomeGutter
import com.metrolist.music.ui.component.home.BaajaHomeSectionTitle
import com.metrolist.music.ui.component.home.BaajaMoreButton
import com.metrolist.music.ui.component.home.BaajaPlainIconButton
import com.metrolist.music.ui.component.home.BaajaSearchField
import com.metrolist.music.ui.component.home.BaajaYouTubeCard
import com.metrolist.music.ui.component.home.BaajaYouTubeRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.Alignment
import com.metrolist.music.ui.component.shimmer.ListItemPlaceHolder
import com.metrolist.music.ui.component.shimmer.ShimmerHost
import com.metrolist.music.ui.menu.YouTubeAlbumMenu
import com.metrolist.music.ui.menu.YouTubeArtistMenu
import com.metrolist.music.ui.menu.YouTubePlaylistMenu
import com.metrolist.music.ui.menu.YouTubeSongMenu
import com.metrolist.music.utils.SearchRoutes
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.OnlineSearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OnlineSearchResult(
    viewModel: OnlineSearchViewModel = hiltViewModel(),
    pureBlack: Boolean = false,
    savedStateHandle: SavedStateHandle? = null
) {
    val navController = LocalNavController.current
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val scrollToTopCount by savedStateHandle
        ?.getStateFlow("scrollToTopCount", 0)
        ?.collectAsStateWithLifecycle(initialValue = 0) ?: remember { mutableIntStateOf(0) }

    var lastHandledCount by rememberSaveable { mutableIntStateOf(0) }
    var isSearchFocused by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(scrollToTopCount) {
        if (scrollToTopCount > lastHandledCount) {
            lastHandledCount = scrollToTopCount
            kotlinx.coroutines.delay(100)
            try {
                focusRequester.requestFocus()
                keyboardController?.show()
            } catch (e: Exception) {
            }
            // Set focused AFTER requesting focus, not before

            isSearchFocused = true
        }
    }

    val pauseSearchHistory by rememberPreference(PauseSearchHistoryKey, defaultValue = false)
    val hideVideoSongs by rememberPreference(HideVideoSongsKey, defaultValue = true)
    val autoRadioQueue by rememberKeepPlayingSimilar()

    BackHandler(enabled = isSearchFocused) {
        isSearchFocused = false
        focusManager.clearFocus()
    }

    // Extract query from navigation arguments
    val encodedQuery = navController.currentBackStackEntry?.arguments?.getString("query") ?: ""
    val decodedQuery = remember(encodedQuery) { SearchRoutes.decodeQuery(encodedQuery) }

    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(decodedQuery, TextRange(decodedQuery.length)))
    }

    val onSearch: (String) -> Unit =
        remember {
            { searchQuery ->
                if (searchQuery.isNotEmpty()) {
                    isSearchFocused = false
                    focusManager.clearFocus()

                    navController.navigate(SearchRoutes.resultRoute(searchQuery)) {
                        popUpTo(SearchRoutes.ROUTE) {
                            inclusive = true
                        }

                        if (!pauseSearchHistory) {
                            coroutineScope.launch(Dispatchers.IO) {
                                database.query {
                                    insert(SearchHistory(query = searchQuery))
                                }
                            }
                        }
                    }
                }
            }
        }

    // Update query when decodedQuery changes
    LaunchedEffect(decodedQuery) {
        query = TextFieldValue(decodedQuery, TextRange(decodedQuery.length))
    }

    // Clear video filter if hideVideoSongs setting is enabled and filter is set to FILTER_VIDEO
    LaunchedEffect(hideVideoSongs) {
        if (hideVideoSongs && viewModel.filter.value == FILTER_VIDEO) {
            viewModel.filter.value = null
        }
    }

    val searchFilter by viewModel.filter.collectAsStateWithLifecycle()
    val searchSummary = viewModel.summaryPage
    val itemsPage by remember(searchFilter) {
        derivedStateOf {
            searchFilter?.value?.let {
                viewModel.viewStateMap[it]
            }
        }
    }

    LaunchedEffect(lazyListState) {
        snapshotFlow {
            lazyListState.layoutInfo.visibleItemsInfo.any { it.key == "loading" }
        }.collect { shouldLoadMore ->
            if (!shouldLoadMore) return@collect
            viewModel.loadMore()
        }
    }

    val showItemMenu: (YTItem) -> Unit = { item ->
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        menuState.show {
            when (item) {
                is SongItem -> {
                    YouTubeSongMenu(
                        song = item,
                        onDismiss = menuState::dismiss,
                    )
                }

                is AlbumItem -> {
                    YouTubeAlbumMenu(
                        albumItem = item,
                        onDismiss = menuState::dismiss,
                    )
                }

                is ArtistItem -> {
                    YouTubeArtistMenu(
                        artist = item,
                        onDismiss = menuState::dismiss,
                    )
                }

                is PlaylistItem -> {
                    YouTubePlaylistMenu(
                        playlist = item,
                        coroutineScope = coroutineScope,
                        onDismiss = menuState::dismiss,
                    )
                }

                is PodcastItem -> {
                    YouTubePlaylistMenu(
                        playlist = item.asPlaylistItem(),
                        coroutineScope = coroutineScope,
                        onDismiss = menuState::dismiss,
                    )
                }

                is EpisodeItem -> {
                    YouTubeSongMenu(
                        song = item.asSongItem(),
                        onDismiss = menuState::dismiss,
                    )
                }
            }
        }
    }

    val openItem: (YTItem) -> Unit = { item ->
        when (item) {
            is SongItem -> {
                if (item.id == mediaMetadata?.id) {
                    playerConnection.togglePlayPause()
                } else {
                    playerConnection.playQueue(
                        if (autoRadioQueue) {
                            YouTubeQueue(
                                WatchEndpoint(videoId = item.id),
                                item.toMediaMetadata(),
                            )
                        } else {
                            ListQueue(
                                title = item.title,
                                items = listOf(item.toMediaItem())
                            )
                        }
                    )
                }
            }

            is AlbumItem -> {
                navController.navigate("album/${item.id}")
            }

            is ArtistItem -> {
                navController.navigate("artist/${item.id}")
            }

            is PlaylistItem -> {
                navController.navigate("online_playlist/${item.id}")
            }

            is PodcastItem -> {
                navController.navigate("online_podcast/${item.id}")
            }

            is EpisodeItem -> {
                if (item.id == mediaMetadata?.id) {
                    playerConnection.togglePlayPause()
                } else {
                    playerConnection.playQueue(
                        YouTubeQueue(
                            WatchEndpoint(videoId = item.id),
                            item.toMediaMetadata(),
                        ),
                    )
                }
            }
        }
    }

    val ytItemContent: @Composable LazyItemScope.(YTItem) -> Unit = { item: YTItem ->
        BaajaYouTubeRow(
            item = item,
            isActive = item.isNowPlaying(mediaMetadata),
            isPlaying = isPlaying,
            trailing = {
                BaajaMoreButton(
                    itemTitle = item.title,
                    onClick = { showItemMenu(item) },
                )
            },
            modifier =
                Modifier
                    .combinedClickable(
                        onClick = { openItem(item) },
                        onLongClick = { showItemMenu(item) },
                    ).animateItem(),
        )
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.surface)
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = BaajaHomeGutter, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            BaajaPlainIconButton(
                icon = R.drawable.arrow_back,
                contentDescription = stringResource(R.string.baaja_home_back),
                onClick = { navController.navigateUp() },
                iconSize = 26.dp,
            )
            BaajaSearchField(
                value = query,
                onValueChange = { newQuery -> query = newQuery },
                onSearch = { onSearch(it) },
                placeholder = stringResource(R.string.search_yt_music),
                highlighted = isSearchFocused,
                focusRequester = focusRequester,
                onFocusChanged = { focused -> if (focused) isSearchFocused = true },
                height = 52.dp,
                modifier = Modifier.weight(1f),
            )
        }

        // Main content area below search bar
        Box(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                val visibleChips =
                    listOf(
                        null to stringResource(R.string.filter_all),
                        FILTER_SONG to stringResource(R.string.filter_songs),
                    ).let { baseChips ->
                        if (!hideVideoSongs) {
                            baseChips + (FILTER_VIDEO to stringResource(R.string.filter_videos))
                        } else {
                            baseChips
                        }
                    } +
                        listOf(
                            FILTER_ALBUM to stringResource(R.string.filter_albums),
                            FILTER_ARTIST to stringResource(R.string.filter_artists),
                            FILTER_COMMUNITY_PLAYLIST to stringResource(R.string.filter_community_playlists),
                            FILTER_FEATURED_PLAYLIST to stringResource(R.string.filter_featured_playlists),
                            FILTER_PODCAST to stringResource(R.string.filter_podcasts),
                            FILTER_EPISODE to stringResource(R.string.filter_episodes),
                            FILTER_PROFILE to stringResource(R.string.filter_profiles),
                        )

                BaajaChipsRow(
                    chips = visibleChips,
                    currentValue = searchFilter,
                    onValueUpdate = {
                        if (viewModel.filter.value != it) {
                            viewModel.filter.value = it
                        }
                        coroutineScope.launch {
                            lazyListState.animateScrollToItem(0)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                val entranceKey = rememberBaajaEntranceKey()
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (searchFilter == null) {
                        searchSummary?.summaries?.forEachIndexed { summaryIndex, summary ->
                            val isTopResult = summary.title == YouTubeConstants.DEFAULT_TOP_RESULT
                            val isCardRow =
                                !isTopResult &&
                                    summary.items.isNotEmpty() &&
                                    summary.items.all { it is AlbumItem || it is PlaylistItem || it is PodcastItem }

                            if (isTopResult && summary.items.isNotEmpty()) {
                                val top = summary.items.first()
                                item(key = "summary_$summaryIndex/top/${top.id}") {
                                    TopResultCard(
                                        item = top,
                                        isActive = top.isNowPlaying(mediaMetadata),
                                        isPlaying = isPlaying,
                                        onOpen = { openItem(top) },
                                        onMore = { showItemMenu(top) },
                                        onPlay = topResultPlayAction(top, playerConnection) { openItem(top) },
                                        onRadio = topResultRadioAction(top, playerConnection),
                                        modifier = Modifier.animateItem().baajaEntrance(0, entranceKey),
                                    )
                                }
                                itemsIndexed(
                                    items = summary.items.drop(1),
                                    key = { index, item -> "summary_$summaryIndex/${item.id}/${index + 1}" },
                                    itemContent = { _, item -> ytItemContent(item) },
                                )
                            } else {
                                item(key = "summary_$summaryIndex/title") {
                                    BaajaHomeSectionTitle(
                                        title = summary.title.toLocalizedSearchSection(),
                                        modifier = Modifier.animateItem().baajaEntrance(summaryIndex + 1, entranceKey),
                                    )
                                }

                                if (isCardRow) {
                                    item(key = "summary_$summaryIndex/cards") {
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = BaajaHomeGutter - 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.animateItem(),
                                        ) {
                                            items(
                                                items = summary.items.distinctBy { it.id },
                                                key = { "${summary.title}/card/${it.id}" },
                                            ) { item ->
                                                BaajaYouTubeCard(
                                                    item = item,
                                                    isActive = item.isNowPlaying(mediaMetadata),
                                                    isPlaying = isPlaying,
                                                    onClick = { openItem(item) },
                                                    onLongClick = { showItemMenu(item) },
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    itemsIndexed(
                                        items = summary.items,
                                        key = { index, item -> "summary_$summaryIndex/${item.id}/$index" },
                                        itemContent = { _, item -> ytItemContent(item) },
                                    )
                                }
                            }
                        }

                        if (searchSummary?.summaries?.isEmpty() == true) {
                            item {
                                EmptyPlaceholder(
                                    icon = R.drawable.search,
                                    text = stringResource(R.string.no_results_found),
                                )
                            }
                        }
                    } else {
                        items(
                            items = itemsPage?.items.orEmpty().distinctBy { it.id },
                            key = { "filtered_${it.id}" },
                            itemContent = ytItemContent,
                        )

                        if (itemsPage?.continuation != null) {
                            item(key = "loading") {
                                ShimmerHost {
                                    repeat(3) {
                                        ListItemPlaceHolder()
                                    }
                                }
                            }
                        }

                        if (itemsPage?.items?.isEmpty() == true) {
                            item {
                                EmptyPlaceholder(
                                    icon = R.drawable.search,
                                    text = stringResource(R.string.no_results_found),
                                )
                            }
                        }
                    }

                    if (searchFilter == null && searchSummary == null || searchFilter != null && itemsPage == null) {
                        item {
                            ShimmerHost {
                                repeat(8) {
                                    ListItemPlaceHolder()
                                }
                            }
                        }
                    }

                    item(key = "bottom_spacer") {
                        Spacer(modifier = Modifier.height(MiniPlayerHeight + MiniPlayerBottomSpacing + NavigationBarHeight))
                    }
                }
            }
            if (isSearchFocused) {
                OnlineSearchScreen(
                    query = query.text,
                    onQueryChange = { query = it },
                    onSearch = onSearch,
                    onDismiss = {
                        isSearchFocused = false
                        focusManager.clearFocus()
                    },
                    pureBlack = pureBlack,
                )
            }
            HideOnScrollFAB(
                lazyListState = lazyListState,
                icon = R.drawable.mic,
                onClick = { navController.navigate("recognition") },
            )
        }
    }
}

@Composable
private fun String.toLocalizedSearchSection(): String =
    when (this) {
        YouTubeConstants.DEFAULT_TOP_RESULT -> stringResource(R.string.top_result)
        YouTubeConstants.DEFAULT_OTHER_RESULTS -> stringResource(R.string.other)
        "Songs" -> stringResource(R.string.songs)
        "Videos" -> stringResource(R.string.filter_videos)
        "Albums" -> stringResource(R.string.albums)
        "Artists" -> stringResource(R.string.artists)
        "Playlists" -> stringResource(R.string.playlists)
        "Podcasts" -> stringResource(R.string.filter_podcasts)
        "Episodes" -> stringResource(R.string.filter_episodes)
        "Profiles" -> stringResource(R.string.filter_profiles)
        else -> this
    }

private fun YTItem.isNowPlaying(mediaMetadata: com.metrolist.music.models.MediaMetadata?): Boolean =
    when (this) {
        is SongItem -> mediaMetadata?.id == id
        is AlbumItem -> mediaMetadata?.album?.id == id
        is EpisodeItem -> mediaMetadata?.id == id
        else -> false
    }
