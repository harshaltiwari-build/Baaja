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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

private const val VINYL_TURN_MS = 12_000

/** False when the user turned animations off in the system settings. */
@Composable
private fun rememberMotionEnabled(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f }
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
