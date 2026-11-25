/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.menu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metrolist.music.R
import com.metrolist.music.ui.component.Material3MenuGroup
import com.metrolist.music.ui.component.Material3MenuItemData

/** Shown above an item's own menu when it was long-pressed on Home. */
@Composable
fun HideFromHomeMenuGroup(onHide: () -> Unit) {
    Box(Modifier.padding(vertical = 8.dp)) {
        Material3MenuGroup(
            items =
                listOf(
                    Material3MenuItemData(
                        title = { Text(stringResource(R.string.baaja_hide_from_home)) },
                        description = { Text(stringResource(R.string.baaja_hide_from_home_desc)) },
                        icon = { Icon(painterResource(R.drawable.visibility_off), contentDescription = null) },
                        onClick = onHide,
                    ),
                ),
        )
    }
}
