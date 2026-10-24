/**
 * Baaja Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component.home

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.exoplayer.offline.Download
import coil3.compose.AsyncImage
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.EpisodeItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.PodcastItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.YTItem
import com.metrolist.music.LocalArtistNameAliases
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalDownloadUtil
import com.metrolist.music.R
import com.metrolist.music.constants.SwipeToSongKey
import com.metrolist.music.db.entities.Song
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.ui.component.PlayingIndicatorBox
import com.metrolist.music.ui.component.SwipeToSongBox
import com.metrolist.music.ui.theme.BaajaInk
import com.metrolist.music.ui.theme.BaajaChip
import com.metrolist.music.ui.theme.baajaInk
import com.metrolist.music.ui.theme.baajaOutline
import com.metrolist.music.ui.theme.baajaSurface
import com.metrolist.music.ui.utils.resize
import com.metrolist.music.utils.ArtistNameAliases
import com.metrolist.music.utils.joinByBullet
import com.metrolist.music.utils.joinToArtistString
import com.metrolist.music.utils.makeTimeString
import com.metrolist.music.utils.rememberSharedBooleanPreference
import kotlinx.coroutines.flow.firstOrNull

/** Horizontal gutter used by Baaja's home and search screens. */
val BaajaHomeGutter = 20.dp

/** Pastel blocks from the Baaja mockup palette, used behind initials and decorative tiles. */
val BaajaPastels =
    listOf(
        Color(0xFFD9CCFF),
        Color(0xFFFFD3DE),
        Color(0xFFFFD9B8),
        Color(0xFFBDEFE6),
        Color(0xFFE3F5C8),
        Color(0xFFC9DAFF),
        Color(0xFFFFE066),
    )

/** Strong partners for [BaajaPastels], used for the decorative bands in [BaajaBlockArt]. */
private val BaajaBands =
    listOf(
        Color(0xFF5B3FD9) to Color(0xFF8D6BFF),
        Color(0xFFE8436A) to Color(0xFF8E1F45),
        Color(0xFFF2783A) to Color(0xFFC2410C),
        Color(0xFF14A39A) to Color(0xFF0E6E73),
        Color(0xFF7BC043) to Color(0xFF2F6B1F),
        Color(0xFF3D6BF2) to Color(0xFF1E3A8A),
        Color(0xFFE8436A) to Color(0xFF5B3FD9),
    )

fun baajaPastelFor(key: String): Color = BaajaPastels[Math.floorMod(key.hashCode(), BaajaPastels.size)]

private fun initialsOf(name: String): String =
    name
        .split(' ', '-', '_')
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifEmpty { "♪" }

