/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.player

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import com.metrolist.music.ui.theme.baajaPressScale
import com.metrolist.music.ui.theme.rememberBaajaMotionEnabled
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.sin
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.R
import com.metrolist.music.ui.theme.baajaInk
import com.metrolist.music.ui.theme.baajaSurface
import com.metrolist.music.utils.makeTimeString

/** Colours for Baaja's player controls; they adapt to the player background style. */
internal data class BaajaPlayerColors(
    val ink: Color,
    val content: Color,
    val mutedContent: Color,
    val accent: Color,
    val onAccent: Color,
    val surface: Color,
    val onSurface: Color,
    val track: Color,
)

enum class BaajaSeekStyle { DEFAULT, WAVY, SLIM }

/**
 * Seek bar from the Now Playing mockup: a 6 dp rounded track with the accent fill and a white
 * thumb ringed in the accent. Uses Material's [Slider] for touch handling and accessibility.
 */
@Composable
internal fun BaajaSeekBar(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    colors: BaajaPlayerColors,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showThumb: Boolean = true,
    style: BaajaSeekStyle = BaajaSeekStyle.DEFAULT,
    playing: Boolean = false,
    waveAmplitude: Dp = 3.dp,
) {
    // The wave only animates (and only invalidates drawing) while it is visible: WAVY style and playing.
    val wavy = style == BaajaSeekStyle.WAVY
    val wavePhase = remember { Animatable(0f) }
    LaunchedEffect(wavy, playing) {
        if (!wavy || !playing) return@LaunchedEffect
        while (true) wavePhase.animateTo(wavePhase.value + 1f, tween(1600, easing = LinearEasing))
    }
    val waveAmount by animateFloatAsState(if (playing && style == BaajaSeekStyle.WAVY) 1f else 0f, tween(400), label = "seekWaveAmount")
    val span = valueRange.endInclusive - valueRange.start
    val fraction = if (span > 0f) ((value - valueRange.start) / span).coerceIn(0f, 1f) else 0f
    val seekLabel = stringResource(R.string.baaja_player_seek)
    val safeRange = valueRange.start..maxOf(valueRange.start, valueRange.endInclusive)
    val sliderState = remember(safeRange) { SliderState(value = value.coerceIn(safeRange), trackRange = safeRange) }
    sliderState.value = value.coerceIn(safeRange)
    Slider(
        state = sliderState,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        enabled = enabled,
        colors = SliderDefaults.colors(),
        modifier = modifier.heightIn(min = 32.dp).semantics { contentDescription = seekLabel },
        thumb = {
            if (style == BaajaSeekStyle.SLIM) {
                Box(Modifier.size(2.dp))
            } else if (showThumb) {
                Canvas(Modifier.size(22.dp)) {
                    val outer = size.minDimension / 2f
                    drawCircle(colors.ink, radius = outer)
                    drawCircle(colors.accent, radius = outer - 1.5.dp.toPx())
                    drawCircle(Color.White, radius = outer - 5.5.dp.toPx())
                }
            } else {
                Box(Modifier.size(2.dp))
            }
        },
        track = {
            Canvas(Modifier.fillMaxWidth().height(22.dp)) {
                val slim = style == BaajaSeekStyle.SLIM
                val stroke = (if (slim) 4.dp else 6.dp).toPx()
                val y = size.height / 2f
                val start = stroke / 2f
                val end = size.width - stroke / 2f
                val activeEnd = start + (end - start) * fraction
                drawLine(colors.track, Offset(start, y), Offset(end, y), strokeWidth = stroke, cap = StrokeCap.Round)
                if (fraction > 0f) {
                    val amp = waveAmplitude.toPx() * waveAmount
                    if (style == BaajaSeekStyle.WAVY && amp > 0.1f) {
                        val wavelength = 28.dp.toPx()
                        val path = Path()
                        var x = start
                        path.moveTo(x, y)
                        while (x < activeEnd) {
                            x = minOf(x + 3f, activeEnd)
                            val t = (x - start) / wavelength - wavePhase.value
                            path.lineTo(x, y + amp * sin(t * 2f * PI.toFloat()))
                        }
                        drawPath(path, colors.accent, style = Stroke(width = stroke, cap = StrokeCap.Round))
                    } else {
                        drawLine(colors.accent, Offset(start, y), Offset(activeEnd, y), strokeWidth = stroke, cap = StrokeCap.Round)
                    }
                }
            }
        },
    )
}

