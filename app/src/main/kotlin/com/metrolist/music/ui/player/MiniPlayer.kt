/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Performance optimized MiniPlayer - prevents unnecessary recomposition
 */

package com.metrolist.music.ui.player

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.Stable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalListenTogetherManager
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.DarkModeKey
import com.metrolist.music.constants.MiniPlayerBottomSpacing
import com.metrolist.music.constants.MiniPlayerHeight
import com.metrolist.music.constants.PureBlackKey
import com.metrolist.music.constants.SwipeSensitivityKey
import com.metrolist.music.constants.SwipeThumbnailKey
import com.metrolist.music.listentogether.ListenTogetherManager
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.playback.CastConnectionHandler
import com.metrolist.music.playback.PlayerConnection
import com.metrolist.music.ui.screens.settings.DarkMode
import com.metrolist.music.ui.utils.resize
import com.metrolist.music.utils.joinToArtistString
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.metrolist.music.ui.component.Icon as MIcon
import com.metrolist.music.constants.MiniPlayerBackgroundStyle
import com.metrolist.music.constants.MiniPlayerBackgroundStyleKey
import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.metrolist.music.ui.theme.PlayerColorExtractor
import com.metrolist.music.ui.theme.baajaOutline
import com.metrolist.music.ui.theme.baajaPressScale
import com.metrolist.music.ui.component.heartBurst
import com.metrolist.music.ui.component.rememberHeartBurst

/**
 * Stable wrapper for progress state - reads values only during draw phase
 * This prevents recomposition when position/duration change
 */
@Stable
class ProgressState(
    private val positionState: MutableLongState,
    private val durationState: MutableLongState,
) {
    val progress: Float
        get() {
            val duration = durationState.longValue
            return if (duration > 0) (positionState.longValue.toFloat() / duration).coerceIn(0f, 1f) else 0f
        }
}

@Composable
fun MiniPlayer(
    positionState: MutableLongState,
    durationState: MutableLongState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    // Stable progress state: doesn't cause recomposition on position changes
    val progressState = remember { ProgressState(positionState, durationState) }

    NewMiniPlayer(
        progressState = progressState,
        modifier = modifier,
        onClick = onClick,
    )
}

/** Dominant artwork colour per song id, so the gradient is only extracted once per song. */
private val miniPlayerArtworkColors = android.util.LruCache<String, Int>(64)

// ============================================================================
// NEW MINI PLAYER DESIGN
// ============================================================================

