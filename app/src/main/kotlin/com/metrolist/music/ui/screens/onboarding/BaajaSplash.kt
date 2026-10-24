/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.onboarding

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.R
import com.metrolist.music.ui.theme.BaajaInk
import com.metrolist.music.ui.theme.BaajaLogo
import com.metrolist.music.ui.theme.BaajaPurple
import com.metrolist.music.ui.theme.BaajaYellow
import com.metrolist.music.ui.theme.baajaSurface
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Process-wide: the splash plays once per cold start, not on every activity recreation. */
internal object BaajaSplashState {
    @Volatile
    var shown = false
}

/** How long the splash stays before fading out; the app underneath is already starting. */
internal const val SPLASH_MILLIS = 2600L

/** Normal launches only; widget, shortcut, deep-link and share launches go straight into the app. */
internal fun Activity?.isPlainLauncherStart(): Boolean {
    val intent = this?.intent ?: return true
    val action = intent.action
    return intent.data == null && (action == null || action == Intent.ACTION_MAIN)
}

private val EqSplashColors =
    listOf(
        BaajaYellow,
        Color.White,
        Color(0xFFFFD3DE),
        Color.White,
        Color(0xFFBDEFE6),
        Color.White,
        BaajaYellow,
        Color.White,
        Color(0xFFFFD3DE),
    )
private val EqSplashPeriods = floatArrayOf(0.9f, 1.1f, 0.8f, 1.2f, 0.95f, 1.05f, 0.85f, 1.15f, 0.9f)
private val EqSplashDelays = floatArrayOf(0f, 0.15f, 0.3f, 0.05f, 0.4f, 0.2f, 0.35f, 0.1f, 0.45f)

/**
 * Launch splash from SplashMotion.dc.html: the logo pops in and bobs with rings radiating from it, "baaja"
 * drops in letter by letter, stickers float, nine equaliser bars and three dots pulse underneath.
 * Everything is driven by one [BaajaClock] and read in graphicsLayer/draw blocks (no recomposition per frame).
 */
