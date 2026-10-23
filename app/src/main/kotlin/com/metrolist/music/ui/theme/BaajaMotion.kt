/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import kotlinx.coroutines.launch

/** Baaja's motion vocabulary: short, springy, and always cheap (graphicsLayer / draw-phase only). */
object BaajaMotion {
    const val ScreenMillis = 300
    const val FadeInMillis = 220
    const val FadeOutMillis = 90
    const val PressedScale = 0.96f
    const val EntranceMillis = 320
    const val EntranceStaggerMillis = 40
    const val EntranceMaxItems = 8

    val PressSpring = spring<Float>(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow * 2f)
    val SoftSpring = spring<Float>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
}

/** True unless the user turned animations off in the system developer/accessibility settings. */
fun baajaMotionEnabled(context: Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f

@Composable
fun rememberBaajaMotionEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) { baajaMotionEnabled(context) }
}

/**
 * 0..1 press progress for [source], springing in on press and out on release/cancel.
 * Only the returned State changes, so read it inside graphicsLayer/draw lambdas to avoid recomposition.
 */
@Composable
fun rememberBaajaPressProgress(source: InteractionSource): State<Float> {
    val progress = remember { Animatable(0f) }
    val enabled = rememberBaajaMotionEnabled()
    val scope = rememberCoroutineScope()
    LaunchedEffect(source, enabled) {
        source.interactions.collect { interaction ->
            val target =
                when (interaction) {
                    is PressInteraction.Press -> 1f
                    is PressInteraction.Release, is PressInteraction.Cancel -> 0f
                    else -> return@collect
                }
            if (!enabled) {
                progress.snapTo(target)
            } else {
                scope.launch { progress.animateTo(target, BaajaMotion.PressSpring) }
            }
        }
    }
    return progress.asState()
}

/**
 * Sticker-press feedback: the element springs down to ~0.96 and, when [shadowOffset] is set, slides toward
 * its hard shadow as if pressed onto the page. Reads only in the layer block, so nothing recomposes.
 */
fun Modifier.baajaPressScale(
    interactionSource: InteractionSource,
    shadowOffset: Dp = 0.dp,
    pressedScale: Float = BaajaMotion.PressedScale,
): Modifier =
    composed {
        val progress = rememberBaajaPressProgress(interactionSource)
        graphicsLayer {
            val p = progress.value
            if (p != 0f) {
                val s = 1f - (1f - pressedScale) * p
                scaleX = s
                scaleY = s
                if (shadowOffset > 0.dp) {
                    val shift = shadowOffset.toPx() * 0.75f * p
                    translationX = shift
                    translationY = shift * 1.33f
                }
            }
        }
    }

/**
 * Light entrance for list/grid items: fades and slides up by a few dp, staggered by [index] (capped), played
 * once per [playKey] (e.g. per screen visit). Items past [BaajaMotion.EntranceMaxItems] appear instantly.
 */
fun Modifier.baajaEntrance(
    index: Int,
    playKey: Any? = Unit,
): Modifier =
    if (index < 0 || index >= BaajaMotion.EntranceMaxItems) {
        this
    } else {
        composed {
            val enabled = rememberBaajaMotionEnabled()
            val played = remember(playKey) { BaajaEntranceRegistry.hasPlayed(playKey, index) }
            val progress = remember(playKey, index) { Animatable(if (enabled && !played) 0f else 1f) }
            LaunchedEffect(progress) {
                if (progress.value < 1f) {
                    BaajaEntranceRegistry.markPlayed(playKey, index)
                    progress.animateTo(
                        1f,
                        tween(
                            BaajaMotion.EntranceMillis,
                            delayMillis = index * BaajaMotion.EntranceStaggerMillis,
                            easing = FastOutSlowInEasing,
                        ),
                    )
                }
            }
            graphicsLayer {
                val p = progress.value
                if (p < 1f) {
                    alpha = p
                    translationY = (1f - p) * 18.dp.toPx()
                }
            }
        }
    }

/** Wrapper for lazy-list items that should play the [baajaEntrance] (the list item scope has no modifier slot). */
@Composable
fun BaajaEntranceBox(
    index: Int,
    playKey: Any?,
    content: @Composable () -> Unit,
) {
    Box(Modifier.baajaEntrance(index, playKey)) { content() }
}

/** Remembers which (screen visit, index) entrances already ran so scrolling back never replays them. */
private object BaajaEntranceRegistry {
    private val played = HashSet<Pair<Any?, Int>>()

    fun hasPlayed(key: Any?, index: Int): Boolean = synchronized(played) { (key to index) in played }

    fun markPlayed(key: Any?, index: Int) {
        synchronized(played) {
            if (played.size > 256) played.clear()
            played.add(key to index)
        }
    }
}

/** Fresh key per screen visit; pass to [baajaEntrance] so the entrance replays when the screen is re-entered. */
@Composable
fun rememberBaajaEntranceKey(): Any = rememberSaveable { System.nanoTime().toString() }

/** True when both ends of the transition are bottom-bar tabs (routes present in [tabRoutes]). */
fun AnimatedContentTransitionScope<NavBackStackEntry>.isBaajaTabSwitch(tabRoutes: Map<out String?, Int>): Boolean =
    tabRoutes.containsKey(initialState.destination.route) && tabRoutes.containsKey(targetState.destination.route)

// ---- Navigation transitions (fade-through for tabs, slight slide + scale for push/pop) ----

private val NavEasing = FastOutSlowInEasing

fun baajaNavEnter(
    context: Context,
    tabSwitch: Boolean,
    pop: Boolean,
): EnterTransition {
    if (!baajaMotionEnabled(context)) return EnterTransition.None
    val fade = fadeIn(tween(BaajaMotion.FadeInMillis, delayMillis = BaajaMotion.FadeOutMillis, easing = NavEasing))
    if (tabSwitch) return fade
    val dir = if (pop) -1 else 1
    return fade +
        slideInHorizontally(tween(BaajaMotion.ScreenMillis, easing = NavEasing)) { dir * it / 12 } +
        scaleIn(tween(BaajaMotion.ScreenMillis, easing = NavEasing), initialScale = 0.96f)
}

fun baajaNavExit(
    context: Context,
    tabSwitch: Boolean,
    pop: Boolean,
): ExitTransition {
    if (!baajaMotionEnabled(context)) return ExitTransition.None
    val fade = fadeOut(tween(BaajaMotion.FadeOutMillis, easing = NavEasing))
    if (tabSwitch) return fade
    val dir = if (pop) 1 else -1
    return fade +
        slideOutHorizontally(tween(BaajaMotion.ScreenMillis, easing = NavEasing)) { dir * it / 12 } +
        scaleOut(tween(BaajaMotion.ScreenMillis, easing = NavEasing), targetScale = 0.96f)
}
