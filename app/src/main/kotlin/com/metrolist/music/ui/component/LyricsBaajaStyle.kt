/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Baaja's lyrics look: the line being sung sits on an outlined [card] with a hard shadow, and the
 * other lines use [text] at reduced alpha. Provided by the Baaja lyrics screen only, so other
 * places that show lyrics keep their usual style.
 */
@Immutable
data class BaajaLyricsStyle(
    val card: Color,
    val ink: Color,
    val text: Color,
)

val LocalBaajaLyricsStyle = staticCompositionLocalOf<BaajaLyricsStyle?> { null }

/** "Share card" taps from the lyrics control card; the visible lyrics view opens its share flow. */
object LyricsShareCardRequests {
    private val requests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events: SharedFlow<Unit> = requests.asSharedFlow()

    fun request() {
        requests.tryEmit(Unit)
    }
}

/**
 * Draws the active-line card behind a lyric line without changing its layout, so line heights
 * (and the scroll positions computed from them) stay identical whether a line is active or not.
 */
internal fun Modifier.baajaLyricCard(
    style: BaajaLyricsStyle?,
    horizontalInset: Dp,
    visibility: () -> Float,
): Modifier =
    if (style == null) {
        this
    } else {
        drawBehind {
            val v = visibility()
            if (v <= 0.01f) return@drawBehind
            val inset = horizontalInset.toPx()
            val shadowX = 3.dp.toPx()
            val shadowY = 4.dp.toPx()
            val border = 2.dp.toPx()
            val w = size.width - inset * 2 - shadowX
            val h = size.height - shadowY
            if (w <= 0f || h <= 0f) return@drawBehind
            val radius = CornerRadius(18.dp.toPx())
            val cardSize = Size(w, h)
            drawRoundRect(style.ink.copy(alpha = v), topLeft = Offset(inset + shadowX, shadowY), size = cardSize, cornerRadius = radius)
            drawRoundRect(style.card.copy(alpha = v), topLeft = Offset(inset, 0f), size = cardSize, cornerRadius = radius)
            drawRoundRect(
                style.ink.copy(alpha = v),
                topLeft = Offset(inset + border / 2, border / 2),
                size = Size(w - border, h - border),
                cornerRadius = radius,
                style = Stroke(border),
            )
        }
    }
