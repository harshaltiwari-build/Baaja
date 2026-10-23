/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.features

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.metrolist.music.ui.component.DropdownMenu
import com.metrolist.music.ui.component.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.local.LocalAlbum
import com.metrolist.music.local.LocalArtist
import com.metrolist.music.local.LocalAudio
import com.metrolist.music.local.LocalMusicPlayback
import com.metrolist.music.local.LocalMusicScanner
import com.metrolist.music.local.LocalSort
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.ui.theme.BaajaCard
import com.metrolist.music.ui.theme.BaajaChip
import com.metrolist.music.ui.theme.BaajaIconButton
import com.metrolist.music.ui.theme.BaajaPillButton
import com.metrolist.music.ui.theme.baajaInk
import com.metrolist.music.ui.theme.baajaSurface
import com.metrolist.music.utils.makeTimeString

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LocalMusicScreen(
    navController: NavController,
    viewModel: LocalMusicViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current
    val unknownArtist = stringResource(R.string.baaja_imp_unknown_artist)

    val hasPermission by viewModel.hasPermission.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val hasScanned by viewModel.hasScanned.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val tab by viewModel.tab.collectAsStateWithLifecycle()
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    val descending by viewModel.descending.collectAsStateWithLifecycle()
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()
    val openGroupKey by viewModel.openGroupKey.collectAsStateWithLifecycle()
    val openGroup by viewModel.openGroup.collectAsStateWithLifecycle()

    val nowPlaying = playerConnection?.mediaMetadata?.collectAsStateWithLifecycle()?.value?.id

    // Re-check the permission whenever the screen comes back (e.g. from the system settings page).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshPermission()
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var permanentlyDenied by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            viewModel.refreshPermission()
            if (!granted) {
                val activity = context.findActivity()
                permanentlyDenied =
                    activity != null &&
                    !ActivityCompat.shouldShowRequestPermissionRationale(activity, LocalMusicScanner.audioPermission)
            }
        }

    BackHandler(enabled = openGroupKey != null) { viewModel.openGroupKey.value = null }

    fun play(list: List<LocalAudio>, start: Int, shuffle: Boolean, title: String?) {
        val connection = playerConnection ?: return
        if (list.isEmpty()) return
        viewModel.register(list, unknownArtist)
        val ordered = if (shuffle) list.shuffled() else list
        connection.playQueue(
            ListQueue(
                title = title,
                items = LocalMusicPlayback.mediaItems(ordered, unknownArtist),
                startIndex = if (shuffle) 0 else start.coerceIn(0, list.lastIndex),
            ),
        )
    }

    val listState = rememberLazyListState()
    LaunchedEffect(tab, openGroupKey) { listState.scrollToItem(0) }

    val insets = LocalPlayerAwareWindowInsets.current
    val localTitle = stringResource(R.string.baaja_imp_local_title)

    LazyColumn(
        state = listState,
        contentPadding = insets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom).asPaddingValues(),
        modifier =
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(insets.only(WindowInsetsSides.Top)),
    ) {
        val group = openGroup
        item(key = "header") {
            FeatureHeader(
                title = group?.title?.ifBlank { null } ?: if (group != null) {
                    stringResource(if (group.isArtist) R.string.baaja_imp_unknown_artist else R.string.baaja_imp_unknown_album)
                } else {
                    localTitle
                },
                subtitle =
                    when {
                        group != null -> pluralStringResource(R.plurals.baaja_imp_song_count, group.songs.size, group.songs.size)
                        hasPermission && hasScanned -> pluralStringResource(R.plurals.baaja_imp_song_count, totalCount, totalCount)
                        else -> null
                    },
                onBack = {
                    if (openGroupKey != null) viewModel.openGroupKey.value = null else navController.navigateUp()
                },
            ) {
                if (hasPermission && group == null) {
                    BaajaIconButton(
                        icon = R.drawable.refresh,
                        contentDescription = stringResource(R.string.baaja_imp_local_rescan),
                        onClick = viewModel::rescan,
                    )
                }
            }
        }

        if (!hasPermission) {
            item(key = "permission") {
                PermissionCard(
                    permanentlyDenied = permanentlyDenied,
                    onAllow = { permissionLauncher.launch(LocalMusicScanner.audioPermission) },
                    onOpenSettings = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    },
                )
            }
            return@LazyColumn
        }

        if (isScanning) {
            item(key = "scanning") {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }

        if (group != null) {
            item(key = "group_header") {
                GroupHeader(
                    artworkUri = group.artworkUri,
                    isArtist = group.isArtist,
                    subtitle = group.subtitle?.ifBlank { null } ?: if (group.isArtist) {
                        pluralStringResource(R.plurals.baaja_imp_album_count, group.albumCount, group.albumCount)
                    } else {
                        unknownArtist
                    },
                    onPlay = { play(group.songs, 0, shuffle = false, title = group.title) },
                    onShuffle = { play(group.songs, 0, shuffle = true, title = group.title) },
                )
            }
            songItems(
                songs = group.songs,
                nowPlaying = nowPlaying,
                unknownArtist = unknownArtist,
                showArtwork = group.isArtist,
                onPlay = { index -> play(group.songs, index, shuffle = false, title = group.title) },
                onPlayNext = { song ->
                    viewModel.register(listOf(song), unknownArtist)
                    playerConnection?.playNext(LocalMusicPlayback.mediaItems(listOf(song), unknownArtist))
                },
                onAddToQueue = { song ->
                    viewModel.register(listOf(song), unknownArtist)
                    playerConnection?.addToQueue(LocalMusicPlayback.mediaItems(listOf(song), unknownArtist))
                },
            )
            return@LazyColumn
        }

        item(key = "search") {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FeatureTextField(
                    value = query,
                    onValueChange = { viewModel.query.value = it },
                    placeholder = stringResource(R.string.baaja_imp_local_search_hint),
                    leadingIcon = R.drawable.search,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Search,
                    trailing =
                        if (query.isNotEmpty()) {
                            {
                                Box(
                                    Modifier.size(44.dp).clip(CircleShape).clickable(role = Role.Button) { viewModel.query.value = "" },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(painterResource(R.drawable.close), stringResource(R.string.baaja_imp_clear_search), modifier = Modifier.size(20.dp))
                                }
                            }
                        } else {
                            null
                        },
                    modifier = Modifier.weight(1f),
                )
                SortButton(
                    sort = sort,
                    descending = descending,
                    onSort = viewModel::setSort,
                    onToggleDescending = viewModel::toggleDescending,
                )
            }
        }

        item(key = "tabs") {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LocalMusicTab.entries.forEach { entry ->
                    BaajaChip(
                        label =
                            stringResource(
                                when (entry) {
                                    LocalMusicTab.SONGS -> R.string.baaja_imp_local_tab_songs
                                    LocalMusicTab.ALBUMS -> R.string.baaja_imp_local_tab_albums
                                    LocalMusicTab.ARTISTS -> R.string.baaja_imp_local_tab_artists
                                },
                            ),
                        selected = tab == entry,
                        onClick = { viewModel.tab.value = entry },
                    )
                }
            }
        }

        if (hasScanned && totalCount == 0 && !isScanning) {
            item(key = "empty") { EmptyLibrary(onRescan = viewModel::rescan) }
            return@LazyColumn
        }
        if (!hasScanned) {
            item(key = "first_scan") {
                Text(
                    stringResource(R.string.baaja_imp_local_scanning),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    textAlign = TextAlign.Center,
                )
            }
            return@LazyColumn
        }
        if (query.isNotBlank() && songs.isEmpty()) {
            item(key = "no_results") {
                Text(
                    stringResource(R.string.baaja_imp_local_no_results, query),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    textAlign = TextAlign.Center,
                )
            }
            return@LazyColumn
        }

        when (tab) {
            LocalMusicTab.SONGS -> {
                item(key = "play_row") {
                    PlayRow(
                        onPlay = { play(songs, 0, shuffle = false, title = localTitle) },
                        onShuffle = { play(songs, 0, shuffle = true, title = localTitle) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
                songItems(
                    songs = songs,
                    nowPlaying = nowPlaying,
                    unknownArtist = unknownArtist,
                    showArtwork = true,
                    onPlay = { index -> play(songs, index, shuffle = false, title = localTitle) },
                    onPlayNext = { song ->
                        viewModel.register(listOf(song), unknownArtist)
                        playerConnection?.playNext(LocalMusicPlayback.mediaItems(listOf(song), unknownArtist))
                    },
                    onAddToQueue = { song ->
                        viewModel.register(listOf(song), unknownArtist)
                        playerConnection?.addToQueue(LocalMusicPlayback.mediaItems(listOf(song), unknownArtist))
                    },
                )
            }

            LocalMusicTab.ALBUMS -> {
                val rows = albums.chunked(2)
                items(count = rows.size, key = { "albums_row_${rows[it].first().key}" }, contentType = { "album_row" }) { index ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        rows[index].forEach { album ->
                            AlbumTile(
                                album = album,
                                unknownAlbum = stringResource(R.string.baaja_imp_unknown_album),
                                unknownArtist = unknownArtist,
                                onClick = { viewModel.openGroupKey.value = album.key },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (rows[index].size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }

            LocalMusicTab.ARTISTS -> {
                items(count = artists.size, key = { artists[it].key }, contentType = { "artist" }) { index ->
                    ArtistRow(
                        artist = artists[index],
                        unknownArtist = unknownArtist,
                        onClick = { viewModel.openGroupKey.value = artists[index].key },
                    )
                }
            }
        }
        item(key = "bottom_space") { Spacer(Modifier.height(24.dp)) }
    }
}

private fun LazyListScope.songItems(
    songs: List<LocalAudio>,
    nowPlaying: String?,
    unknownArtist: String,
    showArtwork: Boolean,
    onPlay: (Int) -> Unit,
    onPlayNext: (LocalAudio) -> Unit,
    onAddToQueue: (LocalAudio) -> Unit,
) {
    itemsIndexed(songs, key = { _, song -> song.uri }, contentType = { _, _ -> "song" }) { index, song ->
        LocalSongRow(
            song = song,
            isPlaying = song.uri == nowPlaying,
            unknownArtist = unknownArtist,
            showArtwork = showArtwork,
            index = index,
            onClick = { onPlay(index) },
            onPlayNext = { onPlayNext(song) },
            onAddToQueue = { onAddToQueue(song) },
        )
    }
}

@Composable
private fun LocalSongRow(
    song: LocalAudio,
    isPlaying: Boolean,
    unknownArtist: String,
    showArtwork: Boolean,
    index: Int,
    onClick: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val accent = MaterialTheme.colorScheme.primary
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(onClick = onClick)
            .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (showArtwork) {
            Box {
                FeatureArtwork(model = song.artworkUri, size = 52.dp)
                if (isPlaying) {
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 6.dp, y = 6.dp)
                            .size(22.dp)
                            .baajaSurface(baajaInk, accent, CircleShape, shadowOffset = 0.dp, borderWidth = 1.5.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painterResource(R.drawable.graphic_eq),
                            stringResource(R.string.baaja_imp_now_playing),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                }
            }
        } else {
            Box(Modifier.width(32.dp), contentAlignment = Alignment.Center) {
                if (isPlaying) {
                    Icon(painterResource(R.drawable.graphic_eq), stringResource(R.string.baaja_imp_now_playing), tint = accent, modifier = Modifier.size(20.dp))
                } else {
                    Text(
                        "${index + 1}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isPlaying) accent else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOf(song.artist.ifBlank { unknownArtist }, song.album).filter { it.isNotBlank() }.joinToString(" · "),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            makeTimeString(song.durationMs),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box {
            Box(
                Modifier.size(width = 36.dp, height = 44.dp).clip(CircleShape).clickable(role = Role.Button) { menuOpen = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.more_vert),
                    stringResource(R.string.baaja_imp_more_options, song.title),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.play_next)) },
                    leadingIcon = { Icon(painterResource(R.drawable.playlist_play), null) },
                    onClick = {
                        menuOpen = false
                        onPlayNext()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.add_to_queue)) },
                    leadingIcon = { Icon(painterResource(R.drawable.queue_music), null) },
                    onClick = {
                        menuOpen = false
                        onAddToQueue()
                    },
                )
            }
        }
    }
}

@Composable
private fun PlayRow(
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        BaajaPillButton(
            text = stringResource(R.string.baaja_imp_play_all),
            onClick = onPlay,
            icon = R.drawable.play,
            height = 50.dp,
            modifier = Modifier.weight(1f),
        )
        BaajaPillButton(
            text = stringResource(R.string.shuffle),
            onClick = onShuffle,
            icon = R.drawable.shuffle,
            primary = false,
            height = 50.dp,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun GroupHeader(
    artworkUri: String?,
    isArtist: Boolean,
    subtitle: String,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FeatureArtwork(
                model = artworkUri,
                size = 112.dp,
                shape = if (isArtist) CircleShape else RoundedCornerShape(22.dp),
                shadowOffset = 4.dp,
                placeholder = if (isArtist) R.drawable.artist else R.drawable.album,
            )
            Text(
                subtitle,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        PlayRow(onPlay = onPlay, onShuffle = onShuffle)
    }
}

@Composable
private fun AlbumTile(
    album: LocalAlbum,
    unknownAlbum: String,
    unknownArtist: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BoxWithSquare {
            FeatureArtwork(
                model = album.artworkUri,
                size = it,
                shape = RoundedCornerShape(20.dp),
                shadowOffset = 3.dp,
                placeholder = R.drawable.album,
            )
        }
        Column(Modifier.padding(horizontal = 2.dp)) {
            Text(
                album.title.ifBlank { unknownAlbum },
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${album.artist.ifBlank { unknownArtist }} · ${pluralStringResource(R.plurals.baaja_imp_song_count, album.songs.size, album.songs.size)}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Gives [content] a square size equal to the available width (minus room for the offset shadow). */
@Composable
private fun BoxWithSquare(content: @Composable (androidx.compose.ui.unit.Dp) -> Unit) {
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(1f)) {
        content(maxWidth - 4.dp)
    }
}

@Composable
private fun ArtistRow(
    artist: LocalArtist,
    unknownArtist: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        FeatureArtwork(model = artist.artworkUri, size = 56.dp, shape = CircleShape, placeholder = R.drawable.artist)
        Column(Modifier.weight(1f)) {
            Text(
                artist.name.ifBlank { unknownArtist },
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                pluralStringResource(R.plurals.baaja_imp_song_count, artist.songs.size, artist.songs.size) +
                    " · " + pluralStringResource(R.plurals.baaja_imp_album_count, artist.albumCount, artist.albumCount),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Icon(painterResource(R.drawable.navigate_next), null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SortButton(
    sort: LocalSort,
    descending: Boolean,
    onSort: (LocalSort) -> Unit,
    onToggleDescending: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        BaajaIconButton(
            icon = R.drawable.tune,
            contentDescription = stringResource(R.string.baaja_imp_sort),
            onClick = { open = true },
            size = 52.dp,
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            LocalSort.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(
                                when (option) {
                                    LocalSort.TITLE -> R.string.baaja_imp_sort_title
                                    LocalSort.ARTIST -> R.string.baaja_imp_sort_artist
                                    LocalSort.ALBUM -> R.string.baaja_imp_sort_album
                                    LocalSort.DATE_ADDED -> R.string.baaja_imp_sort_date
                                },
                            ),
                            fontWeight = if (option == sort) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            painterResource(if (option == sort) R.drawable.radio_button_checked else R.drawable.radio_button_unchecked),
                            null,
                            tint = if (option == sort) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    onClick = {
                        onSort(option)
                        open = false
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.baaja_imp_sort_reverse)) },
                leadingIcon = {
                    Icon(
                        painterResource(if (descending) R.drawable.arrow_upward else R.drawable.arrow_downward),
                        null,
                    )
                },
                trailingIcon = { FeatureCheckbox(checked = descending, modifier = Modifier.size(22.dp)) },
                onClick = {
                    onToggleDescending()
                    open = false
                },
            )
        }
    }
}

@Composable
private fun PermissionCard(
    permanentlyDenied: Boolean,
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    BaajaCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FeatureArtwork(model = null, size = 56.dp, shape = RoundedCornerShape(18.dp), placeholder = R.drawable.library_music)
            Text(
                stringResource(R.string.baaja_imp_local_permission_title),
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                stringResource(
                    if (permanentlyDenied) R.string.baaja_imp_local_permission_denied else R.string.baaja_imp_local_permission_body,
                ),
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            BaajaPillButton(
                text =
                    stringResource(
                        if (permanentlyDenied) R.string.baaja_imp_local_permission_settings else R.string.baaja_imp_local_permission_allow,
                    ),
                onClick = if (permanentlyDenied) onOpenSettings else onAllow,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun EmptyLibrary(onRescan: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FeatureArtwork(model = null, size = 88.dp, shape = RoundedCornerShape(26.dp), shadowOffset = 4.dp, placeholder = R.drawable.music_note)
        Text(stringResource(R.string.baaja_imp_local_empty), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        Text(
            stringResource(R.string.baaja_imp_local_empty_body),
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        BaajaPillButton(
            text = stringResource(R.string.baaja_imp_local_rescan),
            onClick = onRescan,
            icon = R.drawable.refresh,
            primary = false,
            height = 50.dp,
        )
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
