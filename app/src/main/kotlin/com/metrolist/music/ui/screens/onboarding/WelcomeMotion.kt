/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.metrolist.music.ui.theme.BaajaInk
import com.metrolist.music.ui.theme.BaajaYellow

private val BarPeriods = floatArrayOf(0.9f, 1.1f, 0.8f, 1.2f, 0.95f, 1.05f, 0.85f, 1.15f, 0.9f, 1.0f, 0.8f, 1.1f)
private val BarDelays = floatArrayOf(0f, 0.12f, 0.3f, 0.05f, 0.4f, 0.2f, 0.35f, 0.1f, 0.45f, 0.25f, 0.5f, 0.15f)

/** Twelve ink equaliser bars (11 dp wide, up to [barHeight] tall) that bounce up from the card's bottom edge. */
@Composable
internal fun WelcomeEqualizer(
    clock: BaajaClock,
    modifier: Modifier = Modifier,
    barHeight: Dp = 70.dp,
    horizontalArrangement: androidx.compose.foundation.layout.Arrangement.Horizontal =
        androidx.compose.foundation.layout.Arrangement.SpaceBetween,
) {
    Row(modifier.height(barHeight), horizontalArrangement = horizontalArrangement, verticalAlignment = Alignment.Bottom) {
        repeat(12) { index ->
            androidx.compose.foundation.layout.Box(
                Modifier
                    .width(11.dp)
                    .height(barHeight)
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0.5f, 1f)
                        scaleY = 0.2f + 0.8f * clock.wave(BarPeriods[index], BarDelays[index])
                    }.background(BaajaInk, RoundedCornerShape(6.dp)),
            )
        }
    }
}

/** Record with groove rings, a yellow label and a centre hole; the whole layer rotates once every 5 s. */
@Composable
internal fun WelcomeVinyl(
    clock: BaajaClock,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier
            .width(size)
            .height(size)
            .graphicsLayer { rotationZ = 360f * clock.loop(5f) },
    ) {
        val r = this.size.minDimension / 2f
        val c = center
        val unit = this.size.minDimension / 260f
        // Alternating ink / dark-grey bands every couple of dp, like the design's repeating radial gradient.
        var radius = r
        var dark = true
        while (radius > 0f) {
            drawCircle(if (dark) BaajaInk else Color(0xFF34303F), radius, c)
            radius -= (if (dark) 3f else 2f) * unit
            dark = !dark
        }
        drawCircle(BaajaInk, r - 1.25f * unit, c, style = Stroke(2.5f * unit))
        val label = 46f * unit
        drawCircle(BaajaYellow, label, c)
        drawCircle(BaajaInk, label, c, style = Stroke(2.5f * unit))
        drawCircle(BaajaInk, 7f * unit, c)
    }
}

/** A music note (two heads, stems and a beam) drawn at 26x30 dp, filled with [fill] and outlined in ink. */
@Composable
internal fun WelcomeNote(
    clock: BaajaClock,
    delay: Float,
    drift: Dp,
    spin: Float,
    fill: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier
            .width(26.dp)
            .height(30.dp)
            .graphicsLayer {
                // 3.6 s rise of 150 dp: scale .6 -> 1.1, fading in over the first 15% and out by the end.
                val started = clock.enabled && clock.enter(delay, 0.01f) > 0f
                val p = clock.loop(3.6f, delay)
                translationY = -150.dp.toPx() * p
                translationX = drift.toPx() * p
                rotationZ = spin * p
                val s = 0.6f + 0.5f * p
                scaleX = s
                scaleY = s
                alpha = if (!started) 0f else if (p < 0.15f) p / 0.15f else 1f - (p - 0.15f) / 0.85f
            },
    ) {
        val u = size.width / 26f
        fun shape(path: Path) {
            drawPath(path, fill)
            drawPath(path, BaajaInk, style = Stroke(1.6f * u))
        }
        fun head(x: Float) =
            shape(Path().apply { addOval(androidx.compose.ui.geometry.Rect(Offset(x * u, 17f * u), Size(9.4f * u, 9.4f * u))) })
        fun stem(x: Float, top: Float) =
            shape(Path().apply { addRect(androidx.compose.ui.geometry.Rect(Offset(x * u, top * u), Size(2.4f * u, (21.7f - top) * u))) })
        stem(7.2f, 3f)
        stem(21.4f, 5.6f)
        shape(
            Path().apply {
                moveTo(7.2f * u, 3f * u)
                lineTo(23.8f * u, 0.6f * u)
                lineTo(23.8f * u, 6.4f * u)
                lineTo(7.2f * u, 9f * u)
                close()
            },
        )
        head(0.8f)
        head(15f)
    }
}

/** Sticker bob: up 8 dp and a few degrees extra tilt, then back; [baseRotation] keeps the sticker's own tilt. */
internal fun Modifier.stickerBob(
    clock: BaajaClock,
    period: Float,
    delay: Float,
    baseRotation: Float,
): Modifier =
    graphicsLayer {
        val w = clock.wave(period, delay)
        translationY = -8.dp.toPx() * w
        rotationZ = baseRotation + 3f * w
    }

/** Fade up: starts [delay] seconds in, rises 16 dp into place over .7 s. Hidden until it starts. */
internal fun Modifier.fadeUp(
    clock: BaajaClock,
    delay: Float,
): Modifier =
    graphicsLayer {
        val p = clock.enter(delay, 0.7f)
        alpha = p
        translationY = (1f - p) * 16.dp.toPx()
    }
