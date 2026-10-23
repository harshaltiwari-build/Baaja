/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.player

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

private const val GLOW_CYCLE_MS = 24_000
private const val VINYL_TURN_MS = 12_000

/** False when the user turned animations off in the system settings. */
@Composable
private fun rememberMotionEnabled(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f }
}

/**
 * Album-coloured light that drifts slowly behind the player while music plays and holds still when paused.
 * The phase is read only while drawing, so the drift never recomposes the player.
 */
@Composable
fun AmbientGlowBackground(
    colors: List<Color>,
    playing: Boolean,
    modifier: Modifier = Modifier,
) {
    val phase = remember { Animatable(0f) }
    val motionEnabled = rememberMotionEnabled()
    LaunchedEffect(playing, motionEnabled) {
        if (!playing || !motionEnabled) return@LaunchedEffect
        while (true) phase.animateTo(phase.value + 1f, tween(GLOW_CYCLE_MS, easing = LinearEasing))
    }

    val first = colors.getOrElse(0) { Color.DarkGray }
    val second = colors.getOrElse(1) { first }
    val third = colors.getOrElse(2) { second }

    Box(
        modifier.drawBehind {
            val t = phase.value * 2.0 * PI
            val radius = max(size.width, size.height) * 0.6f
            drawRect(Brush.verticalGradient(listOf(first, second, Color.Black)))
            glowBlob(third, 0.25f + 0.15f * cos(t).toFloat(), 0.2f + 0.08f * sin(t).toFloat(), radius)
            glowBlob(first, 0.8f + 0.12f * sin(t * 0.8).toFloat(), 0.45f + 0.1f * cos(t * 0.8).toFloat(), radius * 0.85f)
            glowBlob(second, 0.4f + 0.15f * sin(t * 1.3).toFloat(), 0.85f + 0.06f * cos(t * 1.3).toFloat(), radius)
            drawRect(Color.Black.copy(alpha = 0.25f))
        },
    )
}

private fun DrawScope.glowBlob(color: Color, x: Float, y: Float, radius: Float) {
    val center = Offset(size.width * x, size.height * y)
    drawCircle(
        brush = Brush.radialGradient(listOf(color.copy(alpha = 0.75f), Color.Transparent), center = center, radius = radius),
        radius = radius,
        center = center,
    )
}

/** Slowly turns like a record while [spinning], keeping its angle when paused. */
@Composable
fun rememberVinylRotation(spinning: Boolean): Animatable<Float, AnimationVector1D> {
    val rotation = remember { Animatable(0f) }
    val motionEnabled = rememberMotionEnabled()
    LaunchedEffect(spinning, motionEnabled) {
        if (!spinning || !motionEnabled) return@LaunchedEffect
        while (true) {
            rotation.animateTo(rotation.value + 360f, tween(VINYL_TURN_MS, easing = LinearEasing))
            rotation.snapTo(rotation.value % 360f)
        }
    }
    return rotation
}
