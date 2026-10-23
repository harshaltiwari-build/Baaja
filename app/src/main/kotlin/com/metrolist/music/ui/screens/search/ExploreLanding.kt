/**
 * Baaja Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.search

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    LazyColumn(
        contentPadding = contentPadding,
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
                    onClick = { navController.navigate("charts_screen") },
                )
                ExploreSmallTile(
                    label = stringResource(R.string.baaja_home_new_releases),
                    seed = 2,
                    onClick = { navController.navigate("new_release") },
                    modifier = Modifier.fillMaxWidth(),
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
        BaajaBlockArt(seed = seed, modifier = Modifier.fillMaxSize())
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

@Composable
private fun ExploreSmallTile(
    label: String,
    seed: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = baajaInk
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier =
            modifier
                .height(128.dp)
                .baajaOutline(ink, shape, borderWidth = 2.5.dp, shadowOffset = 3.dp)
                .clip(shape)
                .clickable(role = Role.Button, onClick = onClick),
    ) {
        BaajaBlockArt(seed = seed, modifier = Modifier.fillMaxSize())
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .height(34.dp)
                    .border(2.dp, ink, CircleShape)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
