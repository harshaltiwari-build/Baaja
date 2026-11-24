package com.metrolist.music.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.blocklist.Blocklist
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.ui.component.TextButton
import com.metrolist.music.ui.utils.backToMain
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlocklistSettings(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by Blocklist.state.collectAsState()

    Column(
        Modifier
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
            ).verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)))

        Text(
            text = stringResource(R.string.baaja_blocklist_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
        )
        Spacer(Modifier.height(8.dp))

        if (state.isEmpty) {
            Text(
                text = stringResource(R.string.baaja_blocklist_empty),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
            )
        }

        if (state.artists.isNotEmpty()) {
            Material3SettingsGroup(
                title = stringResource(R.string.baaja_blocklist_artists),
                items =
                    state.artists.map { entry ->
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.person),
                            title = { Text(entry.name) },
                            trailingContent = {
                                TextButton(onClick = { scope.launch { Blocklist.unblockArtist(context, entry.id) } }) {
                                    Text(stringResource(R.string.baaja_blocklist_unblock))
                                }
                            },
                        )
                    },
            )
            Spacer(Modifier.height(16.dp))
        }

        if (state.songs.isNotEmpty()) {
            Material3SettingsGroup(
                title = stringResource(R.string.baaja_blocklist_songs),
                items =
                    state.songs.map { entry ->
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.music_note),
                            title = { Text(entry.name) },
                            description = entry.subtitle?.let { artist -> { Text(artist) } },
                            trailingContent = {
                                TextButton(onClick = { scope.launch { Blocklist.unblockSong(context, entry.id) } }) {
                                    Text(stringResource(R.string.baaja_blocklist_unblock))
                                }
                            },
                        )
                    },
            )
        }
        Spacer(Modifier.height(24.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.baaja_blocklist)) },
        navigationIcon = {
            IconButton(onClick = navController::navigateUp, onLongClick = navController::backToMain) {
                Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
            }
        },
    )
}
