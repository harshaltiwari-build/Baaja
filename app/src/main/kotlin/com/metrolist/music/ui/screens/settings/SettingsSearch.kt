/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.metrolist.music.R
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem

/** One setting a search can find: its title and the page that holds it. */
data class SettingsSearchEntry(
    @StringRes val title: Int,
    val route: String,
    @StringRes val page: Int,
)

/** Every setting title, generated from the settings pages so search opens the page that has it. */
private val settingsSearchIndex =
    listOf(
        SettingsSearchEntry(R.string.lyrics_text_position, "settings/appearance/lyrics", R.string.lyrics),
        SettingsSearchEntry(R.string.respect_agent_positioning, "settings/appearance/lyrics", R.string.lyrics),
        SettingsSearchEntry(R.string.lyrics_click_change, "settings/appearance/lyrics", R.string.lyrics),
        SettingsSearchEntry(R.string.lyrics_auto_scroll, "settings/appearance/lyrics", R.string.lyrics),
        SettingsSearchEntry(R.string.hide_status_bar_fullscreen, "settings/appearance/lyrics", R.string.lyrics),
        SettingsSearchEntry(R.string.mini_player_background_style, "settings/appearance", R.string.appearance),
        SettingsSearchEntry(R.string.player_background_style, "settings/appearance", R.string.appearance),
        SettingsSearchEntry(R.string.player_slider_style, "settings/appearance", R.string.appearance),
        SettingsSearchEntry(R.string.enable_swipe_thumbnail, "settings/appearance", R.string.appearance),
        SettingsSearchEntry(R.string.swipe_sensitivity, "settings/appearance", R.string.appearance),
        SettingsSearchEntry(R.string.default_open_tab, "settings/library", R.string.baaja_settings_library),
        SettingsSearchEntry(R.string.default_lib_chips, "settings/library", R.string.baaja_settings_library),
        SettingsSearchEntry(R.string.swipe_song_to_add, "settings/appearance", R.string.appearance),
        SettingsSearchEntry(R.string.swipe_song_to_remove, "settings/appearance", R.string.appearance),
        SettingsSearchEntry(R.string.force_bottom_navbar, "settings/appearance", R.string.appearance),
        SettingsSearchEntry(R.string.slim_navbar, "settings/appearance", R.string.appearance),
        SettingsSearchEntry(R.string.grid_cell_size, "settings/library", R.string.baaja_settings_library),
        SettingsSearchEntry(R.string.show_liked_playlist, "settings/library", R.string.baaja_settings_library),
        SettingsSearchEntry(R.string.show_downloaded_playlist, "settings/library", R.string.baaja_settings_library),
        SettingsSearchEntry(R.string.show_top_playlist, "settings/library", R.string.baaja_settings_library),
        SettingsSearchEntry(R.string.show_cached_playlist, "settings/library", R.string.baaja_settings_library),
        SettingsSearchEntry(R.string.show_uploaded_playlist, "settings/library", R.string.baaja_settings_library),
        SettingsSearchEntry(R.string.content_language, "settings/content", R.string.baaja_settings_recommendations),
        SettingsSearchEntry(R.string.content_country, "settings/content", R.string.baaja_settings_recommendations),
        SettingsSearchEntry(R.string.set_quick_picks, "settings/content", R.string.baaja_settings_recommendations),
        SettingsSearchEntry(R.string.add_to_playlist_position, "settings/library", R.string.baaja_settings_library),
        SettingsSearchEntry(R.string.baaja_music_languages, "settings/content", R.string.baaja_settings_recommendations),
        SettingsSearchEntry(R.string.baaja_favourite_artists, "settings/content", R.string.baaja_settings_recommendations),
        SettingsSearchEntry(R.string.hide_explicit, "settings/filters", R.string.baaja_settings_filters),
        SettingsSearchEntry(R.string.hide_video_songs, "settings/filters", R.string.baaja_settings_filters),
        SettingsSearchEntry(R.string.hide_youtube_shorts, "settings/filters", R.string.baaja_settings_filters),
        SettingsSearchEntry(R.string.baaja_hide_podcasts, "settings/filters", R.string.baaja_settings_filters),
        SettingsSearchEntry(R.string.baaja_block_ai_artists, "settings/filters", R.string.baaja_settings_filters),
        SettingsSearchEntry(R.string.lyrics_romanization, "settings/appearance/lyrics", R.string.lyrics),
        SettingsSearchEntry(R.string.show_most_stats_playlists, "settings/library", R.string.baaja_settings_library),
        SettingsSearchEntry(R.string.baaja_home_sections, "settings/content", R.string.baaja_settings_recommendations),
        SettingsSearchEntry(R.string.audio_quality, "settings/player/playback", R.string.baaja_settings_sound),
        SettingsSearchEntry(R.string.audio_quality_mobile, "settings/player/playback", R.string.baaja_settings_sound),
        SettingsSearchEntry(R.string.audio_quality_download, "settings/storage", R.string.baaja_settings_downloads_storage),
        SettingsSearchEntry(R.string.loudness_level, "settings/player/playback", R.string.baaja_settings_sound),
        SettingsSearchEntry(R.string.baaja_clean_transition_title, "settings/player/playback", R.string.baaja_settings_sound),
        SettingsSearchEntry(R.string.crossfade_duration, "settings/player/playback", R.string.baaja_settings_sound),
        SettingsSearchEntry(R.string.skip_silence, "settings/player/playback", R.string.baaja_settings_sound),
        SettingsSearchEntry(R.string.skip_silence_instant, "settings/player/playback", R.string.baaja_settings_sound),
        SettingsSearchEntry(R.string.audio_normalization, "settings/player/playback", R.string.baaja_settings_sound),
        SettingsSearchEntry(R.string.seek_seconds_addup, "settings/player/player", R.string.baaja_clean_menu_player_title),
        SettingsSearchEntry(R.string.baaja_keep_paused_title, "settings/player/player", R.string.baaja_clean_menu_player_title),
        SettingsSearchEntry(R.string.baaja_player_show_lyric_preview, "settings/player/player", R.string.baaja_clean_menu_player_title),
        SettingsSearchEntry(R.string.sponsorblock, "settings/player/player", R.string.baaja_clean_menu_player_title),
        SettingsSearchEntry(R.string.google_cast, "settings/player/player", R.string.baaja_clean_menu_player_title),
        SettingsSearchEntry(R.string.baaja_pb_output_title, "settings/player/player", R.string.baaja_clean_menu_player_title),
        SettingsSearchEntry(R.string.baaja_pb_float_title, "settings/player/player", R.string.baaja_clean_menu_player_title),
        SettingsSearchEntry(R.string.persistent_queue, "settings/player/queue", R.string.baaja_settings_queue),
        SettingsSearchEntry(R.string.baaja_clean_keep_similar_title, "settings/player/queue", R.string.baaja_settings_queue),
        SettingsSearchEntry(R.string.baaja_clean_remember_shuffle_title, "settings/player/queue", R.string.baaja_settings_queue),
        SettingsSearchEntry(R.string.shuffle_playlist_first, "settings/player/queue", R.string.baaja_settings_queue),
        SettingsSearchEntry(R.string.prevent_duplicate_tracks_in_queue, "settings/player/queue", R.string.baaja_settings_queue),
        SettingsSearchEntry(R.string.auto_skip_next_on_error, "settings/player/queue", R.string.baaja_settings_queue),
        SettingsSearchEntry(R.string.stop_music_on_task_clear, "settings/player/player", R.string.baaja_clean_menu_player_title),
        SettingsSearchEntry(R.string.pause_music_when_media_is_muted, "settings/player/player", R.string.baaja_clean_menu_player_title),
        SettingsSearchEntry(R.string.resume_on_bluetooth_connect, "settings/player/player", R.string.baaja_clean_menu_player_title),
        SettingsSearchEntry(R.string.keep_screen_on_when_player_is_expanded, "settings/player/player", R.string.baaja_clean_menu_player_title),
        SettingsSearchEntry(R.string.enable_automatic_sleeptimer, "settings/player/timers", R.string.baaja_settings_sleep),
        SettingsSearchEntry(R.string.baaja_clean_days_title, "settings/player/timers", R.string.baaja_settings_sleep),
        SettingsSearchEntry(R.string.sleep_timer_stop_after_current_song_title, "settings/player/timers", R.string.baaja_settings_sleep),
        SettingsSearchEntry(R.string.sleep_timer_fade_out_title, "settings/player/timers", R.string.baaja_settings_sleep),
        SettingsSearchEntry(R.string.listen_history, "settings/privacy", R.string.privacy),
        SettingsSearchEntry(R.string.pause_listen_history, "settings/privacy", R.string.privacy),
        SettingsSearchEntry(R.string.clear_listen_history, "settings/privacy", R.string.privacy),
        SettingsSearchEntry(R.string.search_history, "settings/privacy", R.string.privacy),
        SettingsSearchEntry(R.string.pause_search_history, "settings/privacy", R.string.privacy),
        SettingsSearchEntry(R.string.clear_search_history, "settings/privacy", R.string.privacy),
        SettingsSearchEntry(R.string.disable_screenshot, "settings/privacy", R.string.privacy),
        SettingsSearchEntry(R.string.baaja_blocklists, "settings/filters", R.string.baaja_settings_filters),
        SettingsSearchEntry(R.string.baaja_blocked_artists, "settings/filters", R.string.baaja_settings_filters),
        SettingsSearchEntry(R.string.baaja_hidden_from_home_list, "settings/filters", R.string.baaja_settings_filters),
        SettingsSearchEntry(R.string.clear_all_downloads, "settings/storage", R.string.baaja_settings_downloads_storage),
        SettingsSearchEntry(R.string.clear_song_cache, "settings/storage", R.string.baaja_settings_downloads_storage),
        SettingsSearchEntry(R.string.clear_image_cache, "settings/storage", R.string.baaja_settings_downloads_storage),
        SettingsSearchEntry(R.string.baaja_clean_downloads_group, "settings/storage", R.string.baaja_settings_downloads_storage),
        SettingsSearchEntry(R.string.downloaded_songs, "settings/storage", R.string.baaja_settings_downloads_storage),
        SettingsSearchEntry(R.string.baaja_pb_smart_title, "settings/storage", R.string.baaja_settings_downloads_storage),
        SettingsSearchEntry(R.string.baaja_pb_smart_wifi_title, "settings/storage", R.string.baaja_settings_downloads_storage),
        SettingsSearchEntry(R.string.auto_download_on_like, "settings/storage", R.string.baaja_settings_downloads_storage),
        SettingsSearchEntry(R.string.song_cache, "settings/storage", R.string.baaja_settings_downloads_storage),
        SettingsSearchEntry(R.string.baaja_clean_cache_size_title, "settings/storage", R.string.baaja_settings_downloads_storage),
        SettingsSearchEntry(R.string.image_cache, "settings/storage", R.string.baaja_settings_downloads_storage),
        SettingsSearchEntry(R.string.auto_backup, "settings/backup_restore", R.string.backup_restore),
        SettingsSearchEntry(R.string.action_backup, "settings/backup_restore", R.string.backup_restore),
        SettingsSearchEntry(R.string.action_restore, "settings/backup_restore", R.string.backup_restore),
        SettingsSearchEntry(R.string.import_online, "settings/backup_restore", R.string.backup_restore),
        SettingsSearchEntry(R.string.import_csv, "settings/backup_restore", R.string.backup_restore),
        SettingsSearchEntry(R.string.auto_backup_folder, "settings/backup_restore", R.string.backup_restore),
        SettingsSearchEntry(R.string.android_auto_visible_sections, "settings/android_auto", R.string.android_auto),
        SettingsSearchEntry(R.string.android_auto_target_playlist, "settings/android_auto", R.string.android_auto),
        SettingsSearchEntry(R.string.mixes, "settings/android_auto", R.string.android_auto),
        SettingsSearchEntry(R.string.android_auto_youtube_playlists, "settings/android_auto", R.string.android_auto),
        SettingsSearchEntry(R.string.current_version, "settings/updater", R.string.updater),
        SettingsSearchEntry(R.string.update_settings, "settings/updater", R.string.updater),
        SettingsSearchEntry(R.string.check_for_updates, "settings/updater", R.string.updater),
        SettingsSearchEntry(R.string.update_notifications, "settings/updater", R.string.updater),
        SettingsSearchEntry(R.string.baaja_home_sections, "settings/content/home_sections", R.string.baaja_home_sections),
        SettingsSearchEntry(R.string.baaja_dc_title, "device_connect", R.string.baaja_dc_title),
        SettingsSearchEntry(R.string.baaja_settings_filters, "settings/filters", R.string.baaja_settings_filters),
        SettingsSearchEntry(R.string.baaja_settings_library, "settings/library", R.string.baaja_settings_library),
        SettingsSearchEntry(R.string.baaja_video_title, "settings/music_videos", R.string.baaja_video_title),
        SettingsSearchEntry(R.string.baaja_settings_devices_title, "settings/devices", R.string.baaja_settings_devices_title),
        SettingsSearchEntry(R.string.listen_together, "settings/listen_together", R.string.baaja_settings_devices_title),
        SettingsSearchEntry(R.string.baaja_video_enabled, "settings/music_videos", R.string.baaja_video_title),
        SettingsSearchEntry(R.string.baaja_video_quality, "settings/music_videos", R.string.baaja_video_title),
        SettingsSearchEntry(R.string.baaja_video_autostart, "settings/music_videos", R.string.baaja_video_title),
        SettingsSearchEntry(R.string.baaja_downloads_title, "downloads", R.string.baaja_settings_downloads_storage),
        SettingsSearchEntry(R.string.lyrics, "settings/appearance/lyrics", R.string.lyrics),
        SettingsSearchEntry(R.string.appearance, "settings/appearance", R.string.appearance),
        SettingsSearchEntry(R.string.content, "settings/content", R.string.baaja_settings_recommendations),
        SettingsSearchEntry(R.string.baaja_clean_menu_playback_title, "settings/player/playback", R.string.baaja_settings_sound),
        SettingsSearchEntry(R.string.baaja_clean_menu_player_title, "settings/player/player", R.string.baaja_clean_menu_player_title),
        SettingsSearchEntry(R.string.queue, "settings/player/queue", R.string.baaja_settings_queue),
        SettingsSearchEntry(R.string.sleep_timer, "settings/player/timers", R.string.baaja_settings_sleep),
        SettingsSearchEntry(R.string.privacy, "settings/privacy", R.string.privacy),
        SettingsSearchEntry(R.string.storage, "settings/storage", R.string.baaja_settings_downloads_storage),
        SettingsSearchEntry(R.string.backup_restore, "settings/backup_restore", R.string.backup_restore),
        SettingsSearchEntry(R.string.android_auto, "settings/android_auto", R.string.android_auto),
        SettingsSearchEntry(R.string.updater, "settings/updater", R.string.updater),
        SettingsSearchEntry(R.string.baaja_home_sections, "settings/content/home_sections", R.string.baaja_home_sections),
    )

@Composable
fun SettingsSearchResults(
    query: String,
    navController: NavController,
) {
    val context = LocalContext.current
    val terms = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
    val matches =
        settingsSearchIndex
            .map { it to context.getString(it.title) }
            .filter { (entry, title) ->
                val haystack = (title + " " + context.getString(entry.page)).lowercase()
                terms.all { it in haystack }
            }.distinctBy { (entry, title) -> title to entry.route }
            // Exact title matches first, then the rest in page order.
            .sortedBy { (_, title) -> if (title.lowercase().startsWith(terms.firstOrNull().orEmpty())) 0 else 1 }

    if (matches.isEmpty()) {
        Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.baaja_settings_search_empty),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    Material3SettingsGroup(
        title = stringResource(R.string.baaja_settings_search_results),
        items =
            matches.take(30).map { (entry, title) ->
                Material3SettingsItem(
                    icon = painterResource(R.drawable.search),
                    title = { Text(title) },
                    description = { Text(stringResource(entry.page)) },
                    onClick = { navController.navigate(entry.route) },
                )
            },
    )
}
