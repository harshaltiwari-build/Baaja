/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.metrolist.music.ui.theme.BaajaLogo
import com.metrolist.music.ui.theme.BaajaPurple
import com.metrolist.music.ui.theme.rememberBaajaMotionEnabled
import kotlinx.coroutines.delay

/**
 * The splash standing still: the exact first frame of both launch animations (purple, logo centred at the
 * system splash's size). It is shown while the app composes behind it, so the heavy first draw happens here
 * and not in the middle of the animation.
 */
@Composable
fun BaajaSplashHold(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().clearAndSetSemantics { }.background(BaajaPurple)) {
        BaajaLogo(Modifier.align(Alignment.Center), size = 128.dp)
    }
}

/** A brief pause so the hand-off from the system splash is invisible before the logo starts to leave. */
private const val QUICK_OPEN_HOLD_MILLIS = 60L

/** The whole exit; about a fifth of a second, so everyday launches feel immediate. */
private const val QUICK_OPEN_MILLIS = 220

/**
 * The everyday launch animation. It starts as an exact copy of the system splash (purple, logo centred, same
 * size), then the logo swells and fades while the purple dissolves to reveal the app that is already composed
 * underneath. The whole thing takes under 300 ms and does no work per frame beyond a graphicsLayer update.
 * The first-ever launch uses the longer [BaajaSplash] instead.
 */
@Composable
fun BaajaQuickOpen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val motionEnabled = rememberBaajaMotionEnabled()
    val progress = remember { Animatable(0f) }
    val finished = rememberUpdatedState(onFinished)

    LaunchedEffect(Unit) {
        if (motionEnabled) {
            delay(QUICK_OPEN_HOLD_MILLIS)
            progress.animateTo(1f, tween(QUICK_OPEN_MILLIS, easing = FastOutSlowInEasing))
        }
        finished.value()
    }

    Box(modifier.fillMaxSize().clearAndSetSemantics { }) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 1f - progress.value }
                .background(BaajaPurple),
        )
        BaajaLogo(
            Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    val p = progress.value
                    val scale = 1f + 0.16f * p
                    scaleX = scale
                    scaleY = scale
                    alpha = 1f - p
                },
            size = 128.dp,
        )
    }
}
