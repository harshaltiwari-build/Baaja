/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachReversed
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.exoplayer.offline.Download
import androidx.navigation.NavController
import androidx.media3.exoplayer.offline.DownloadService
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.playback.ExoDownloadService
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.ui.component.BaajaCollectionHeader
import com.metrolist.music.ui.component.BaajaDownloadAction
import com.metrolist.music.ui.component.BaajaHeroArtwork
import com.metrolist.music.ui.component.BaajaHeroPlayButton
import com.metrolist.music.ui.component.BaajaLikeAction
import com.metrolist.music.ui.component.BaajaRoundAction
import com.metrolist.music.ui.theme.BaajaSectionHeader
import androidx.compose.foundation.layout.add
import coil3.compose.AsyncImage
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalDownloadUtil
import com.metrolist.music.LocalListenTogetherManager
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.HideVideoSongsKey
import com.metrolist.music.db.entities.Album
import com.metrolist.music.playback.queues.LocalAlbumRadio
import com.metrolist.music.ui.component.ClickableArtistText
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.NavigationTitle
import com.metrolist.music.ui.component.SongListItem
import com.metrolist.music.ui.component.YouTubeGridItem
import com.metrolist.music.ui.menu.AlbumMenu
import com.metrolist.music.ui.menu.SelectionSongMenu
import com.metrolist.music.ui.menu.SongMenu
import com.metrolist.music.ui.menu.YouTubeAlbumMenu
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.ui.utils.resize
import com.metrolist.music.utils.makeTimeString
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.AlbumViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AlbumScreen(
    navController: NavController,
    viewModel: AlbumViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return
    val listenTogetherManager = LocalListenTogetherManager.current
    val isListenTogetherGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false

    val scope = rememberCoroutineScope()

    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()

    val playlistId by viewModel.playlistId.collectAsStateWithLifecycle()
    val albumWithSongs by viewModel.albumWithSongs.collectAsStateWithLifecycle()
    val otherVersions by viewModel.otherVersions.collectAsStateWithLifecycle()
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = true)
    val hideVideoSongs by rememberPreference(key = HideVideoSongsKey, defaultValue = true)

    val filteredSongs =
        remember(albumWithSongs, hideExplicit, hideVideoSongs) {
            var songs = albumWithSongs?.songs ?: emptyList()
            if (hideExplicit) {
                songs = songs.filter { !it.song.explicit }
            }
            if (hideVideoSongs) {
                songs = songs.filter { !it.song.isVideo }
            }
            songs
        }

    var inSelectMode by rememberSaveable { mutableStateOf(false) }
    val selection =
        rememberSaveable(
            saver =
                listSaver<MutableList<String>, String>(
                    save = { it.toList() },
                    restore = { it.toMutableStateList() },
                ),
        ) { mutableStateListOf() }
    val onExitSelectionMode = {
        inSelectMode = false
        selection.clear()
    }
    if (inSelectMode) {
        BackHandler(onBack = onExitSelectionMode)
    }

    LaunchedEffect(filteredSongs) {
        selection.fastForEachReversed { songId ->
            if (filteredSongs.find { it.id == songId } == null) {
                selection.remove(songId)
            }
        }
    }

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember {
        mutableIntStateOf(Download.STATE_STOPPED)
    }

    LaunchedEffect(albumWithSongs) {
        val songs = albumWithSongs?.songs?.map { it.id }
        if (songs.isNullOrEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs.all { downloads[it]?.state == Download.STATE_COMPLETED }) {
                    Download.STATE_COMPLETED
                } else if (songs.all {
                        downloads[it]?.state == Download.STATE_QUEUED ||
                            downloads[it]?.state == Download.STATE_DOWNLOADING ||
                            downloads[it]?.state == Download.STATE_COMPLETED
                    }
                ) {
                    Download.STATE_DOWNLOADING
                } else {
                    Download.STATE_STOPPED
                }
        }
    }

    LazyColumn(
        contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
    ) {
        val albumWithSongs = albumWithSongs
        if (albumWithSongs != null && albumWithSongs.songs.isNotEmpty()) {
            item(key = "album_header") {
                val totalDuration = albumWithSongs.songs.sumOf { it.song.duration }
                val albumKind = stringResource(R.string.baaja_library_album_kind)
                val songCountText =
                    pluralStringResource(R.plurals.n_song, albumWithSongs.songs.size, albumWithSongs.songs.size)
                val meta =
                    remember(albumWithSongs, songCountText, albumKind, totalDuration) {
                        listOfNotNull(
                            albumKind,
                            albumWithSongs.album.year?.toString(),
                            songCountText,
                            if (totalDuration > 0) makeTimeString(totalDuration * 1000L) else null,
                        ).joinToString(" · ")
                    }
                val isLiked = albumWithSongs.album.bookmarkedAt != null

                BaajaCollectionHeader(
                    title = albumWithSongs.album.title,
                    meta = meta,
                    subtitle = {
                        ClickableArtistText(
                            artists = albumWithSongs.artists,
                            style =
                                MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center,
                                ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    artwork = {
                        BaajaHeroArtwork(
                            model = albumWithSongs.album.thumbnailUrl?.resize(1080, 1080),
                            placeholder = R.drawable.album,
                        )
                    },
                    actions = {
                        BaajaDownloadAction(
                            downloadState = downloadState,
                            onClick = {
                                when (downloadState) {
                                    Download.STATE_QUEUED, Download.STATE_DOWNLOADING ->
                                        downloadUtil.cancelUnfinished(albumWithSongs.songs.map { it.id })

                                    Download.STATE_COMPLETED -> {
                                        albumWithSongs.songs.forEach { song ->
                                            DownloadService.sendRemoveDownload(
                                                context,
                                                ExoDownloadService::class.java,
                                                song.id,
                                                false,
                                            )
                                        }
                                    }

                                    else -> albumWithSongs.songs.forEach { downloadUtil.download(it) }
                                }
                            },
                        )

                        BaajaLikeAction(
                            liked = isLiked,
                            onClick = {
                                database.query {
                                    update(albumWithSongs.album.toggleLike())
                                }
                            },
                        )

                        BaajaHeroPlayButton(
                            enabled = !isListenTogetherGuest,
                            onClick = {
                                if (!isListenTogetherGuest) {
                                    playerConnection.service.getAutomix(playlistId)
                                    playerConnection.playQueue(
                                        LocalAlbumRadio(albumWithSongs),
                                    )
                                }
                            },
                        )

                        BaajaRoundAction(
                            icon = R.drawable.shuffle,
                            contentDescription = stringResource(R.string.shuffle),
                            enabled = !isListenTogetherGuest && filteredSongs.isNotEmpty(),
                            onClick = {
                                if (!isListenTogetherGuest && filteredSongs.isNotEmpty()) {
                                    playerConnection.playQueue(
                                        ListQueue(
                                            title = albumWithSongs.album.title,
                                            items = filteredSongs.shuffled().map { it.toMediaItem() },
                                        ),
                                    )
                                }
                            },
                        )

                        BaajaRoundAction(
                            icon = R.drawable.more_vert,
                            contentDescription = stringResource(R.string.more_options),
                            onClick = {
                                menuState.show {
                                    AlbumMenu(
                                        originalAlbum =
                                            Album(
                                                albumWithSongs.album,
                                                albumWithSongs.artists,
                                            ),
                                        onDismiss = menuState::dismiss,
                                    )
                                }
                            },
                        )
                    },
                )
            }

            if (filteredSongs.isNotEmpty()) {
                itemsIndexed(
                    items = filteredSongs,
                    key = { index, song -> "${song.id}_$index" },
                ) { index, song ->
                    val onCheckedChange: (Boolean) -> Unit = {
                        if (it) {
                            selection.add(song.id)
                        } else {
                            selection.remove(song.id)
                        }
                    }

                    SongListItem(
                        song = song,
                        albumIndex = index + 1,
                        isActive = song.id == mediaMetadata?.id,
                        isPlaying = isPlaying,
                        showInLibraryIcon = true,
                        trailingContent = {
                            if (inSelectMode) {
                                Checkbox(
                                    checked = song.id in selection,
                                    onCheckedChange = onCheckedChange,
                                )
                            } else {
                                IconButton(
                                    onClick = {
                                        menuState.show {
                                            SongMenu(
                                                originalSong = song,
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    },
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.more_vert),
                                        contentDescription = null,
                                    )
                                }
                            }
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .animateItem()
                                .combinedClickable(
                                    onClick = {
                                        if (inSelectMode) {
                                            onCheckedChange(song.id !in selection)
                                        } else if (!isListenTogetherGuest) {
                                            if (song.id == mediaMetadata?.id) {
                                                playerConnection.togglePlayPause()
                                            } else {
                                                playerConnection.service.getAutomix(playlistId)
                                                playerConnection.playQueue(
                                                    LocalAlbumRadio(albumWithSongs, startIndex = index),
                                                )
                                            }
                                        }
                                    },
                                    onLongClick = {
                                        if (!inSelectMode) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            inSelectMode = true
                                            onCheckedChange(true)
                                        }
                                    },
                                ),
                    )
                }
            }

            if (otherVersions.isNotEmpty()) {
                item(key = "other_versions_title") {
                    BaajaSectionHeader(
                        title = stringResource(R.string.other_versions),
                        modifier =
                            Modifier
                                .padding(start = 20.dp, end = 12.dp, top = 20.dp)
                                .animateItem(),
                    )
                }
                item(key = "other_versions_list") {
                    LazyRow(
                        contentPadding =
                            WindowInsets.systemBars
                                .only(WindowInsetsSides.Horizontal)
                                .add(WindowInsets(left = 8.dp, right = 8.dp))
                                .asPaddingValues(),
                    ) {
                        items(
                            items = otherVersions.distinctBy { it.id },
                            key = { "album_other_${it.id}" },
                        ) { item ->
                            YouTubeGridItem(
                                item = item,
                                isActive = mediaMetadata?.album?.id == item.id,
                                isPlaying = isPlaying,
                                coroutineScope = scope,
                                modifier =
                                    Modifier
                                        .combinedClickable(
                                            onClick = { navController.navigate("album/${item.id}") },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    YouTubeAlbumMenu(
                                                        albumItem = item,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        ).animateItem(),
                            )
                        }
                    }
                }
            }
        } else {
            item(key = "loading") {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    ContainedLoadingIndicator()
                }
            }
        }
    }

    TopAppBar(
        title = {
            if (inSelectMode) {
                Text(pluralStringResource(R.plurals.n_selected, selection.size, selection.size))
            } else {
                Text(
                    text = albumWithSongs?.album?.title.orEmpty(),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        navigationIcon = {
            if (inSelectMode) {
                IconButton(onClick = onExitSelectionMode) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = null,
                    )
                }
            } else {
                IconButton(
                    onClick = { navController.navigateUp() },
                    onLongClick = { navController.backToMain() },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = null,
                    )
                }
            }
        },
        actions = {
            if (inSelectMode) {
                Checkbox(
                    checked = selection.size == filteredSongs.size && selection.isNotEmpty(),
                    onCheckedChange = {
                        if (selection.size == filteredSongs.size) {
                            selection.clear()
                        } else {
                            selection.clear()
                            selection.addAll(filteredSongs.map { it.id })
                        }
                    },
                )
                IconButton(
                    enabled = selection.isNotEmpty(),
                    onClick = {
                        menuState.show {
                            SelectionSongMenu(
                                songSelection =
                                    selection.mapNotNull { songId ->
                                        filteredSongs.find { it.id == songId }
                                    },
                                onDismiss = menuState::dismiss,
                                clearAction = onExitSelectionMode,
                            )
                        }
                    },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.more_vert),
                        contentDescription = null,
                    )
                }
            }
        },
    )
}
