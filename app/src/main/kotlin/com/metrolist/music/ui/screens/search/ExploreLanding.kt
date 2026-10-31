/**
 * Baaja Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.search

import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.ArtistItem
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.ContentScale
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metrolist.music.ui.component.home.BaajaYourArtistsRow
import com.metrolist.music.ui.component.home.rememberTasteArtists
import com.metrolist.music.ui.component.home.BaajaCoverCard
import com.metrolist.music.ui.component.home.TasteCard
import coil3.compose.AsyncImage
import com.metrolist.music.ui.utils.resize
import com.metrolist.music.viewmodels.ChartsViewModel
import com.metrolist.music.viewmodels.NewReleaseViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.innertube.pages.MoodAndGenres
import com.metrolist.music.LocalNavController
import com.metrolist.music.R
import com.metrolist.music.db.entities.SearchHistory
import com.metrolist.music.ui.component.home.BaajaBlockArt
import com.metrolist.music.ui.component.home.BaajaHomeGutter
import com.metrolist.music.ui.component.home.BaajaHomeSectionTitle
import com.metrolist.music.ui.component.home.BaajaRecentChip
import com.metrolist.music.ui.component.shimmer.ShimmerHost
import com.metrolist.music.ui.component.shimmer.TextPlaceholder
import com.metrolist.music.ui.screens.MoodAndGenresButton
import com.metrolist.music.ui.screens.MoodAndGenresButtonHeight
import com.metrolist.music.ui.theme.baajaInk
import com.metrolist.music.ui.theme.baajaOutline

private const val EXPLORE_MOOD_COLUMNS = 3
private const val EXPLORE_MOOD_COUNT = 9
private const val EXPLORE_COVER_COUNT = 4

/** Explore landing shown on the Search tab before the user starts typing. */
@Composable
fun ExploreLanding(
    recentQueries: List<SearchHistory>,
    onRecentClick: (String) -> Unit,
    moods: List<MoodAndGenres.Item>?,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val navController = LocalNavController.current
    val chartsViewModel: ChartsViewModel = hiltViewModel()
    val newReleaseViewModel: NewReleaseViewModel = hiltViewModel()
    val chartsPage by chartsViewModel.chartsPage.collectAsStateWithLifecycle()
    val newReleases by newReleaseViewModel.newReleaseAlbums.collectAsStateWithLifecycle()
    // Loaded once per back-stack entry; the ViewModel keeps the result so re-opening Explore is instant.
    // A first load that fails (slow network, YouTube hiccup) is retried a few times instead of leaving a blank tile.
    LaunchedEffect(chartsViewModel) {
        var attempt = 0
        while (chartsViewModel.chartsPage.value == null && attempt < 4) {
            chartsViewModel.loadCharts()
            delay(if (attempt == 0) 8_000L else 5_000L)
            attempt++
        }
    }
    // New releases can also come back empty on the first try; ask again a few times.
    LaunchedEffect(newReleaseViewModel) {
        var attempt = 0
        while (newReleaseViewModel.newReleaseAlbums.value.isEmpty() && attempt < 4) {
            delay(8_000L)
            if (newReleaseViewModel.newReleaseAlbums.value.isEmpty()) newReleaseViewModel.load()
            attempt++
        }
    }
    // Music only: the top artists' photos (or songs' covers), never YouTube's chart art that says "VIDEOS".
    val chartCovers =
        remember(chartsPage) {
            chartsPage
                ?.sections
                .orEmpty()
                .flatMap { it.items }
                .filter { it is ArtistItem || (it is SongItem && !it.isVideoSong) }
                .mapNotNull { it.thumbnail }
                .distinct()
                .take(EXPLORE_COVER_COUNT)
        }
    val newReleaseCovers =
        remember(newReleases) {
            newReleases.mapNotNull { it.thumbnail }.distinct().take(EXPLORE_COVER_COUNT)
        }
    LazyColumn(
        // Extra room at the end so the last mood row can scroll clear of the mini player and the bottom bar.
        contentPadding =
            PaddingValues(
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding() + 96.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        if (recentQueries.isNotEmpty()) {
            item(key = "explore_recent") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = BaajaHomeGutter),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(recentQueries.take(12), key = { "explore_recent_${it.query}" }) { history ->
                        BaajaRecentChip(
                            query = history.query,
                            onClick = { onRecentClick(history.query) },
                        )
                    }
                }
            }
        }

        item(key = "explore_tiles") {
            Column(
                modifier = Modifier.padding(horizontal = BaajaHomeGutter),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ExploreFeatureTile(
                    badge = stringResource(R.string.charts),
                    title = stringResource(R.string.baaja_home_top_charts),
                    subtitle = stringResource(R.string.baaja_home_top_charts_sub),
                    seed = 5,
                    covers = chartCovers,
                    onClick = { navController.navigate("charts_screen") },
                )
                ExploreFeatureTile(
                    badge = stringResource(R.string.baaja_explore_new_badge),
                    title = stringResource(R.string.baaja_home_new_releases),
                    subtitle = stringResource(R.string.baaja_explore_new_releases_sub),
                    seed = 2,
                    covers = newReleaseCovers,
                    onClick = { navController.navigate("new_release") },
                )
            }
        }

        item(key = "explore_moods_title") {
            BaajaHomeSectionTitle(
                title = stringResource(R.string.baaja_home_moods_genres),
                actionLabel = stringResource(R.string.baaja_home_see_all),
                onAction = { navController.navigate("mood_and_genres") },
            )
        }

        if (moods == null) {
            item(key = "explore_moods_loading") {
                ShimmerHost {
                    repeat(3) {
                        Row(
                            modifier = Modifier.padding(horizontal = BaajaHomeGutter, vertical = 5.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            repeat(EXPLORE_MOOD_COLUMNS) {
                                TextPlaceholder(
                                    height = MoodAndGenresButtonHeight,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        } else {
            moods.take(EXPLORE_MOOD_COUNT).chunked(EXPLORE_MOOD_COLUMNS).forEachIndexed { rowIndex, row ->
                item(key = "explore_moods_row_$rowIndex") {
                    Row(
                        modifier = Modifier.padding(horizontal = BaajaHomeGutter),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        row.forEach { mood ->
                            MoodAndGenresButton(
                                title = mood.title,
                                stripeColor = mood.stripeColor,
                                onClick = {
                                    navController.navigate(
                                        "youtube_browse/${mood.endpoint.browseId}?params=${mood.endpoint.params}",
                                    )
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(EXPLORE_MOOD_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExploreFeatureTile(
    badge: String,
    title: String,
    subtitle: String,
    seed: Int,
    covers: List<String>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = baajaInk
    val shape = RoundedCornerShape(26.dp)
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(172.dp)
                .baajaOutline(ink, shape, borderWidth = 2.5.dp, shadowOffset = 4.dp)
                .clip(shape)
                .clickable(role = Role.Button, onClick = onClick),
    ) {
        // The illustration is the placeholder; real covers fade in over it once they load.
        BaajaBlockArt(seed = seed, modifier = Modifier.fillMaxSize())
        if (covers.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxSize()) {
                covers.forEachIndexed { index, url ->
                    if (index > 0) {
                        Box(Modifier.fillMaxHeight().width(2.dp).background(ink))
                    }
                    AsyncImage(
                        model = url.resize(400, 400),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
        }
        Box(
            modifier =
                Modifier
                    .padding(14.dp)
                    .height(30.dp)
                    .border(1.5.dp, ink, CircleShape)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary)
                    .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = badge,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondary,
            )
        }
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .drawBehind {
                        drawLine(ink, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 2.dp.toPx())
                    }.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