/** Section title in Baaja's style: bold ink title, optional label/thumbnail and a text action on the right. */
@Composable
fun BaajaHomeSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    thumbnail: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                .heightIn(min = 52.dp)
                .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
                .padding(start = BaajaHomeGutter, end = 12.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        thumbnail?.invoke()
        Column(Modifier.weight(1f)) {
            if (label != null) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = title,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (actionLabel != null && onAction != null) {
            Box(
                modifier =
                    Modifier
                        .heightIn(min = 44.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClick = onAction)
                        .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = actionLabel,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
            }
        }
        if (onClick != null) {
            Icon(
                painter = painterResource(R.drawable.arrow_forward),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/** Horizontally scrolling row of [BaajaChip]s. */
@Composable
fun <E> BaajaChipsRow(
    chips: List<Pair<E, String>>,
    currentValue: E,
    onValueUpdate: (E) -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                .horizontalScroll(rememberScrollState())
                .padding(start = BaajaHomeGutter, end = BaajaHomeGutter, top = 6.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        chips.forEach { (value, label) ->
            BaajaChip(
                label = label,
                selected = currentValue == value,
                onClick = { onValueUpdate(value) },
            )
        }
    }
}

/** Pill-shaped page indicator: the current page is a wide ink pill, others are small dots. */
@Composable
fun BaajaPageIndicator(
    pageCount: Int,
    currentPage: Int,
    onPageClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (pageCount <= 1) return
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            val width by animateDpAsState(if (selected) 24.dp else 8.dp, label = "baajaDotWidth")
            val description = stringResource(R.string.baaja_home_show_page, index + 1, pageCount)
            Box(
                modifier =
                    Modifier
                        .size(width = 28.dp, height = 44.dp)
                        .clickable(role = Role.Tab, onClickLabel = description) { onPageClick(index) }
                        .semantics { contentDescription = description },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .height(8.dp)
                        .width(width)
                        .clip(CircleShape)
                        .background(
                            if (selected) baajaInk else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                        ),
                )
            }
        }
    }
}

/** Large featured card: full-bleed artwork, white caption strip and an accent play button. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BaajaFeaturedCard(
    thumbnailUrl: String?,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
    caption: String? = null,
    onLongClick: (() -> Unit)? = null,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
) {
    val shape = RoundedCornerShape(28.dp)
    val ink = baajaInk
    val surface = MaterialTheme.colorScheme.surface
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(280.dp)
                .baajaOutline(ink, shape, borderWidth = 2.5.dp, shadowOffset = 4.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .combinedClickable(
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.baaja_home_open_named, title),
                    onClick = onClick,
                    onLongClick = onLongClick,
                ),
    ) {
        AsyncImage(
            model = thumbnailUrl?.resize(1080, 1080),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(surface.copy(alpha = 0.94f))
                    .drawBehind {
                        drawLine(ink, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 2.dp.toPx())
                    }.padding(start = 18.dp, end = 88.dp, top = 14.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
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
            if (!caption.isNullOrBlank()) {
                Text(
                    text = caption,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)
                    .size(56.dp)
                    .baajaSurface(ink, MaterialTheme.colorScheme.primary, CircleShape, shadowOffset = 2.dp)
                    .clickable(role = Role.Button, onClick = onPlay),
            contentAlignment = Alignment.Center,
        ) {
            if (isActive) {
                PlayingIndicatorBox(
                    isActive = true,
                    playWhenReady = isPlaying,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.play),
                    contentDescription = stringResource(R.string.baaja_home_play_named, title),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

/** Round artist avatar with ink outline and the name underneath; initials show while artwork loads. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BaajaArtistAvatar(
    name: String,
    thumbnailUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    size: Dp = 76.dp,
) {
    val ink = baajaInk
    Column(
        modifier =
            modifier
                .width(size + 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onLongClick)
                .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(size)
                    .border(2.5.dp, ink, CircleShape)
                    .clip(CircleShape)
                    .background(baajaPastelFor(name)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initialsOf(name),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = BaajaInk,
            )
            if (thumbnailUrl != null) {
                AsyncImage(
                    model = thumbnailUrl.resize(300, 300),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Text(
            text = name,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Outlined artwork box with the "now playing" overlay. */
@Composable
fun BaajaArtworkBox(
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(14.dp),
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    shadowOffset: Dp = 3.dp,
    borderWidth: Dp = 2.dp,
    placeholderKey: String? = null,
    imageSize: Int = 544,
) {
    Box(
        modifier =
            modifier.baajaSurface(
                baajaInk,
                placeholderKey?.let { baajaPastelFor(it) } ?: MaterialTheme.colorScheme.surfaceVariant,
                shape,
                shadowOffset = shadowOffset,
                borderWidth = borderWidth,
            ),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = thumbnailUrl?.resize(imageSize, imageSize),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (isActive) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
            )
            PlayingIndicatorBox(isActive = true, playWhenReady = isPlaying, color = Color.White)
        }
    }
}

