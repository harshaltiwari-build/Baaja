/**
 * Baaja (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Shared building blocks for Baaja's collection screens (album, playlist, artist, liked, downloads,
 * history and the library tiles).
 */

package com.metrolist.music.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
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
import androidx.media3.exoplayer.offline.Download
import coil3.compose.AsyncImage
import com.metrolist.music.R
import com.metrolist.music.ui.theme.baajaInk
import com.metrolist.music.ui.theme.baajaOutline
import com.metrolist.music.ui.theme.baajaSurface

/** Decorative pastel tile colours from the mockups, with dark-mode friendly fallbacks. */
object BaajaTileColors {
    @Composable
    fun liked(): Color = if (isDark()) Color(0xFF4A2230) else Color(0xFFFFD3DE)

    @Composable
    fun downloads(): Color = MaterialTheme.colorScheme.secondaryContainer

    @Composable
    fun mint(): Color = if (isDark()) Color(0xFF173F3B) else Color(0xFFBDEFE6)

    @Composable
    fun lilac(): Color = if (isDark()) Color(0xFF2E2552) else Color(0xFFD9CCFF)

    @Composable
    fun peach(): Color = if (isDark()) Color(0xFF4A2E1C) else Color(0xFFFFD9B8)

    @Composable
    fun sky(): Color = if (isDark()) Color(0xFF1C2B4A) else Color(0xFFC9DAFF)

    @Composable
    private fun isDark(): Boolean = MaterialTheme.colorScheme.surface.luminance() < 0.5f
}

/** Big outlined artwork used at the top of album / playlist screens (3dp ink, hard 5×7 shadow). */
@Composable
fun BaajaHeroArtwork(
    model: Any?,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    tilt: Float = 0f,
    shape: Shape = RoundedCornerShape(30.dp),
    @DrawableRes placeholder: Int = R.drawable.queue_music,
    contentDescription: String? = null,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    BaajaHeroFrame(modifier = modifier, size = size, tilt = tilt, shape = shape) {
        if (model == null) {
            Icon(
                painter = painterResource(placeholder),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(size * 0.36f),
            )
        } else {
            AsyncImage(
                model = model,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        overlay()
    }
}

/** The outlined hero frame itself, for custom content (mosaics, edit overlays…). */
@Composable
fun BaajaHeroFrame(
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    tilt: Float = 0f,
    shape: Shape = RoundedCornerShape(30.dp),
    color: Color = MaterialTheme.colorScheme.surfaceVariant,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .size(size)
                .then(if (tilt != 0f) Modifier.rotate(tilt) else Modifier)
                .baajaOutline(baajaInk, shape, borderWidth = 3.dp, shadowOffset = 5.dp)
                .clip(shape)
                .background(color),
        content = content,
    )
}

/** Centered title, optional custom subtitle (e.g. clickable artists) and grey meta line. */
@Composable
fun BaajaCollectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    meta: String? = null,
    subtitle: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        subtitle?.invoke()
        if (!meta.isNullOrBlank()) {
            Text(
                text = meta,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Standard header: hero artwork, title block, then a centered row of round actions. */
@Composable
fun BaajaCollectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    meta: String? = null,
    subtitle: (@Composable () -> Unit)? = null,
    artwork: @Composable () -> Unit,
    below: @Composable ColumnScope.() -> Unit = {},
    actions: @Composable RowScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        artwork()
        Spacer(Modifier.height(22.dp))
        BaajaCollectionTitle(title = title, meta = meta, subtitle = subtitle)
        below()
        Spacer(Modifier.height(18.dp))
        BaajaActionRow(content = actions)
    }
}

@Composable
fun BaajaActionRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** Outlined round action (download, like, shuffle, share, more…). */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun BaajaRoundAction(
    @DrawableRes icon: Int,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 50.dp,
    container: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    content: (@Composable BoxScope.() -> Unit)? = null,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .size(size)
                .baajaSurface(
                    baajaInk,
                    if (enabled) container else container.copy(alpha = 0.5f),
                    CircleShape,
                    shadowOffset = if (enabled) 2.dp else 0.dp,
                ).then(
                    if (onLongClick != null) {
                        Modifier.combinedClickable(enabled = enabled, role = Role.Button, onClick = onClick, onLongClick = onLongClick)
                    } else {
                        Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                    },
                ).semantics { if (contentDescription != null) this.contentDescription = contentDescription },
    ) {
        if (content != null) {
            content()
        } else {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = if (enabled) contentColor else contentColor.copy(alpha = 0.5f),
                modifier = Modifier.size(size * 0.46f),
            )
        }
    }
}

