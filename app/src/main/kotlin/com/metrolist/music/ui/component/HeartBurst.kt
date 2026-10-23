/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val BURST_DURATION_MS = 650
private const val BURST_PARTICLES = 8

@Stable
class HeartBurstState internal constructor() {
    internal val progress = Animatable(1f)
}

/**
 * Plays a burst when [liked] turns true for the same [key]; changing [key] (e.g. the song id)
 * resets the baseline so switching to an already-liked song doesn't burst.
 */
@Composable
fun rememberHeartBurst(liked: Boolean, key: Any?): HeartBurstState {
    val state = remember { HeartBurstState() }
    val baseline = remember(key) { booleanArrayOf(liked) }
    LaunchedEffect(key, liked) {
        val wasLiked = baseline[0]
        baseline[0] = liked
        if (liked && !wasLiked) {
            state.progress.snapTo(0f)
            state.progress.animateTo(1f, tween(BURST_DURATION_MS, easing = LinearOutSlowInEasing))
        }
    }
    return state
}

/** Pops the content and draws a ring and radiating dots while [state] is animating. */
fun Modifier.heartBurst(state: HeartBurstState, color: Color): Modifier =
    graphicsLayer {
        val p = state.progress.value
        val pop = if (p < 1f) 1f + 0.22f * sin(PI.toFloat() * p) else 1f
        scaleX = pop
        scaleY = pop
    }.drawWithContent {
        drawContent()
        val p = state.progress.value
        if (p >= 1f) return@drawWithContent

        val fade = 1f - p
        val base = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        drawCircle(
            color = color,
            radius = base * (0.85f + 0.65f * p),
            center = center,
            alpha = fade * 0.6f,
            style = Stroke(width = base * 0.14f * fade),
        )
        repeat(BURST_PARTICLES) { i ->
            val angle = (i * 360.0 / BURST_PARTICLES - 90.0) * PI / 180.0
            val distance = base * (0.9f + (if (i % 2 == 0) 1.05f else 0.8f) * p)
            drawCircle(
                color = color,
                radius = base * (if (i % 2 == 0) 0.13f else 0.09f) * fade,
                center = center + Offset(cos(angle).toFloat() * distance, sin(angle).toFloat() * distance),
                alpha = fade,
            )
        }
    }