@Composable
fun BaajaSplash(modifier: Modifier = Modifier) {
    val clock = rememberBaajaClock()
    val density = LocalDensity.current.density

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(BaajaPurple)
            .clearAndSetSemantics { },
    ) {
        val h = maxHeight
        val logoTop = h * 0.308f

        // Floating stickers.
        Box(
            Modifier
                .offset(36.dp, h * 0.13f)
                .graphicsLayer {
                    alpha = clock.enter(0.35f, 0.5f)
                    val w = clock.wave(4.2f)
                    translationY = -16.dp.toPx() * w
                    rotationZ = 10f * w
                }.size(54.dp)
                .baajaSurface(BaajaInk, BaajaYellow, CircleShape, shadowOffset = 3.dp, borderWidth = 2.5.dp),
        )
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset((-44).dp, h * 0.2f)
                .graphicsLayer {
                    alpha = clock.enter(0.35f, 0.5f)
                    val w = clock.wave(3.6f, 0.6f)
                    translationY = -16.dp.toPx() * w
                    rotationZ = 18f + 10f * w
                }.size(44.dp)
                .baajaSurface(BaajaInk, Color(0xFFFFD3DE), RoundedCornerShape(10.dp), shadowOffset = 3.dp, borderWidth = 2.5.dp),
        )
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset((-36).dp, h * 0.71f)
                .graphicsLayer {
                    alpha = clock.enter(0.35f, 0.5f)
                    val s = 1f + 0.22f * clock.wave(2.6f)
                    scaleX = s
                    scaleY = s
                }.size(78.dp)
                .border(3.dp, BaajaInk, CircleShape),
        )
        Box(
            Modifier
                .offset(58.dp, h * 0.76f)
                .graphicsLayer {
                    alpha = clock.enter(0.35f, 0.5f)
                    val a = clock.loop(5f) * 2f * PI.toFloat()
                    translationX = 14.dp.toPx() * cos(a)
                    translationY = 14.dp.toPx() * sin(a)
                }.size(32.dp)
                .baajaSurface(BaajaInk, Color(0xFFBDEFE6), CircleShape, shadowOffset = 2.dp, borderWidth = 2.5.dp),
        )
        // Squiggle whose dashes flow along the line.
        Canvas(
            Modifier
                .offset(150.dp, h * 0.133f)
                .graphicsLayer { alpha = clock.enter(0.35f, 0.5f) }
                .size(64.dp, 40.dp),
        ) {
            val u = density
            val path =
                Path().apply {
                    moveTo(2 * u, 20 * u)
                    cubicTo(8 * u, 6 * u, 14 * u, 6 * u, 20 * u, 20 * u)
                    cubicTo(26 * u, 34 * u, 32 * u, 34 * u, 38 * u, 20 * u)
                    cubicTo(44 * u, 6 * u, 50 * u, 6 * u, 56 * u, 20 * u)
                }
            val phase = 16f * u * clock.loop(1.6f)
            drawPath(
                path,
                BaajaInk,
                style =
                    Stroke(
                        3 * u,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10 * u, 6 * u), phase),
                    ),
            )
        }

        // Rings radiating from the logo.
        listOf(0f, 0.8f, 1.6f).forEach { delay ->
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = logoTop)
                    .graphicsLayer {
                        val seconds = clock.loop(2.4f, 1.0f + delay)
                        val started = clock.enabled && clock.enter(1.0f + delay, 0.01f) > 0f
                        val s = lerpF(0.8f, 2.4f, seconds)
                        scaleX = s
                        scaleY = s
                        alpha = if (started) 0.6f * (1f - seconds) else 0f
                    }.size(128.dp)
                    .border(3.dp, Color.White, RoundedCornerShape(34.dp)),
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = logoTop),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            BaajaLogo(
                Modifier.graphicsLayer {
                    // Starts where the system splash leaves the logo (screen centre, same size) so the hand-off has no jump,
                    // then glides up into place with a small overshoot while the rest of the scene arrives.
                    val m = clock.enter(0.3f, 0.85f)
                    if (m < 1f) {
                        val eased = easeOutBack(m, 1.2f)
                        translationY = (1f - eased) * (h.toPx() * 0.5f - (logoTop.toPx() + 64.dp.toPx()))
                        val bump = 1f + 0.08f * sin(PI.toFloat() * m)
                        scaleX = bump
                        scaleY = bump
                    } else if (clock.enabled) {
                        val w = clock.wave(3.4f, 1.15f)
                        translationY = -10.dp.toPx() * w
                        rotationZ = -2f + 4f * w
                    }
                },
                size = 128.dp,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val letters = stringResource(R.string.baaja_onb_wordmark)
                Row {
                    letters.forEachIndexed { index, letter ->
                        Text(
                            letter.toString(),
                            style =
                                TextStyle(
                                    fontSize = 64.sp,
                                    lineHeight = 64.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    shadow = Shadow(BaajaInk, Offset(3f * density, 4f * density), 0f),
                                ),
                            modifier =
                                Modifier.graphicsLayer {
                                    val p = clock.enter(0.8f + 0.09f * index, 0.7f)
                                    if (p < 1f || clock.enter(1.95f + 0.2f * index, 0.01f) == 0f) {
                                        // Drop: +46dp & small -> overshoot at 70% -> rest.
                                        val a = (p / 0.7f).coerceAtMost(1f)
                                        val b = ((p - 0.7f) / 0.3f).coerceIn(0f, 1f)
                                        translationY = (if (p < 0.7f) lerpF(46f, -10f, a) else lerpF(-10f, 0f, b)).dp.toPx()
                                        val s = if (p < 0.7f) lerpF(0.5f, 1.08f, a) else lerpF(1.08f, 1f, b)
                                        scaleX = s
                                        scaleY = s
                                        alpha = a
                                    } else {
                                        translationY = -7.dp.toPx() * clock.wave(2.2f, 1.95f + 0.2f * index)
                                    }
                                },
                        )
                    }
                }
                Text(
                    stringResource(R.string.baaja_onb_splash_tagline),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier
                            .padding(horizontal = 24.dp)
                            .graphicsLayer {
                                val p = clock.enter(1.5f, 0.7f)
                                alpha = p
                                translationY = (1f - p) * 14.dp.toPx()
                            },
                )
            }
        }

        // Equaliser bars above the loading dots.
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 150.dp)
                .height(56.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            EqSplashColors.forEachIndexed { index, color ->
                Box(
                    Modifier
                        .width(10.dp)
                        .height(56.dp)
                        .graphicsLayer {
                            alpha = clock.enter(0.9f, 0.5f)
                            transformOrigin = TransformOrigin(0.5f, 1f)
                            scaleY = 0.22f + 0.78f * clock.wave(EqSplashPeriods[index], EqSplashDelays[index])
                        }.background(color, RoundedCornerShape(6.dp))
                        .border(2.dp, BaajaInk, RoundedCornerShape(6.dp)),
                )
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(Color.White, BaajaYellow, Color.White).forEachIndexed { index, fill ->
                    Box(
                        Modifier
                            .size(14.dp)
                            .graphicsLayer {
                                val w = clock.wave(1.2f, 0.2f * index)
                                val s = 0.7f + 0.6f * w
                                scaleX = s
                                scaleY = s
                                alpha = (0.55f + 0.45f * w) * clock.enter(1.0f, 0.5f)
                            }.background(fill, CircleShape)
                            .border(2.dp, BaajaInk, CircleShape),
                    )
                }
            }
            Text(
                stringResource(R.string.baaja_onb_splash_tuning),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.graphicsLayer { alpha = clock.enter(1.1f, 0.5f) },
            )
        }
    }
}
