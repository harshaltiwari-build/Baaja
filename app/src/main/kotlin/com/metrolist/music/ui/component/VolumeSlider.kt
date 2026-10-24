/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Baaja volume slider: a round speaker button (tap to mute), a thick outlined bar with a big outlined handle,
 * and the level as a percentage.
 */

package com.metrolist.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.R
import com.metrolist.music.ui.theme.baajaInk
import com.metrolist.music.ui.theme.baajaSurface
import kotlin.math.roundToInt

/** Level restored by the speaker button when the volume is muted. */
private const val UNMUTE_LEVEL = 0.7f

@Composable
fun VolumeSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary,
) {
    val ink = baajaInk
    val fraction = value.coerceIn(0f, 1f)
    val percent = (fraction * 100).roundToInt()
    var barWidthPx by remember { mutableFloatStateOf(1f) }
    val thumbSize = 34.dp
    val thumbPx = with(LocalDensity.current) { thumbSize.toPx() }

    val icon =
        when {
            fraction <= 0f -> R.drawable.volume_off
            fraction < 0.33f -> R.drawable.volume_mute
            fraction < 0.66f -> R.drawable.volume_down
            else -> R.drawable.volume_up
        }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(44.dp)
                    .baajaSurface(ink, MaterialTheme.colorScheme.surface, CircleShape, shadowOffset = 2.dp)
                    .clickable(enabled = enabled, role = Role.Button) {
                        onValueChange(if (fraction > 0f) 0f else UNMUTE_LEVEL)
                        onValueChangeFinished?.invoke()
                    },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp),
            )
        }

        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .height(40.dp)
                    .onSizeChanged { barWidthPx = it.width.toFloat().coerceAtLeast(1f) }
                    .semantics {
                        role = Role.Button
                        contentDescription = "Volume"
                        stateDescription = "$percent%"
                    }.pointerInput(enabled) {
                        if (enabled) {
                            detectTapGestures { offset ->
                                onValueChange((offset.x / barWidthPx).coerceIn(0f, 1f))
                                onValueChangeFinished?.invoke()
                            }
                        }
                    }.pointerInput(enabled) {
                        if (enabled) {
                            detectHorizontalDragGestures(
                                onDragEnd = { onValueChangeFinished?.invoke() },
                                onDragCancel = { onValueChangeFinished?.invoke() },
                            ) { change, _ ->
                                change.consume()
                                onValueChange((change.position.x / barWidthPx).coerceIn(0f, 1f))
                            }
                        }
                    },
            contentAlignment = Alignment.CenterStart,
        ) {
            val barShape = RoundedCornerShape(10.dp)
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .clip(barShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(fraction)
                            .height(20.dp)
                            .background(accentColor),
                )
                Box(Modifier.matchParentSize().border(2.5.dp, ink, barShape))
            }
            Box(
                modifier =
                    Modifier
                        .offset {
                            val travel = (barWidthPx - thumbPx).coerceAtLeast(0f)
                            IntOffset((travel * fraction).roundToInt(), 0)
                        }.size(thumbSize)
                        .baajaSurface(ink, MaterialTheme.colorScheme.surface, CircleShape, shadowOffset = 2.dp, borderWidth = 2.5.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(12.dp).background(accentColor, CircleShape))
            }
        }

        Text(
            text = "$percent%",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(46.dp),
        )
    }
}
