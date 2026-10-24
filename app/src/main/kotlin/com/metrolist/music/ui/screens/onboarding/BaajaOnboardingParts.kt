/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.metrolist.music.R
import com.metrolist.music.ui.theme.BaajaInk
import com.metrolist.music.ui.theme.BaajaLogo
import com.metrolist.music.ui.theme.BaajaYellow
import com.metrolist.music.ui.theme.baajaInk
import com.metrolist.music.ui.theme.baajaSurface

/** Decorative pastel fills from the Baaja mockups (badges, tiles). They stay light in dark mode, so pair them with [BaajaInk]. */
internal val BaajaPastels =
    listOf(
        Color(0xFFD9CCFF),
        Color(0xFFBDEFE6),
        Color(0xFFFFE066),
        Color(0xFFFFD9B8),
        Color(0xFFC9DAFF),
        Color(0xFFFFD3DE),
        Color(0xFFE3F5C8),
    )

internal fun baajaPastel(index: Int): Color = BaajaPastels[Math.floorMod(index, BaajaPastels.size)]

/** Placeholder "art" palettes (background, sun, near band, far band) used before real artwork exists. */
private val ArtPalettes =
    listOf(
        listOf(Color(0xFFD9CCFF), Color(0xFFFFC94D), Color(0xFF5B3FD9), Color(0xFF8D6BFF)),
        listOf(Color(0xFFFFD9B8), Color(0xFFFFF3C4), Color(0xFFF2783A), Color(0xFFC2410C)),
        listOf(Color(0xFFBDEFE6), Color(0xFFFFFFFF), Color(0xFF14A39A), Color(0xFF0E6E73)),
        listOf(Color(0xFFFFD3DE), Color(0xFFFFE066), Color(0xFFE8436A), Color(0xFF8E1F45)),
    )