/** Compact "1:43 ——— 3:42" row used inside the lyrics and immersive control cards. */
@Composable
internal fun BaajaSeekRow(
    position: Long,
    duration: Long,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    colors: BaajaPlayerColors,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BaajaTimeText(makeTimeString(position), colors.mutedContent)
        BaajaSeekBar(
            value = position.toFloat(),
            valueRange = 0f..duration.coerceAtLeast(0L).toFloat(),
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            colors = colors,
            enabled = enabled,
            showThumb = false,
            modifier = Modifier.weight(1f),
        )
        BaajaTimeText(if (duration > 0) makeTimeString(duration) else "", colors.mutedContent)
    }
}

@Composable
internal fun BaajaTimeText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = color,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        style = TextStyle(fontFeatureSettings = "tnum"),
        maxLines = 1,
        modifier = modifier,
    )
}

/** The big round accent play/pause button with an ink outline and hard shadow. */
@Composable
internal fun BaajaPlayButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    colors: BaajaPlayerColors,
    modifier: Modifier = Modifier,
    size: Dp = 84.dp,
    shape: Shape = CircleShape,
    container: Color = colors.accent,
    content: Color = colors.onAccent,
) {
    val source = remember { MutableInteractionSource() }
    val motionEnabled = rememberBaajaMotionEnabled()
    // Play/pause morph: the glyph swaps with a quick scale+fade while a small spring rotation settles it.
    val turn = remember { Animatable(0f) }
    var previousIcon by remember { mutableIntStateOf(icon) }
    LaunchedEffect(icon) {
        if (previousIcon != icon && motionEnabled) {
            turn.snapTo(-90f)
            turn.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium))
        }
        previousIcon = icon
    }
    Box(
        modifier =
            modifier
                .size(size)
                .baajaSurface(
                    ink = colors.ink,
                    color = container,
                    shape = shape,
                    shadowOffset = (size.value / 22f).coerceIn(2f, 4f).dp,
                    borderWidth = 2.5.dp,
                    interactionSource = source,
                ).clickable(interactionSource = source, indication = LocalIndication.current, role = Role.Button, onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = icon,
            transitionSpec = {
                (fadeIn(tween(140, delayMillis = 40)) + scaleIn(tween(180), initialScale = 0.5f)) togetherWith
                    (fadeOut(tween(90)) + scaleOut(tween(120), targetScale = 0.5f))
            },
            contentAlignment = Alignment.Center,
            label = "playPauseMorph",
            modifier = Modifier.graphicsLayer { rotationZ = turn.value },
        ) { glyph ->
            Icon(
                painter = painterResource(glyph),
                contentDescription = contentDescription,
                tint = content,
                modifier = Modifier.size(size * 0.4f),
            )
        }
    }
}

/** Plain icon control (previous / next) with a 48 dp touch target. */
@Composable
internal fun BaajaControlIcon(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 52.dp,
    iconSize: Dp = 30.dp,
) {
    val source = remember { MutableInteractionSource() }
    Box(
        modifier =
            modifier
                .size(size)
                .alpha(if (enabled) 1f else 0.4f)
                .baajaPressScale(source, pressedScale = 0.88f)
                .clip(CircleShape)
                .clickable(interactionSource = source, indication = LocalIndication.current, enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription, tint = tint, modifier = Modifier.size(iconSize))
    }
}

/** Shuffle / repeat style toggle: a plain icon when off, a yellow outlined disc when on. */
@Composable
internal fun BaajaToggleIcon(
    @DrawableRes icon: Int,
    contentDescription: String,
    active: Boolean,
    onClick: () -> Unit,
    colors: BaajaPlayerColors,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 48.dp,
) {
    val activeContainer = MaterialTheme.colorScheme.secondaryContainer
    val activeContent = MaterialTheme.colorScheme.onSecondaryContainer
    val source = remember { MutableInteractionSource() }
    Box(
        modifier =
            modifier
                .size(size)
                .alpha(if (enabled) 1f else 0.4f)
                .then(
                    if (active) {
                        Modifier.baajaSurface(colors.ink, activeContainer, CircleShape, shadowOffset = 2.dp, interactionSource = source)
                    } else {
                        Modifier.baajaPressScale(source, pressedScale = 0.88f).clip(CircleShape)
                    },
                ).clickable(interactionSource = source, indication = LocalIndication.current, enabled = enabled, role = Role.Switch, onClick = onClick)
                .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = if (active) activeContent else colors.content,
            modifier = Modifier.size(size * 0.52f),
        )
    }
}

