/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.metrolist.music.R
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.ui.theme.baajaOutline
import com.metrolist.music.ui.theme.baajaSurface
import kotlin.math.abs

private val RecordBlack = Color(0xFF17151F)
private val RecordGroove = Color(0xFF2C2836)

/**
 * Immersive Now Playing from the Immersive mockup: the artwork fills the screen, a record with the
 * artwork as its label spins while music plays, and a white control card sits at the bottom.
 * Tapping the record hides or shows the controls; swiping it sideways changes song.
 */
@Composable
internal fun ImmersivePlayer(
    mediaMetadata: MediaMetadata,
    isPlaying: Boolean,
    colors: BaajaPlayerColors,
    swipeEnabled: Boolean,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onExit: () -> Unit,
    onBehindTheSong: () -> Unit,
    onOpenLyrics: () -> Unit,
    onArtistClick: (() -> Unit)?,
    seekRow: @Composable () -> Unit,
    playButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showControls by rememberSaveable { mutableStateOf(true) }
    val cardColors = colors.onCard()

    Box(modifier = modifier.fillMaxSize().background(RecordBlack)) {
        AnimatedContent(
            targetState = mediaMetadata.thumbnailUrl,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "immersiveBackdrop",
        ) { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        // Keeps the status bar and the white header buttons readable over bright artwork.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.35f),
                        0.25f to Color.Transparent,
                        0.7f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.3f),
                    ),
                ),
        )

        Column(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                BaajaRoundButton(
                    icon = R.drawable.expand_more,
                    contentDescription = stringResource(R.string.baaja_player_exit_immersive),
                    onClick = onExit,
                    colors = cardColors,
                )
                BaajaLabelChip(
                    text = stringResource(R.string.baaja_player_immersive),
                    colors = cardColors,
                    height = 36.dp,
                )
                BaajaRoundButton(
                    icon = R.drawable.info,
                    contentDescription = stringResource(R.string.baaja_player_behind_the_song),
                    onClick = onBehindTheSong,
                    colors = cardColors,
                )
            }

            BoxWithConstraints(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                val recordSize = minOf(maxWidth - 64.dp, maxHeight - 24.dp, 340.dp).coerceAtLeast(120.dp)
                SpinningRecord(
                    artworkUrl = mediaMetadata.thumbnailUrl,
                    spinning = isPlaying,
                    recordSize = recordSize,
                    swipeEnabled = swipeEnabled,
                    canSkipPrevious = canSkipPrevious,
                    canSkipNext = canSkipNext,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    toggleLabel =
                        stringResource(
                            if (showControls) R.string.baaja_player_hide_controls else R.string.baaja_player_show_controls,
                        ),
                    onTap = { showControls = !showControls },
                )
            }

            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = showControls,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut(),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier =
                            Modifier
                                .padding(start = 16.dp, end = 16.dp, bottom = 20.dp)
                                .fillMaxWidth()
                                .baajaSurface(cardColors.ink, cardColors.surface, RoundedCornerShape(30.dp), shadowOffset = 5.dp, borderWidth = 2.5.dp)
                                .padding(20.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = mediaMetadata.title,
                                    color = cardColors.onSurface,
                                    fontSize = 26.sp,
                                    lineHeight = 30.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee(iterations = 1, initialDelayMillis = 3000),
                                )
                                Text(
                                    text = mediaMetadata.artists.joinToString { it.name },
                                    color = cardColors.mutedContent,
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier =
                                        if (onArtistClick != null) {
                                            Modifier.clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button, onClick = onArtistClick)
                                        } else {
                                            Modifier
                                        },
                                )
                            }
                            BaajaPlayerPill(
                                text = stringResource(R.string.lyrics),
                                onClick = onOpenLyrics,
                                colors = cardColors,
                                active = true,
                                height = 40.dp,
                                shadowOffset = 0.dp,
                            )
                        }
                        seekRow()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            BaajaControlIcon(
                                icon = R.drawable.skip_previous,
                                contentDescription = stringResource(R.string.previous),
                                onClick = onPrevious,
                                enabled = canSkipPrevious,
                                tint = cardColors.onSurface,
                            )
                            playButton()
                            BaajaControlIcon(
                                icon = R.drawable.skip_next,
                                contentDescription = stringResource(R.string.next),
                                onClick = onNext,
                                enabled = canSkipNext,
                                tint = cardColors.onSurface,
                            )
                        }
                    }
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = !showControls,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    BaajaLabelChip(
                        text = stringResource(R.string.baaja_player_tap_record_hint),
                        colors = cardColors,
                        height = 38.dp,
                        modifier = Modifier.padding(bottom = 28.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SpinningRecord(
    artworkUrl: String?,
    spinning: Boolean,
    recordSize: Dp,
    swipeEnabled: Boolean,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    toggleLabel: String,
    onTap: () -> Unit,
) {
    val rotation = rememberVinylRotation(spinning = spinning)
    val layoutDirection = LocalLayoutDirection.current
    val currentOnPrevious by rememberUpdatedState(onPrevious)
    val currentOnNext by rememberUpdatedState(onNext)
    val currentCanPrevious by rememberUpdatedState(canSkipPrevious)
    val currentCanNext by rememberUpdatedState(canSkipNext)
    val dragX = remember { mutableFloatStateOf(0f) }

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(recordSize)
                .graphicsLayer { translationX = dragX.floatValue * 0.4f }
                .baajaOutline(RecordBlack, CircleShape, borderWidth = 3.dp, shadowOffset = 6.dp)
                .clip(CircleShape)
                .semantics { contentDescription = toggleLabel }
                .clickable(role = Role.Button, onClickLabel = toggleLabel, onClick = onTap)
                .then(
                    if (swipeEnabled) {
                        Modifier.pointerInput(Unit) {
                            val threshold = 72.dp.toPx()
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    val dx = if (layoutDirection == LayoutDirection.Rtl) -dragX.floatValue else dragX.floatValue
                                    if (abs(dx) > threshold) {
                                        if (dx < 0 && currentCanNext) currentOnNext() else if (dx > 0 && currentCanPrevious) currentOnPrevious()
                                    }
                                    dragX.floatValue = 0f
                                },
                                onDragCancel = { dragX.floatValue = 0f },
                                onHorizontalDrag = { change, amount ->
                                    change.consume()
                                    dragX.floatValue += amount
                                },
                            )
                        }
                    } else {
                        Modifier
                    },
                ),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(RecordBlack)
            val groove = 2.dp.toPx()
            val step = 5.dp.toPx()
            var r = size.minDimension / 2f - step
            val labelRadius = size.minDimension * 0.19f
            while (r > labelRadius) {
                drawCircle(RecordGroove, radius = r, style = Stroke(groove))
                r -= step
            }
        }
        val labelSize = recordSize * 0.36f
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(labelSize)
                    .graphicsLayer { rotationZ = rotation.value }
                    .baajaOutline(RecordBlack, CircleShape, borderWidth = 3.dp, shadowOffset = 0.dp)
                    .clip(CircleShape),
        ) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .size(16.dp)
                    .baajaSurface(RecordBlack, Color.White, CircleShape, shadowOffset = 0.dp),
            )
        }
    }
}
