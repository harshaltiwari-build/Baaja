/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.theme

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.metrolist.music.R

/** Ink used for Baaja outlines and offset shadows; follows the theme so dark mode stays legible. */
val baajaInk: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.outline

/** Surface with an ink outline and hard offset shadow, optionally clickable. */
fun Modifier.baajaSurface(
    ink: Color,
    color: Color,
    shape: Shape,
    shadowOffset: Dp = 3.dp,
    borderWidth: Dp = 2.dp,
    interactionSource: InteractionSource? = null,
): Modifier =
    if (interactionSource == null) {
        baajaOutline(ink, shape, borderWidth = borderWidth, shadowOffset = shadowOffset)
            .clip(shape)
            .background(color)
    } else {
        // The shadow stays put; the surface springs down and slides onto it while pressed.
        baajaShadow(ink, shape, shadowOffset)
            .baajaPressScale(interactionSource, shadowOffset)
            .border(borderWidth, ink, shape)
            .clip(shape)
            .background(color)
    }

@Composable
fun BaajaCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    color: Color = MaterialTheme.colorScheme.surface,
    shadowOffset: Dp = 4.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    Box(
        modifier
            .baajaSurface(baajaInk, color, shape, shadowOffset, interactionSource = if (onClick != null) source else null)
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = source, indication = LocalIndication.current, role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                },
            ),
    ) { content() }
}

/** Big rounded call-to-action button, filled with the accent ([primary]) or white. */
@Composable
fun BaajaPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    @DrawableRes icon: Int? = null,
    enabled: Boolean = true,
    height: Dp = 56.dp,
) {
    val container = if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val content = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val source = remember { MutableInteractionSource() }
    Row(
        modifier
            .heightIn(min = height)
            .baajaSurface(
                baajaInk,
                container.copy(alpha = if (enabled) 1f else 0.5f),
                CircleShape,
                if (enabled) 4.dp else 0.dp,
                interactionSource = source,
            ).clickable(interactionSource = source, indication = LocalIndication.current, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 22.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(painterResource(icon), null, tint = content, modifier = Modifier.size(20.dp))
        Text(text, color = content, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1)
    }
}

/**
 * Outlined chip; [selected] chips turn ink-filled like the mockups' mood chips. Chips are secondary
 * controls, so they get a thinner outline and no offset shadow; cards and main buttons keep the bold look.
 */
@Composable
fun BaajaChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    accent: Boolean = false,
) {
    val ink = baajaInk
    val container =
        when {
            accent -> MaterialTheme.colorScheme.primary
            selected -> ink
            else -> MaterialTheme.colorScheme.surface
        }
    val content =
        when {
            accent -> MaterialTheme.colorScheme.onPrimary
            selected -> MaterialTheme.colorScheme.surface
            else -> MaterialTheme.colorScheme.onSurface
        }
    val source = remember { MutableInteractionSource() }
    Row(
        modifier
            .defaultMinSize(minHeight = 40.dp)
            .baajaSurface(ink, container, CircleShape, if (accent) 2.dp else 0.dp, borderWidth = 1.5.dp, interactionSource = source)
            .selectable(selected = selected, interactionSource = source, indication = LocalIndication.current, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(painterResource(icon), null, tint = content, modifier = Modifier.size(16.dp))
        Text(label, color = content, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1)
    }
}

/** Round outlined icon button (back, more, share…). */
@Composable
fun BaajaIconButton(
    @DrawableRes icon: Int,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    size: Dp = 46.dp,
    tint: Color? = null,
) {
    val container = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val content = tint ?: if (filled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val source = remember { MutableInteractionSource() }
    Box(
        modifier
            .size(size)
            .baajaSurface(baajaInk, container, CircleShape, if (filled) 2.dp else 0.dp, interactionSource = source)
            .clickable(interactionSource = source, indication = LocalIndication.current, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription, tint = content, modifier = Modifier.size(size * 0.48f))
    }
}

/** Artwork with Baaja's outline and shadow. */
@Composable
fun BaajaArtwork(
    model: Any?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    shadowOffset: Dp = 3.dp,
    contentDescription: String? = null,
) {
    AsyncImage(
        model = model,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier.baajaSurface(baajaInk, MaterialTheme.colorScheme.surfaceVariant, shape, shadowOffset),
    )
}

/** Section title with an optional "See all"-style action on the right. */
@Composable
fun BaajaSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (actionLabel != null && onAction != null) {
            Text(
                actionLabel,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                modifier =
                    Modifier
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClick = onAction)
                        .padding(horizontal = 8.dp, vertical = 10.dp),
            )
        }
    }
}

/** The Baaja logo: the app's own artwork (purple tile, white "b", ink outline and shadow). */
@Composable
fun BaajaLogo(
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
) {
    Image(
        painter = painterResource(R.drawable.baaja_logo),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier.size(size),
    )
}