/** Outlined pill button (Lyrics, Sleep timer, Queue, Share…) with an optional icon. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun BaajaPlayerPill(
    text: String?,
    onClick: () -> Unit,
    colors: BaajaPlayerColors,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    contentDescription: String? = null,
    active: Boolean = false,
    enabled: Boolean = true,
    activeContainer: Color = MaterialTheme.colorScheme.secondaryContainer,
    activeContent: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    height: Dp = 44.dp,
    shadowOffset: Dp = 2.dp,
    onLongClick: (() -> Unit)? = null,
) {
    val container = if (active) activeContainer else colors.surface
    val content = if (active) activeContent else colors.onSurface
    val source = remember { MutableInteractionSource() }
    Row(
        modifier =
            modifier
                .heightIn(min = height)
                .alpha(if (enabled) 1f else 0.45f)
                .baajaSurface(colors.ink, container, CircleShape, shadowOffset = shadowOffset, interactionSource = source)
                .combinedClickable(
                    interactionSource = source,
                    indication = LocalIndication.current,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                    onLongClick = onLongClick,
                ).then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier)
                .padding(horizontal = if (text == null) 0.dp else 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(20.dp),
            )
        }
        if (text != null) {
            Text(
                text = text,
                color = content,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                style = TextStyle(fontFeatureSettings = "tnum"),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Default player colours for the current theme (light/dark aware via the colour scheme). */
@Composable
internal fun rememberBaajaPlayerColors(
    onImageBackground: Boolean,
    accent: Color = MaterialTheme.colorScheme.primary,
    onAccent: Color = MaterialTheme.colorScheme.onPrimary,
): BaajaPlayerColors {
    val scheme = MaterialTheme.colorScheme
    val ink = baajaInk
    return if (onImageBackground) {
        BaajaPlayerColors(
            ink = Color(0xFF17151F),
            content = Color.White,
            mutedContent = Color.White.copy(alpha = 0.75f),
            accent = accent,
            onAccent = onAccent,
            surface = Color.White,
            onSurface = Color(0xFF17151F),
            track = Color.White.copy(alpha = 0.3f),
        )
    } else {
        BaajaPlayerColors(
            ink = ink,
            content = scheme.onSurface,
            mutedContent = scheme.onSurfaceVariant,
            accent = accent,
            onAccent = onAccent,
            surface = scheme.surface,
            onSurface = scheme.onSurface,
            track = scheme.outlineVariant,
        )
    }
}

/** Ink-on-surface variant used on Baaja's white control cards regardless of the player background. */
@Composable
internal fun BaajaPlayerColors.onCard(): BaajaPlayerColors {
    val scheme = MaterialTheme.colorScheme
    return copy(
        ink = baajaInk,
        content = scheme.onSurface,
        mutedContent = scheme.onSurfaceVariant,
        surface = scheme.surface,
        onSurface = scheme.onSurface,
        track = scheme.outlineVariant,
    )
}

/** Icon-only round outlined button (exit, info, more…). */
@Composable
internal fun BaajaRoundButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    colors: BaajaPlayerColors,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    outlined: Boolean = true,
) {
    val source = remember { MutableInteractionSource() }
    Box(
        modifier =
            modifier
                .size(size)
                .then(
                    if (outlined) {
                        Modifier.baajaSurface(colors.ink, colors.surface, CircleShape, shadowOffset = 2.dp, interactionSource = source)
                    } else {
                        Modifier.baajaPressScale(source, pressedScale = 0.9f).clip(CircleShape)
                    },
                ).clickable(interactionSource = source, indication = LocalIndication.current, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = if (outlined) colors.onSurface else colors.content,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

/** Small outlined label chip, e.g. "OPUS · 160 kbps" or "Immersive". */
@Composable
internal fun BaajaLabelChip(
    text: String,
    colors: BaajaPlayerColors,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    filled: Boolean = true,
    height: Dp = 28.dp,
) {
    Row(
        modifier =
            modifier
                .heightIn(min = height)
                .baajaSurface(colors.ink, if (filled) colors.surface else Color.Transparent, CircleShape, shadowOffset = 0.dp, borderWidth = 1.5.dp)
                .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val content = if (filled) colors.onSurface else colors.content
        if (icon != null) Icon(painterResource(icon), null, tint = content, modifier = Modifier.size(14.dp))
        Text(text, color = content, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}