/** Simple sun-over-hills artwork drawn in code, used where no real cover is available. */
@Composable
internal fun BaajaPlaceholderArt(
    variant: Int,
    modifier: Modifier = Modifier,
) {
    val palette = ArtPalettes[Math.floorMod(variant, ArtPalettes.size)]
    Canvas(modifier) {
        drawRect(palette[0])
        val sunX = if (variant % 2 == 0) 0.7f else 0.32f
        drawCircle(palette[1], radius = size.minDimension * 0.13f, center = Offset(size.width * sunX, size.height * 0.3f))
        val far =
            Path().apply {
                moveTo(0f, size.height * 0.62f)
                lineTo(size.width, size.height * 0.48f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
        drawPath(far, palette[3])
        val near =
            Path().apply {
                moveTo(0f, size.height * 0.72f)
                lineTo(size.width, size.height * 0.86f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
        drawPath(near, palette[2])
    }
}

/** Logo tile + "baaja" wordmark + yellow Hindi tag, as on the welcome screen header. */
@Composable
internal fun BaajaBrandRow(
    modifier: Modifier = Modifier,
    logoSize: Dp = 50.dp,
    clock: BaajaClock? = null,
) {
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val logoDescription = stringResource(R.string.baaja_onb_logo_description)
        BaajaLogo(
            Modifier
                .graphicsLayer {
                    // A quick wiggle every 6 seconds, after a 1.5 s pause.
                    val p = clock?.loop(6f, 1.5f) ?: 0f
                    val (rot, sc) =
                        when {
                            p < 0.88f -> 0f to 1f
                            p < 0.92f -> lerpF(0f, -10f, (p - 0.88f) / 0.04f) to lerpF(1f, 1.1f, (p - 0.88f) / 0.04f)
                            p < 0.96f -> lerpF(-10f, 8f, (p - 0.92f) / 0.04f) to 1.1f
                            else -> lerpF(8f, 0f, (p - 0.96f) / 0.04f) to lerpF(1.1f, 1f, (p - 0.96f) / 0.04f)
                        }
                    rotationZ = rot
                    scaleX = sc
                    scaleY = sc
                }.semantics { contentDescription = logoDescription },
            size = logoSize,
        )
        Text(
            stringResource(R.string.baaja_onb_wordmark),
            fontSize = 34.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            stringResource(R.string.baaja_onb_wordmark_hindi),
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = BaajaInk,
            modifier =
                Modifier
                    .baajaSurface(baajaInk, MaterialTheme.colorScheme.secondaryContainer.takeIfLight(), CircleShape, shadowOffset = 0.dp, borderWidth = 1.5.dp)
                    .padding(horizontal = 10.dp, vertical = 2.dp),
        )
    }
}

/** Keeps the mockup's yellow tag even when the dark scheme's secondary container is dark. */
private fun Color.takeIfLight(): Color = if (luminanceApprox() > 0.5f) this else BaajaYellow

private fun Color.luminanceApprox(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue

/** Round avatar: remote image when available, otherwise the first letter on yellow. */
@Composable
internal fun BaajaAvatar(
    name: String,
    imageUrl: Any?,
    size: Dp,
    modifier: Modifier = Modifier,
    shadowOffset: Dp = 4.dp,
) {
    Box(
        modifier
            .size(size)
            .baajaSurface(baajaInk, BaajaYellow, CircleShape, shadowOffset = shadowOffset, borderWidth = if (size > 80.dp) 3.dp else 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            profileInitial(name),
            fontSize = (size.value * 0.38f).sp,
            fontWeight = FontWeight.ExtraBold,
            color = BaajaInk,
        )
        if (imageUrl != null && imageUrl != "") {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize().clip(CircleShape),
            )
        }
    }
}

/** Rounded-square pastel badge holding an icon, used by settings rows and profile links. */
@Composable
internal fun BaajaIconBadge(
    @DrawableRes icon: Int,
    background: Color,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    enabled: Boolean = true,
) {
    Box(
        modifier
            .size(size)
            .baajaSurface(baajaInk, background, RoundedCornerShape(size * 0.3f), shadowOffset = 0.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(icon),
            contentDescription = null,
            tint = BaajaInk.copy(alpha = if (enabled) 1f else 0.38f),
            modifier = Modifier.size(size * 0.55f),
        )
    }
}

/** Outlined pill segmented control; the selected segment is ink-filled (theme switch, time ranges). */
@Composable
internal fun BaajaSegmentedToggle(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = baajaInk
    Row(
        modifier
            .fillMaxWidth()
            .baajaSurface(ink, MaterialTheme.colorScheme.surface, CircleShape, shadowOffset = 0.dp)
            .padding(3.dp),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .clip(CircleShape)
                    .then(if (selected) Modifier.baajaSurface(ink, ink, CircleShape, 0.dp, 0.dp) else Modifier)
                    .semantics { this.selected = selected }
                    .clickable(role = Role.Tab) { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/** Big rounded CTA with a trailing arrow, matching the welcome/login mockups. A [clock] adds the pulsing shadow and nudging arrow. */
@Composable
internal fun BaajaCtaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    @DrawableRes trailingIcon: Int? = R.drawable.arrow_forward,
    leading: (@Composable () -> Unit)? = null,
    clock: BaajaClock? = null,
) {
    val container = if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val content = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val ink = baajaInk
    val shape = CircleShape
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .drawBehind {
                // Hard shadow whose offset breathes between 4 and 7 dp (when animated).
                val grow = clock?.wave(2.4f, 1.8f) ?: 0f
                val offset = lerpF(4f, 7f, grow).dp.toPx()
                val outline = shape.createOutline(size, layoutDirection, this)
                translate(left = offset, top = offset * 1.25f) { drawOutline(outline, ink) }
            }.border(2.5.dp, ink, shape)
            .clip(shape)
            .background(container)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 22.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        Text(text, color = content, fontWeight = FontWeight.Bold, fontSize = 19.sp, maxLines = 1)
        if (trailingIcon != null) {
            Icon(
                painterResource(trailingIcon),
                null,
                tint = content,
                modifier =
                    Modifier
                        .size(22.dp)
                        .graphicsLayer { translationX = 6.dp.toPx() * (clock?.wave(1.2f, 1.8f) ?: 0f) },
            )
        }
    }
}