/** Generic Baaja list row: outlined artwork, bold title, grey subtitle, trailing actions. */
@Composable
fun BaajaMediaRow(
    title: String,
    subtitle: String?,
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
    artworkWidth: Dp = 58.dp,
    artworkHeight: Dp = 58.dp,
    artworkShape: Shape = RoundedCornerShape(14.dp),
    height: Dp = 76.dp,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    titleSize: Int = 17,
    badges: @Composable RowScope.() -> Unit = {},
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(height)
                .padding(start = BaajaHomeGutter, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        BaajaArtworkBox(
            thumbnailUrl = thumbnailUrl,
            shape = artworkShape,
            isActive = isActive,
            isPlaying = isPlaying,
            imageSize = 320,
            modifier = Modifier.size(artworkWidth, artworkHeight),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = title,
                fontSize = titleSize.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                badges()
                if (!subtitle.isNullOrBlank()) {
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
        trailing()
    }
}

/** 44dp "more" button with an accessible label. */
@Composable
fun BaajaMoreButton(
    itemTitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.more_vert),
            contentDescription = stringResource(R.string.baaja_home_more_options_for, itemTitle),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun BadgeIcon(
    @DrawableRes icon: Int,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Icon(
        painter = painterResource(icon),
        contentDescription = null,
        tint = tint,
        modifier =
            Modifier
                .size(18.dp)
                .padding(end = 2.dp),
    )
}

@Composable
private fun DownloadBadge(songId: String) {
    val download by LocalDownloadUtil.current.getDownload(songId).collectAsStateWithLifecycle(null)
    when (download?.state) {
        Download.STATE_COMPLETED -> BadgeIcon(R.drawable.offline)
        Download.STATE_QUEUED, Download.STATE_DOWNLOADING ->
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                modifier =
                    Modifier
                        .size(16.dp)
                        .padding(end = 2.dp),
            )
        else -> Unit
    }
}

/** Liked / explicit / downloaded badges for a library song. */
@Composable
fun RowScope.BaajaSongBadges(song: Song, showInLibraryIcon: Boolean = false) {
    if (song.song.liked) BadgeIcon(R.drawable.favorite, MaterialTheme.colorScheme.error)
    if (song.song.explicit) BadgeIcon(R.drawable.explicit)
    if (showInLibraryIcon && song.song.inLibrary != null) BadgeIcon(R.drawable.library_add_check)
    DownloadBadge(song.id)
}

/** Liked / explicit / downloaded badges for a YouTube item. */
@Composable
fun RowScope.BaajaYouTubeBadges(item: YTItem) {
    val database = LocalDatabase.current
    val liked by produceState(initialValue = false, item.id) {
        value =
            when (item) {
                is SongItem -> database.song(item.id).firstOrNull()?.song?.liked == true
                is AlbumItem -> database.album(item.id).firstOrNull()?.album?.bookmarkedAt != null
                else -> false
            }
    }
    if (liked) BadgeIcon(R.drawable.favorite, MaterialTheme.colorScheme.error)
    if (item.explicit) BadgeIcon(R.drawable.explicit)
    if (item is SongItem) DownloadBadge(item.id)
}

/** Subtitle text for a YouTube item, matching what the shared list items show. */
@Composable
fun baajaYouTubeSubtitle(item: YTItem): String? {
    val aliases = LocalArtistNameAliases.current
    val separator = " ${stringResource(R.string.and)} "
    return when (item) {
        is SongItem ->
            joinByBullet(
                item.artists.joinToArtistString(separator) { ArtistNameAliases.resolve(aliases, it.id, it.name) },
                makeTimeString(item.duration?.times(1000L)),
            )
        is AlbumItem ->
            joinByBullet(
                item.artists?.joinToArtistString(separator) { ArtistNameAliases.resolve(aliases, it.id, it.name) },
                item.year?.toString(),
            )
        is ArtistItem -> null
        is PlaylistItem ->
            joinByBullet(item.author?.let { ArtistNameAliases.resolve(aliases, it.id, it.name) }, item.songCountText)
        is PodcastItem ->
            joinByBullet(item.author?.let { ArtistNameAliases.resolve(aliases, it.id, it.name) }, item.episodeCountText)
        is EpisodeItem ->
            joinByBullet(
                item.author?.let { ArtistNameAliases.resolve(aliases, it.id, it.name) },
                makeTimeString(item.duration?.times(1000L)),
            )
    }
}

@Composable
fun baajaYouTubeTitle(item: YTItem): String =
    if (item is ArtistItem) {
        ArtistNameAliases.resolve(LocalArtistNameAliases.current, item.id, item.title)
    } else {
        item.title
    }

/** Baaja row for any YouTube item; keeps the swipe-to-queue gesture for songs. */
@Composable
fun BaajaYouTubeRow(
    item: YTItem,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    isSwipeable: Boolean = true,
    artworkWidth: Dp = 58.dp,
    artworkHeight: Dp = 58.dp,
    height: Dp = 76.dp,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val swipeEnabled by rememberSharedBooleanPreference(SwipeToSongKey, defaultValue = false)
    val content: @Composable () -> Unit = {
        BaajaMediaRow(
            title = baajaYouTubeTitle(item),
            subtitle = baajaYouTubeSubtitle(item),
            thumbnailUrl = item.thumbnail,
            artworkWidth = if (item is ArtistItem) artworkHeight else artworkWidth,
            artworkHeight = artworkHeight,
            artworkShape = if (item is ArtistItem) CircleShape else RoundedCornerShape(14.dp),
            height = height,
            isActive = isActive,
            isPlaying = isPlaying,
            badges = { BaajaYouTubeBadges(item) },
            trailing = trailing,
            modifier = modifier,
        )
    }
    if (item is SongItem && isSwipeable && swipeEnabled) {
        SwipeToSongBox(
            mediaItem = item.copy(thumbnail = item.thumbnail.resize(1080, 1080)).toMediaItem(),
            modifier = Modifier.fillMaxWidth(),
        ) { content() }
    } else {
        content()
    }
}

/** Baaja row for a library song ("By artist" subtitle, wide outlined artwork like Quick picks). */
@Composable
fun BaajaSongRow(
    song: Song,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    showInLibraryIcon: Boolean = false,
    artworkWidth: Dp = 96.dp,
    artworkHeight: Dp = 68.dp,
    height: Dp = 84.dp,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val aliases = LocalArtistNameAliases.current
    val separator = " ${stringResource(R.string.and)} "
    val artists = song.artists.joinToString(separator) { ArtistNameAliases.resolve(aliases, it.id, it.name) }
    BaajaMediaRow(
        title = song.song.title,
        subtitle = if (artists.isNotBlank()) stringResource(R.string.baaja_home_song_by, artists) else null,
        thumbnailUrl = song.song.thumbnailUrl,
        artworkWidth = artworkWidth,
        artworkHeight = artworkHeight,
        artworkShape = RoundedCornerShape(16.dp),
        height = height,
        isActive = isActive,
        isPlaying = isPlaying,
        titleSize = 18,
        badges = { BaajaSongBadges(song, showInLibraryIcon) },
        trailing = trailing,
        modifier = modifier,
    )
}

/** Square outlined card for albums/playlists in horizontal rows. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BaajaYouTubeCard(
    item: YTItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    size: Dp = 140.dp,
) {
    Column(
        modifier =
            modifier
                .width(size + 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        BaajaArtworkBox(
            thumbnailUrl = item.thumbnail,
            shape = if (item is ArtistItem) CircleShape else RoundedCornerShape(22.dp),
            isActive = isActive,
            isPlaying = isPlaying,
            shadowOffset = 3.dp,
            borderWidth = 2.5.dp,
            modifier = Modifier.size(size),
        )
        Text(
            text = baajaYouTubeTitle(item),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
        baajaYouTubeSubtitle(item)?.let {
            Text(
                text = it,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Genre/mood tile: a pastel block tinted from YouTube's stripe colour, with a solid colour notch and
 * an ink outline. The tint is mixed over the surface so text stays legible in light and dark mode.
 */
@Composable
fun BaajaGenreTile(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    stripeColor: Color? = null,
    height: Dp = 64.dp,
) {
    val surface = MaterialTheme.colorScheme.surface
    val base = stripeColor ?: baajaPastelFor(title)
    val background = remember(base, surface) { base.copy(alpha = 0.32f).compositeOver(surface) }
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier =
            modifier
                .height(height)
                .baajaSurface(baajaInk, background, shape, shadowOffset = 2.dp)
                .clickable(role = Role.Button, onClick = onClick)
                .drawBehind {
                    val notch = 14.dp.toPx()
                    drawRoundRect(
                        color = base,
                        topLeft = Offset(size.width - notch * 1.6f, -notch * 0.6f),
                        size = Size(notch * 2.2f, notch * 2.2f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(notch),
                    )
                }.padding(horizontal = 10.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Start,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(end = 34.dp),
        )
        Image(
            painter = painterResource(baajaMoodIconFor(title)),
            contentDescription = null,
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 4.dp)
                    .size(30.dp),
        )
    }
}

/** Decorative colour-block "artwork" (sun + two bands) for tiles that have no real artwork. */
@Composable
fun BaajaBlockArt(
    seed: Int,
    modifier: Modifier = Modifier,
) {
    val index = Math.floorMod(seed, BaajaPastels.size)
    val bg = BaajaPastels[index]
    val (bandA, bandB) = BaajaBands[index]
    Canvas(modifier = modifier.background(bg)) {
        val w = size.width
        val h = size.height
        drawCircle(Color(0xFFFFE9A8), radius = h * 0.18f, center = Offset(w * 0.72f, h * 0.3f))
        val first =
            Path().apply {
                moveTo(0f, h * 0.62f)
                lineTo(w, h * 0.38f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
        drawPath(first, bandA)
        val second =
            Path().apply {
                moveTo(0f, h * 0.78f)
                lineTo(w, h * 0.66f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
        drawPath(second, bandB)
    }
}
