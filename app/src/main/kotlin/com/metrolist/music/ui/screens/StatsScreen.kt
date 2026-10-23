/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.ui.screens.onboarding.BaajaSegmentedToggle
import com.metrolist.music.ui.theme.BaajaArtwork
import com.metrolist.music.ui.theme.BaajaCard
import com.metrolist.music.ui.theme.BaajaInk
import com.metrolist.music.ui.theme.BaajaYellow
import com.metrolist.music.ui.theme.baajaInk
import com.metrolist.music.ui.theme.baajaSurface
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.text.NumberFormat
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.metrolist.innertube.models.Artist
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.innertube.utils.parseCookieString
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.CONTENT_TYPE_ARTIST
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.constants.StatPeriod
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.component.ArtistListItem
import com.metrolist.music.ui.component.ChoiceChipsRow
import com.metrolist.music.ui.component.EmptyPlaceholder
import com.metrolist.music.ui.component.HideOnScrollFAB
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.LocalAlbumsGrid
import com.metrolist.music.ui.component.LocalArtistsGrid
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.LocalSongsGrid
import com.metrolist.music.ui.component.NavigationTitle
import com.metrolist.music.ui.component.PlaylistGridItem
import com.metrolist.music.ui.component.TimeTransfer
import com.metrolist.music.ui.menu.AlbumMenu
import com.metrolist.music.ui.menu.ArtistMenu
import com.metrolist.music.ui.menu.SongMenu
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.joinByBullet
import com.metrolist.music.utils.makeTimeString
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.StatsViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StatsScreen(
    navController: NavController,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val sArtists = viewModel.selectedArtists // SnapshotStateList<Artist>

// Helper actions:
    val toggleArtistSelection: (Artist) -> Unit = { artist ->
        if (sArtists.any { it.id == artist.id }) {
            sArtists.removeAll { it.id == artist.id }
        } else {
            sArtists.add(artist)
        }
    }

    val clearArtistSelection: () -> Unit = {
        sArtists.clear()
    }
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
        }
    }
    if (isSearching) {
        BackHandler {
            isSearching = false
            query = TextFieldValue()
        }
    }

    val indexChips by viewModel.indexChips.collectAsStateWithLifecycle()
    val mostPlayedSongs by viewModel.mostPlayedSongs.collectAsStateWithLifecycle()
    val mostPlayedSongsStats by viewModel.filteredSongs.collectAsStateWithLifecycle()
    val mostPlayedArtists by viewModel.filteredArtists.collectAsStateWithLifecycle()
    val mostPlayedAlbums by viewModel.filteredAlbums.collectAsStateWithLifecycle()
    val allArtists by viewModel.mostPlayedArtists.collectAsStateWithLifecycle()
    val firstEvent by viewModel.firstEvent.collectAsStateWithLifecycle()
    val weeklyMostPlaylist by viewModel.weeklyMostPlaylist.collectAsStateWithLifecycle()
    val monthlyMostPlaylist by viewModel.monthlyMostPlaylist.collectAsStateWithLifecycle()
    val recapPlaylists by viewModel.recapPlaylists.collectAsStateWithLifecycle()
    val currentDate = LocalDateTime.now()
    val orderedMostPlayedSongs =
        remember(mostPlayedSongsStats, mostPlayedSongs) {
            val songsById = mostPlayedSongs.associateBy { it.song.id }
            mostPlayedSongsStats.mapNotNull { statsSong -> songsById[statsSong.id] }
        }
    val mostPeriodPlaylists = listOfNotNull(weeklyMostPlaylist, monthlyMostPlaylist)
    val (innerTubeCookie) = rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn =
        remember(innerTubeCookie) {
            "SAPISID" in parseCookieString(innerTubeCookie)
        }
    val visibleStatsPlaylists =
        remember(mostPeriodPlaylists, recapPlaylists, isLoggedIn) {
            if (isLoggedIn) {
                (mostPeriodPlaylists + recapPlaylists).distinctBy { it.id }
            } else {
                mostPeriodPlaylists
            }
        }

    val database = LocalDatabase.current
    var chartRange by rememberSaveable { mutableStateOf(ListeningChartRange.WEEK) }
    val listeningChart by remember(chartRange, database) { listeningChartFlow(database, chartRange) }
        .collectAsStateWithLifecycle(null)
    var showAllArtists by rememberSaveable { mutableStateOf(false) }
    val periodTotalMs = remember(mostPlayedSongsStats) { mostPlayedSongsStats.sumOf { it.timeListened ?: 0L } }

    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val selectedOption by viewModel.selectedOption.collectAsStateWithLifecycle()

    var showTimeTransfer by rememberSaveable { mutableStateOf(false) }
    var prevOptionOrdinal by rememberSaveable { mutableStateOf<OptionStats?>(null) }
    var prevIndexChips by rememberSaveable { mutableStateOf<Int?>(null) }

    LaunchedEffect(showTimeTransfer) {
        if (showTimeTransfer) {
            if (prevOptionOrdinal == null) prevOptionOrdinal = selectedOption
            if (prevIndexChips == null) prevIndexChips = indexChips
            viewModel.selectedOption.value = OptionStats.CONTINUOUS // "throughout time" in your VM
            viewModel.indexChips.value = StatPeriod.ALL.ordinal // optional: ensure it’s actually “now -> throughout time”
        }
    }

    if (showTimeTransfer) {
        TimeTransfer(
            onDismiss = {
                showTimeTransfer = false
                prevOptionOrdinal?.let { viewModel.selectedOption.value = it }
                prevIndexChips?.let { viewModel.indexChips.value = it }

                // Clear snapshots for the next open
                prevOptionOrdinal = null
                prevIndexChips = null
            },
        )
    }

    LaunchedEffect(Unit) {
        viewModel.syncMostPlaylistsIfNeeded()
    }

    val weeklyDates =
        if (currentDate != null && firstEvent != null) {
            generateSequence(currentDate) { it.minusWeeks(1) }
                .takeWhile { it.isAfter(firstEvent?.event?.timestamp?.minusWeeks(1)) }
                .mapIndexed { index, date ->
                    val endDate = date.plusWeeks(1).minusDays(1).coerceAtMost(currentDate)
                    val formatter = DateTimeFormatter.ofPattern("dd MMM")

                    val startDateFormatted = formatter.format(date)
                    val endDateFormatted = formatter.format(endDate)

                    val startMonth = date.month
                    val endMonth = endDate.month
                    val startYear = date.year
                    val endYear = endDate.year

                    val text =
                        when {
                            startYear != currentDate.year -> "$startDateFormatted, $startYear - $endDateFormatted, $endYear"
                            startMonth != endMonth -> "$startDateFormatted - $endDateFormatted"
                            else -> "${date.dayOfMonth} - $endDateFormatted"
                        }
                    Pair(index, text)
                }.toList()
        } else {
            emptyList()
        }

    val monthlyDates =
        if (currentDate != null && firstEvent != null) {
            generateSequence(
                currentDate.plusMonths(1).withDayOfMonth(1).minusDays(1),
            ) { it.minusMonths(1) }
                .takeWhile {
                    it.isAfter(
                        firstEvent
                            ?.event
                            ?.timestamp
                            ?.withDayOfMonth(1),
                    )
                }.mapIndexed { index, date ->
                    val formatter = DateTimeFormatter.ofPattern("MMM")
                    val formattedDate = formatter.format(date)
                    val text =
                        if (date.year != currentDate.year) {
                            "$formattedDate ${date.year}"
                        } else {
                            formattedDate
                        }
                    Pair(index, text)
                }.toList()
        } else {
            emptyList()
        }

    val yearlyDates =
        if (currentDate != null && firstEvent != null) {
            generateSequence(
                currentDate
                    .plusYears(1)
                    .withDayOfYear(1)
                    .minusDays(1),
            ) { it.minusYears(1) }
                .takeWhile {
                    it.isAfter(
                        firstEvent
                            ?.event
                            ?.timestamp,
                    )
                }.mapIndexed { index, date ->
                    Pair(index, "${date.year}")
                }.toList()
        } else {
            emptyList()
        }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            contentPadding =
                LocalPlayerAwareWindowInsets.current
                    .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                    .asPaddingValues(),
            modifier =
                Modifier.windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top),
                ),
        ) {
            val filteredArtists =
                allArtists
                    .map { artistWrapper ->
                        Artist(
                            id = artistWrapper.artist.id,
                            name = artistWrapper.artist.name,
                        )
                    }.filter { artist ->
                        artist.name.contains(query.text, ignoreCase = true)
                    }

            item(key = "choice_chips") {
                ChoiceChipsRow(
                    chips =
                        when (selectedOption) {
                            OptionStats.WEEKS -> {
                                weeklyDates
                            }

                            OptionStats.MONTHS -> {
                                monthlyDates
                            }

                            OptionStats.YEARS -> {
                                yearlyDates
                            }

                            OptionStats.CONTINUOUS -> {
                                listOf(
                                    StatPeriod.WEEK_1.ordinal to
                                        pluralStringResource(
                                            R.plurals.n_week,
                                            1,
                                            1,
                                        ),
                                    StatPeriod.MONTH_1.ordinal to
                                        pluralStringResource(
                                            R.plurals.n_month,
                                            1,
                                            1,
                                        ),
                                    StatPeriod.MONTH_3.ordinal to
                                        pluralStringResource(
                                            R.plurals.n_month,
                                            3,
                                            3,
                                        ),
                                    StatPeriod.MONTH_6.ordinal to
                                        pluralStringResource(
                                            R.plurals.n_month,
                                            6,
                                            6,
                                        ),
                                    StatPeriod.YEAR_1.ordinal to
                                        pluralStringResource(
                                            R.plurals.n_year,
                                            1,
                                            1,
                                        ),
                                    StatPeriod.ALL.ordinal to stringResource(R.string.filter_all),
                                )
                            }
                        },
                    options =
                        listOf(
                            OptionStats.CONTINUOUS to stringResource(id = R.string.continuous),
                            OptionStats.WEEKS to stringResource(R.string.weeks),
                            OptionStats.MONTHS to stringResource(R.string.months),
                            OptionStats.YEARS to stringResource(R.string.years),
                        ),
                    selectedOption = selectedOption,
                    onSelectionChange = {
                        viewModel.selectedOption.value = it
                        viewModel.indexChips.value = 0
                    },
                    currentValue = indexChips,
                    onValueUpdate = { viewModel.indexChips.value = it },
                )
            }

            if (!isSearching) {
                item(key = "baaja_summary") {
                    StatsSummaryTiles(
                        periodTotalMs = periodTotalMs,
                        songCount = mostPlayedSongsStats.size,
                        topArtist = mostPlayedArtists.firstOrNull(),
                        onTopArtistClick = { artist -> navController.navigate("artist/${artist.id}") },
                        modifier = Modifier.animateItem(),
                    )
                }
                item(key = "baaja_chart") {
                    ListeningChartCard(
                        range = chartRange,
                        onRangeChange = { chartRange = it },
                        chart = listeningChart,
                        modifier = Modifier.animateItem(),
                    )
                }
            }

            if (visibleStatsPlaylists.isNotEmpty() && !isSearching && sArtists.isEmpty()) {
                item(key = "mostPeriodPlaylistsTitle") {
                    NavigationTitle(
                        title =
                            pluralStringResource(
                                R.plurals.n_playlist,
                                visibleStatsPlaylists.size,
                                visibleStatsPlaylists.size,
                            ),
                        modifier = Modifier.animateItem(),
                    )
                }

                item(key = "mostPeriodPlaylists") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        modifier = Modifier.animateItem(),
                    ) {
                        itemsIndexed(
                            items = visibleStatsPlaylists,
                            key = { _, playlist -> playlist.id },
                        ) { _, playlist ->
                            PlaylistGridItem(
                                playlist = playlist,
                                autoPlaylist = true,
                                modifier =
                                    Modifier
                                        .combinedClickable(
                                            onClick = {
                                                navController.navigate("local_playlist/${playlist.id}")
                                            },
                                        ).animateItem(),
                            )
                        }
                    }
                }
            }

            if (!isSearching) {
                item(key = "mostPlayedSongs") {
                    NavigationTitle(
                        title = "${mostPlayedSongsStats.size} ${stringResource(id = R.string.songs)}",
                        onPlayAllClick =
                            if (orderedMostPlayedSongs.isNotEmpty()) {
                                {
                                    playerConnection.playQueue(
                                        ListQueue(
                                            title = context.getString(R.string.most_played_songs),
                                            items = orderedMostPlayedSongs.map { it.toMediaMetadata().toMediaItem() },
                                        ),
                                    )
                                }
                            } else {
                                null
                            },
                        modifier = Modifier.animateItem(),
                    )

                    LazyRow(
                        modifier = Modifier.animateItem(),
                    ) {
                        itemsIndexed(
                            items = mostPlayedSongsStats,
                            key = { index, song -> "${song.id}_$index" },
                        ) { index, song ->
                            LocalSongsGrid(
                                title = "${index + 1}. ${song.title}",
                                subtitle =
                                    joinByBullet(
                                        pluralStringResource(
                                            R.plurals.n_time,
                                            song.songCountListened,
                                            song.songCountListened,
                                        ),
                                        makeTimeString(song.timeListened),
                                    ),
                                thumbnailUrl = song.thumbnailUrl,
                                isActive = song.id == mediaMetadata?.id,
                                isPlaying = isPlaying,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {
                                                if (song.id == mediaMetadata?.id) {
                                                    playerConnection.togglePlayPause()
                                                } else {
                                                    val targetSong = mostPlayedSongs.find { it.id == song.id }
                                                    if (targetSong != null) {
                                                        playerConnection.playQueue(
                                                            YouTubeQueue(
                                                                endpoint = WatchEndpoint(song.id),
                                                                preloadItem = targetSong.toMediaMetadata(),
                                                            ),
                                                        )
                                                    }
                                                }
                                            },
                                            onLongClick = {
                                                val targetSong = mostPlayedSongs.find { it.id == song.id }
                                                if (targetSong != null) {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        SongMenu(
                                                            originalSong = targetSong,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                }
                                            },
                                        ).animateItem(),
                            )
                        }
                    }
                }
            }

            if (!isSearching) {
                item(key = "mostPlayedArtists") {
                    NavigationTitle(
                        title = "${mostPlayedArtists.size} ${stringResource(id = R.string.artists)}",
                        modifier = Modifier.animateItem(),
                    )
                }
                val visibleArtists = if (showAllArtists) mostPlayedArtists else mostPlayedArtists.take(TOP_ARTISTS_PREVIEW)
                val maxArtistTime = mostPlayedArtists.firstOrNull()?.timeListened?.toLong()?.coerceAtLeast(1L) ?: 1L
                itemsIndexed(
                    items = visibleArtists,
                    key = { _, artist -> "stats_bar_artist_${artist.id}" },
                ) { index, artist ->
                    StatsArtistBarRow(
                        rank = index + 1,
                        artist = artist,
                        maxTimeListened = maxArtistTime,
                        modifier =
                            Modifier
                                .animateItem()
                                .combinedClickable(
                                    onClick = {
                                        navController.navigate("artist/${artist.id}")
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        menuState.show {
                                            ArtistMenu(
                                                originalArtist = artist,
                                                coroutineScope = coroutineScope,
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    },
                                ),
                    )
                }
                if (mostPlayedArtists.size > TOP_ARTISTS_PREVIEW) {
                    item(key = "mostPlayedArtistsToggle") {
                        Text(
                            text = stringResource(if (showAllArtists) R.string.show_less else R.string.show_more),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier =
                                Modifier
                                    .animateItem()
                                    .padding(horizontal = 12.dp)
                                    .clip(CircleShape)
                                    .clickable { showAllArtists = !showAllArtists }
                                    .padding(horizontal = 12.dp, vertical = 14.dp),
                        )
                    }
                }
            }

            if (!isSearching) {
                item(key = "mostPlayedAlbums") {
                    NavigationTitle(
                        title = "${mostPlayedAlbums.size} ${stringResource(id = R.string.albums)}",
                        modifier = Modifier.animateItem(),
                    )

                    if (mostPlayedAlbums.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier.animateItem(),
                        ) {
                            itemsIndexed(
                                items = mostPlayedAlbums,
                                key = { _, album -> album.id },
                            ) { index, album ->
                                LocalAlbumsGrid(
                                    title = "${index + 1}. ${album.album.title}",
                                    subtitle =
                                        joinByBullet(
                                            pluralStringResource(
                                                R.plurals.n_time,
                                                album.songCountListened ?: 0,
                                                album.songCountListened ?: 0,
                                            ),
                                            makeTimeString(album.timeListened),
                                        ),
                                    thumbnailUrl = album.album.thumbnailUrl,
                                    isActive = album.id == mediaMetadata?.album?.id,
                                    isPlaying = isPlaying,
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = {
                                                    navController.navigate("album/${album.id}")
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        AlbumMenu(
                                                            originalAlbum = album,
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
            }

            if (isSearching) {
                items(
                    items =
                        allArtists.filter { artist ->
                            artist.artist.name.contains(query.text, ignoreCase = true)
                        },
                    key = { "stats_artist_${it.id}" },
                    contentType = { CONTENT_TYPE_ARTIST },
                ) { artist ->
                    val uiArtist = Artist(name = artist.artist.name, id = artist.id)
                    val isChecked = sArtists.any { it.id == uiArtist.id }
                    Row( // Use a row to arrange the checkbox and ArtistListItem horizontally
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    toggleArtistSelection(uiArtist)
                                }.padding(8.dp),
                    ) {
                        ArtistListItem(
                            artist = artist,
                            modifier = Modifier.weight(1f), // Allow ArtistListItem to take remaining space
                        )

                        Checkbox(
                            checked = sArtists.contains(Artist(name = artist.artist.name, id = artist.id)), // Get the current checked state
                            onCheckedChange = {
                                toggleArtistSelection(uiArtist)
                            },
                        )
                    }
                }
            }
            if (query.text.isNotEmpty() && filteredArtists.isEmpty()) {
                item(key = "no_result") {
                    EmptyPlaceholder(
                        icon = R.drawable.search,
                        text = stringResource(R.string.no_results_found),
                    )
                }
            }
        }
        // FAB to shuffle most played songs
        if (mostPlayedSongsStats.isNotEmpty() && !isSearching) {
            HideOnScrollFAB(
                visible = true,
                lazyListState = lazyListState,
                icon = R.drawable.shuffle,
                onClick = {
                    playerConnection.playQueue(
                        ListQueue(
                            title = context.getString(R.string.most_played_songs),
                            items = orderedMostPlayedSongs.map { it.toMediaMetadata().toMediaItem() }.shuffled(),
                        ),
                    )
                },
            )
        }
    }

    TopAppBar(
        title = {
            if (isSearching) {
                Row {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.search),
                                style = MaterialTheme.typography.titleLarge,
                            )
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleLarge,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        colors =
                            TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                            ),
                        modifier =
                            Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                    )

                    if (sArtists.isNotEmpty()) {
                        androidx.compose.material3.IconButton(onClick = clearArtistSelection) {
                            Icon(
                                painter = painterResource(R.drawable.close),
                                contentDescription = stringResource(R.string.baaja_onb_stats_clear_artists),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            } else {
                Text(
                    stringResource(R.string.baaja_onb_stats_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
        navigationIcon = {
            IconButton(
                onClick = {
                    if (isSearching) {
                        isSearching = false
                        query = TextFieldValue()
                    } else {
                        navController.navigateUp()
                    }
                },
                onLongClick = {
                    if (!isSearching) {
                        navController.backToMain()
                    }
                },
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = stringResource(R.string.back_button_desc),
                )
            }
        },
        actions = {
            if (!isSearching) {
                androidx.compose.material3.IconButton(
                    onClick = { isSearching = true },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.search),
                        contentDescription = stringResource(R.string.baaja_onb_stats_search),
                    )
                }
                androidx.compose.material3.IconButton(onClick = { showTimeTransfer = true }) {
                    Icon(
                        painter = painterResource(R.drawable.sync),
                        contentDescription = stringResource(R.string.baaja_onb_stats_time_transfer),
                    )
                }
            }
        },
    )
}

enum class OptionStats { WEEKS, MONTHS, YEARS, CONTINUOUS }

private const val TOP_ARTISTS_PREVIEW = 8

/** Time window for the listening-time bar chart. */
enum class ListeningChartRange { WEEK, MONTH, YEAR }

private data class ListeningBucket(
    val shortLabel: String,
    val fullLabel: String,
    val start: LocalDateTime,
    val end: LocalDateTime,
)

@Immutable
private data class ListeningChart(
    val bars: List<Pair<ListeningBucket, Long>>,
    val totalMinutes: Long,
    val previousTotalMinutes: Long,
)

private fun listeningBuckets(
    range: ListeningChartRange,
    now: LocalDateTime,
): List<ListeningBucket> {
    val locale = Locale.getDefault()
    val today = now.toLocalDate()
    return when (range) {
        ListeningChartRange.WEEK -> {
            (6 downTo 0).map { back ->
                val day = today.minusDays(back.toLong())
                ListeningBucket(
                    shortLabel = day.dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
                    fullLabel = day.dayOfWeek.getDisplayName(TextStyle.FULL, locale),
                    start = day.atStartOfDay(),
                    end = day.plusDays(1).atStartOfDay(),
                )
            }
        }

        ListeningChartRange.MONTH -> {
            val formatter = DateTimeFormatter.ofPattern("d MMM", locale)
            (3 downTo 0).map { back ->
                val first = today.minusDays(back * 7L + 6)
                ListeningBucket(
                    shortLabel = formatter.format(first),
                    fullLabel = "${formatter.format(first)} – ${formatter.format(first.plusDays(6))}",
                    start = first.atStartOfDay(),
                    end = first.plusDays(7).atStartOfDay(),
                )
            }
        }

        ListeningChartRange.YEAR -> {
            val thisMonth = today.withDayOfMonth(1)
            (11 downTo 0).map { back ->
                val month = thisMonth.minusMonths(back.toLong())
                ListeningBucket(
                    shortLabel = month.month.getDisplayName(TextStyle.NARROW_STANDALONE, locale),
                    fullLabel = "${month.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)} ${month.year}",
                    start = month.atStartOfDay(),
                    end = month.plusMonths(1).atStartOfDay(),
                )
            }
        }
    }
}

/** Listening time per bucket from the recorded play events, plus the total for the window before it. */
private fun listeningChartFlow(
    database: MusicDatabase,
    range: ListeningChartRange,
): Flow<ListeningChart> {
    val buckets = listeningBuckets(range, LocalDateTime.now())
    val windowStart = buckets.first().start
    val previousStart =
        when (range) {
            ListeningChartRange.WEEK -> windowStart.minusDays(7)
            ListeningChartRange.MONTH -> windowStart.minusDays(28)
            ListeningChartRange.YEAR -> windowStart.minusMonths(12)
        }
    val bucketFlows =
        buckets.map { bucket ->
            database.getTotalPlayTimeInRange(bucket.start, bucket.end.minusNanos(1_000_000)).map { it ?: 0L }
        }
    val previousFlow = database.getTotalPlayTimeInRange(previousStart, windowStart.minusNanos(1_000_000)).map { it ?: 0L }
    return combine(bucketFlows + previousFlow) { totals ->
        val minutes = totals.map { it / 60_000L }
        ListeningChart(
            bars = buckets.zip(minutes.dropLast(1)),
            totalMinutes = totals.dropLast(1).sum() / 60_000L,
            previousTotalMinutes = minutes.last(),
        )
    }
}

@Composable
private fun StatsSummaryTiles(
    periodTotalMs: Long,
    songCount: Int,
    topArtist: com.metrolist.music.db.entities.Artist?,
    onTopArtistClick: (com.metrolist.music.db.entities.Artist) -> Unit,
    modifier: Modifier = Modifier,
) {
    val numberFormat = remember { NumberFormat.getIntegerInstance() }
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            Modifier
                .weight(1f)
                .heightIn(min = 118.dp)
                .baajaSurface(baajaInk, BaajaYellow, RoundedCornerShape(22.dp), shadowOffset = 3.dp)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(stringResource(R.string.baaja_onb_stats_minutes_listened), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = BaajaInk)
            Text(
                numberFormat.format(periodTotalMs / 60_000L),
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = BaajaInk,
                maxLines = 1,
            )
            Text(pluralStringResource(R.plurals.n_song, songCount, songCount), fontSize = 13.sp, color = BaajaInk)
        }
        Column(
            Modifier
                .weight(1f)
                .heightIn(min = 118.dp)
                .baajaSurface(baajaInk, Color(0xFFBDEFE6), RoundedCornerShape(22.dp), shadowOffset = 3.dp)
                .then(if (topArtist != null) Modifier.clickable { onTopArtistClick(topArtist) } else Modifier)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(stringResource(R.string.baaja_onb_stats_top_artist), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = BaajaInk)
            Text(
                topArtist?.artist?.name ?: stringResource(R.string.baaja_onb_stats_top_artist_none),
                fontSize = 22.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = BaajaInk,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val share =
                if (topArtist != null && periodTotalMs > 0) {
                    ((topArtist.timeListened ?: 0).toLong() * 100 / periodTotalMs).toInt().coerceIn(0, 100)
                } else {
                    null
                }
            if (share != null) {
                Text(stringResource(R.string.baaja_onb_stats_top_artist_share, share), fontSize = 13.sp, color = BaajaInk)
            }
        }
    }
}

@Composable
private fun ListeningChartCard(
    range: ListeningChartRange,
    onRangeChange: (ListeningChartRange) -> Unit,
    chart: ListeningChart?,
    modifier: Modifier = Modifier,
) {
    val numberFormat = remember { NumberFormat.getIntegerInstance() }
    var selected by rememberSaveable(range) { mutableStateOf(-1) }
    val bars = chart?.bars.orEmpty()
    val selectedIndex = if (selected in bars.indices) selected else bars.lastIndex
    val maxMinutes = (bars.maxOfOrNull { it.second } ?: 0L).coerceAtLeast(1L)
    val ranges = ListeningChartRange.entries
    val ink = baajaInk

    BaajaCard(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BaajaSegmentedToggle(
                options =
                    listOf(
                        stringResource(R.string.baaja_onb_stats_range_week),
                        stringResource(R.string.baaja_onb_stats_range_month),
                        stringResource(R.string.baaja_onb_stats_range_year),
                    ),
                selectedIndex = ranges.indexOf(range),
                onSelect = { onRangeChange(ranges[it]) },
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(
                        when (range) {
                            ListeningChartRange.WEEK -> R.string.baaja_onb_stats_minutes_per_day
                            ListeningChartRange.MONTH -> R.string.baaja_onb_stats_minutes_per_week
                            ListeningChartRange.YEAR -> R.string.baaja_onb_stats_minutes_per_month
                        },
                    ),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                bars.getOrNull(selectedIndex)?.let { (bucket, minutes) ->
                    Text(
                        stringResource(R.string.baaja_onb_stats_bar_label, bucket.fullLabel, numberFormat.format(minutes)),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 8.dp).weight(1f, fill = false),
                    )
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                bars.forEachIndexed { index, (bucket, minutes) ->
                    val fraction = (minutes.toFloat() / maxMinutes).coerceIn(0.02f, 1f)
                    val description = stringResource(R.string.baaja_onb_stats_bar_label, bucket.fullLabel, numberFormat.format(minutes))
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { selected = index }
                            .semantics { contentDescription = description },
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(
                            Modifier
                                .widthIn(max = 30.dp)
                                .fillMaxWidth()
                                .fillMaxHeight(fraction)
                                .then(
                                    if (index == selectedIndex) {
                                        Modifier.border(2.dp, ink, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    } else {
                                        Modifier
                                    },
                                ).clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(
                                    if (index == selectedIndex) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                                    },
                                ),
                        )
                    }
                }
            }
            HorizontalDivider(thickness = 1.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                bars.forEach { (bucket, _) ->
                    Text(
                        bucket.shortLabel,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (chart != null) {
                val previous = chart.previousTotalMinutes
                val deltaText =
                    if (previous <= 0L) {
                        stringResource(R.string.baaja_onb_stats_vs_previous_none)
                    } else {
                        val pct = ((chart.totalMinutes - previous) * 100 / previous).toInt()
                        if (pct >= 0) {
                            stringResource(R.string.baaja_onb_stats_vs_previous_up, pct)
                        } else {
                            stringResource(R.string.baaja_onb_stats_vs_previous_down, -pct)
                        }
                    }
                Text(
                    "${stringResource(R.string.baaja_onb_stats_minutes_short, numberFormat.format(chart.totalMinutes))} · $deltaText",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatsArtistBarRow(
    rank: Int,
    artist: com.metrolist.music.db.entities.Artist,
    maxTimeListened: Long,
    modifier: Modifier = Modifier,
) {
    val numberFormat = remember { NumberFormat.getIntegerInstance() }
    val time = (artist.timeListened ?: 0).toLong()
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("$rank", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.width(24.dp))
        BaajaArtwork(
            model = artist.artist.thumbnailUrl,
            shape = CircleShape,
            shadowOffset = 0.dp,
            modifier = Modifier.size(46.dp),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    artist.artist.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    stringResource(R.string.baaja_onb_stats_minutes_short, numberFormat.format(time / 60_000L)),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth((time.toFloat() / maxTimeListened).coerceIn(0.02f, 1f))
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}
