package com.metrolist.music.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.metrolist.music.constants.PlayerStyle
import com.metrolist.music.ui.theme.BaajaInk
import com.metrolist.music.ui.theme.BaajaPurple
import com.metrolist.music.ui.theme.BaajaYellow
import com.metrolist.music.ui.theme.baajaOutline
import com.metrolist.music.ui.theme.rememberBaajaMotionEnabled
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Artwork drawn in one of the non-classic [PlayerStyle]s, keeping Baaja's ink outline and offset shadow.
 * [isActive] is true while this item is the current song and it is playing; motion pauses otherwise.
 */
@Composable
fun StyledArtwork(
    style: PlayerStyle,
    artworkUri: String?,
    size: Dp,
    outlineColor: Color,
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val motion = rememberBaajaMotionEnabled()
    val clock = rememberPlayClock(isActive && motion)
    when (style) {
        PlayerStyle.CLASSIC, PlayerStyle.FULL_COVER -> FullCover(artworkUri, size, outlineColor, modifier)
        PlayerStyle.VINYL -> Vinyl(artworkUri, size, outlineColor, clock, modifier)
        PlayerStyle.CASSETTE -> Cassette(artworkUri, size, outlineColor, clock, modifier)
        PlayerStyle.HALO -> Halo(artworkUri, size, outlineColor, clock, isActive && motion, modifier)
    }
}

/** Seconds of playing time; only advances while [running], and keeps its value across pauses. */
@Composable
private fun rememberPlayClock(running: Boolean): MutableFloatState {
    val seconds = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                seconds.floatValue += (now - last) / 1_000_000_000f
                last = now
            }
        }
    }
    return seconds
}

@Composable
private fun Art(
    artworkUri: String?,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current).data(artworkUri).build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
    )
}

@Composable
private fun FullCover(
    artworkUri: String?,
    size: Dp,
    outlineColor: Color,
    modifier: Modifier,
) {
    // Edge to edge square, sharper corners and a heavier sticker shadow than Classic.
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .baajaOutline(outlineColor, shape, borderWidth = 3.dp, shadowOffset = 8.dp)
            .clip(shape),
    ) {
        Art(artworkUri, Modifier.fillMaxSize())
    }
}

@Composable
private fun Vinyl(
    artworkUri: String?,
    size: Dp,
    outlineColor: Color,
    clock: MutableFloatState,
    modifier: Modifier,
) {
    Box(
        modifier
            .size(size)
            .baajaOutline(outlineColor, CircleShape, borderWidth = 3.dp, shadowOffset = 5.dp)
            .clip(CircleShape)
            .graphicsLayer { rotationZ = (clock.floatValue * DEGREES_PER_SECOND) % 360f },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(BaajaInk)
            val radius = this.size.minDimension / 2
            var r = radius * 0.95f
            while (r > radius * 0.4f) {
                drawCircle(Color.White.copy(alpha = 0.07f), r, style = Stroke(1.dp.toPx()))
                r -= 5.dp.toPx()
            }
            // A soft highlight so the spin is visible.
            rotate(-35f) {
                drawArc(
                    Color.White.copy(alpha = 0.12f),
                    startAngle = -20f,
                    sweepAngle = 40f,
                    useCenter = true,
                )
            }
        }
        Art(
            artworkUri,
            Modifier
                .size(size * 0.42f)
                .clip(CircleShape),
        )
        Box(
            Modifier
                .size(size * 0.05f)
                .background(MaterialTheme.colorScheme.surface, CircleShape),
        )
    }
}

@Composable
private fun Cassette(
    artworkUri: String?,
    size: Dp,
    outlineColor: Color,
    clock: MutableFloatState,
    modifier: Modifier,
) {
    val bodyShape = RoundedCornerShape(14.dp)
    val labelShape = RoundedCornerShape(8.dp)
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(1.5f)
            .baajaOutline(outlineColor, bodyShape, borderWidth = 3.dp, shadowOffset = 5.dp)
            .clip(bodyShape)
            .background(BaajaYellow),
    ) {
        Art(
            artworkUri,
            Modifier
                .padding(12.dp)
                .fillMaxWidth()
                .aspectRatio(2.6f)
                .baajaOutline(BaajaInk, labelShape, borderWidth = 2.dp, shadowOffset = 0.dp)
                .clip(labelShape),
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp)
                .fillMaxWidth(0.62f)
                .aspectRatio(2.8f)
                .baajaOutline(BaajaInk, labelShape, borderWidth = 2.dp, shadowOffset = 0.dp)
                .clip(labelShape)
                .background(BaajaInk.copy(alpha = 0.85f)),
        ) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Reel(clock, Modifier.weight(1f))
                Reel(clock, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Reel(
    clock: MutableFloatState,
    modifier: Modifier,
) {
    Canvas(modifier.fillMaxSize().padding(6.dp)) {
        val radius = size.minDimension / 2
        drawCircle(Color.White, radius)
        drawCircle(BaajaInk, radius * 0.35f)
        rotate(clock.floatValue * DEGREES_PER_SECOND * 1.6f) {
            repeat(6) { i ->
                val a = (i * 60f) * (PI / 180f).toFloat()
                drawLine(
                    BaajaInk,
                    start = center + Offset(cos(a), sin(a)) * radius * 0.45f,
                    end = center + Offset(cos(a), sin(a)) * radius * 0.85f,
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

@Composable
private fun Halo(
    artworkUri: String?,
    size: Dp,
    outlineColor: Color,
    clock: MutableFloatState,
    pulsing: Boolean,
    modifier: Modifier,
) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val t = clock.floatValue
            val outer = this.size.minDimension / 2
            val inner = outer * 0.80f
            repeat(HALO_BARS) { i ->
                val a = (i * 360f / HALO_BARS) * (PI / 180f).toFloat()
                val level =
                    if (pulsing) {
                        0.35f + 0.65f * (0.5f + 0.5f * sin(t * 4.1f + i * 0.9f) * cos(t * 2.3f + i * 0.37f))
                    } else {
                        0.25f
                    }
                val dir = Offset(cos(a), sin(a))
                drawLine(
                    if (i % 2 == 0) BaajaPurple else BaajaPurple.copy(alpha = 0.6f),
                    start = center + dir * inner,
                    end = center + dir * (inner + (outer - inner) * level),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }
        Box(
            Modifier
                .size(size * 0.74f)
                .graphicsLayer {
                    val breathe = if (pulsing) 1f + 0.02f * sin(clock.floatValue * 2f) else 1f
                    scaleX = breathe
                    scaleY = breathe
                }.baajaOutline(outlineColor, CircleShape, borderWidth = 3.dp, shadowOffset = 5.dp)
                .clip(CircleShape),
        ) {
            Art(artworkUri, Modifier.fillMaxSize())
        }
    }
}

private const val DEGREES_PER_SECOND = 24f
private const val HALO_BARS = 64
