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
import androidx.compose.foundation.layout.statusBars
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
internal const val SPLASH_MILLIS = 4000L

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

/** Seconds on the launch timeline (see "01c Launch (smooth)" in the design canvas). */
private const val T_HOLD = 0.4f
private const val T_RISE = 0.78f
private const val T_SETTLE = 1.18f
private const val T_LEAVE = 3.1f
private const val T_MID = 3.55f
private const val T_DOCK = 3.95f

private fun smooth(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}

private fun segment(
    now: Float,
    from: Float,
    to: Float,
): Float = smooth((now - from) / (to - from))

/** A circle that shrinks onto [center]; everything outside it is cut away, revealing the app underneath. */
private class RevealShape(
    private val center: Offset,
    private val radius: Float,
) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density,
    ): androidx.compose.ui.graphics.Outline =
        androidx.compose.ui.graphics.Outline.Generic(
            Path().apply { addOval(androidx.compose.ui.geometry.Rect(center, radius.coerceAtLeast(0f))) },
        )
}

/**
 * Launch animation. It starts as an exact copy of the system splash (logo centred, same size), so the hand-off
 * has no jump; the logo then rises into place while the scene pops in, holds, and finally a circle of purple
 * shrinks onto the logo as the logo flies up into the Home header. Driven by one [BaajaClock] and read in
 * graphicsLayer/draw blocks (no recomposition per frame).
 */
