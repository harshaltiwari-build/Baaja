/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.metrolist.music.ui.theme.BaajaLogo
import com.metrolist.music.ui.theme.BaajaPurple
import com.metrolist.music.ui.theme.rememberBaajaMotionEnabled
import kotlinx.coroutines.delay
import kotlin.math.hypot

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

/** A brief pause so the hand-off from the system splash is invisible before the logo starts to move. */
private const val QUICK_OPEN_HOLD_MILLIS = 60L

/** The whole animation: about half a second, so everyday launches still feel immediate. */
private const val QUICK_OPEN_MILLIS = 460

/** Starts quickly and settles gently, so the circle closes briskly and lands softly on the logo. */
private val QuickOpenEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

/**
 * The everyday launch animation, played on every launch after the first. It starts as an exact copy of the
 * system splash (purple, logo centred, same size). The logo pops up a little, then a circle of purple closes
 * onto it while it shrinks away, revealing the app that is already composed underneath: the same motion the
 * first-ever intro ends with, in under half a second. Nothing is recomposed per frame; the circle and the logo
 * are driven from graphicsLayer blocks. The first-ever launch uses the longer [BaajaSplash] instead.
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
            progress.animateTo(1f, tween(QUICK_OPEN_MILLIS, easing = QuickOpenEasing))
        }
        finished.value()
    }

    BoxWithConstraints(modifier.fillMaxSize().clearAndSetSemantics { }) {
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val heightPx = with(LocalDensity.current) { maxHeight.toPx() }
        val center = Offset(widthPx / 2f, heightPx / 2f)
        val reach = hypot(widthPx, heightPx)

        // The purple, cut by a circle that closes onto the logo as the animation runs.
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val reveal = ((progress.value - 0.12f) / 0.88f).coerceIn(0f, 1f)
                    shape = RevealShape(center, reach * (1f - reveal))
                    clip = true
                }.background(BaajaPurple),
        )
        // The logo pops up first, then shrinks and fades as the circle reaches it.
        BaajaLogo(
            Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    val p = progress.value
                    val scale = if (p < 0.28f) 1f + 0.14f * (p / 0.28f) else 1.14f - 0.52f * ((p - 0.28f) / 0.72f)
                    scaleX = scale
                    scaleY = scale
                    alpha = if (p < 0.7f) 1f else 1f - ((p - 0.7f) / 0.25f).coerceIn(0f, 1f)
                },
            size = 128.dp,
        )
    }
}
