/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.rememberPreference

/** Ids of the Home sections the user switched off. Missing means every section shows. */
val HiddenHomeSectionsKey = stringSetPreferencesKey("baajaHiddenHomeSections")

/** A section of Home the user can switch off, matched to Home's own section ids. */
enum class HomeSectionToggle(
    val id: String,
    @StringRes val title: Int,
    @DrawableRes val icon: Int,
) {
    SPEED_DIAL("speed_dial", R.string.speed_dial, R.drawable.grid_view),
    MADE_FOR_YOU("made_for_you", R.string.baaja_home_made_for_you, R.drawable.person),
    QUICK_PICKS("quick_picks", R.string.quick_picks, R.drawable.music_note),
    YOUTUBE_SHELVES("youtube_shelves", R.string.baaja_home_youtube_shelves, R.drawable.queue_music),
    KEEP_LISTENING("keep_listening", R.string.keep_listening, R.drawable.history),
    SIMILAR("similar", R.string.similar_to, R.drawable.radio),
    COMMUNITY("from_the_community", R.string.from_the_community, R.drawable.playlist_add),
    FORGOTTEN_FAVORITES("forgotten_favorites", R.string.forgotten_favorites, R.drawable.favorite),
    MOODS("mood_and_genres", R.string.mood_and_genres, R.drawable.lyrics),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeSectionsSettings(navController: NavController) {
    val (hidden, onHiddenChange) = rememberPreference(HiddenHomeSectionsKey, defaultValue = emptySet())

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(
            Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)),
        )
        Material3SettingsGroup(
            title = stringResource(R.string.baaja_home_sections_desc),
            items =
                HomeSectionToggle.entries.map { section ->
                    switchItem(
                        icon = section.icon,
                        title = stringResource(section.title),
                        description = null,
                        checked = section.id !in hidden,
                        onChange = { show -> onHiddenChange(if (show) hidden - section.id else hidden + section.id) },
                    )
                },
        )
        Spacer(Modifier.height(16.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.baaja_home_sections)) },
        navigationIcon = {
            IconButton(onClick = navController::navigateUp, onLongClick = navController::backToMain) {
                Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
            }
        },
    )
}