@Composable
fun BaajaSplash(modifier: Modifier = Modifier) {
    val clock = rememberBaajaClock()
    val density = LocalDensity.current.density
    val topInset = WindowInsets.statusBars.getTop(LocalDensity.current).toFloat()

    BoxWithConstraints(modifier.fillMaxSize().clearAndSetSemantics { }) {
        val h = maxHeight
        val logoTop = h * 0.308f
        val wPx = with(LocalDensity.current) { maxWidth.toPx() }
        val hPx = with(LocalDensity.current) { h.toPx() }
        val logoSizePx = 128f * density
        val restTopPx = with(LocalDensity.current) { logoTop.toPx() }
        val restCenter = Offset(wPx / 2f, restTopPx + logoSizePx / 2f)
        val screenCenter = Offset(wPx / 2f, hPx / 2f)
        val dockCenter = Offset(34f * density, topInset + 32f * density)
        val dockScale = 36f / 128f

        // The purple scene, cut by the shrinking circle during the last second.
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val p = clock.enter(T_LEAVE, T_DOCK - T_LEAVE)
                    val reveal = if (clock.enabled) segment(clock.seconds, T_LEAVE + 0.1f, T_DOCK) else 1f
                    val full = kotlin.math.hypot(wPx, hPx)
                    shape = RevealShape(restCenter, full * (1f - reveal))
                    clip = true
                    alpha = if (p >= 1f) 0f else 1f
                }.background(BaajaPurple),
        ) {
            SplashScene(clock, density, logoTop)
        }

        // The logo travels outside the clipped scene: centre -> rest -> header.
        BaajaLogo(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = logoTop)
                .graphicsLayer {
                    val now = clock.seconds
                    if (!clock.enabled) return@graphicsLayer
                    val toCentre = screenCenter - restCenter
                    var tx: Float
                    var ty: Float
                    var sc: Float
                    var rot: Float
                    when {
                        now < T_HOLD -> {
                            tx = toCentre.x; ty = toCentre.y; sc = 1f; rot = 0f
                        }
                        now < T_RISE -> {
                            val p = segment(now, T_HOLD, T_RISE)
                            tx = toCentre.x * (1f - p)
                            ty = toCentre.y * (1f - p) - 18f * density * p
                            sc = 1f + 0.08f * p
                            rot = -3f * p
                        }
                        now < T_SETTLE -> {
                            val p = segment(now, T_RISE, T_SETTLE)
                            tx = 0f
                            ty = -18f * density * (1f - p)
                            sc = 1.08f - 0.08f * p
                            rot = -3f * (1f - p)
                        }
                        now < T_LEAVE -> {
                            tx = 0f
                            ty = -9f * density * clock.wave(3.4f, T_SETTLE)
                            sc = 1f
                            rot = -2f + 4f * clock.wave(3.4f, T_SETTLE)
                        }
                        now < T_MID -> {
                            val p = segment(now, T_LEAVE, T_MID)
                            val midX = (dockCenter.x + restCenter.x) / 2f - restCenter.x
                            val midY = (dockCenter.y + restCenter.y) / 2f - restCenter.y - 40f * density
                            tx = midX * p
                            ty = midY * p
                            sc = 1f - (1f - 0.62f) * p
                            rot = -8f * p
                        }
                        else -> {
                            val p = segment(now, T_MID, T_DOCK)
                            val midX = (dockCenter.x + restCenter.x) / 2f - restCenter.x
                            val midY = (dockCenter.y + restCenter.y) / 2f - restCenter.y - 40f * density
                            tx = lerpF(midX, dockCenter.x - restCenter.x, p)
                            ty = lerpF(midY, dockCenter.y - restCenter.y, p)
                            sc = lerpF(0.62f, dockScale, p)
                            rot = -8f * (1f - p)
                        }
                    }
                    translationX = tx
                    translationY = ty
                    scaleX = sc
                    scaleY = sc
                    rotationZ = rot
                },
            size = 128.dp,
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.SplashScene(
    clock: BaajaClock,
    density: Float,
    logoTop: androidx.compose.ui.unit.Dp,
) {
    val h = logoTop / 0.308f
    val popAlpha: (Float) -> Float = { start -> clock.enter(start, 0.5f) }

    // Floating stickers, popping in one after another.
    Box(
        Modifier
            .offset(36.dp, h * 0.13f)
            .graphicsLayer {
                val w = clock.wave(4.2f, 1f)
                translationY = -16.dp.toPx() * w
                rotationZ = 10f * w
                alpha = popAlpha(0.9f)
                val sc = easeOutBack(clock.enter(0.9f, 0.55f))
                scaleX = sc; scaleY = sc
            }.size(54.dp)
            .baajaSurface(BaajaInk, BaajaYellow, CircleShape, shadowOffset = 3.dp, borderWidth = 2.5.dp),
    )
    Box(
        Modifier
            .align(Alignment.TopEnd)
            .offset((-44).dp, h * 0.2f)
            .graphicsLayer {
                val w = clock.wave(3.6f, 1.6f)
                translationY = -16.dp.toPx() * w
                rotationZ = 18f + 10f * w
                alpha = popAlpha(1.0f)
                val sc = easeOutBack(clock.enter(1.0f, 0.55f))
                scaleX = sc; scaleY = sc
            }.size(44.dp)
            .baajaSurface(BaajaInk, Color(0xFFFFD3DE), RoundedCornerShape(10.dp), shadowOffset = 3.dp, borderWidth = 2.5.dp),
    )
    Box(
        Modifier
            .align(Alignment.TopEnd)
            .offset((-36).dp, h * 0.71f)
            .graphicsLayer {
                alpha = popAlpha(1.1f)
                val s = easeOutBack(clock.enter(1.1f, 0.55f)) * (1f + 0.22f * clock.wave(2.6f, 1.6f))
                scaleX = s; scaleY = s
            }.size(78.dp)
            .border(3.dp, BaajaInk, CircleShape),
    )
    Box(
        Modifier
            .offset(58.dp, h * 0.76f)
            .graphicsLayer {
                alpha = popAlpha(1.2f)
                val a = clock.loop(5f, 1.2f) * 2f * PI.toFloat()
                translationX = 14.dp.toPx() * cos(a)
                translationY = 14.dp.toPx() * sin(a)
                val sc = easeOutBack(clock.enter(1.2f, 0.55f))
                scaleX = sc; scaleY = sc
            }.size(32.dp)
            .baajaSurface(BaajaInk, Color(0xFFBDEFE6), CircleShape, shadowOffset = 2.dp, borderWidth = 2.5.dp),
    )
    Canvas(
        Modifier
            .offset(150.dp, h * 0.133f)
            .graphicsLayer { alpha = popAlpha(1.3f) }
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
        drawPath(
            path,
            BaajaInk,
            style =
                Stroke(
                    3 * u,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10 * u, 6 * u), 16f * u * clock.loop(1.6f)),
                ),
        )
    }

    // Rings radiating from the logo once it has landed.
    listOf(0f, 0.4f, 0.8f).forEach { delay ->
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = logoTop)
                .graphicsLayer {
                    val start = T_SETTLE - 0.05f + delay
                    val seconds = clock.loop(2.4f, start)
                    val started = clock.enabled && clock.enter(start, 0.01f) > 0f
                    val live = 1f - segment(clock.seconds, T_LEAVE - 0.2f, T_LEAVE + 0.1f)
                    val s = lerpF(0.8f, 2.2f, seconds)
                    scaleX = s; scaleY = s
                    alpha = if (started) 0.6f * (1f - seconds) * live else 0f
                }.size(128.dp)
                .border(3.dp, Color.White, RoundedCornerShape(34.dp)),
        )
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = logoTop + 128.dp + 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
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
                            val p = clock.enter(1.25f + 0.09f * index, 0.7f)
                            val out = segment(clock.seconds, T_LEAVE, T_LEAVE + 0.4f)
                            val a = (p / 0.7f).coerceAtMost(1f)
                            val b = ((p - 0.7f) / 0.3f).coerceIn(0f, 1f)
                            val rise = if (p < 0.7f) lerpF(46f, -10f, a) else lerpF(-10f, 0f, b)
                            translationY = rise.dp.toPx() - 26.dp.toPx() * out
                            val sc = if (p < 0.7f) lerpF(0.5f, 1.08f, a) else lerpF(1.08f, 1f, b)
                            scaleX = sc * (1f - 0.2f * out)
                            scaleY = sc * (1f - 0.2f * out)
                            alpha = a * (1f - out)
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
                    .padding(top = 4.dp, start = 24.dp, end = 24.dp)
                    .graphicsLayer {
                        val p = clock.enter(1.9f, 0.6f)
                        val out = segment(clock.seconds, T_LEAVE, T_LEAVE + 0.3f)
                        alpha = p * (1f - out)
                        translationY = (1f - p) * 14.dp.toPx() - 12.dp.toPx() * out
                    },
        )
    }

    // Equaliser bars and the loading line, gone before the circle closes.
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
                        transformOrigin = TransformOrigin(0.5f, 1f)
                        val grow = segment(clock.seconds, 2.0f + 0.05f * index, 2.5f + 0.05f * index)
                        val out = 1f - segment(clock.seconds, T_LEAVE, T_LEAVE + 0.35f)
                        scaleY = (0.22f + 0.78f * clock.wave(EqSplashPeriods[index], EqSplashDelays[index] + 2f)) * grow * out
                    }.background(color, RoundedCornerShape(6.dp))
                    .border(2.dp, BaajaInk, RoundedCornerShape(6.dp)),
            )
        }
    }
    Text(
        stringResource(R.string.baaja_onb_splash_tuning),
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color.White,
        modifier =
            Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 64.dp)
                .graphicsLayer {
                    alpha = clock.enter(2.2f, 0.4f) * (1f - segment(clock.seconds, T_LEAVE, T_LEAVE + 0.3f))
                },
    )
}