@Composable
private fun NewMiniPlayer(
    progressState: ProgressState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val playerConnection = LocalPlayerConnection.current ?: return

    // Theme settings - these rarely change
    val miniPlayerBackground by rememberEnumPreference(
        MiniPlayerBackgroundStyleKey,
        defaultValue = MiniPlayerBackgroundStyle.DEFAULT,
    )
    val context = LocalContext.current
    var artworkColor by remember { mutableStateOf<Color?>(null) }
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val useDarkTheme =
        remember(darkTheme, isSystemInDarkTheme) {
            if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
        }
    val pureBlackEnabled by rememberPreference(PureBlackKey, defaultValue = false)
    val backdropColor = if (pureBlackEnabled && useDarkTheme) Color.Black else MaterialTheme.colorScheme.surface

    // Player states - only collect what's needed at this level
    val playbackState by playerConnection.playbackState.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsStateWithLifecycle()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsStateWithLifecycle()

    // Cast state - safely access castConnectionHandler to prevent crashes during service lifecycle changes
    val castHandler =
        remember(playerConnection) {
            try {
                playerConnection.service.castConnectionHandler
            } catch (e: Exception) {
                null
            }
        }
    val isCasting by castHandler?.isCasting?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }

    // Swipe settings
    val swipeSensitivity by rememberPreference(SwipeSensitivityKey, 0.73f)
    val swipeThumbnailPref by rememberPreference(SwipeThumbnailKey, true)

    // Disable swipe for Listen Together guests
    val listenTogetherManager = LocalListenTogetherManager.current
    val isListenTogetherGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false
    val swipeThumbnail = swipeThumbnailPref && !isListenTogetherGuest

    val layoutDirection = LocalLayoutDirection.current
    val coroutineScope = rememberCoroutineScope()

    val windowInfo = LocalWindowInfo.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val isTabletLandscape =
        remember(windowInfo.containerSize.width, configuration.orientation) {
            (windowInfo.containerSize.width / density.density) >= 600f && configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        }

    // Swipe animation state
    val offsetXAnimatable = remember { Animatable(0f) }
    var dragStartTime by remember { mutableLongStateOf(0L) }
    var totalDragDistance by remember { mutableFloatStateOf(0f) }

    val animationSpec =
        remember {
            spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
        }

    val autoSwipeThreshold =
        remember(swipeSensitivity) {
            (600 / (1f + kotlin.math.exp(-(-11.44748 * swipeSensitivity + 9.04945)))).roundToInt()
        }

    val songId = mediaMetadata?.id
    val thumbnailUrlForColor = mediaMetadata?.thumbnailUrl
    LaunchedEffect(songId, thumbnailUrlForColor, miniPlayerBackground) {
        if (miniPlayerBackground != MiniPlayerBackgroundStyle.GRADIENT || songId == null) {
            artworkColor = null
            return@LaunchedEffect
        }
        val cached = miniPlayerArtworkColors.get(songId)
        if (cached != null) {
            artworkColor = Color(cached)
            return@LaunchedEffect
        }
        if (thumbnailUrlForColor == null) {
            artworkColor = null
            return@LaunchedEffect
        }
        val extracted =
            withContext(Dispatchers.IO) {
                val request =
                    ImageRequest.Builder(context)
                        .data(thumbnailUrlForColor)
                        .size(100, 100)
                        .allowHardware(false)
                        .build()
                val bitmap = runCatching { context.imageLoader.execute(request).image?.toBitmap() }.getOrNull()
                bitmap?.let {
                    withContext(Dispatchers.Default) {
                        val palette =
                            Palette.from(it)
                                .maximumColorCount(8)
                                .resizeBitmapArea(100 * 100)
                                .generate()
                        PlayerColorExtractor
                            .extractGradientColors(palette = palette, fallbackColor = 0xFF000000.toInt())
                            .first()
                    }
                }
            }
        extracted?.let { miniPlayerArtworkColors.put(songId, it.toArgb()) }
        artworkColor = extracted
    }

    // Gradient: the artwork colour fading to a darker shade (darker overall in dark mode). Flat surface
    // colour until the artwork colour is known, so there is no jump when it arrives.
    val surfaceColor = MaterialTheme.colorScheme.surface
    val gradientStart = artworkColor?.let { if (useDarkTheme) lerp(it, Color.Black, 0.35f) else it } ?: surfaceColor
    val gradientEnd = artworkColor?.let { lerp(it, Color.Black, if (useDarkTheme) 0.7f else 0.35f) } ?: surfaceColor
    val gradientStartState = animateColorAsState(gradientStart, label = "miniGradientStart")
    val gradientEndState = animateColorAsState(gradientEnd, label = "miniGradientEnd")

    val onGradient = artworkColor != null
    val onGradientLight = onGradient && lerp(gradientStart, gradientEnd, 0.5f).luminance() > 0.5f
    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outline
    val onSurfaceColor =
        when {
            !onGradient -> MaterialTheme.colorScheme.onSurface
            onGradientLight -> Color(0xFF1B1B1F)
            else -> Color.White
        }
    val errorColor = MaterialTheme.colorScheme.error

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(MiniPlayerHeight)
                // Backdrop from a short fade above down to the nav bar's, so list content doesn't peek
                // out around and below the mini player.
                .drawBehind {
                    val fade = MiniPlayerBackdropFade.toPx()
                    drawRect(
                        brush = Brush.verticalGradient(listOf(backdropColor.copy(alpha = 0f), backdropColor), startY = -fade, endY = 0f),
                        topLeft = Offset(0f, -fade),
                        size = Size(size.width, fade),
                    )
                    drawRect(backdropColor, size = Size(size.width, size.height + MiniPlayerBottomSpacing.toPx()))
                }
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = 12.dp)
                .let { baseModifier ->
                    if (swipeThumbnail) {
                        baseModifier.pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragStart = {
                                    dragStartTime = System.currentTimeMillis()
                                    totalDragDistance = 0f
                                },
                                onDragCancel = {
                                    coroutineScope.launch {
                                        offsetXAnimatable.animateTo(0f, animationSpec)
                                    }
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    val adjustedDragAmount =
                                        if (layoutDirection == LayoutDirection.Rtl) -dragAmount else dragAmount
                                    val canSkipPrevious = playerConnection.player.previousMediaItemIndex != -1
                                    val canSkipNext = playerConnection.player.nextMediaItemIndex != -1
                                    val tryingToSwipeRight = adjustedDragAmount > 0
                                    val tryingToSwipeLeft = adjustedDragAmount < 0
                                    val allowLeft = tryingToSwipeLeft && canSkipNext
                                    val allowRight = tryingToSwipeRight && canSkipPrevious

                                    val canReturnToCenter =
                                        (tryingToSwipeRight && !canSkipPrevious && offsetXAnimatable.value < 0) ||
                                            (tryingToSwipeLeft && !canSkipNext && offsetXAnimatable.value > 0)

                                    if (allowLeft || allowRight || canReturnToCenter) {
                                        totalDragDistance += kotlin.math.abs(adjustedDragAmount)
                                        coroutineScope.launch {
                                            offsetXAnimatable.snapTo(offsetXAnimatable.value + adjustedDragAmount)
                                        }
                                    }
                                },
                                onDragEnd = {
                                    val dragDuration = System.currentTimeMillis() - dragStartTime
                                    val velocity = if (dragDuration > 0) totalDragDistance / dragDuration else 0f
                                    val currentOffset = offsetXAnimatable.value
                                    val minDistanceThreshold = 50f
                                    val velocityThreshold = (swipeSensitivity * -8.25f) + 8.5f

                                    val shouldChangeSong =
                                        (kotlin.math.abs(currentOffset) > minDistanceThreshold && velocity > velocityThreshold) ||
                                            (kotlin.math.abs(currentOffset) > autoSwipeThreshold)

                                    if (shouldChangeSong) {
                                        if (currentOffset > 0 && canSkipPrevious) {
                                            playerConnection.player.seekToPreviousMediaItem()
                                        } else if (currentOffset <= 0 && canSkipNext) {
                                            playerConnection.player.seekToNext()
                                        }
                                    }
                                    coroutineScope.launch {
                                        offsetXAnimatable.animateTo(0f, animationSpec)
                                    }
                                },
                            )
                        }
                    } else {
                        baseModifier
                    }
                },
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        Box(
            modifier =
                Modifier
                    .then(if (isTabletLandscape) Modifier.width(500.dp).align(Alignment.Center) else Modifier.fillMaxWidth())
                    .height(64.dp)
                    .offset { IntOffset(offsetXAnimatable.value.roundToInt(), 0) }
                    .baajaPressScale(interactionSource)
                    .baajaOutline(color = outlineColor, shape = RoundedCornerShape(22.dp))
                    .clip(RoundedCornerShape(22.dp))
                    .background(color = surfaceColor)
                    .drawBehind {
                        if (miniPlayerBackground == MiniPlayerBackgroundStyle.GRADIENT) {
                            drawRect(Brush.horizontalGradient(listOf(gradientStartState.value, gradientEndState.value)))
                        }
                    }
                    .clickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        onClick = onClick
                    ),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize().padding(start = 8.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            ) {
                // Artwork tile (mockup: outlined rounded square)
                val thumbnailUrl = remember(mediaMetadata?.thumbnailUrl) { mediaMetadata?.thumbnailUrl?.resize(144, 144) }
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .size(46.dp)
                            .baajaOutline(color = outlineColor, shape = RoundedCornerShape(12.dp), shadowOffset = 0.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Song info - isolated composable
                NewMiniPlayerSongInfo(
                    mediaMetadata = mediaMetadata,
                    onSurfaceColor = onSurfaceColor,
                    errorColor = errorColor,
                    modifier = Modifier.weight(1f),
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Cast indicator
                if (isCasting) {
                    Icon(
                        painter = painterResource(R.drawable.cast_connected),
                        contentDescription = stringResource(R.string.baaja_fix2_casting),
                        tint = primaryColor,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Next, like YouTube Music's mini player; following the artist lives on the artist page
                NextButton(
                    enabled = canSkipNext,
                    outlineColor = outlineColor,
                    onSurfaceColor = onSurfaceColor,
                    onClick = { playerConnection.player.seekToNext() },
                )
                Spacer(modifier = Modifier.width(6.dp))

                // Favorite button - isolated composable
                mediaMetadata?.let {
                    FavoriteButton(
                        songId = it.id,
                        errorColor = errorColor,
                        outlineColor = outlineColor,
                        onSurfaceColor = onSurfaceColor,
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Round accent play button with the progress ring around it
                NewMiniPlayerPlayButton(
                    progressState = progressState,
                    playbackState = playbackState,
                    isCasting = isCasting,
                    castHandler = castHandler,
                    playerConnection = playerConnection,
                    mediaMetadata = mediaMetadata,
                    primaryColor = primaryColor,
                    outlineColor = outlineColor,
                    listenTogetherManager = listenTogetherManager,
                )
            }
        }
    }
}

/**
 * Play button with circular progress indicator
 * Uses drawWithContent to update progress without recomposition
 */
@Composable
private fun NewMiniPlayerPlayButton(
    progressState: ProgressState,
    playbackState: Int,
    isCasting: Boolean,
    castHandler: CastConnectionHandler?,
    playerConnection: PlayerConnection,
    mediaMetadata: MediaMetadata?,
    primaryColor: Color,
    outlineColor: Color,
    listenTogetherManager: ListenTogetherManager?,
) {
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val castIsPlaying by castHandler?.castIsPlaying?.collectAsState() ?: remember { mutableStateOf(false) }
    val effectiveIsPlaying = if (isCasting) castIsPlaying else isPlaying
    val isListenTogetherGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false
    val isMuted by playerConnection.isMuted.collectAsStateWithLifecycle()

    val trackColor = outlineColor.copy(alpha = 0.15f)
    val strokeWidth = 3.dp
    val accent = MaterialTheme.colorScheme.primary
    val onAccent = MaterialTheme.colorScheme.onPrimary
    val label =
        when {
            isListenTogetherGuest -> stringResource(if (isMuted) R.string.unmute else R.string.mute)
            effectiveIsPlaying && playbackState != Player.STATE_ENDED -> stringResource(R.string.player_pause)
            else -> stringResource(R.string.play)
        }

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(52.dp)
                .drawWithContent {
                    drawContent()
                    // Progress ring drawn in the draw phase only, so position ticks don't recompose.
                    val progress = progressState.progress
                    val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                    val diameter = size.minDimension - strokeWidth.toPx()
                    val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
                    drawArc(
                        color = trackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = stroke,
                    )
                    drawArc(
                        color = primaryColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = stroke,
                    )
                },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accent)
                    .border(2.dp, outlineColor, CircleShape)
                    .clickable(onClickLabel = label) {
                        if (isListenTogetherGuest) {
                            playerConnection.toggleMute()
                            return@clickable
                        }
                        if (isCasting) {
                            if (castIsPlaying) castHandler?.pause() else castHandler?.play()
                        } else if (playbackState == Player.STATE_ENDED) {
                            playerConnection.player.seekTo(0, 0)
                            playerConnection.player.playWhenReady = true
                        } else {
                            playerConnection.togglePlayPause()
                        }
                    },
        ) {
            Icon(
                painter =
                    painterResource(
                        when {
                            isListenTogetherGuest -> if (isMuted) R.drawable.volume_off else R.drawable.volume_up
                            playbackState == Player.STATE_ENDED -> R.drawable.replay
                            effectiveIsPlaying -> R.drawable.pause
                            else -> R.drawable.play
                        },
                    ),
                contentDescription = label,
                tint = onAccent,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Song info display - title and artist
 */
@Composable
private fun NewMiniPlayerSongInfo(
    mediaMetadata: MediaMetadata?,
    onSurfaceColor: Color,
    errorColor: Color,
    modifier: Modifier = Modifier,
) {
    val error by LocalPlayerConnection.current?.error?.collectAsState() ?: remember { mutableStateOf(null) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
    ) {
        mediaMetadata?.let { metadata ->
            Text(
                text = metadata.title,
                color = onSurfaceColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier.basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp),
            )
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (metadata.explicit) MIcon.Explicit()
                 if (metadata.artists.any { it.name.isNotBlank() }) {
                     Text(
                         text = metadata.artists.joinToArtistString(" ${stringResource(R.string.and)} ") { it.name },
                         color = onSurfaceColor.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier.basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp),
                    )
                }
            }

            AnimatedVisibility(visible = error != null, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    text = stringResource(R.string.error_playing),
                    color = errorColor,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ============================================================================
// ISOLATED BUTTON COMPOSABLES - Prevent parent recomposition
// ============================================================================

@Composable
private fun NextButton(
    enabled: Boolean,
    outlineColor: Color,
    onSurfaceColor: Color,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(width = 1.dp, color = outlineColor.copy(alpha = 0.3f), shape = CircleShape)
                .clickable(enabled = enabled, onClick = onClick),
    ) {
        Icon(
            painter = painterResource(R.drawable.skip_next),
            contentDescription = stringResource(R.string.next),
            tint = onSurfaceColor.copy(alpha = if (enabled) 0.7f else 0.3f),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun FavoriteButton(
    songId: String,
    errorColor: Color,
    outlineColor: Color,
    onSurfaceColor: Color,
) {
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val librarySong by database.song(songId).collectAsStateWithLifecycle(initialValue = null)
    // For episodes, show saved state (inLibrary); for songs, show liked state
    val isEpisode = librarySong?.song?.isEpisode == true
    val isLiked = if (isEpisode) librarySong?.song?.inLibrary != null else librarySong?.song?.liked == true
    val heartBurst = rememberHeartBurst(liked = isLiked, key = librarySong?.song?.id)

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(40.dp)
                .heartBurst(heartBurst, errorColor)
                .clip(CircleShape)
                .border(
                    width = 1.dp,
                    color = if (isLiked) errorColor.copy(alpha = 0.5f) else outlineColor.copy(alpha = 0.3f),
                    shape = CircleShape,
                ).background(
                    color = if (isLiked) errorColor.copy(alpha = 0.1f) else Color.Transparent,
                    shape = CircleShape,
                ).clickable { playerConnection.service.toggleLike() },
    ) {
        Icon(
            painter = painterResource(if (isLiked) R.drawable.favorite else R.drawable.favorite_border),
            contentDescription = null,
            tint = if (isLiked) errorColor else onSurfaceColor.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp),
        )
    }
}

private val MiniPlayerBackdropFade = 16.dp
