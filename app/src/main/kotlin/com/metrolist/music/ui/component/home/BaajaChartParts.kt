package com.metrolist.music.ui.component.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.music.R
import com.metrolist.music.ui.component.BaajaTileColors
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import com.metrolist.music.ui.theme.baajaSurface
import com.metrolist.music.ui.theme.baajaInk
import com.metrolist.music.ui.theme.baajaOutline

/**
 * YouTube's chart names, for a music-only app: its India charts are lists of songs' music videos, which
 * Baaja plays as audio, so "Top 100 Music Videos India" reads "Top 100 Songs India".
 */
internal fun musicChartTitle(title: String): String =
    title
        .replace(Regex("(?i)\\bvideo charts\\b"), "Song charts")
        .replace(Regex("(?i)\\b(music\\s+)?videos?\\b"), "Songs")
        .replace(Regex("\\s+"), " ")
        .trim()

/**
 * YouTube publishes a real weekly song chart for India, the one YouTube Music shows as "Top 100 Songs India",
 * but leaves it out of the charts page it sends. In India it takes the place of the music-video Top 100.
 */
internal fun songChartTiles(
    playlists: List<PlaylistItem>,
    country: String?,
): List<PlaylistItem> {
    if (!country.equals("IN", ignoreCase = true) || playlists.none { musicChartTitle(it.title) == INDIA_TOP_100.title }) return playlists
    return listOf(INDIA_TOP_100) + playlists.filterNot { musicChartTitle(it.title) == INDIA_TOP_100.title }
}

private val INDIA_TOP_100 =
    PlaylistItem(
        id = "PL4fGSI1pDJn4pTWyM3t61lOyZ6_4jcNOw",
        title = "Top 100 Songs India",
        author = null,
        songCountText = null,
        thumbnail = null,
        playEndpoint = null,
        shuffleEndpoint = null,
        radioEndpoint = null,
    )

/**
 * Chart playlists as Baaja tiles: pastel, ink-outlined, with the song chart name. YouTube's own chart
 * artwork says "VIDEOS", which doesn't fit a music-only app.
 */
@Composable
fun BaajaChartTileRow(
    playlists: List<PlaylistItem>,
    onOpen: (PlaylistItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tints = listOf(BaajaTileColors.lilac(), BaajaTileColors.peach(), BaajaTileColors.mint(), BaajaTileColors.sky(), BaajaTileColors.liked())
    LazyRow(
        contentPadding = PaddingValues(horizontal = BaajaHomeGutter, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        items(playlists.size, key = { "chart_tile_${playlists[it].id}" }) { index ->
            val playlist = playlists[index]
            Box(
                modifier =
                    Modifier
                        .width(156.dp)
                        .height(112.dp)
                        .baajaSurface(baajaInk, tints[index % tints.size], RoundedCornerShape(20.dp), shadowOffset = 3.dp)
                        .clickable(role = Role.Button, onClick = { onOpen(playlist) })
                        .padding(14.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.trending_up),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp).align(Alignment.TopEnd),
                )
                Text(
                    text = musicChartTitle(playlist.title),
                    fontSize = 16.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.BottomStart).padding(end = 8.dp),
                )
            }
        }
    }
}

/** A row of playlists with their artwork, e.g. New releases' featured playlist. */
@Composable
fun BaajaChartPlaylistRow(
    playlists: List<PlaylistItem>,
    onOpen: (PlaylistItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = BaajaHomeGutter, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        items(playlists, key = { "chart_playlist_${it.id}" }) { playlist ->
            BaajaYouTubeCard(item = playlist, onClick = { onOpen(playlist) }, onLongClick = { onOpen(playlist) })
        }
    }
}

/** YouTube Music's ranked Top artists: rank, round photo, name and subscribers. */
@Composable
fun BaajaChartArtistList(
    artists: List<ArtistItem>,
    onOpen: (ArtistItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        artists.forEachIndexed { index, artist ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button, onClick = { onOpen(artist) })
                        .padding(horizontal = BaajaHomeGutter, vertical = 8.dp),
            ) {
                Text(
                    text = (artist.chartPosition ?: index + 1).toString(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(32.dp),
                )
                AsyncImage(
                    model = artist.thumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .size(56.dp)
                            .baajaOutline(baajaInk, CircleShape, borderWidth = 2.dp, shadowOffset = 0.dp)
                            .clip(CircleShape),
                )
                Column(Modifier.weight(1f)) {
                    Text(artist.title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    artist.subscriberText?.let {
                        Text(it, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
