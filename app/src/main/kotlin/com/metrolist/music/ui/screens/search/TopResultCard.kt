/**
 * Baaja Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.EpisodeItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.PodcastItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.YTItem
import com.metrolist.music.R
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.PlayerConnection
import com.metrolist.music.playback.queues.YouTubeAlbumRadio
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.component.home.BaajaArtworkBox
import com.metrolist.music.ui.component.home.BaajaHomeGutter
import com.metrolist.music.ui.component.home.baajaYouTubeSubtitle
import com.metrolist.music.ui.component.home.baajaYouTubeTitle
import com.metrolist.music.ui.theme.BaajaCard
import com.metrolist.music.ui.theme.BaajaIconButton
import com.metrolist.music.ui.theme.BaajaPillButton
import com.metrolist.music.ui.theme.baajaInk
import com.metrolist.music.ui.theme.baajaSurface

/** "Top result" card from the Results mockup: big artwork, title, kind, play / radio / more actions. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TopResultCard(
    item: YTItem,
    onOpen: () -> Unit,
    onMore: () -> Unit,
    onPlay: (() -> Unit)?,
    onRadio: (() -> Unit)?,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
) {
    val title = baajaYouTubeTitle(item)
    val subtitle =
        if (item is ArtistItem) stringResource(R.string.baaja_home_kind_artist) else baajaYouTubeSubtitle(item)
    BaajaCard(
        modifier =
            modifier
                .padding(horizontal = BaajaHomeGutter, vertical = 6.dp)
                .fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(R.string.top_result).uppercase(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .combinedClickable(role = Role.Button, onClick = onOpen, onLongClick = onMore),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                BaajaArtworkBox(
                    thumbnailUrl = item.thumbnail,
                    shape = if (item is ArtistItem) CircleShape else RoundedCornerShape(18.dp),
                    isActive = isActive,
                    isPlaying = isPlaying,
                    shadowOffset = 0.dp,
                    borderWidth = 2.5.dp,
                    modifier = Modifier.size(76.dp),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = title,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onPlay != null) {
                    Box(
                        modifier =
                            Modifier
                                .size(50.dp)
                                .baajaSurface(baajaInk, MaterialTheme.colorScheme.primary, CircleShape, shadowOffset = 2.dp)
                                .clickable(role = Role.Button, onClick = onPlay),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(if (isActive && isPlaying) R.drawable.pause else R.drawable.play),
                            contentDescription = stringResource(R.string.baaja_home_play_named, title),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                if (onRadio != null) {
                    BaajaPillButton(
                        text = stringResource(R.string.radio),
                        onClick = onRadio,
                        primary = false,
                        icon = R.drawable.radio,
                        height = 50.dp,
                    )
                }
                BaajaIconButton(
                    icon = R.drawable.more_horiz,
                    contentDescription = stringResource(R.string.baaja_home_more_options_for, title),
                    onClick = onMore,
                    size = 50.dp,
                )
            }
        }
    }
}

/** What the top result's play button does; songs and episodes reuse the normal tap behaviour. */
fun topResultPlayAction(
    item: YTItem,
    playerConnection: PlayerConnection,
    openItem: () -> Unit,
): (() -> Unit)? =
    when (item) {
        is SongItem, is EpisodeItem -> openItem
        is ArtistItem -> (item.shuffleEndpoint ?: item.playEndpoint)?.let { endpoint -> { playerConnection.playQueue(YouTubeQueue(endpoint)) } }
        is AlbumItem -> { { playerConnection.playQueue(YouTubeAlbumRadio(item.playlistId)) } }
        is PlaylistItem -> item.playEndpoint?.let { endpoint -> { playerConnection.playQueue(YouTubeQueue(endpoint)) } }
        is PodcastItem -> item.playEndpoint?.let { endpoint -> { playerConnection.playQueue(YouTubeQueue(endpoint)) } }
    }

/** Radio for the top result, when YouTube offers one. */
fun topResultRadioAction(
    item: YTItem,
    playerConnection: PlayerConnection,
): (() -> Unit)? =
    when (item) {
        is SongItem -> { { playerConnection.playQueue(YouTubeQueue.radio(item.toMediaMetadata())) } }
        is ArtistItem -> item.radioEndpoint?.let { endpoint -> { playerConnection.playQueue(YouTubeQueue(endpoint)) } }
        is PlaylistItem -> item.radioEndpoint?.let { endpoint -> { playerConnection.playQueue(YouTubeQueue(endpoint)) } }
        else -> null
    }
