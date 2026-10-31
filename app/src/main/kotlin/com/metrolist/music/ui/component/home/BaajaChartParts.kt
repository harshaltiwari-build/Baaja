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
import com.metrolist.music.ui.theme.baajaInk
import com.metrolist.music.ui.theme.baajaOutline

/** A row of chart playlists, as on YouTube Music's Charts page (Trending 20, Top 100…). */
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
