/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.metrolist.music.ui.screens.onboarding.baajaPastel
import com.metrolist.music.ui.theme.BaajaInk
import com.metrolist.music.ui.theme.baajaInk
import com.metrolist.music.ui.theme.baajaSurface

/**
 * Baaja menu group: one ink-outlined card with a hard shadow, rows split by hairlines and each led
 * by a pastel icon badge, like the Settings rows.
 */
@Composable
fun Material3MenuGroup(
    items: List<Material3MenuItemData>
) {
    val ink = baajaInk
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 4.dp, bottom = 4.dp)
            .baajaSurface(
                ink = ink,
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(24.dp),
                shadowOffset = 3.dp,
            )
            .animateContentSize(),
    ) {
        items.forEachIndexed { index, item ->
            Material3MenuItemRow(
                item = item,
                badgeColor = baajaPastel(index + 1),
            )
            if (index < items.lastIndex) {
                HorizontalDivider(
                    thickness = 1.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun Material3MenuItemRow(
    item: Material3MenuItemData,
    badgeColor: Color,
) {
    val rowColor = item.cardColors?.containerColor ?: Color.Unspecified
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (rowColor != Color.Unspecified && rowColor != Color.Transparent) Modifier.background(rowColor) else Modifier)
            .heightIn(min = 56.dp)
            .clickable(
                enabled = item.onClick != null,
                onClick = { item.onClick?.invoke() }
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item.icon?.let { icon ->
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .baajaSurface(
                        ink = baajaInk,
                        color = badgeColor,
                        shape = RoundedCornerShape(12.dp),
                        shadowOffset = 0.dp,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                CompositionLocalProvider(LocalContentColor provides BaajaInk) {
                    Box(Modifier.size(21.dp), contentAlignment = Alignment.Center) { icon() }
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            ProvideTextStyle(MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)) {
                item.title()
            }

            item.description?.let { desc ->
                Spacer(modifier = Modifier.height(2.dp))
                ProvideTextStyle(
                    MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    desc()
                }
            }
        }
        item.trailingContent?.let { trailing ->
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}

data class Material3MenuItemData(
    val icon: (@Composable () -> Unit)? = null,
    val title: @Composable () -> Unit,
    val description: (@Composable () -> Unit)? = null,
    val onClick: (() -> Unit)? = null,
    val cardColors: CardColors? = null,
    val trailingContent: (@Composable () -> Unit)? = null
)
