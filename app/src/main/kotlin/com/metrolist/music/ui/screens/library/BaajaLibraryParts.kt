/**
 * Baaja (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Baaja look for the Library tab: outlined filter chips, the sort/search/layout toolbar and the
 * collection tiles (Liked songs, Downloaded, cached, top, uploaded, device music, imports).
 */

package com.metrolist.music.ui.screens.library

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import com.metrolist.music.ui.theme.baajaEntrance
import com.metrolist.music.ui.theme.rememberBaajaEntranceKey
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.exoplayer.offline.Download
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalDownloadUtil
import com.metrolist.music.R
import com.metrolist.music.constants.LibraryFilter
import com.metrolist.music.constants.LibraryViewType
import com.metrolist.music.ui.component.BaajaTileColors
import com.metrolist.music.ui.screens.playlist.BaajaSortPill
import com.metrolist.music.ui.theme.BaajaChip
import com.metrolist.music.ui.theme.BaajaIconButton
import com.metrolist.music.ui.theme.baajaInk
import com.metrolist.music.ui.theme.baajaOutline
import com.metrolist.music.ui.theme.baajaSurface

/** Routes owned by other parts of Baaja that the Library links to. */
internal object BaajaLibraryRoutes {
    const val LIKED = "auto_playlist/liked"
    const val DOWNLOADED = "auto_playlist/downloaded"
    const val UPLOADED = "auto_playlist/uploaded"
    const val CACHED = "cache_playlist/cached"
    const val LOCAL_MUSIC = "local_music"
    const val IMPORT_SPOTIFY = "import_spotify"

    fun top(size: Int) = "top_playlist/$size"
}

/** Top-level library chips. Tapping the selected chip goes back to the mixed view. */
@Composable
internal fun BaajaLibraryFilterRow(
    filterType: LibraryFilter,
    onFilterClick: (LibraryFilter) -> Unit,
    isLoggedIn: Boolean,
    modifier: Modifier = Modifier,
) {
    val chips =
        listOfNotNull(
            LibraryFilter.PLAYLISTS to stringResource(R.string.filter_playlists),
            LibraryFilter.SONGS to stringResource(R.string.filter_songs),
            LibraryFilter.ALBUMS to stringResource(R.string.filter_albums),
            LibraryFilter.ARTISTS to stringResource(R.string.filter_artists),
            if (isLoggedIn) LibraryFilter.PODCASTS to stringResource(R.string.filter_podcasts) else null,
        )
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        chips.forEach { (filter, label) ->
            BaajaChip(
                label = label,
                selected = filterType == filter,
                onClick = { onFilterClick(filter) },
            )
        }
    }
}

/**
 * Chip row for a single library section: an accent chip naming the section (tap to close it,
 * like the old "×" chip) followed by that section's own filters.
 */
@Composable
internal fun <E> BaajaSectionFilterRow(
    sectionLabel: String,
    onDeselect: () -> Unit,
    chips: List<Pair<E, String>>,
    currentValue: E,
    onValueUpdate: (E) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BaajaChip(
            label = sectionLabel,
            selected = true,
            accent = true,
            icon = R.drawable.close,
            onClick = onDeselect,
        )
        chips.forEach { (value, label) ->
            BaajaChip(
                label = label,
                selected = value == currentValue,
                onClick = { onValueUpdate(value) },
            )
        }
    }
}

/** Sort pill on the left; item count, search, layout toggle and "new playlist" on the right. */
@Composable
internal fun BaajaLibraryToolbar(
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    countText: String? = null,
    viewType: LibraryViewType? = null,
    onToggleViewType: (() -> Unit)? = null,
    onCreatePlaylist: (() -> Unit)? = null,
    sort: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BaajaSortPill(content = sort)
        Spacer(Modifier.weight(1f))
        if (countText != null) {
            Text(
                text = countText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 2.dp),
            )
        }
        BaajaIconButton(
            icon = R.drawable.search,
            contentDescription = stringResource(R.string.baaja_library_search),
            onClick = onSearch,
            size = 44.dp,
        )
        if (viewType != null && onToggleViewType != null) {
            BaajaIconButton(
                icon =
                    when (viewType) {
                        LibraryViewType.LIST -> R.drawable.list
                        LibraryViewType.GRID -> R.drawable.grid_view
                    },
                contentDescription =
                    stringResource(
                        when (viewType) {
                            LibraryViewType.LIST -> R.string.switch_to_grid_view
                            LibraryViewType.GRID -> R.string.switch_to_list_view
                        },
                    ),
                onClick = onToggleViewType,
                size = 44.dp,
            )
        }
        if (onCreatePlaylist != null) {
            BaajaIconButton(
                icon = R.drawable.add,
                contentDescription = stringResource(R.string.baaja_library_new_playlist),
                onClick = onCreatePlaylist,
                filled = true,
                size = 44.dp,
            )
        }
    }
}

/**
 * The collection tiles at the top of the Library: two big tiles (Liked songs, Downloaded) and
 * compact tiles for the other auto playlists plus device music and Spotify import.
 */
