/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metrolist.music.LocalNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.EpisodeItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.PodcastItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.innertube.models.YTItem
import com.metrolist.innertube.utils.completed
import com.metrolist.innertube.utils.parseCookieString
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalArtistNameAliases
import com.metrolist.music.LocalListenTogetherManager
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.AutoRadioQueueKey
import com.metrolist.music.constants.GridItemSize
import com.metrolist.music.constants.GridItemsSizeKey
import com.metrolist.music.constants.GridThumbnailHeight
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.constants.ListThumbnailSize
import com.metrolist.music.constants.SmallGridThumbnailHeight
import com.metrolist.music.constants.ThumbnailCornerRadius
import com.metrolist.music.db.entities.Album
import com.metrolist.music.db.entities.Artist
import com.metrolist.music.db.entities.LocalItem
import com.metrolist.music.db.entities.Playlist
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.db.entities.Song
import com.metrolist.music.constants.BaajaPickedArtistsKey
import com.metrolist.music.ui.screens.onboarding.decodeArtists
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.clickable
import com.metrolist.music.ui.theme.baajaOutline
import com.metrolist.music.ui.component.home.BaajaMixHero
import com.metrolist.music.ui.component.home.BaajaArtistMixTile
import com.metrolist.music.ui.component.home.BaajaTasteSongCard
import com.metrolist.music.ui.component.BaajaTileColors
import com.metrolist.music.ui.screens.onboarding.BaajaArtist
import com.metrolist.music.ui.component.home.TasteCard
import com.metrolist.music.ui.component.home.TasteMix
import com.metrolist.music.ui.component.home.TasteShelf
import com.metrolist.music.ui.component.home.TasteStage
import com.metrolist.music.ui.component.home.tasteHome
import androidx.compose.foundation.lazy.LazyListScope
import java.time.LocalDateTime
import com.metrolist.music.ui.component.home.BaajaCommunityCard
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.playback.queues.LocalAlbumRadio
import com.metrolist.music.playback.queues.YouTubeAlbumRadio
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.component.AlbumGridItem
import com.metrolist.music.ui.component.ArtistGridItem
import com.metrolist.music.ui.component.LocalBottomSheetPageState
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.RandomizeGridItem
import com.metrolist.music.ui.component.SongGridItem
import com.metrolist.music.ui.component.SpeedDialGridItem
import com.metrolist.music.ui.component.YouTubeGridItem
import com.metrolist.music.ui.component.shimmer.GridItemPlaceHolder
import com.metrolist.music.ui.component.shimmer.ShimmerHost
import com.metrolist.music.ui.component.shimmer.TextPlaceholder
import com.metrolist.music.ui.menu.AlbumMenu
import com.metrolist.music.ui.menu.ArtistMenu
import com.metrolist.music.ui.menu.SongMenu
import com.metrolist.music.ui.menu.YouTubeAlbumMenu
import com.metrolist.music.ui.menu.YouTubeArtistMenu
import com.metrolist.music.ui.menu.YouTubePlaylistMenu
import com.metrolist.music.ui.menu.YouTubeSongMenu
import com.metrolist.music.ui.utils.SnapLayoutInfoProvider
import com.metrolist.music.ui.utils.resize
import com.metrolist.music.utils.joinToArtistString
import com.metrolist.music.utils.ArtistNameAliases
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.CommunityPlaylistItem
import com.metrolist.music.viewmodels.HomeViewModel
import com.metrolist.music.ui.component.home.BaajaArtistAvatar
import com.metrolist.music.ui.theme.BaajaEntranceBox
import com.metrolist.music.ui.theme.rememberBaajaEntranceKey
import com.metrolist.music.ui.component.home.BaajaArtworkBox
import com.metrolist.music.ui.component.home.BaajaChipsRow
import com.metrolist.music.ui.component.home.BaajaFeaturedCard
import com.metrolist.music.ui.component.home.BaajaHomeGutter
import com.metrolist.music.ui.component.home.BaajaHomeSectionTitle
import com.metrolist.music.ui.component.home.BaajaMoreButton
import com.metrolist.music.ui.component.home.BaajaPageIndicator
import com.metrolist.music.ui.component.home.BaajaSongRow
import com.metrolist.music.ui.component.home.BaajaYouTubeRow
import com.metrolist.music.ui.theme.BaajaCard
import com.metrolist.music.ui.theme.BaajaIconButton
import com.metrolist.music.ui.theme.BaajaPillButton
import com.metrolist.music.ui.theme.baajaInk
import com.metrolist.music.ui.theme.baajaSurface
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.random.Random

sealed class HomeSection(
    val id: String,
    val baseWeight: Int,
) {
    data object SpeedDial : HomeSection("speed_dial", 100)

    data object QuickPicks : HomeSection("quick_picks", 90)

    data object DailyDiscover : HomeSection("daily_discover", 80)

    data object KeepListening : HomeSection("keep_listening", 50)

    data object AccountPlaylists : HomeSection("account_playlists", 40)

    data object ForgottenFavorites : HomeSection("forgotten_favorites", 30)

    data object FromTheCommunity : HomeSection("from_the_community", 20)

    data class SimilarRecommendation(
        val index: Int,
    ) : HomeSection("similar_recommendation_$index", 10)

    data class HomePageSection(
        val index: Int,
    ) : HomeSection("home_page_section_$index", 10)

    data object MoodAndGenres : HomeSection("mood_and_genres", 5)
}

