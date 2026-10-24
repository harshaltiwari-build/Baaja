/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.onboarding

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import com.metrolist.music.ui.theme.rememberBaajaMotionEnabled
import kotlin.math.PI
import kotlin.math.cos

/**
 * One running clock (seconds since the screen appeared) that every splash/welcome animation derives from.
 * Read it only inside graphicsLayer / draw lambdas so the frame updates never recompose anything.
 * With animations turned off ([enabled] false) entrances are finished and every loop rests at its start pose.
 */
class BaajaClock(
    private val state: State<Float>,
    val enabled: Boolean,
) {
    val seconds: Float get() = state.value

    /** 0..1 progress of a one-shot animation that starts after [delay] and lasts [duration] seconds. */
    fun enter(
        delay: Float,
        duration: Float,
    ): Float {
        if (!enabled) return 1f
        return ((seconds - delay) / duration).coerceIn(0f, 1f)
    }

    /** 0..1 position within a repeating cycle of [period] seconds that begins after [delay]; 0 before it starts. */
    fun loop(
        period: Float,
        delay: Float = 0f,
    ): Float {
        if (!enabled) return 0f
        val x = seconds - delay
        return if (x < 0f) 0f else (x / period) % 1f
    }

    /** Ease-in-out 0 -> 1 -> 0 over one cycle: the shape of `0%,100% {a} 50% {b}` keyframes. */
    fun wave(
        period: Float,
        delay: Float = 0f,
    ): Float = 0.5f * (1f - cos(loop(period, delay) * 2f * PI.toFloat()))
}

/** A [BaajaClock] that ticks for an hour; plenty for a screen the user sits on, and free when motion is off. */
@Composable
fun rememberBaajaClock(): BaajaClock {
    val enabled = rememberBaajaMotionEnabled()
    if (!enabled) return BaajaClock(rememberUpdatedState(0f), false)
    val transition = rememberInfiniteTransition(label = "baajaClock")
    val seconds =
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 3600f,
            animationSpec = infiniteRepeatable(tween(3_600_000, easing = LinearEasing), RepeatMode.Restart),
            label = "seconds",
        )
    return BaajaClock(seconds, true)
}

/** CSS-like overshoot easing (`cubic-bezier(.2,1.4,.4,1)` look) for pop-in entrances; maps 0..1 to 0..~1.05..1. */
internal fun easeOutBack(
    p: Float,
    overshoot: Float = 1.6f,
): Float {
    val q = p - 1f
    return 1f + (overshoot + 1f) * q * q * q + overshoot * q * q
}

internal fun lerpF(
    a: Float,
    b: Float,
    t: Float,
): Float = a + (b - a) * t