@Composable
internal fun BaajaLibraryCollections(
    showLiked: Boolean,
    showDownloaded: Boolean,
    showCached: Boolean,
    showTop: Boolean,
    showUploaded: Boolean,
    topLabel: String,
    topSize: Int,
    showEntryPoints: Boolean,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val smallTiles =
        buildList {
            if (showCached) {
                add(SmallTile(R.drawable.cached, stringResource(R.string.cached_playlist), null, BaajaLibraryRoutes.CACHED, BaajaTileColors.mint()))
            }
            if (showTop) {
                add(SmallTile(R.drawable.trending_up, topLabel, null, BaajaLibraryRoutes.top(topSize), BaajaTileColors.peach()))
            }
            if (showUploaded) {
                add(SmallTile(R.drawable.backup, stringResource(R.string.uploaded_playlist), null, BaajaLibraryRoutes.UPLOADED, BaajaTileColors.sky()))
            }
            if (showEntryPoints) {
                add(
                    SmallTile(
                        R.drawable.library_music,
                        stringResource(R.string.baaja_library_local_music),
                        stringResource(R.string.baaja_library_local_music_desc),
                        BaajaLibraryRoutes.LOCAL_MUSIC,
                        BaajaTileColors.lilac(),
                    ),
                )
                add(
                    SmallTile(
                        R.drawable.playlist_add,
                        stringResource(R.string.baaja_library_import_spotify),
                        stringResource(R.string.baaja_library_import_spotify_desc),
                        BaajaLibraryRoutes.IMPORT_SPOTIFY,
                        BaajaTileColors.mint(),
                    ),
                )
            }
        }
    if (!showLiked && !showDownloaded && smallTiles.isEmpty()) return
    val entranceKey = rememberBaajaEntranceKey()

    Column(
        modifier = modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (showLiked || showDownloaded) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                if (showLiked) {
                    val database = LocalDatabase.current
                    val likedCount by remember(database) { database.likedSongsCount() }
                        .collectAsStateWithLifecycle(initialValue = 0)
                    BigTile(
                        title = stringResource(R.string.liked_songs),
                        subtitle = pluralStringResource(R.plurals.n_song, likedCount, likedCount),
                        icon = R.drawable.favorite,
                        container = BaajaTileColors.liked(),
                        content = MaterialTheme.colorScheme.onSurface,
                        iconContainer = MaterialTheme.colorScheme.secondary,
                        iconTint = MaterialTheme.colorScheme.onSecondary,
                        onClick = { onNavigate(BaajaLibraryRoutes.LIKED) },
                        modifier = Modifier.weight(1f).baajaEntrance(0, entranceKey),
                    )
                }
                if (showDownloaded) {
                    val downloads by LocalDownloadUtil.current.downloads.collectAsStateWithLifecycle()
                    val downloadedCount =
                        remember(downloads) { downloads.values.count { it.state == Download.STATE_COMPLETED } }
                    BigTile(
                        title = stringResource(R.string.filter_downloaded),
                        subtitle = pluralStringResource(R.plurals.n_song, downloadedCount, downloadedCount),
                        icon = R.drawable.download,
                        container = BaajaTileColors.downloads(),
                        content = MaterialTheme.colorScheme.onSecondaryContainer,
                        iconContainer = MaterialTheme.colorScheme.surface,
                        iconTint = MaterialTheme.colorScheme.onSurface,
                        onClick = { onNavigate(BaajaLibraryRoutes.DOWNLOADED) },
                        modifier = Modifier.weight(1f).baajaEntrance(1, entranceKey),
                    )
                }
            }
        }
        smallTiles.chunked(2).forEachIndexed { rowIndex, pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                pair.forEachIndexed { column, tile ->
                    SmallTileView(
                        tile = tile,
                        onClick = { onNavigate(tile.route) },
                        modifier = Modifier.weight(1f).baajaEntrance(2 + rowIndex * 2 + column, entranceKey),
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

private data class SmallTile(
    @DrawableRes val icon: Int,
    val title: String,
    val subtitle: String?,
    val route: String,
    val accent: Color,
)

@Composable
private fun BigTile(
    title: String,
    subtitle: String,
    @DrawableRes icon: Int,
    container: Color,
    content: Color,
    iconContainer: Color,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(24.dp)
    val source = remember { MutableInteractionSource() }
    Column(
        modifier =
            modifier
                .height(128.dp)
                .baajaSurface(baajaInk, container, shape, shadowOffset = 3.dp, borderWidth = 2.5.dp, interactionSource = source)
                .clickable(interactionSource = source, indication = LocalIndication.current, role = Role.Button, onClick = onClick)
                .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(40.dp)
                    .baajaSurface(baajaInk, iconContainer, CircleShape, shadowOffset = 0.dp),
        ) {
            Icon(painterResource(icon), null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Column {
            Text(
                text = title,
                color = content,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(text = subtitle, color = content, fontSize = 14.sp, maxLines = 1)
        }
    }
}

@Composable
private fun SmallTileView(
    tile: SmallTile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(20.dp)
    val source = remember { MutableInteractionSource() }
    Row(
        modifier =
            modifier
                .heightIn(min = 64.dp)
                .baajaSurface(baajaInk, MaterialTheme.colorScheme.surface, shape, shadowOffset = 2.dp, interactionSource = source)
                .clickable(interactionSource = source, indication = LocalIndication.current, role = Role.Button, onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(40.dp)
                    .baajaSurface(baajaInk, tile.accent, RoundedCornerShape(12.dp), shadowOffset = 0.dp),
        ) {
            Icon(
                painterResource(tile.icon),
                null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = tile.title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (tile.subtitle != null) {
                Text(
                    text = tile.subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
