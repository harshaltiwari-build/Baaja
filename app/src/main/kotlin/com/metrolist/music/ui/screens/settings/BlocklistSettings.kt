/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metrolist.music.R
import com.metrolist.music.ui.component.DefaultDialog
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.ui.component.TextButton
import com.metrolist.music.utils.BlockedEntry
import com.metrolist.music.utils.ContentBlocklist
import com.metrolist.music.utils.contentBlocklist
import com.metrolist.music.utils.restoreToHome
import com.metrolist.music.utils.unblockArtist
import kotlinx.coroutines.launch

private enum class BlocklistDialog { ARTISTS, HIDDEN }

/** Blocked artists and items hidden from Home, each restorable one by one. */
@Composable
fun BlocklistSettingsGroup() {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val blocklist by remember(context) { context.contentBlocklist }.collectAsStateWithLifecycle(ContentBlocklist())
    var dialog by remember { mutableStateOf<BlocklistDialog?>(null) }

    Material3SettingsGroup(
        title = stringResource(R.string.baaja_blocklists),
        items =
            listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.block),
                    title = { Text(stringResource(R.string.baaja_blocked_artists)) },
                    description = { Text(countText(blocklist.blockedArtists.size)) },
                    onClick = { dialog = BlocklistDialog.ARTISTS },
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.visibility_off),
                    title = { Text(stringResource(R.string.baaja_hidden_from_home_list)) },
                    description = { Text(countText(blocklist.hiddenHomeItems.size)) },
                    onClick = { dialog = BlocklistDialog.HIDDEN },
                ),
            ),
    )

    when (dialog) {
        BlocklistDialog.ARTISTS ->
            BlocklistDialog(
                title = stringResource(R.string.baaja_blocked_artists),
                icon = R.drawable.block,
                entries = blocklist.blockedArtists,
                actionLabel = stringResource(R.string.unblock),
                onAction = { entry -> scope.launch { context.unblockArtist(entry.id) } },
                onDismiss = { dialog = null },
            )

        BlocklistDialog.HIDDEN ->
            BlocklistDialog(
                title = stringResource(R.string.baaja_hidden_from_home_list),
                icon = R.drawable.visibility_off,
                entries = blocklist.hiddenHomeItems,
                actionLabel = stringResource(R.string.baaja_restore),
                onAction = { entry -> scope.launch { context.restoreToHome(entry.id) } },
                onDismiss = { dialog = null },
            )

        null -> Unit
    }
}

@Composable
private fun countText(count: Int): String =
    if (count == 0) {
        stringResource(R.string.baaja_blocklist_empty)
    } else {
        pluralStringResource(R.plurals.baaja_blocklist_count, count, count)
    }

@Composable
private fun BlocklistDialog(
    title: String,
    icon: Int,
    entries: List<BlockedEntry>,
    actionLabel: String,
    onAction: (BlockedEntry) -> Unit,
    onDismiss: () -> Unit,
) {
    DefaultDialog(
        onDismiss = onDismiss,
        icon = { Icon(painterResource(icon), contentDescription = null) },
        title = { Text(title) },
        buttons = {
            Button(onClick = onDismiss) { Text(stringResource(android.R.string.ok)) }
        },
    ) {
        // Fits a short list, and scrolls once there are more entries than fit comfortably.
        Box(Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
            if (entries.isEmpty()) {
                Text(
                    text = stringResource(R.string.baaja_blocklist_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center).padding(vertical = 24.dp),
                )
            } else {
                LazyColumn(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(entries, key = { it.id }) { entry ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = entry.name,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { onAction(entry) }) { Text(actionLabel) }
                        }
                    }
                }
            }
        }
    }
}
