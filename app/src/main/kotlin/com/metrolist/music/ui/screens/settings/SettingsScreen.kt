/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.metrolist.music.BuildConfig
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.ui.component.home.BaajaSearchField
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.ui.screens.onboarding.BaajaQuickAppearanceCard
import com.metrolist.music.ui.screens.onboarding.BaajaSettingsFooter
import com.metrolist.music.ui.screens.onboarding.BaajaSettingsProfileCard
import com.metrolist.music.ui.screens.onboarding.SettingsChevron
import com.metrolist.music.ui.utils.backToMain

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
) {
    var searchQuery by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Top
                )
            )
        )

        Spacer(modifier = Modifier.height(8.dp))
        BaajaSearchField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            onSearch = {},
            placeholder = stringResource(R.string.baaja_settings_search_hint),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(20.dp))
        if (searchQuery.text.isNotBlank()) {
            SettingsSearchResults(searchQuery.text, navController)
        } else {
        BaajaSettingsProfileCard(onClick = { navController.navigate("profile") })
        Spacer(modifier = Modifier.height(20.dp))
        BaajaQuickAppearanceCard(onMoreClick = { navController.navigate("settings/appearance") })
        Spacer(modifier = Modifier.height(20.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.baaja_settings_group_listening),
            items = listOf(
                settingsLink(R.drawable.graphic_eq, R.string.baaja_settings_sound, R.string.baaja_settings_sound_desc) { navController.navigate("settings/player/playback") },
                settingsLink(R.drawable.play, R.string.baaja_clean_menu_player_title, R.string.baaja_settings_player_desc) { navController.navigate("settings/player/player") },
                settingsLink(R.drawable.videocam, R.string.baaja_video_title, R.string.baaja_settings_videos_desc) { navController.navigate("settings/music_videos") },
                settingsLink(R.drawable.queue_music, R.string.baaja_settings_queue, R.string.baaja_settings_queue_desc) { navController.navigate("settings/player/queue") },
                settingsLink(R.drawable.bedtime, R.string.baaja_settings_sleep, R.string.baaja_settings_sleep_desc) { navController.navigate("settings/player/timers") },
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.baaja_settings_group_music),
            items = listOf(
                settingsLink(R.drawable.language, R.string.baaja_settings_recommendations, R.string.baaja_settings_recommendations_desc) { navController.navigate("settings/content") },
                settingsLink(R.drawable.block, R.string.baaja_settings_filters, R.string.baaja_settings_filters_desc) { navController.navigate("settings/filters") },
                settingsLink(R.drawable.lyrics, R.string.lyrics, R.string.baaja_settings_lyrics_desc) { navController.navigate("settings/appearance/lyrics") },
                settingsLink(R.drawable.library_music, R.string.baaja_settings_library, R.string.baaja_settings_library_desc) { navController.navigate("settings/library") },
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.baaja_settings_group_app),
            items = listOf(
                settingsLink(R.drawable.palette, R.string.appearance, R.string.baaja_settings_appearance_desc) { navController.navigate("settings/appearance") },
                settingsLink(R.drawable.download, R.string.baaja_settings_downloads_storage, R.string.baaja_settings_downloads_storage_desc) { navController.navigate("settings/storage") },
                settingsLink(R.drawable.restore, R.string.backup_restore, R.string.baaja_settings_backup_desc) { navController.navigate("settings/backup_restore") },
                settingsLink(R.drawable.devices, R.string.baaja_settings_devices_title, R.string.baaja_settings_devices_desc) { navController.navigate("settings/devices") },
                settingsLink(R.drawable.security, R.string.privacy, R.string.baaja_settings_privacy_desc) { navController.navigate("settings/privacy") },
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Material3SettingsGroup(
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.info),
                    title = { Text(stringResource(R.string.baaja_settings_about)) },
                    description = { Text(stringResource(R.string.baaja_settings_about_desc, BuildConfig.VERSION_NAME)) },
                    trailingContent = { SettingsChevron() },
                    onClick = { navController.navigate("settings/about_baaja") }
                ),
            )
        )
        Spacer(modifier = Modifier.height(24.dp))
        BaajaSettingsFooter()
        Spacer(modifier = Modifier.height(24.dp))
        }
    }

    TopAppBar(
        title = {
            Text(
                stringResource(R.string.settings),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = stringResource(R.string.back_button_desc)
                )
            }
        }
    )
}

/** A row on the Settings home that opens one settings page; the hint says what's on it. */
@Composable
private fun settingsLink(
    icon: Int,
    title: Int,
    description: Int,
    onClick: () -> Unit,
) = Material3SettingsItem(
    icon = painterResource(icon),
    title = { Text(stringResource(title)) },
    description = { Text(stringResource(description)) },
    trailingContent = { SettingsChevron() },
    onClick = onClick,
)
