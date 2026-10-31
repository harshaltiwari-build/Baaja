package com.metrolist.music.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.AppBarHeight
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.ui.component.home.BaajaBrowseTopBar
import com.metrolist.music.ui.component.home.BaajaErrorState
import com.metrolist.music.ui.component.home.BaajaHomeGutter
import com.metrolist.music.ui.component.home.BaajaLoadingLabel
import com.metrolist.music.ui.component.home.BaajaYouTubeRow
import com.metrolist.music.ui.component.home.musicChartTitle
import com.metrolist.music.ui.menu.YouTubeSongMenu
import com.metrolist.music.ui.theme.BaajaPillButton
import com.metrolist.music.viewmodels.ChartSongsViewModel

/** A YouTube chart as audio songs, in chart order, in Baaja's style (music only, no videos). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChartSongsScreen(
    navController: NavController,
    title: String,
    viewModel: ChartSongsViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val hasError by viewModel.hasError.collectAsStateWithLifecycle()
    val chartTitle = musicChartTitle(title)

    fun play(list: List<SongItem>, start: Int) =
        playerConnection.playQueue(ListQueue(title = chartTitle, items = list.map { it.toMediaItem() }, startIndex = start))

    if (hasError && songs.isNullOrEmpty()) {
        BaajaErrorState(
            title = stringResource(R.string.baaja_charts_error_title),
            body = stringResource(R.string.baaja_charts_error_body),
            onRetry = viewModel::load,
            modifier = Modifier.padding(top = AppBarHeight),
        )
    } else {
        LazyColumn(contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()) {
            item(key = "chart_header") {
                Spacer(Modifier.height(AppBarHeight))
                Text(
                    text = chartTitle,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = BaajaHomeGutter),
                )
                Text(
                    text =
                        songs?.let { pluralStringResource(R.plurals.baaja_chart_songs_count, it.size, it.size) }
                            ?: stringResource(R.string.baaja_chart_finding_songs),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = BaajaHomeGutter, vertical = 4.dp),
                )
                songs?.takeIf { it.isNotEmpty() }?.let { list ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = BaajaHomeGutter, vertical = 12.dp),
                    ) {
                        BaajaPillButton(text = stringResource(R.string.play_all), icon = R.drawable.play, onClick = { play(list, 0) }, modifier = Modifier.weight(1f), height = 48.dp)
                        BaajaPillButton(
                            text = stringResource(R.string.shuffle),
                            icon = R.drawable.shuffle,
                            primary = false,
                            onClick = { play(list.shuffled(), 0) },
                            modifier = Modifier.weight(1f),
                            height = 48.dp,
                        )
                    }
                }
            }
            val list = songs
            if (list == null) {
                item(key = "chart_loading") { BaajaLoadingLabel() }
            } else {
                itemsIndexed(list, key = { _, song -> "chart_song_${song.id}" }) { index, song ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = BaajaHomeGutter - 4.dp)) {
                        Text(
                            text = "${index + 1}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(28.dp),
                        )
                        BaajaYouTubeRow(
                            item = song,
                            isActive = song.id == mediaMetadata?.id,
                            isPlaying = isPlaying,
                            isSwipeable = false,
                            modifier =
                                Modifier.weight(1f).combinedClickable(
                                    onClick = { if (song.id == mediaMetadata?.id) playerConnection.togglePlayPause() else play(list, index) },
                                    onLongClick = { menuState.show { YouTubeSongMenu(song = song, onDismiss = menuState::dismiss) } },
                                ),
                        )
                    }
                }
            }
        }
    }

    BaajaBrowseTopBar(title = chartTitle, onBack = navController::navigateUp)
}
