/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.metrolist.music.R
import com.metrolist.music.ui.screens.onboarding.SettingsChevron
import com.metrolist.music.ui.theme.BaajaCard
import com.metrolist.music.ui.theme.baajaInk

/** "Player and audio": a menu of the four settings pages. */
@Composable
fun PlayerSettings(navController: NavController) {
    SettingsSubPage(stringResource(R.string.player_and_audio), navController) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            PlayerMenuRow(
                icon = R.drawable.play,
                badge = MaterialTheme.colorScheme.primary,
                onBadge = MaterialTheme.colorScheme.onPrimary,
                title = R.string.baaja_clean_menu_playback_title,
                description = R.string.baaja_clean_menu_playback_desc,
                onClick = { navController.navigate("settings/player/playback") },
            )
            PlayerMenuRow(
                icon = R.drawable.fullscreen,
                badge = MaterialTheme.colorScheme.secondary,
                onBadge = MaterialTheme.colorScheme.onSecondary,
                title = R.string.baaja_clean_menu_player_title,
                description = R.string.baaja_clean_menu_player_desc,
                onClick = { navController.navigate("settings/player/player") },
            )
            PlayerMenuRow(
                icon = R.drawable.queue_music,
                badge = MaterialTheme.colorScheme.tertiary,
                onBadge = MaterialTheme.colorScheme.onTertiary,
                title = R.string.baaja_clean_menu_queue_title,
                description = R.string.baaja_clean_menu_queue_desc,
                onClick = { navController.navigate("settings/player/queue") },
            )
            PlayerMenuRow(
                icon = R.drawable.timer,
                badge = MaterialTheme.colorScheme.secondaryContainer,
                onBadge = MaterialTheme.colorScheme.onSecondaryContainer,
                title = R.string.baaja_clean_menu_timers_title,
                description = R.string.baaja_clean_menu_timers_desc,
                onClick = { navController.navigate("settings/player/timers") },
            )
        }
    }
}

@Composable
private fun PlayerMenuRow(
    @DrawableRes icon: Int,
    badge: Color,
    onBadge: Color,
    @StringRes title: Int,
    @StringRes description: Int,
    onClick: () -> Unit,
) {
    BaajaCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val shape = RoundedCornerShape(14.dp)
            Box(
                Modifier
                    .size(48.dp)
                    .clip(shape)
                    .background(badge)
                    .border(2.dp, baajaInk, shape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(icon), contentDescription = null, tint = onBadge, modifier = Modifier.size(24.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(description),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            SettingsChevron()
        }
    }
}