@Composable
fun CommunityPlaylistCard(
    item: CommunityPlaylistItem,
    onClick: () -> Unit,
    onSongClick: (SongItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current
    val listenTogetherManager = LocalListenTogetherManager.current
    val isListenTogetherGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false
    val scope = rememberCoroutineScope()
    val artistNameAliases = LocalArtistNameAliases.current
    val ink = baajaInk

    val dbPlaylist by database.playlistByBrowseId(item.playlist.id).collectAsStateWithLifecycle(initialValue = null)
    val isBookmarked = dbPlaylist?.playlist?.bookmarkedAt != null

    BaajaCard(
        modifier =
            modifier
                .width(320.dp)
                .height(420.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 2x2 grid of the first song thumbnails
                Box(
                    modifier =
                        Modifier
                            .size(100.dp)
                            .baajaSurface(ink, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        for (rowIndex in 0..1) {
                            Row(modifier = Modifier.weight(1f)) {
                                for (colIndex in 0..1) {
                                    AsyncImage(
                                        model =
                                            item.songs
                                                .getOrNull(rowIndex * 2 + colIndex)
                                                ?.thumbnail
                                                ?.resize(200, 200),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier =
                                            Modifier
                                                .weight(1f)
                                                .fillMaxSize(),
                                    )
                                }
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f).height(100.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = item.playlist.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text =
                            item.playlist.author?.let {
                                ArtistNameAliases.resolve(artistNameAliases, it.id, it.name)
                            }.orEmpty(),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 12.dp),
            ) {
                item.songs.take(3).forEach { song ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .combinedClickable(onClick = { onSongClick(song) })
                                .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        BaajaArtworkBox(
                            thumbnailUrl = song.thumbnail,
                            shape = RoundedCornerShape(12.dp),
                            shadowOffset = 2.dp,
                            modifier = Modifier.size(52.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = song.artists.joinToArtistString(" ${stringResource(R.string.and)} ") {
                                    ArtistNameAliases.resolve(artistNameAliases, it.id, it.name)
                                },
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            ) {
                BaajaIconButton(
                    icon = R.drawable.play,
                    contentDescription = stringResource(R.string.baaja_home_play_named, item.playlist.title),
                    filled = true,
                    size = 48.dp,
                    onClick = {
                        if (!isListenTogetherGuest) {
                            item.playlist.playEndpoint?.let {
                                playerConnection?.playQueue(YouTubeQueue(it))
                            }
                        }
                    },
                )

                BaajaIconButton(
                    icon = R.drawable.radio,
                    contentDescription = stringResource(R.string.start_radio),
                    size = 48.dp,
                    onClick = {
                        if (!isListenTogetherGuest) {
                            item.playlist.radioEndpoint?.let {
                                playerConnection?.playQueue(YouTubeQueue(it))
                            }
                        }
                    },
                )

                BaajaIconButton(
                    icon = if (isBookmarked) R.drawable.library_add_check else R.drawable.library_add,
                    contentDescription =
                        stringResource(if (isBookmarked) R.string.remove_from_library else R.string.add_to_library),
                    size = 48.dp,
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            if (dbPlaylist?.playlist == null) {
                                val playlistEntity =
                                    PlaylistEntity(
                                        name = item.playlist.title,
                                        browseId = item.playlist.id,
                                        thumbnailUrl = item.playlist.thumbnail,
                                        remoteSongCount =
                                            item.playlist.songCountText
                                                ?.split(" ")
                                                ?.firstOrNull()
                                                ?.toIntOrNull(),
                                        playEndpointParams = item.playlist.playEndpoint?.params,
                                        shuffleEndpointParams = item.playlist.shuffleEndpoint?.params,
                                        radioEndpointParams = item.playlist.radioEndpoint?.params,
                                    ).toggleLike()
                                val songMetadata =
                                    item.songs
                                        .ifEmpty {
                                            YouTube
                                                .playlist(item.playlist.id)
                                                .completed()
                                                .getOrNull()
                                                ?.songs
                                                .orEmpty()
                                        }.map { it.toMediaMetadata() }
                                if (songMetadata.isNotEmpty()) {
                                    database.withTransaction {
                                        insert(playlistEntity)
                                        songMetadata.onEach { insert(it) }
                                        val songIds = songMetadata.map { it.id to it.setVideoId }
                                        val createdPlaylist = database.playlistBlocking(playlistEntity.id)
                                        if (createdPlaylist != null) {
                                            addSongsToPlaylist(createdPlaylist, songIds)
                                        }
                                    }
                                }
                            } else {
                                database.transaction {
                                    val currentPlaylist = dbPlaylist!!.playlist
                                    update(currentPlaylist.toggleLike())
                                }
                            }
                        }
                    },
                )
            }
        }
    }
}

/** One page of Home's "Mixed for you" carousel, built from a Daily Discover recommendation. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DailyDiscoverCard(
    dailyDiscover: com.metrolist.music.viewmodels.DailyDiscoverItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
) {
    val database = LocalDatabase.current
    val playCount by database.getLifetimePlayCount(dailyDiscover.recommendation.id).collectAsStateWithLifecycle(initialValue = 0)
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val artistNameAliases = LocalArtistNameAliases.current

    val song = dailyDiscover.recommendation as? SongItem
    val playsString = stringResource(R.string.plays)
    val andString = " ${stringResource(R.string.and)} "

    val messages =
        listOf(
            R.string.daily_discover_sounds_like,
            R.string.daily_discover_because_you_listen_to,
            R.string.daily_discover_similar_to,
            R.string.daily_discover_based_on,
            R.string.daily_discover_for_fans_of,
        )
    val messageRes =
        remember(dailyDiscover.seed.id) {
            messages[kotlin.math.abs(dailyDiscover.seed.id.hashCode()) % messages.size]
        }
    val seedArtists =
        dailyDiscover.seed.artists.joinToArtistString(andString) {
            ArtistNameAliases.resolve(artistNameAliases, it.id, it.name)
        }

    BaajaFeaturedCard(
        thumbnailUrl = dailyDiscover.recommendation.thumbnail,
        title = dailyDiscover.recommendation.title,
        subtitle =
            buildString {
                append(
                    song?.artists?.joinToArtistString(andString) {
                        ArtistNameAliases.resolve(artistNameAliases, it.id, it.name)
                    }.orEmpty(),
                )
                if (playCount > 0) {
                    append(" | $playCount $playsString")
                }
            },
        caption = stringResource(messageRes, "${dailyDiscover.seed.title} • $seedArtists"),
        onClick = onClick,
        onPlay = onClick,
        isActive = isActive,
        isPlaying = isPlaying,
        onLongClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            if (song != null) {
                menuState.show {
                    YouTubeSongMenu(
                        song = song,
                        onDismiss = { menuState.dismiss() },
                    )
                }
            }
        },
        modifier = modifier,
    )
}

/** An entry in Home's "Popular artists" row, from local play history or the YouTube home feed. */
private data class PopularArtistEntry(
    val id: String,
    val name: String,
    val thumbnailUrl: String?,
    val local: Artist? = null,
    val remote: ArtistItem? = null,
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    snackbarHostState: SnackbarHostState,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val navController = LocalNavController.current
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current
    val listenTogetherManager = LocalListenTogetherManager.current
    val isListenTogetherGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false

    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()

    val quickPicks by viewModel.quickPicks.collectAsStateWithLifecycle()
    val forgottenFavorites by viewModel.forgottenFavorites.collectAsStateWithLifecycle()
    val keepListening by viewModel.keepListening.collectAsStateWithLifecycle()
    val similarRecommendations by viewModel.similarRecommendations.collectAsStateWithLifecycle()
    val accountPlaylists by viewModel.accountPlaylists.collectAsStateWithLifecycle()
    val homePage by viewModel.homePage.collectAsStateWithLifecycle()
    val explorePage by viewModel.explorePage.collectAsStateWithLifecycle()
    val dailyDiscover by viewModel.dailyDiscover.collectAsStateWithLifecycle()
    val communityPlaylists by viewModel.communityPlaylists.collectAsStateWithLifecycle()
    val popularArtists by viewModel.popularArtists.collectAsStateWithLifecycle()
    val artistNameAliases = LocalArtistNameAliases.current

    val allLocalItems by viewModel.allLocalItems.collectAsStateWithLifecycle()
    val allYtItems by viewModel.allYtItems.collectAsStateWithLifecycle()
    val speedDialItems by viewModel.speedDialItems.collectAsStateWithLifecycle()
    val pinnedSpeedDialItems by viewModel.pinnedSpeedDialItems.collectAsStateWithLifecycle()
    val selectedChip by viewModel.selectedChip.collectAsStateWithLifecycle()

    // Official podcast API data
    val savedPodcastShows by viewModel.savedPodcastShows.collectAsStateWithLifecycle()
    val episodesForLater by viewModel.episodesForLater.collectAsStateWithLifecycle()

    val isLoading: Boolean by viewModel.isLoading.collectAsStateWithLifecycle()
    val isMoodAndGenresLoading = isLoading && explorePage?.moodAndGenres == null
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isRandomizing by viewModel.isRandomizing.collectAsStateWithLifecycle()
    val pullRefreshState = rememberPullToRefreshState()

    val quickPicksLazyGridState = rememberLazyGridState()
    val forgottenFavoritesLazyGridState = rememberLazyGridState()

    val accountName by viewModel.accountName.collectAsStateWithLifecycle()
    val accountImageUrl by viewModel.accountImageUrl.collectAsStateWithLifecycle()
    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val autoRadioQueue by rememberPreference(AutoRadioQueueKey, defaultValue = true)

    LaunchedEffect(Unit) { viewModel.loadHomeData() }

    val isLoggedIn =
        remember(innerTubeCookie) {
            "SAPISID" in parseCookieString(innerTubeCookie)
        }
    val url = if (isLoggedIn) accountImageUrl else null

    // Extract unique podcasts from episodes for "Podcast Channels" row
    // Cache the podcasts to prevent them from disappearing during refresh
    var cachedPodcasts by remember { mutableStateOf<List<PodcastItem>>(emptyList()) }

    val featuredPodcasts =
        remember(homePage, selectedChip) {
            if (selectedChip == null) {
                cachedPodcasts = emptyList()
                emptyList()
            } else {
                val newPodcasts =
                    homePage
                        ?.sections
                        ?.flatMap { it.items }
                        ?.filterIsInstance<EpisodeItem>()
                        ?.mapNotNull { episode ->
                            episode.podcast?.let { podcast ->
                                PodcastItem(
                                    id = podcast.id,
                                    title = podcast.name,
                                    author = episode.author,
                                    episodeCountText = null,
                                    thumbnail = episode.thumbnail,
                                    playEndpoint = null,
                                    shuffleEndpoint = null,
                                )
                            }
                        }?.distinctBy { it.id }
                        ?.shuffled()
                        ?.take(10)
                        ?: emptyList()

                // Only update cache if we got valid data; keep old data during refresh
                if (newPodcasts.isNotEmpty()) {
                    cachedPodcasts = newPodcasts
                }
                cachedPodcasts
            }
        }

    val scope = rememberCoroutineScope()
    // Track randomization job
    var randomizeJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    val lazylistState = rememberLazyListState()
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)
    val currentGridHeight = if (gridItemSize == GridItemSize.BIG) GridThumbnailHeight else SmallGridThumbnailHeight
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsStateWithLifecycle()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            lazylistState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow {
            lazylistState.layoutInfo.visibleItemsInfo
                .lastOrNull()
                ?.index
        }.collect { lastVisibleIndex ->
            val len = lazylistState.layoutInfo.totalItemsCount
            if (lastVisibleIndex != null && lastVisibleIndex >= len - 3) {
                viewModel.loadMoreYouTubeItems(homePage?.continuation)
            }
        }
    }

    if (selectedChip != null) {
        BackHandler {
            // if a chip is selected, go back to the normal homepage first
            viewModel.toggleChip(selectedChip)
        }
    }

    val localGridItem: @Composable (LocalItem) -> Unit = {
        when (it) {
            is Song -> {
                SongGridItem(
                    song = it,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    if (!isListenTogetherGuest) {
                                        if (it.id == mediaMetadata?.id) {
                                            playerConnection.togglePlayPause()
                                        } else {
                                            playerConnection.playQueue(
                                                if (autoRadioQueue) {
                                                    YouTubeQueue.radio(it.toMediaMetadata())
                                                } else {
                                                    ListQueue(
                                                        title = it.title,
                                                        items = listOf(it.toMediaItem())
                                                    )
                                                }
                                            )
                                        }
                                    }
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(
                                        HapticFeedbackType.LongPress,
                                    )
                                    menuState.show {
                                        SongMenu(
                                            originalSong = it,
                                            onDismiss = menuState::dismiss,
                                        )
                                    }
                                },
                            ),
                    isActive = it.id == mediaMetadata?.id,
                    isPlaying = isPlaying,
                )
            }

            is Album -> {
                AlbumGridItem(
                    album = it,
                    isActive = it.id == mediaMetadata?.album?.id,
                    isPlaying = isPlaying,
                    coroutineScope = scope,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    navController.navigate("album/${it.id}")
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        AlbumMenu(
                                            originalAlbum = it,
                                            onDismiss = menuState::dismiss,
                                        )
                                    }
                                },
                            ),
                )
            }

            is Artist -> {
                ArtistGridItem(
                    artist = it,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    navController.navigate("artist/${it.id}")
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(
                                        HapticFeedbackType.LongPress,
                                    )
                                    menuState.show {
                                        ArtistMenu(
                                            originalArtist = it,
                                            coroutineScope = scope,
                                            onDismiss = menuState::dismiss,
                                        )
                                    }
                                },
                            ),
                )
            }

            is Playlist -> {}
        }
    }

    val ytGridItem: @Composable (YTItem) -> Unit = { item ->
        YouTubeGridItem(
            item = item,
            isActive = item.id in listOf(mediaMetadata?.album?.id, mediaMetadata?.id),
            isPlaying = isPlaying,
            coroutineScope = scope,
            thumbnailRatio = 1f,
            modifier =
                Modifier
                    .combinedClickable(
                        onClick = {
                            when (item) {
                                is SongItem -> {
                                    if (!isListenTogetherGuest) {
                                        playerConnection.playQueue(
                                            if (autoRadioQueue) {
                                                YouTubeQueue(
                                                    item.endpoint ?: WatchEndpoint(
                                                        videoId = item.id,
                                                    ),
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
                                    if (!isListenTogetherGuest) {
                                        playerConnection.playQueue(
                                            ListQueue(
                                                title = item.title,
                                                items = listOf(item.toMediaMetadata().toMediaItem()),
                                            ),
                                        )
                                    }
                                }
                            }
                        },
                        onLongClick = {
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
                                            coroutineScope = scope,
                                            onDismiss = menuState::dismiss,
                                        )
                                    }

                                    is PodcastItem -> {
                                        YouTubePlaylistMenu(
                                            playlist = item.asPlaylistItem(),
                                            coroutineScope = scope,
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
                        },
                    ),
        )
    }

    val homeSections =
        remember(
            selectedChip,
            speedDialItems,
            quickPicks,
            dailyDiscover,
            keepListening,
            accountPlaylists,
            forgottenFavorites,
            communityPlaylists,
            similarRecommendations,
            homePage?.sections,
            explorePage?.moodAndGenres,
        ) {
            val list = mutableListOf<HomeSection>()
            val chipActive = selectedChip != null

            if (!chipActive && speedDialItems.isNotEmpty()) list.add(HomeSection.SpeedDial)
            if (!chipActive && quickPicks?.isNotEmpty() == true) list.add(HomeSection.QuickPicks)
            if (!chipActive && communityPlaylists?.isNotEmpty() == true) list.add(HomeSection.FromTheCommunity)
            if (!chipActive && keepListening?.isNotEmpty() == true) list.add(HomeSection.KeepListening)
            if (!chipActive && accountPlaylists?.isNotEmpty() == true) list.add(HomeSection.AccountPlaylists)
            if (!chipActive && forgottenFavorites?.isNotEmpty() == true) list.add(HomeSection.ForgottenFavorites)

            if (!chipActive) {
                similarRecommendations?.indices?.forEach { i ->
                    list.add(HomeSection.SimilarRecommendation(i))
                }
            }

            homePage?.sections?.indices?.forEach { i ->
                list.add(HomeSection.HomePageSection(i))
            }

            if (explorePage?.moodAndGenres != null) list.add(HomeSection.MoodAndGenres)

            val defaultOrder =
                mapOf(
                    HomeSection.SpeedDial to 100,
                    HomeSection.QuickPicks to 90,
                    HomeSection.FromTheCommunity to 80,
                    HomeSection.DailyDiscover to 70,
                    HomeSection.KeepListening to 60,
                    HomeSection.AccountPlaylists to 50,
                    HomeSection.ForgottenFavorites to 40,
                    HomeSection.MoodAndGenres to 10,
                )

            list.sortedByDescending { section ->
                when (section) {
                    is HomeSection.SimilarRecommendation -> 30 - section.index
                    is HomeSection.HomePageSection -> 20 - section.index
                    else -> defaultOrder[section] ?: 0
                }
            }
        }

    val (pickedArtistsRaw) = rememberPreference(BaajaPickedArtistsKey, "")
    val pickedArtists = remember(pickedArtistsRaw) { decodeArtists(pickedArtistsRaw) }
    // History decides how much of Home is personalised by picks (see TasteStage).
    val historySongs by remember(database) {
        database.mostPlayedSongs(fromTimeStamp = LocalDateTime.now().minusWeeks(8), limit = 24)
    }.collectAsStateWithLifecycle(emptyList())
    // Signed in with Google: the account's own YouTube Music history, liked songs and followed artists
    // shape Home from the very first launch, before Baaja has recorded any plays.
    var accountSongs by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    val likedLocal by remember(database) { database.likedSongsByCreateDateAsc() }.collectAsStateWithLifecycle(emptyList())
    val bookmarkedArtists by remember(database) { database.artistsBookmarkedByCreateDateAsc() }.collectAsStateWithLifecycle(emptyList())
    LaunchedEffect(isLoggedIn) {
        accountSongs =
            if (isLoggedIn) {
                YouTube.musicHistory().getOrNull()?.sections.orEmpty().flatMap { it.songs }.distinctBy { it.id }.take(40)
            } else {
                emptyList()
            }
    }
    // "Similar to your likes": the radio YouTube Music builds from your two most recent liked songs.
    var likeRadios by remember { mutableStateOf<List<Pair<String, List<SongItem>>>>(emptyList()) }
    val recentLikes = remember(likedLocal) { likedLocal.takeLast(12).reversed().distinctBy { it.artists.firstOrNull()?.id }.take(2) }
    LaunchedEffect(isLoggedIn, recentLikes.map { it.id }) {
        likeRadios =
            if (!isLoggedIn) {
                emptyList()
            } else {
                recentLikes
                    .map { liked ->
                        async {
                            val songs =
                                YouTube
                                    .next(WatchEndpoint(videoId = liked.id, playlistId = "RDAMVM" + liked.id))
                                    .getOrNull()
                                    ?.items
                                    .orEmpty()
                                    .filter { it.id != liked.id }
                                    .take(15)
                            liked.song.title to songs
                        }
                    }.awaitAll().filter { it.second.size >= 4 }
            }
    }
    val accountArtists =
        remember(isLoggedIn, accountSongs, likedLocal, bookmarkedArtists) {
            if (!isLoggedIn) {
                emptyList()
            } else {
                val scores = LinkedHashMap<String, Pair<BaajaArtist, Int>>()

                fun add(id: String?, name: String, thumb: String?, weight: Int) {
                    if (id.isNullOrBlank() || name.isBlank()) return
                    val current = scores[id]
                    scores[id] = (current?.first ?: BaajaArtist(id, name, thumb)) to ((current?.second ?: 0) + weight)
                }
                accountSongs.forEach { song -> song.artists.forEach { add(it.id, it.name, null, 2) } }
                bookmarkedArtists.forEach { add(it.id, it.title, it.thumbnailUrl, 5) }
                likedLocal.forEachIndexed { index, song ->
                    song.artists.forEach { add(it.id, it.name, it.thumbnailUrl, if (index >= likedLocal.size - 30) 4 else 2) }
                }
                scores.values.sortedByDescending { it.second }.map { it.first }.take(8)
            }
        }
    val historyArtists =
        remember(popularArtists, accountArtists) {
            (accountArtists + popularArtists.orEmpty().map { BaajaArtist(it.id, it.title, it.thumbnailUrl) })
                .distinctBy { it.id }
                .take(6)
        }
    val tasteStage =
        when {
            accountArtists.size >= 3 -> TasteStage.HISTORY
            historySongs.size >= 10 -> TasteStage.HISTORY
            historySongs.isNotEmpty() -> TasteStage.MIXED
            else -> TasteStage.PICKS
        }
    val tasteArtists =
        remember(tasteStage, historyArtists, pickedArtists, accountArtists) {
            when (tasteStage) {
                TasteStage.PICKS -> pickedArtists
                TasteStage.MIXED ->
                    buildList {
                        val picks = pickedArtists
                        for (i in 0 until maxOf(historyArtists.size, picks.size)) {
                            picks.getOrNull(i)?.let(::add)
                            historyArtists.getOrNull(i)?.let(::add)
                        }
                    }.distinctBy { it.id }
                TasteStage.HISTORY -> (historyArtists + pickedArtists).distinctBy { it.id }
            }.take(8)
        }
    var tasteSongs by remember { mutableStateOf<Map<String, List<SongItem>>>(emptyMap()) }
    var tasteAlbums by remember { mutableStateOf<Map<String, List<AlbumItem>>>(emptyMap()) }
    // Artists that come from listening history often have no saved photo, so use the one on their artist page.
    var tasteThumbs by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    LaunchedEffect(tasteArtists.map { it.id }) {
        val fetched =
            tasteArtists
                .take(8)
                .map { artist ->
                    async {
                        val page = YouTube.artist(artist.id).getOrNull()
                        // The artist page only lists a handful of top songs; "See all" has the full list.
                        val topSection = page?.sections?.firstOrNull { section -> section.items.any { it is SongItem } }
                        val topSongs = topSection?.items?.filterIsInstance<SongItem>().orEmpty()
                        val moreSongs =
                            topSection
                                ?.moreEndpoint
                                ?.let { YouTube.artistItems(it).getOrNull() }
                                ?.items
                                ?.filterIsInstance<SongItem>()
                                .orEmpty()
                        val songs = (topSongs + moreSongs).distinctBy { it.id }.take(40)
                        val albums =
                            page
                                ?.sections
                                ?.firstOrNull { section -> section.items.any { it is AlbumItem } }
                                ?.items
                                ?.filterIsInstance<AlbumItem>()
                                ?.take(4)
                                .orEmpty()
                        page?.artist?.thumbnail?.let { thumb -> tasteThumbs = tasteThumbs + (artist.id to thumb) }
                        Triple(artist.id, songs, albums)
                    }
                }.awaitAll()
        tasteSongs = fetched.filter { it.second.isNotEmpty() }.associate { it.first to it.second }
        tasteAlbums = fetched.filter { it.third.isNotEmpty() }.associate { it.first to it.third }
    }
    val mixArtists =
        remember(tasteArtists, tasteSongs, tasteThumbs) {
            tasteArtists
                .filter { tasteSongs.containsKey(it.id) }
                .map { artist -> if (artist.thumbnail.isNullOrBlank()) artist.copy(thumbnail = tasteThumbs[artist.id]) else artist }
        }
    val tasteMix =
        remember(mixArtists, tasteSongs) {
            val lists = mixArtists.mapNotNull { tasteSongs[it.id] }
            val mixed = mutableListOf<SongItem>()
            for (i in 0 until 20) lists.forEach { list -> list.getOrNull(i)?.let(mixed::add) }
            mixed.distinctBy { it.id }.take(100)
        }
    val tasteActive = pickedArtists.isNotEmpty() || historySongs.isNotEmpty() || accountArtists.isNotEmpty()

    val tasteUi: (LazyListScope.() -> Unit)? =
        if (tasteMix.isNotEmpty()) {
        val fromPicks = stringResource(R.string.baaja_home_tag_picks)
        val fromHistory = stringResource(R.string.baaja_home_tag_history)
        val newText = stringResource(R.string.baaja_home_new)
        val historyIds = historyArtists.map { it.id }.toSet()
        val pickedIds = pickedArtists.map { it.id }.toSet()
        fun playSongs(title: String, songs: List<SongItem>, start: Int = 0) {
            playerConnection?.playQueue(
                ListQueue(title = title, items = songs.map { it.toMediaItem() }, startIndex = start),
            )
        }
        fun songCards(songs: List<SongItem>) =
            songs.map { TasteCard("s_${it.id}", it.title, it.artists.joinToString { a -> a.name }, it.thumbnail) }
        val accountShelf =
            accountSongs.takeIf { it.isNotEmpty() }?.let { songs ->
                TasteShelf(
                    title = stringResource(R.string.baaja_home_jump_back_in),
                    label = null,
                    cards = songs.take(12).map { TasteCard("a_${it.id}", it.title, it.artists.joinToString { a -> a.name }, it.thumbnail) },
                    onClick = { index -> playSongs("History", songs.take(12), index) },
                )
            }
        val historyShelf =
            if (historySongs.isEmpty()) accountShelf else historySongs.takeIf { it.isNotEmpty() }?.let { songs ->
                TasteShelf(
                    title = stringResource(if (tasteStage == TasteStage.HISTORY) R.string.baaja_home_jump_back_in else R.string.baaja_home_listen_again),
                    label = null,
                    cards = songs.take(12).map { TasteCard("h_${it.id}", it.title, it.artists.joinToString { a -> a.name }, it.song.thumbnailUrl) },
                    onClick = { index ->
                        playerConnection?.playQueue(
                            ListQueue(title = "History", items = songs.take(12).map { it.toMediaItem() }, startIndex = index),
                        )
                    },
                )
            }
        val topHistory = historyArtists.firstOrNull { tasteSongs.containsKey(it.id) }
        val becauseHistory =
            topHistory?.let { artist ->
                val songs = tasteSongs[artist.id].orEmpty()
                TasteShelf(
                    title = stringResource(R.string.baaja_home_because_listened, artist.name),
                    label = null,
                    cards = songCards(songs),
                    onClick = { index -> playSongs(artist.name, songs, index) },
                )
            }
        val becausePicks =
            pickedArtists.filter { tasteSongs.containsKey(it.id) }.take(2).map { artist ->
                val songs = tasteSongs[artist.id].orEmpty()
                TasteShelf(
                    title = stringResource(R.string.baaja_home_because, artist.name),
                    label = null,
                    cards = songCards(songs),
                    onClick = { index -> playSongs(artist.name, songs, index) },
                )
            }
        val releaseAlbums = historyArtists.flatMap { tasteAlbums[it.id].orEmpty().take(1) }.take(6)
        val releasesShelf =
            releaseAlbums.takeIf { it.isNotEmpty() }?.let { albums ->
                TasteShelf(
                    title = stringResource(R.string.baaja_home_new_from_artists),
                    label = null,
                    cards = albums.map { TasteCard("a_${it.id}", it.title, it.artists?.joinToString { a -> a.name }.orEmpty(), it.thumbnail, newText) },
                    onClick = { index -> navController.navigate("album/${albums[index].id}") },
                )
            }
        val discoverSongs =
            pickedArtists
                .filter { it.id !in historyIds }
                .flatMap { tasteSongs[it.id].orEmpty().take(3) }
                .distinctBy { it.id }
        val discoverShelf =
            discoverSongs.takeIf { it.isNotEmpty() }?.let { songs ->
                TasteShelf(
                    title = stringResource(R.string.baaja_home_discover),
                    label = null,
                    cards = songCards(songs),
                    onClick = { index -> playSongs("Discover", songs, index) },
                )
            }
        val mixTiles =
            mixArtists.take(4).map { artist ->
                val isNew = tasteStage != TasteStage.PICKS && artist.id in historyIds && artist.id !in pickedIds
                TasteMix(
                    artist = artist,
                    badge = if (isNew) newText else null,
                    tagline = stringResource(if (isNew) R.string.baaja_home_mix_tagline_history else R.string.baaja_home_mix_tagline),
                    onClick = { playSongs(artist.name, tasteSongs[artist.id].orEmpty()) },
                )
            }
        val tasteTints = listOf(BaajaTileColors.lilac(), BaajaTileColors.peach(), BaajaTileColors.liked(), BaajaTileColors.mint())
        val tasteBlock: LazyListScope.() -> Unit = {
        tasteHome(
            stage = tasteStage,
            heroArtists = mixArtists,
            onPlayMix = { playSongs("Your mix", tasteMix) },
            mixes = mixTiles,
            mixesLabel = null,
            historyShelf = historyShelf,
            releasesShelf = releasesShelf,
            becauseHistory = becauseHistory,
            becausePicks = becausePicks,
            discoverShelf = discoverShelf,
            tints = tasteTints,
        )
        }
        tasteBlock

        } else {
            null
        }


    val popularArtistEntries =
        remember(popularArtists, homePage?.sections, pickedArtists, tasteThumbs) {
            pickedArtists.map { PopularArtistEntry(it.id, it.name, it.thumbnail) } + (popularArtists
                ?.takeIf { it.isNotEmpty() }
                ?.map { PopularArtistEntry(it.id, it.title, it.thumbnailUrl, local = it) }
                ?: homePage
                    ?.sections
                    ?.flatMap { it.items }
                    ?.filterIsInstance<ArtistItem>()
                    ?.distinctBy { it.id }
                    ?.take(12)
                    ?.map { PopularArtistEntry(it.id, it.title, it.thumbnail, remote = it) }
                    .orEmpty())
                .distinctBy { it.id }
                .map { entry -> if (entry.thumbnailUrl.isNullOrBlank()) entry.copy(thumbnailUrl = tasteThumbs[entry.id]) else entry }
        }

    LaunchedEffect(quickPicks) {
        quickPicksLazyGridState.scrollToItem(0)
    }

    LaunchedEffect(forgottenFavorites) {
        forgottenFavoritesLazyGridState.scrollToItem(0)
    }

    PullToRefreshBox(
        state = pullRefreshState,
        isRefreshing = isRefreshing,
        onRefresh = viewModel::refresh,
        indicator = {
            Indicator(
                isRefreshing = isRefreshing,
                state = pullRefreshState,
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()),
            )
        },
    ) {
            val onLuckyShuffle: () -> Unit = {
            if (!isListenTogetherGuest) {
                // Only songs and albums can be started from a local item.
                val luckyLocalItems = allLocalItems.filter { it is Song || it is Album }
                val local =
                    when {
                        luckyLocalItems.isNotEmpty() && allYtItems.isNotEmpty() -> Random.nextFloat() < 0.5
                        luckyLocalItems.isNotEmpty() -> true
                        else -> false
                    }
                scope.launch(Dispatchers.Main) {
                    if (local) {
                        when (val luckyItem = luckyLocalItems.random()) {
                            is Song -> {
                                playerConnection.playQueue(YouTubeQueue.radio(luckyItem.toMediaMetadata()))
                            }

                            is Album -> {
                                val albumWithSongs =
                                    withContext(Dispatchers.IO) {
                                        database.albumWithSongs(luckyItem.id).first()
                                    }
                                albumWithSongs?.let {
                                    playerConnection.playQueue(LocalAlbumRadio(it))
                                }
                            }

                            else -> Unit
                        }
                    } else {
                        when (val luckyItem = allYtItems.random()) {
                            is SongItem -> {
                                playerConnection.playQueue(YouTubeQueue.radio(luckyItem.toMediaMetadata()))
                            }

                            is AlbumItem -> {
                                playerConnection.playQueue(YouTubeAlbumRadio(luckyItem.playlistId))
                            }

                            is ArtistItem -> {
                                luckyItem.radioEndpoint?.let {
                                    playerConnection.playQueue(YouTubeQueue(it))
                                }
                            }

                            is PlaylistItem -> {
                                luckyItem.playEndpoint?.let {
                                    playerConnection.playQueue(YouTubeQueue(it))
                                }
                            }

                            is PodcastItem -> {
                                luckyItem.playEndpoint?.let {
                                    playerConnection.playQueue(YouTubeQueue(it))
                                }
                            }

                            is EpisodeItem -> {
                                playerConnection.playQueue(
                                    ListQueue(
                                        title = luckyItem.title,
                                        items = listOf(luckyItem.toMediaMetadata().toMediaItem()),
                                    ),
                                )
                            }
                        }
                    }
                }
            }
    }

        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopStart,
        ) {
            val horizontalLazyGridItemWidthFactor = if (maxWidth * 0.475f >= 320.dp) 0.475f else 0.9f
            val horizontalLazyGridItemWidth = maxWidth * horizontalLazyGridItemWidthFactor
            val quickPicksSnapLayoutInfoProvider =
                remember(quickPicksLazyGridState) {
                    SnapLayoutInfoProvider(
                        lazyGridState = quickPicksLazyGridState,
                        positionInLayout = { layoutSize, itemSize ->
                            (layoutSize * horizontalLazyGridItemWidthFactor / 2f - itemSize / 2f)
                        },
                    )
                }
            val forgottenFavoritesSnapLayoutInfoProvider =
                remember(forgottenFavoritesLazyGridState) {
                    SnapLayoutInfoProvider(
                        lazyGridState = forgottenFavoritesLazyGridState,
                        positionInLayout = { layoutSize, itemSize ->
                            (layoutSize * horizontalLazyGridItemWidthFactor / 2f - itemSize / 2f)
                        },
                    )
                }

            val entranceKey = rememberBaajaEntranceKey()
            LazyColumn(
                state = lazylistState,
                contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
            ) {
                item(key = "home_chips") {
 BaajaEntranceBox(0, entranceKey) {
                    BaajaChipsRow(
                        chips = homePage?.chips?.filterNot { it.title.contains("Podcast", ignoreCase = true) }?.map { it to it.title } ?: emptyList(),
                        currentValue = selectedChip,
                        onValueUpdate = {
                            viewModel.toggleChip(it)
                        },
                    )
                }
}

                if (isLoading && homePage?.chips.isNullOrEmpty()) {
                    item(key = "chips_shimmer") {
                        ShimmerHost(showGradient = false) {
                            LazyRow(
                                contentPadding =
                                    WindowInsets.systemBars
                                        .only(WindowInsetsSides.Horizontal)
                                        .asPaddingValues(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            ) {
                                items(5) {
                                    TextPlaceholder(
                                        height = 40.dp,
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier.width(72.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                // Show podcast sections FIRST when podcast chip is selected (fixed at top)
                if (selectedChip?.title?.contains("Podcast", ignoreCase = true) == true) {
                    // Show "Your Shows" section from official API
                    if (savedPodcastShows.isNotEmpty()) {
                        item(key = "00_your_shows_title") {
                            BaajaHomeSectionTitle(
                                title = stringResource(R.string.your_shows),
                                onClick = {
                                    navController.navigate("youtube_browse/FEmusic_library_non_music_audio_list")
                                },
                            )
                        }

                        item(key = "00_your_shows_list") {
                            LazyRow(
                                contentPadding =
                                    WindowInsets.systemBars
                                        .only(WindowInsetsSides.Horizontal)
                                        .asPaddingValues(),
                            ) {
                                items(savedPodcastShows.distinctBy { it.id }, key = { "home_saved_podcast_${it.id}" }) { podcast ->
                                    ytGridItem(podcast)
                                }
                            }
                        }
                    }

                    // Show "Episodes for Later" section from official API
                    if (episodesForLater.isNotEmpty()) {
                        item(key = "00_episodes_for_later_title") {
                            BaajaHomeSectionTitle(
                                title = stringResource(R.string.episodes_for_later),
                                onClick = {
                                    navController.navigate("online_playlist/SE")
                                },
                            )
                        }

                        item(key = "00_episodes_for_later_list") {
                            LazyRow(
                                contentPadding =
                                    WindowInsets.systemBars
                                        .only(WindowInsetsSides.Horizontal)
                                        .asPaddingValues(),
                            ) {
                                items(episodesForLater.distinctBy { it.id }, key = { "home_episode_later_${it.id}" }) { episode ->
                                    ytGridItem(episode)
                                }
                            }
                        }
                    }

                    // Show Podcast Channels row if we have any (extracted from episodes)
                    // Only show if "Your Shows" from official API is empty (to avoid duplicates)
                    if (featuredPodcasts.isNotEmpty() && savedPodcastShows.isEmpty()) {
                        item(key = "0_podcast_channels_title") {
                            BaajaHomeSectionTitle(
                                title = stringResource(R.string.podcast_channels),
                            )
                        }

                        item(key = "0_podcast_channels_list") {
                            LazyRow(
                                contentPadding =
                                    WindowInsets.systemBars
                                        .only(WindowInsetsSides.Horizontal)
                                        .asPaddingValues(),
                            ) {
                                items(featuredPodcasts.distinctBy { it.id }, key = { "home_featured_podcast_${it.id}" }) { podcast ->
                                    ytGridItem(podcast)
                                }
                            }
                        }
                    }

                    // Render the regular sections from the chip (episodes grouped by category)
                    // Use key prefix "1_" to ensure episodes sort after channels "0_"
                    // Skip sections that duplicate official API sections (Your Shows, Episodes for Later)
                    homeSections.filterIsInstance<HomeSection.HomePageSection>().forEach { section ->
                        val sectionData = homePage?.sections?.getOrNull(section.index)
                        // Skip if this section duplicates an official API section
                        val skipTitles = listOf("your shows", "episodes for later", "podcast channels", "new episodes")
                        if (sectionData?.title?.lowercase()?.let { title -> skipTitles.any { title.contains(it) } } == true) {
                            return@forEach
                        }
                        sectionData?.let {
                            item(key = "1_chip_section_title_${section.index}") {
                                BaajaHomeSectionTitle(
                                    title = sectionData.title,
                                    label = sectionData.label,
                                    thumbnail =
                                        sectionData.thumbnail?.let { thumbnailUrl ->
                                            {
                                                val shape =
                                                    if (sectionData.endpoint?.isArtistEndpoint == true) {
                                                        CircleShape
                                                    } else {
                                                        RoundedCornerShape(
                                                            ThumbnailCornerRadius,
                                                        )
                                                    }
                                                AsyncImage(
                                                    model = thumbnailUrl,
                                                    contentDescription = null,
                                                    modifier =
                                                        Modifier
                                                            .size(ListThumbnailSize)
                                                            .clip(shape),
                                                )
                                            }
                                        },
                                    onClick =
                                        sectionData.endpoint?.let { endpoint ->
                                            {
                                                when {
                                                    endpoint.browseId == "FEmusic_moods_and_genres" -> {
                                                        navController.navigate("mood_and_genres")
                                                    }

                                                    endpoint.params != null -> {
                                                        navController.navigate(
                                                            "youtube_browse/${endpoint.browseId}?params=${endpoint.params}",
                                                        )
                                                    }

                                                    else -> {
                                                        navController.navigate("browse/${endpoint.browseId}")
                                                    }
                                                }
                                            }
                                        },
                                )
                            }

                            item(key = "1_chip_section_list_${section.index}") {
                                LazyRow(
                                    contentPadding =
                                        WindowInsets.systemBars
                                            .only(WindowInsetsSides.Horizontal)
                                            .asPaddingValues(),
                                ) {
                                    items(sectionData.items.distinctBy { it.id }, key = { "home_chip_section_${it.id}" }) { item ->
                                        ytGridItem(item)
                                    }
                                }
                            }
                        }
                    }
                }

                if (selectedChip == null) {
                    dailyDiscover?.takeIf { it.isNotEmpty() }?.let { discoverList ->
                        item(key = "mixed_for_you_title") {
 BaajaEntranceBox(1, entranceKey) {
                            val title = stringResource(R.string.baaja_home_mixed_for_you)
                            BaajaHomeSectionTitle(
                                title = title,
                                actionLabel = stringResource(R.string.play_all),
                                onAction =
                                    if (!isListenTogetherGuest) {
                                        {
                                            val queueItems =
                                                discoverList.mapNotNull {
                                                    (it.recommendation as? SongItem)?.toMediaMetadata()
                                                }
                                            if (queueItems.isNotEmpty()) {
                                                playerConnection.playQueue(
                                                    ListQueue(
                                                        title = title,
                                                        items = queueItems.map { it.toMediaItem() },
                                                    ),
                                                )
                                            }
                                        }
                                    } else {
                                        null
                                    },
                            )
                        }
}

                        item(key = "mixed_for_you_content") {
 BaajaEntranceBox(2, entranceKey) {
                            val pagerState = rememberPagerState(pageCount = { discoverList.size })
                            Column(modifier = Modifier.fillMaxWidth()) {
                                HorizontalPager(
                                    state = pagerState,
                                    contentPadding = PaddingValues(horizontal = BaajaHomeGutter),
                                    pageSpacing = 14.dp,
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .height(292.dp),
                                ) { page ->
                                    val item = discoverList[page]
                                    DailyDiscoverCard(
                                        dailyDiscover = item,
                                        isActive = item.recommendation.id == mediaMetadata?.id,
                                        isPlaying = isPlaying,
                                        onClick = {
                                            if (!isListenTogetherGuest) {
                                                val song = item.recommendation as? SongItem
                                                val metadata = song?.toMediaMetadata()
                                                if (song != null && metadata != null) {
                                                    if (song.id == mediaMetadata?.id) {
                                                        playerConnection.togglePlayPause()
                                                    } else {
                                                        playerConnection.playQueue(
                                                            if (autoRadioQueue) {
                                                                YouTubeQueue(
                                                                    song.endpoint ?: WatchEndpoint(videoId = song.id),
                                                                    metadata,
                                                                )
                                                            } else {
                                                                ListQueue(
                                                                    title = song.title,
                                                                    items = listOf(song.toMediaItem()),
                                                                )
                                                            },
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        modifier =
                                            Modifier
                                                .graphicsLayer {
                                                    // Neighbouring cards shrink and fade a little, like a stack of stickers.
                                                    val distance =
                                                        kotlin.math
                                                            .abs((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                                                            .coerceAtMost(1f)
                                                    val scale = 1f - 0.06f * distance
                                                    scaleX = scale
                                                    scaleY = scale
                                                    alpha = 1f - 0.25f * distance
                                                }.padding(end = 4.dp, bottom = 10.dp),
                                    )
                                }
                                BaajaPageIndicator(
                                    pageCount = discoverList.size,
                                    currentPage = pagerState.currentPage,
                                    onPageClick = { page -> scope.launch { pagerState.animateScrollToPage(page) } },
                                )
                            }
                        }
}
                    }


                    if (popularArtistEntries.isNotEmpty()) {
                        item(key = "popular_artists_title") {
 BaajaEntranceBox(3, entranceKey) {
                            BaajaHomeSectionTitle(title = stringResource(if (tasteStage == TasteStage.HISTORY && tasteActive) R.string.baaja_home_your_top_artists else if (tasteActive) R.string.baaja_home_your_artists else R.string.baaja_home_popular_artists))
                        }
}
                        item(key = "popular_artists_list") {
 BaajaEntranceBox(4, entranceKey) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = BaajaHomeGutter - 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                items(popularArtistEntries, key = { "home_popular_artist_${it.id}" }) { entry ->
                                    BaajaArtistAvatar(
                                        name = ArtistNameAliases.resolve(artistNameAliases, entry.id, entry.name),
                                        thumbnailUrl = entry.thumbnailUrl,
                                        onClick = { navController.navigate("artist/${entry.id}") },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                when {
                                                    entry.local != null ->
                                                        ArtistMenu(
                                                            originalArtist = entry.local,
                                                            coroutineScope = scope,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    entry.remote != null ->
                                                        YouTubeArtistMenu(
                                                            artist = entry.remote,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                }
                                            }
                                        },
                                    )
                                }
                            }
                        }
}
                    }
                }

                var tasteShown = false
                homeSections.forEach { section ->
                    when (section) {
                        HomeSection.SpeedDial -> {
                            speedDialItems.takeIf { it.isNotEmpty() }?.let { items ->
                                item(key = "speed_dial_title") {
 BaajaEntranceBox(5, entranceKey) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        BaajaHomeSectionTitle(
                                            title = stringResource(R.string.speed_dial),
                                            modifier = Modifier.weight(1f),
                                        )
                                        if (allLocalItems.isNotEmpty() || allYtItems.isNotEmpty()) {
                                            BaajaIconButton(
                                                icon = R.drawable.shuffle,
                                                contentDescription = stringResource(R.string.baaja_sd_shuffle),
                                                onClick = onLuckyShuffle,
                                                size = 40.dp,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(end = 20.dp),
                                            )
                                        }
                                    }
                                }
}

                                item(key = "speed_dial_list") {
 BaajaEntranceBox(6, entranceKey) {
                                    val targetItemSize = 160.dp
                                    val availableWidth = maxWidth - 32.dp
                                    val columns = (availableWidth / targetItemSize).toInt().coerceAtLeast(3)
                                    val rows =
                                        if (columns >= 6) {
                                            1
                                        } else if (columns >= 4) {
                                            2
                                        } else {
                                            3
                                        }
                                    val itemsPerPage = columns * rows
                                    val itemWidth = availableWidth / columns

                                    val pagerState = rememberPagerState(pageCount = { (items.size + itemsPerPage - 1) / itemsPerPage })

                                    Column(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth(),
                                    ) {
                                        HorizontalPager(
                                            state = pagerState,
                                            contentPadding = PaddingValues(horizontal = 16.dp),
                                            pageSpacing = 16.dp,
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .height(itemWidth * rows),
                                        ) { page ->
                                            val pageStartIndex = page * itemsPerPage
                                            val pageItems = items.drop(pageStartIndex).take(itemsPerPage)

                                            Column(modifier = Modifier.fillMaxSize()) {
                                                for (row in 0 until rows) {
                                                    Row(modifier = Modifier.fillMaxWidth()) {
                                                        for (col in 0 until columns) {
                                                            val itemIndex = row * columns + col

                                                            val isRandomizeSlot = (page == 0 && itemIndex == itemsPerPage - 1)

                                                            if (isRandomizeSlot) {
                                                                Box(
                                                                    modifier =
                                                                        Modifier
                                                                            .width(itemWidth)
                                                                            .height(itemWidth)
                                                                            .padding(start = 2.dp, top = 2.dp),
                                                                ) {
                                                                    RandomizeGridItem(
                                                                        isLoading = isRandomizing,
                                                                        onClick = {
                                                                            if (isRandomizing) {
                                                                                randomizeJob?.cancel()
                                                                            } else if (!isListenTogetherGuest) {
                                                                                randomizeJob =
                                                                                    scope.launch {
                                                                                        val randomItem = viewModel.getRandomItem()
                                                                                        if (randomItem != null) {
                                                                                            when (randomItem) {
                                                                                                is SongItem -> {
                                                                                                    playerConnection.playQueue(
                                                                                                        if (autoRadioQueue) {
                                                                                                            YouTubeQueue(
                                                                                                                randomItem.endpoint
                                                                                                                    ?: WatchEndpoint(
                                                                                                                        videoId = randomItem.id,
                                                                                                                    ),
                                                                                                                randomItem.toMediaMetadata(),
                                                                                                            )
                                                                                                        } else {
                                                                                                            ListQueue(
                                                                                                                title = randomItem.title,
                                                                                                                items = listOf(randomItem.toMediaItem())
                                                                                                            )
                                                                                                        }
                                                                                                    )
                                                                                                }

                                                                                                is AlbumItem -> {
                                                                                                    navController.navigate(
                                                                                                        "album/${randomItem.id}",
                                                                                                    )
                                                                                                }

                                                                                                is ArtistItem -> {
                                                                                                    navController.navigate(
                                                                                                        "artist/${randomItem.id}",
                                                                                                    )
                                                                                                }

                                                                                                is PlaylistItem -> {
                                                                                                    navController.navigate(
                                                                                                        "online_playlist/${randomItem.id}",
                                                                                                    )
                                                                                                }

                                                                                                is PodcastItem -> {
                                                                                                    navController.navigate(
                                                                                                        "online_podcast/${randomItem.id}",
                                                                                                    )
                                                                                                }

                                                                                                is EpisodeItem -> {
                                                                                                    playerConnection.playQueue(
                                                                                                        ListQueue(
                                                                                                            title = randomItem.title,
                                                                                                            items =
                                                                                                                listOf(
                                                                                                                    randomItem
                                                                                                                        .toMediaMetadata()
                                                                                                                        .toMediaItem(),
                                                                                                                ),
                                                                                                        ),
                                                                                                    )
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                    }
                                                                            }
                                                                        },
                                                                    )
                                                                }
                                                            } else if (itemIndex < pageItems.size) {
                                                                val item = pageItems[itemIndex]
                                                                val isPinned by database.speedDialDao
                                                                    .isPinned(
                                                                        item.id,
                                                                    ).collectAsStateWithLifecycle(initialValue = false)

                                                                Box(
                                                                    modifier =
                                                                        Modifier
                                                                            .width(itemWidth)
                                                                            .height(itemWidth)
                                                                            .padding(start = 2.dp, top = 2.dp),
                                                                ) {
                                                                    SpeedDialGridItem(
                                                                        item = item,
                                                                        isPinned = isPinned,
                                                                        isActive =
                                                                            item.id in listOf(mediaMetadata?.album?.id, mediaMetadata?.id),
                                                                        isPlaying = isPlaying,
                                                                        modifier =
                                                                            Modifier
                                                                                .fillMaxSize()
                                                                                .combinedClickable(
                                                                                    onClick = {
                                                                                        when (item) {
                                                                                            is SongItem -> {
                                                                                                if (!isListenTogetherGuest) {
                                                                                                    playerConnection.playQueue(
                                                                                                        if (autoRadioQueue) {
                                                                                                            YouTubeQueue(
                                                                                                                item.endpoint
                                                                                                                    ?: WatchEndpoint(
                                                                                                                        videoId = item.id,
                                                                                                                    ),
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
                                                                                                val rawType =
                                                                                                    pinnedSpeedDialItems
                                                                                                        .find {
                                                                                                            it.id ==
                                                                                                                item.id
                                                                                                        }?.type
                                                                                                if (rawType == "LOCAL_PLAYLIST") {
                                                                                                    navController.navigate(
                                                                                                        "local_playlist/${item.id}",
                                                                                                    )
                                                                                                } else {
                                                                                                    navController.navigate(
                                                                                                        "online_playlist/${item.id}",
                                                                                                    )
                                                                                                }
                                                                                            }

                                                                                            is PodcastItem -> {
                                                                                                navController.navigate(
                                                                                                    "online_podcast/${item.id}",
                                                                                                )
                                                                                            }

                                                                                            is EpisodeItem -> {
                                                                                                if (!isListenTogetherGuest) {
                                                                                                    playerConnection.playQueue(
                                                                                                        ListQueue(
                                                                                                            title = item.title,
                                                                                                            items =
                                                                                                                listOf(
                                                                                                                    item
                                                                                                                        .toMediaMetadata()
                                                                                                                        .toMediaItem(),
                                                                                                                ),
                                                                                                        ),
                                                                                                    )
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                    },
                                                                                    onLongClick = {
                                                                                        haptic.performHapticFeedback(
                                                                                            HapticFeedbackType.LongPress,
                                                                                        )
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
                                                                                                        coroutineScope = scope,
                                                                                                        onDismiss = menuState::dismiss,
                                                                                                    )
                                                                                                }

                                                                                                is PodcastItem -> {
                                                                                                    YouTubePlaylistMenu(
                                                                                                        playlist = item.asPlaylistItem(),
                                                                                                        coroutineScope = scope,
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
                                                                                    },
                                                                                ),
                                                                    )
                                                                }
                                                            } else {
                                                                Spacer(modifier = Modifier.width(itemWidth))
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        BaajaPageIndicator(
                                            pageCount = pagerState.pageCount,
                                            currentPage = pagerState.currentPage,
                                            onPageClick = { page -> scope.launch { pagerState.animateScrollToPage(page) } },
                                        )
                                    }
                                }
}
                            }

                            // Like YouTube Music: Speed dial first, then the shelves made for you.
                            tasteShown = true
                            tasteUi?.invoke(this)
                        }

                        HomeSection.QuickPicks -> {
                            quickPicks?.takeIf { it.isNotEmpty() }?.let { quickPicks ->
                                item(key = "quick_picks_title") {
 BaajaEntranceBox(7, entranceKey) {
                                    val quickPicksTitle = stringResource(R.string.quick_picks)
                                    BaajaHomeSectionTitle(
                                        title = quickPicksTitle,
                                        actionLabel = stringResource(R.string.play_all),
                                        onAction =
                                            if (!isListenTogetherGuest) {
                                                {
                                                    playerConnection.playQueue(
                                                        ListQueue(
                                                            title = quickPicksTitle,
                                                            items = quickPicks.distinctBy { it.id }.map { it.toMediaItem() },
                                                        ),
                                                    )
                                                }
                                            } else {
                                                null
                                            },
                                    )
                                }
}

                                item(key = "quick_picks_list") {
 BaajaEntranceBox(8, entranceKey) {
                                    LazyHorizontalGrid(
                                        state = quickPicksLazyGridState,
                                        rows = GridCells.Fixed(4),
                                        flingBehavior = rememberSnapFlingBehavior(quickPicksSnapLayoutInfoProvider),
                                        contentPadding =
                                            WindowInsets.systemBars
                                                .only(WindowInsetsSides.Horizontal)
                                                .asPaddingValues(),
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .height(HomeSongRowHeight * 4),
                                        ) {
                                            items(
                                                items = quickPicks.distinctBy { it.id },
                                                key = { "home_quickpick_${it.id}" },
                                            ) { originalSong ->
                                            // fetch song from database to keep updated
                                            val song by database
                                                .song(originalSong.id)
                                                .collectAsStateWithLifecycle(initialValue = originalSong)

                                            BaajaSongRow(
                                                song = song!!,
                                                showInLibraryIcon = true,
                                                isActive = song!!.id == mediaMetadata?.id,
                                                isPlaying = isPlaying,
                                                height = HomeSongRowHeight,
                                                trailing = {
                                                    BaajaMoreButton(
                                                        itemTitle = song!!.title,
                                                        onClick = {
                                                            menuState.show {
                                                                SongMenu(
                                                                    originalSong = song!!,
                                                                    onDismiss = menuState::dismiss,
                                                                )
                                                            }
                                                        },
                                                    )
                                                },
                                                modifier =
                                                    Modifier
                                                        .width(horizontalLazyGridItemWidth)
                                                        .combinedClickable(
                                                            onClick = {
                                                                if (!isListenTogetherGuest) {
                                                                    if (song!!.id == mediaMetadata?.id) {
                                                                        playerConnection.togglePlayPause()
                                                                    } else {
                                                                        playerConnection.playQueue(
                                                                            if (autoRadioQueue) {
                                                                                YouTubeQueue.radio(
                                                                                    song!!.toMediaMetadata(),
                                                                                )
                                                                            } else {
                                                                                ListQueue(
                                                                                    title = song!!.title,
                                                                                    items = listOf(song!!.toMediaItem())
                                                                                )
                                                                            }
                                                                        )
                                                                    }
                                                                }
                                                            },
                                                            onLongClick = {
                                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                menuState.show {
                                                                    SongMenu(
                                                                        originalSong = song!!,
                                                                        onDismiss = menuState::dismiss,
                                                                    )
                                                                }
                                                            },
                                                        ),
                                            )
                                        }
                                    }
                                }
}
                            }
                        }

                        HomeSection.FromTheCommunity -> {
                            communityPlaylists?.takeIf { it.isNotEmpty() }?.let { playlists ->
                                item(key = "community_playlists_title") {
                                    BaajaHomeSectionTitle(
                                        title = stringResource(R.string.from_the_community),
                                        label = stringResource(R.string.baaja_home_community_label),
                                    )
                                }

                                item(key = "community_playlists_content") {
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = BaajaHomeGutter, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    ) {
                                        items(playlists, key = { "community_${it.playlist.id}" }) { item ->
                                            BaajaCommunityCard(
                                                title = item.playlist.title,
                                                author =
                                                    item.playlist.author
                                                        ?.let { ArtistNameAliases.resolve(artistNameAliases, it.id, it.name) }
                                                        .orEmpty(),
                                                covers =
                                                    item.songs.mapNotNull { it.thumbnail?.resize(200, 200) }.ifEmpty {
                                                        listOfNotNull(item.playlist.thumbnail?.resize(400, 400))
                                                    },
                                                onClick = {
                                                    navController.navigate("online_playlist/${item.playlist.id.removePrefix("VL")}")
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HomeSection.DailyDiscover -> {
                            // Rendered at the top of Home as the "Mixed for you" carousel.
                        }

                        HomeSection.KeepListening -> {
                            keepListening?.takeIf { it.isNotEmpty() }?.let { keepListening ->
                                item(key = "keep_listening_title") {
                                    BaajaHomeSectionTitle(
                                        title = stringResource(R.string.keep_listening),
                                    )
                                }

                                item(key = "keep_listening_list") {
                                    val rows = if (keepListening.size > 6) 2 else 1
                                    LazyHorizontalGrid(
                                        state = remember("keep_listening_grid") { LazyGridState() },
                                        rows = GridCells.Fixed(rows),
                                        contentPadding =
                                            WindowInsets.systemBars
                                                .only(WindowInsetsSides.Horizontal)
                                                .asPaddingValues(),
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .height(
                                                    (
                                                        currentGridHeight +
                                                            with(LocalDensity.current) {
                                                                MaterialTheme.typography.bodyLarge.lineHeight
                                                                    .toDp() * 2 +
                                                                    MaterialTheme.typography.bodyMedium.lineHeight
                                                                        .toDp() * 2
                                                            }
                                                    ) * rows,
                                                ),
                                    ) {
                                        items(keepListening.distinctBy { it.id }, key = { "home_keep_listening_${it.id}" }) {
                                            localGridItem(it)
                                        }
                                    }
                                }
                            }
                        }

                        HomeSection.AccountPlaylists -> {
                            accountPlaylists?.takeIf { it.isNotEmpty() }?.let { accountPlaylists ->
                                item(key = "account_playlists_title") {
                                    BaajaHomeSectionTitle(
                                        label = stringResource(R.string.mixes),
                                        title = accountName,
                                        thumbnail = {
                                            if (url != null) {
                                                AsyncImage(
                                                    model =
                                                        ImageRequest
                                                            .Builder(LocalContext.current)
                                                            .data(url)
                                                            .diskCachePolicy(CachePolicy.ENABLED)
                                                            .diskCacheKey(url)
                                                            .crossfade(false)
                                                            .build(),
                                                    placeholder = painterResource(id = R.drawable.person),
                                                    error = painterResource(id = R.drawable.person),
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier =
                                                        Modifier
                                                            .size(ListThumbnailSize)
                                                            .clip(CircleShape),
                                                )
                                            } else {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.person),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(ListThumbnailSize),
                                                )
                                            }
                                        },
                                        onClick = {
                                            navController.navigate("account")
                                        },
                                    )
                                }

                                item(key = "account_playlists_list") {
                                    LazyRow(
                                        contentPadding =
                                            WindowInsets.systemBars
                                                .only(WindowInsetsSides.Horizontal)
                                                .asPaddingValues(),
                                    ) {
                                        items(
                                            items = accountPlaylists.distinctBy { it.id },
                                            key = { "home_account_playlist_${it.id}" },
                                        ) { item ->
                                            ytGridItem(item)
                                        }
                                    }
                                }
                            }
                        }

                        HomeSection.ForgottenFavorites -> {
                            forgottenFavorites?.takeIf { it.isNotEmpty() }?.let { forgottenFavorites ->
                                item(key = "forgotten_favorites_title") {
                                    val forgottenFavoritesTitle = stringResource(R.string.forgotten_favorites)
                                    BaajaHomeSectionTitle(
                                        title = forgottenFavoritesTitle,
                                        actionLabel = stringResource(R.string.play_all),
                                        onAction =
                                            if (!isListenTogetherGuest) {
                                                {
                                                    playerConnection.playQueue(
                                                        ListQueue(
                                                            title = forgottenFavoritesTitle,
                                                            items = forgottenFavorites.distinctBy { it.id }.map { it.toMediaItem() },
                                                        ),
                                                    )
                                                }
                                            } else {
                                                null
                                            },
                                    )
                                }

                                item(key = "forgotten_favorites_list") {
                                    // take min in case list size is less than 4
                                    val rows = min(4, forgottenFavorites.size)
                                    LazyHorizontalGrid(
                                        state = forgottenFavoritesLazyGridState,
                                        rows = GridCells.Fixed(rows),
                                        contentPadding =
                                            WindowInsets.systemBars
                                                .only(WindowInsetsSides.Horizontal)
                                                .asPaddingValues(),
                                        flingBehavior =
                                            rememberSnapFlingBehavior(
                                                forgottenFavoritesSnapLayoutInfoProvider,
                                            ),
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .height(HomeSongRowHeight * rows),
                                        ) {
                                            items(
                                                items = forgottenFavorites.distinctBy { it.id },
                                                key = { "home_forgotten_${it.id}" },
                                            ) { originalSong ->
                                            val song by database
                                                .song(originalSong.id)
                                                .collectAsStateWithLifecycle(initialValue = originalSong)

                                            BaajaSongRow(
                                                song = song!!,
                                                showInLibraryIcon = true,
                                                isActive = song!!.id == mediaMetadata?.id,
                                                isPlaying = isPlaying,
                                                height = HomeSongRowHeight,
                                                trailing = {
                                                    BaajaMoreButton(
                                                        itemTitle = song!!.title,
                                                        onClick = {
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            menuState.show {
                                                                SongMenu(
                                                                    originalSong = song!!,
                                                                    onDismiss = menuState::dismiss,
                                                                )
                                                            }
                                                        },
                                                    )
                                                },
                                                modifier =
                                                    Modifier
                                                        .width(horizontalLazyGridItemWidth)
                                                        .combinedClickable(
                                                            onClick = {
                                                                if (!isListenTogetherGuest) {
                                                                    if (song!!.id == mediaMetadata?.id) {
                                                                        playerConnection.togglePlayPause()
                                                                    } else {
                                                                        playerConnection.playQueue(
                                                                            if (autoRadioQueue) {
                                                                                YouTubeQueue.radio(
                                                                                    song!!.toMediaMetadata(),
                                                                                )
                                                                            } else {
                                                                                ListQueue(
                                                                                    title = song!!.title,
                                                                                    items = listOf(song!!.toMediaItem())
                                                                                )
                                                                            }
                                                                        )
                                                                    }
                                                                }
                                                            },
                                                            onLongClick = {
                                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                menuState.show {
                                                                    SongMenu(
                                                                        originalSong = song!!,
                                                                        onDismiss = menuState::dismiss,
                                                                    )
                                                                }
                                                            },
                                                        ),
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        is HomeSection.SimilarRecommendation -> {
                            val recommendation = similarRecommendations?.getOrNull(section.index)
                            recommendation?.let {
                                item(key = "similar_to_title_${section.index}") {
                                    BaajaHomeSectionTitle(
                                        label = stringResource(R.string.similar_to),
                                        title = recommendation.title.title,
                                        thumbnail =
                                            recommendation.title.thumbnailUrl?.let { thumbnailUrl ->
                                                {
                                                    val shape =
                                                        if (recommendation.title is Artist) {
                                                            CircleShape
                                                        } else {
                                                            RoundedCornerShape(
                                                                ThumbnailCornerRadius,
                                                            )
                                                        }
                                                    AsyncImage(
                                                        model = thumbnailUrl,
                                                        contentDescription = null,
                                                        modifier =
                                                            Modifier
                                                                .size(ListThumbnailSize)
                                                                .clip(shape),
                                                    )
                                                }
                                            },
                                        onClick = {
                                            when (recommendation.title) {
                                                is Song -> {
                                                    navController.navigate("album/${recommendation.title.album!!.id}")
                                                }

                                                is Album -> {
                                                    navController.navigate("album/${recommendation.title.id}")
                                                }

                                                is Artist -> {
                                                    navController.navigate("artist/${recommendation.title.id}")
                                                }

                                                is Playlist -> {}
                                            }
                                        },
                                    )
                                }

                                item(key = "similar_to_list_${section.index}") {
                                    LazyRow(
                                        contentPadding =
                                            WindowInsets.systemBars
                                                .only(WindowInsetsSides.Horizontal)
                                                .asPaddingValues(),
                                    ) {
                                        items(recommendation.items.distinctBy { it.id }, key = { "home_similar_${it.id}" }) { item ->
                                            ytGridItem(item)
                                        }
                                    }
                                }
                            }
                        }

                        is HomeSection.HomePageSection -> {
                            // Skip HomePageSection rendering when podcast chip is selected
                            // Podcast sections are handled separately with special UI
                            if (selectedChip?.title?.contains("Podcast", ignoreCase = true) == true) {
                                return@forEach
                            }
                            val sectionData = homePage?.sections?.getOrNull(section.index)
                            sectionData?.let {
                                // Check if section contains songs for Play All functionality
                                val sectionSongs = sectionData.items.filterIsInstance<SongItem>()
                                val hasPlayableSongs = sectionSongs.isNotEmpty()
                                // Check if this section contains ONLY songs (like Quick picks, Trending songs)
                                val isSongsOnlySection =
                                    sectionData.items.isNotEmpty() &&
                                        sectionData.items.all { it is SongItem }

                                item(key = "home_section_title_${section.index}") {
                                    BaajaHomeSectionTitle(
                                        title = sectionData.title,
                                        label = sectionData.label,
                                        thumbnail =
                                            sectionData.thumbnail?.let { thumbnailUrl ->
                                                {
                                                    val shape =
                                                        if (sectionData.endpoint?.isArtistEndpoint == true) {
                                                            CircleShape
                                                        } else {
                                                            RoundedCornerShape(
                                                                ThumbnailCornerRadius,
                                                            )
                                                        }
                                                    AsyncImage(
                                                        model = thumbnailUrl,
                                                        contentDescription = null,
                                                        modifier =
                                                            Modifier
                                                                .size(ListThumbnailSize)
                                                                .clip(shape),
                                                    )
                                                }
                                            },
                                        onClick =
                                            sectionData.endpoint?.let { endpoint ->
                                                {
                                                    when {
                                                        endpoint.browseId == "FEmusic_moods_and_genres" -> {
                                                            navController.navigate("mood_and_genres")
                                                        }

                                                        // Handle podcast-related browse endpoints
                                                        endpoint.browseId.startsWith("FEmusic_library_non_music_audio") ||
                                                            endpoint.browseId.startsWith("FEmusic_non_music_audio") -> {
                                                            navController.navigate("youtube_browse/${endpoint.browseId}")
                                                        }

                                                        endpoint.params != null -> {
                                                            navController.navigate(
                                                                "youtube_browse/${endpoint.browseId}?params=${endpoint.params}",
                                                            )
                                                        }

                                                        else -> {
                                                            navController.navigate("browse/${endpoint.browseId}")
                                                        }
                                                    }
                                                }
                                            },
                                        actionLabel = stringResource(R.string.play_all),
                                        onAction =
                                            if (hasPlayableSongs && !isListenTogetherGuest) {
                                                {
                                                    playerConnection.playQueue(
                                                        ListQueue(
                                                            title = sectionData.title,
                                                            items = sectionSongs.map { it.toMediaMetadata().toMediaItem() },
                                                        ),
                                                    )
                                                }
                                            } else {
                                                null
                                            },
                                    )
                                }

                                if (isSongsOnlySection) {
                                    // Render songs as a horizontal scrollable list (like Quick picks in YouTube Music)
                                    item(key = "home_section_list_${section.index}") {
                                        LazyHorizontalGrid(
                                            state = remember("section_${section.index}_grid") { LazyGridState() },
                                            rows = GridCells.Fixed(4),
                                            contentPadding =
                                                WindowInsets.systemBars
                                                    .only(WindowInsetsSides.Horizontal)
                                                    .asPaddingValues(),
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .height(HomeYtRowHeight * 4),
                                        ) {
                                            items(
                                                items = sectionSongs.distinctBy { it.id },
                                                key = { "home_section_${section.index}_song_${it.id}" },
                                            ) { song ->
                                                BaajaYouTubeRow(
                                                    item = song,
                                                    isActive = song.id == mediaMetadata?.id,
                                                    isPlaying = isPlaying,
                                                    isSwipeable = false,
                                                    height = HomeYtRowHeight,
                                                    trailing = {
                                                        BaajaMoreButton(
                                                            itemTitle = song.title,
                                                            onClick = {
                                                                menuState.show {
                                                                    YouTubeSongMenu(
                                                                        song = song,
                                                                        onDismiss = menuState::dismiss,
                                                                    )
                                                                }
                                                            },
                                                        )
                                                    },
                                                    modifier =
                                                        Modifier
                                                            .width(horizontalLazyGridItemWidth)
                                                            .combinedClickable(
                                                                onClick = {
                                                                    if (!isListenTogetherGuest) {
                                                                        playerConnection.playQueue(
                                                                            if (autoRadioQueue) {
                                                                                YouTubeQueue(
                                                                                    song.endpoint ?: WatchEndpoint(videoId = song.id),
                                                                                    song.toMediaMetadata(),
                                                                                )
                                                                            } else {
                                                                                ListQueue(
                                                                                    title = song.title,
                                                                                    items = listOf(song.toMediaItem())
                                                                                )
                                                                            }
                                                                        )
                                                                    }
                                                                },
                                                                onLongClick = {
                                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                    menuState.show {
                                                                        YouTubeSongMenu(
                                                                            song = song,
                                                                            onDismiss = menuState::dismiss,
                                                                        )
                                                                    }
                                                                },
                                                            ),
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    // Render mixed content as horizontal grid items (albums, playlists, artists, etc.)
                                    item(key = "home_section_list_${section.index}") {
                                        LazyRow(
                                            contentPadding =
                                                WindowInsets.systemBars
                                                    .only(WindowInsetsSides.Horizontal)
                                                    .asPaddingValues(),
                                        ) {
                                            items(
                                                items = sectionData.items.distinctBy { it.id },
                                                key = { "home_section_${section.index}_item_${it.id}" },
                                            ) { item ->
                                                ytGridItem(item)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        HomeSection.MoodAndGenres -> {
                            // Skip MoodAndGenres when podcast chip is selected
                            if (selectedChip?.title?.contains("Podcast", ignoreCase = true) == true) {
                                return@forEach
                            }
                            explorePage?.moodAndGenres?.let { moodAndGenres ->
                                item(key = "mood_and_genres_title") {
                                    BaajaHomeSectionTitle(
                                        title = stringResource(R.string.mood_and_genres),
                                        onClick = {
                                            navController.navigate("mood_and_genres")
                                        },
                                    )
                                }
                                item(key = "mood_and_genres_list") {
                                    LazyHorizontalGrid(
                                        rows = GridCells.Fixed(3),
                                        contentPadding = PaddingValues(horizontal = BaajaHomeGutter - 6.dp, vertical = 6.dp),
                                        modifier =
                                            Modifier
                                                .height((MoodAndGenresButtonHeight + 12.dp) * 3 + 12.dp),
                                    ) {
                                        items(moodAndGenres.distinctBy { "${it.title}_${it.endpoint.browseId}_${it.endpoint.params}" }, key = { "${it.title}_${it.endpoint.browseId}_${it.endpoint.params}" }) {
                                            MoodAndGenresButton(
                                                title = it.title,
                                                stripeColor = it.stripeColor,
                                                onClick = {
                                                    navController.navigate(
                                                        "youtube_browse/${it.endpoint.browseId}?params=${it.endpoint.params}",
                                                    )
                                                },
                                                modifier =
                                                    Modifier
                                                        .padding(6.dp)
                                                        .width(150.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Only show shimmer during initial loading, not for pagination
                if (isLoading && homePage?.sections.isNullOrEmpty()) {
                    item(key = "loading_shimmer") {
                        ShimmerHost(
                        ) {
                            repeat(2) {
                                TextPlaceholder(
                                    height = 36.dp,
                                    modifier =
                                        Modifier
                                            .padding(12.dp)
                                            .width(250.dp),
                                )
                                LazyRow(
                                    contentPadding =
                                        WindowInsets.systemBars
                                            .only(WindowInsetsSides.Horizontal)
                                            .asPaddingValues(),
                                ) {
                                    items(4) {
                                        GridItemPlaceHolder()
                                    }
                                }
                            }

                            TextPlaceholder(
                                height = 36.dp,
                                modifier =
                                    Modifier
                                        .padding(vertical = 12.dp, horizontal = 12.dp)
                                        .width(250.dp),
                            )
                            repeat(4) {
                                Row {
                                    repeat(2) {
                                        TextPlaceholder(
                                            height = MoodAndGenresButtonHeight,
                                            shape = RoundedCornerShape(6.dp),
                                            modifier =
                                                Modifier
                                                    .padding(horizontal = 12.dp)
                                                    .width(200.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }
    }
}

private val HomeSongRowHeight = 84.dp
private val HomeYtRowHeight = 76.dp