/** The big accent play button in the middle of the action row. */
@Composable
fun BaajaHeroPlayButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes icon: Int = R.drawable.play,
    contentDescription: String? = stringResource(R.string.play),
    size: Dp = 74.dp,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .size(size)
                .baajaOutline(baajaInk, CircleShape, borderWidth = 2.5.dp, shadowOffset = if (enabled) 3.5.dp else 0.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 1f else 0.5f))
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .semantics { if (contentDescription != null) this.contentDescription = contentDescription },
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(size * 0.44f),
        )
    }
}

/** Like / save toggle: turns pink with a filled heart when [liked]. */
@Composable
fun BaajaLikeAction(
    liked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 50.dp,
    likedDescription: String = stringResource(R.string.baaja_library_unlike_album),
    unlikedDescription: String = stringResource(R.string.baaja_library_like_album),
) {
    BaajaRoundAction(
        icon = if (liked) R.drawable.favorite else R.drawable.favorite_border,
        contentDescription = if (liked) likedDescription else unlikedDescription,
        onClick = onClick,
        modifier = modifier,
        size = size,
        container = if (liked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surface,
        contentColor = if (liked) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface,
    )
}

/** Download toggle reflecting an aggregate [Download] state. */
@Composable
fun BaajaDownloadAction(
    downloadState: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 50.dp,
) {
    when (downloadState) {
        Download.STATE_COMPLETED ->
            BaajaRoundAction(
                icon = R.drawable.offline,
                contentDescription = stringResource(R.string.remove_download),
                onClick = onClick,
                modifier = modifier,
                size = size,
                container = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
            )

        Download.STATE_QUEUED, Download.STATE_DOWNLOADING ->
            BaajaRoundAction(
                icon = R.drawable.download,
                contentDescription = stringResource(R.string.downloading),
                onClick = onClick,
                modifier = modifier,
                size = size,
            ) {
                CircularProgressIndicator(
                    strokeWidth = 2.5.dp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(size * 0.46f),
                )
            }

        else ->
            BaajaRoundAction(
                icon = R.drawable.download,
                contentDescription = stringResource(R.string.action_download),
                onClick = onClick,
                modifier = modifier,
                size = size,
            )
    }
}

/** Accent "Play" pill next to a white "Shuffle" pill, as on the Liked and Artist mockups. */
@Composable
fun BaajaPlayShufflePills(
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BaajaPill(
            text = stringResource(R.string.play),
            icon = R.drawable.play,
            onClick = onPlay,
            primary = true,
            enabled = enabled,
        )
        BaajaPill(
            text = stringResource(R.string.shuffle),
            icon = R.drawable.shuffle,
            onClick = onShuffle,
            primary = false,
            enabled = enabled,
        )
        trailing()
    }
}

/** Compact pill button (50dp) used inside cards and header rows. */
@Composable
fun BaajaPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    primary: Boolean = true,
    selected: Boolean = false,
    enabled: Boolean = true,
    height: Dp = 50.dp,
) {
    val ink = baajaInk
    val container =
        when {
            primary -> MaterialTheme.colorScheme.primary
            selected -> ink
            else -> MaterialTheme.colorScheme.surface
        }
    val content =
        when {
            primary -> MaterialTheme.colorScheme.onPrimary
            selected -> MaterialTheme.colorScheme.surface
            else -> MaterialTheme.colorScheme.onSurface
        }
    Row(
        modifier =
            modifier
                .heightIn(min = height)
                .baajaSurface(
                    ink,
                    if (enabled) container else container.copy(alpha = 0.5f),
                    CircleShape,
                    shadowOffset = if (selected || !enabled) 0.dp else 2.dp,
                ).clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(painterResource(icon), null, tint = content, modifier = Modifier.size(20.dp))
        Text(text, color = content, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
    }
}

/** Coloured outlined hero card (Liked songs pink, Downloads yellow…). */
@Composable
fun BaajaHeroCard(
    containerColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .baajaOutline(baajaInk, RoundedCornerShape(28.dp), borderWidth = 2.5.dp, shadowOffset = 4.5.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(containerColor)
                .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content,
    )
}

/** Rounded-square outlined badge holding an icon (heart on Liked, arrow on Downloads). */
@Composable
fun BaajaIconBadge(
    @DrawableRes icon: Int,
    container: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 60.dp,
    shape: Shape = RoundedCornerShape(18.dp),
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .size(size)
                .baajaSurface(baajaInk, container, shape, shadowOffset = 0.dp, borderWidth = 2.5.dp),
    ) {
        Icon(painterResource(icon), null, tint = contentColor, modifier = Modifier.size(size * 0.48f))
    }
}

/** Section heading for track groups ("Today", "Downloaded · 86 songs"). */
@Composable
fun BaajaGroupTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        modifier = modifier.padding(horizontal = 20.dp, vertical = 8.dp),
    )
}
