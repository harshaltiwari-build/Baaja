/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.settings

import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.navigation.NavController
import com.metrolist.music.BuildConfig
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.constants.AudioNormalizationKey
import com.metrolist.music.constants.AudioOutputDeviceKey
import com.metrolist.music.constants.AudioQuality
import com.metrolist.music.constants.AudioQualityKey
import com.metrolist.music.constants.AutoSkipNextOnErrorKey
import com.metrolist.music.constants.CrossfadeDurationKey
import com.metrolist.music.constants.DownloadAudioQualityKey
import com.metrolist.music.constants.EnableGoogleCastKey
import com.metrolist.music.constants.FloatOutputKey
import com.metrolist.music.constants.HistoryDuration
import com.metrolist.music.constants.KeepScreenOn
import com.metrolist.music.constants.LoudnessLevel
import com.metrolist.music.constants.LoudnessLevelKey
import com.metrolist.music.constants.MobileAudioQualityKey
import com.metrolist.music.constants.PauseOnMute
import com.metrolist.music.constants.PersistentQueueKey
import com.metrolist.music.constants.PreventDuplicateTracksInQueueKey
import com.metrolist.music.constants.RememberShuffleAndRepeatKey
import com.metrolist.music.constants.ResumeOnBluetoothConnectKey
import com.metrolist.music.constants.SeekExtraSeconds
import com.metrolist.music.constants.ShowLyricPreviewOnPlayerKey
import com.metrolist.music.constants.ShufflePlaylistFirstKey
import com.metrolist.music.constants.SkipSilenceInstantKey
import com.metrolist.music.constants.SkipSilenceKey
import com.metrolist.music.constants.SleepTimerCustomDaysKey
import com.metrolist.music.constants.SleepTimerDayTimesKey
import com.metrolist.music.constants.SleepTimerEnabledKey
import com.metrolist.music.constants.SleepTimerEndTimeKey
import com.metrolist.music.constants.SleepTimerFadeOutKey
import com.metrolist.music.constants.SleepTimerRepeatKey
import com.metrolist.music.constants.SleepTimerStartTimeKey
import com.metrolist.music.constants.SleepTimerStopAfterCurrentSongKey
import com.metrolist.music.constants.SponsorBlockEnabledKey
import com.metrolist.music.constants.StopMusicOnTaskClearKey
import com.metrolist.music.constants.TransitionStyle
import com.metrolist.music.constants.rememberKeepPlayingSimilar
import com.metrolist.music.constants.rememberSetKeepPlayingSimilar
import com.metrolist.music.constants.rememberSetTransitionStyle
import com.metrolist.music.constants.rememberTransitionStyle
import com.metrolist.music.playback.AudioOutputDevice
import com.metrolist.music.playback.AudioOutputKind
import com.metrolist.music.playback.audioOutputKeyName
import com.metrolist.music.playback.connectedAudioOutputs
import com.metrolist.music.ui.component.DefaultDialog
import com.metrolist.music.ui.component.EnumDialog
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.ui.component.SleepTimerDialog
import com.metrolist.music.ui.component.decodeDayTimes
import com.metrolist.music.ui.component.encodeDayTimes
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.ui.utils.getLoudnessLevelLabel
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import kotlin.math.roundToInt

/** Scrolling settings page with the standard top bar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsSubPage(
    title: String,
    navController: NavController,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
            ).verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)))
        content()
        Spacer(modifier = Modifier.height(16.dp))
    }

    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
            }
        },
    )
}

@Composable
internal fun switchItem(
    @DrawableRes icon: Int,
    title: String,
    description: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) = Material3SettingsItem(
    icon = painterResource(icon),
    title = { Text(title) },
    description = description?.let { { Text(it) } },
    enabled = enabled,
    trailingContent = {
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled,
            thumbContent = {
                Icon(
                    painter = painterResource(if (checked) R.drawable.check else R.drawable.close),
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize),
                )
            },
        )
    },
    onClick = { if (enabled) onChange(!checked) },
)

@Composable
private fun audioQualityLabel(quality: AudioQuality): String =
    when (quality) {
        AudioQuality.AUTO -> stringResource(R.string.audio_quality_auto)
        AudioQuality.HIGH -> stringResource(R.string.audio_quality_high)
        AudioQuality.LOW -> stringResource(R.string.audio_quality_low)
    }

@Composable
private fun AudioQualityDialog(
    title: String,
    current: AudioQuality,
    autoLabel: String,
    onSelect: (AudioQuality) -> Unit,
    onDismiss: () -> Unit,
) {
    EnumDialog(
        onDismiss = onDismiss,
        onSelect = onSelect,
        title = title,
        current = current,
        values = AudioQuality.entries.toList(),
        valueText = { if (it == AudioQuality.AUTO) autoLabel else audioQualityLabel(it) },
    )
}

@Composable
private fun transitionLabel(style: TransitionStyle): String =
    stringResource(
        when (style) {
            TransitionStyle.OFF -> R.string.baaja_clean_transition_off
            TransitionStyle.CROSSFADE -> R.string.baaja_clean_transition_crossfade
            TransitionStyle.AUTOMIX -> R.string.baaja_clean_transition_automix
        },
    )

@Composable
private fun transitionDescription(style: TransitionStyle): String =
    stringResource(
        when (style) {
            TransitionStyle.OFF -> R.string.baaja_clean_transition_off_desc
            TransitionStyle.CROSSFADE -> R.string.baaja_clean_transition_crossfade_desc
            TransitionStyle.AUTOMIX -> R.string.baaja_clean_transition_automix_desc
        },
    )

@Composable
fun PlaybackSettings(navController: NavController) {
    val (mobileAudioQuality, onMobileAudioQualityChange) = rememberEnumPreference(MobileAudioQualityKey, defaultValue = AudioQuality.AUTO)
    val (downloadAudioQuality, onDownloadAudioQualityChange) = rememberEnumPreference(DownloadAudioQualityKey, defaultValue = AudioQuality.AUTO)
    val (audioQuality, onAudioQualityChange) = rememberEnumPreference(AudioQualityKey, defaultValue = AudioQuality.AUTO)
    val transitionStyle by rememberTransitionStyle()
    val setTransitionStyle = rememberSetTransitionStyle()
    val (crossfadeDuration, onCrossfadeDurationChange) = rememberPreference(CrossfadeDurationKey, defaultValue = 5f)
    val (skipSilence, onSkipSilenceChange) = rememberPreference(SkipSilenceKey, defaultValue = false)
    val (skipSilenceInstant, onSkipSilenceInstantChange) = rememberPreference(SkipSilenceInstantKey, defaultValue = false)
    val (audioNormalization, onAudioNormalizationChange) = rememberPreference(AudioNormalizationKey, defaultValue = true)
    val (loudnessLevel, onLoudnessLevelChange) = rememberEnumPreference(LoudnessLevelKey, defaultValue = LoudnessLevel.BALANCED)
    val (seekExtraSeconds, onSeekExtraSeconds) = rememberPreference(SeekExtraSeconds, defaultValue = false)
    val (historyDuration, onHistoryDurationChange) = rememberPreference(HistoryDuration, defaultValue = 30f)

    var showAudioQualityDialog by remember { mutableStateOf(false) }
    var showMobileQualityDialog by remember { mutableStateOf(false) }
    var showDownloadQualityDialog by remember { mutableStateOf(false) }
    var showLoudnessLevelDialog by remember { mutableStateOf(false) }
    var showTransitionDialog by remember { mutableStateOf(false) }

    if (showAudioQualityDialog) {
        AudioQualityDialog(
            title = stringResource(R.string.audio_quality),
            current = audioQuality,
            autoLabel = stringResource(R.string.audio_quality_auto),
            onSelect = { onAudioQualityChange(it); showAudioQualityDialog = false },
            onDismiss = { showAudioQualityDialog = false },
        )
    }
    if (showMobileQualityDialog) {
        AudioQualityDialog(
            title = stringResource(R.string.audio_quality_mobile),
            current = mobileAudioQuality,
            autoLabel = stringResource(R.string.audio_quality_auto_save_data),
            onSelect = { onMobileAudioQualityChange(it); showMobileQualityDialog = false },
            onDismiss = { showMobileQualityDialog = false },
        )
    }
    if (showDownloadQualityDialog) {
        AudioQualityDialog(
            title = stringResource(R.string.audio_quality_download),
            current = downloadAudioQuality,
            autoLabel = stringResource(R.string.audio_quality_auto),
            onSelect = { onDownloadAudioQualityChange(it); showDownloadQualityDialog = false },
            onDismiss = { showDownloadQualityDialog = false },
        )
    }
    if (showLoudnessLevelDialog) {
        EnumDialog(
            onDismiss = { showLoudnessLevelDialog = false },
            onSelect = { onLoudnessLevelChange(it); showLoudnessLevelDialog = false },
            title = stringResource(R.string.loudness_level),
            current = loudnessLevel,
            values = LoudnessLevel.entries.toList(),
            valueText = { getLoudnessLevelLabel(it) },
        )
    }
    if (showTransitionDialog) {
        EnumDialog(
            onDismiss = { showTransitionDialog = false },
            onSelect = { setTransitionStyle(it); showTransitionDialog = false },
            title = stringResource(R.string.baaja_clean_transition_title),
            current = transitionStyle,
            values = TransitionStyle.entries.toList(),
            valueText = { transitionLabel(it) },
            valueDescription = { transitionDescription(it) },
        )
    }

    SettingsSubPage(stringResource(R.string.baaja_clean_menu_playback_title), navController) {
        Material3SettingsGroup(
            title = stringResource(R.string.baaja_clean_menu_playback_title),
            items = buildList {
                add(Material3SettingsItem(
                    icon = painterResource(R.drawable.graphic_eq),
                    title = { Text(stringResource(R.string.audio_quality)) },
                    description = { Text(audioQualityLabel(audioQuality)) },
                    onClick = { showAudioQualityDialog = true },
                ))
                add(Material3SettingsItem(
                    icon = painterResource(R.drawable.graphic_eq),
                    title = { Text(stringResource(R.string.audio_quality_mobile)) },
                    description = {
                        Text(
                            if (mobileAudioQuality == AudioQuality.AUTO) stringResource(R.string.audio_quality_auto_save_data)
                            else audioQualityLabel(mobileAudioQuality),
                        )
                    },
                    onClick = { showMobileQualityDialog = true },
                ))
                add(Material3SettingsItem(
                    icon = painterResource(R.drawable.download),
                    title = { Text(stringResource(R.string.audio_quality_download)) },
                    description = { Text(audioQualityLabel(downloadAudioQuality)) },
                    onClick = { showDownloadQualityDialog = true },
                ))
                add(Material3SettingsItem(
                    icon = painterResource(R.drawable.linear_scale),
                    title = { Text(stringResource(R.string.baaja_clean_transition_title)) },
                    description = { Text(transitionLabel(transitionStyle)) },
                    onClick = { showTransitionDialog = true },
                ))
                if (transitionStyle == TransitionStyle.CROSSFADE) {
                    add(Material3SettingsItem(
                        icon = painterResource(R.drawable.timer),
                        title = { Text(stringResource(R.string.crossfade_duration)) },
                        description = {
                            Column {
                                Text(pluralStringResource(R.plurals.seconds, crossfadeDuration.toInt(), crossfadeDuration.toInt()))
                                Slider(
                                    value = crossfadeDuration,
                                    onValueChange = onCrossfadeDurationChange,
                                    valueRange = 1f..15f,
                                    steps = 14,
                                )
                            }
                        },
                    ))
                }
                add(switchItem(
                    icon = R.drawable.fast_forward,
                    title = stringResource(R.string.skip_silence),
                    description = stringResource(R.string.skip_silence_desc),
                    checked = skipSilence,
                    onChange = onSkipSilenceChange,
                ))
                if (skipSilence) {
                    add(switchItem(
                        icon = R.drawable.skip_next,
                        title = stringResource(R.string.skip_silence_instant),
                        description = stringResource(R.string.skip_silence_instant_desc),
                        checked = skipSilenceInstant,
                        onChange = onSkipSilenceInstantChange,
                    ))
                }
                add(switchItem(
                    icon = R.drawable.volume_up,
                    title = stringResource(R.string.audio_normalization),
                    description = null,
                    checked = audioNormalization,
                    onChange = onAudioNormalizationChange,
                ))
                if (audioNormalization) {
                    add(Material3SettingsItem(
                        icon = painterResource(R.drawable.volume_up),
                        title = { Text(stringResource(R.string.loudness_level)) },
                        description = { Text(getLoudnessLevelLabel(loudnessLevel)) },
                        onClick = { showLoudnessLevelDialog = true },
                    ))
                }
                add(switchItem(
                    icon = R.drawable.arrow_forward,
                    title = stringResource(R.string.seek_seconds_addup),
                    description = stringResource(R.string.baaja_clean_seek_desc),
                    checked = seekExtraSeconds,
                    onChange = onSeekExtraSeconds,
                ))
                add(Material3SettingsItem(
                    icon = painterResource(R.drawable.history),
                    title = { Text(stringResource(R.string.baaja_clean_count_play_title, historyDuration.roundToInt())) },
                    description = {
                        Column {
                            Text(stringResource(R.string.baaja_clean_count_play_desc))
                            Slider(
                                value = historyDuration,
                                onValueChange = { onHistoryDurationChange(it.roundToInt().toFloat()) },
                                valueRange = 1f..100f,
                            )
                        }
                    },
                ))
            },
        )
    }
}

@Composable
fun PlayerPageSettings(navController: NavController) {
    val (showLyricPreview, onShowLyricPreviewChange) = rememberPreference(ShowLyricPreviewOnPlayerKey, defaultValue = true)
    val (sponsorBlockEnabled, onSponsorBlockEnabledChange) = rememberPreference(SponsorBlockEnabledKey, defaultValue = false)
    val (enableGoogleCast, onEnableGoogleCastChange) = rememberPreference(EnableGoogleCastKey, defaultValue = true)
    val (floatOutput, onFloatOutputChange) = rememberPreference(FloatOutputKey, defaultValue = false)
    val (audioOutputKey, onAudioOutputKeyChange) = rememberPreference(AudioOutputDeviceKey, defaultValue = "")
    val outputDevices = rememberConnectedAudioOutputs()
    var showAudioOutputDialog by remember { mutableStateOf(false) }

    if (showAudioOutputDialog) {
        val options = listOf("") + outputDevices.map { it.key } +
            listOfNotNull(audioOutputKey.takeIf { key -> key.isNotBlank() && outputDevices.none { it.key == key } })
        EnumDialog(
            onDismiss = { showAudioOutputDialog = false },
            onSelect = {
                onAudioOutputKeyChange(it)
                showAudioOutputDialog = false
            },
            title = stringResource(R.string.baaja_pb_output_dialog_title),
            current = audioOutputKey,
            values = options,
            valueText = { key -> audioOutputLabel(key, outputDevices) },
            valueDescription = { key ->
                when {
                    key.isBlank() -> stringResource(R.string.baaja_pb_output_automatic_desc)
                    key == audioOutputKey -> stringResource(R.string.baaja_pb_output_remembered)
                    else -> outputDevices.firstOrNull { it.key == key }?.kind?.let { audioOutputKindLabel(it) }.orEmpty()
                }
            },
        )
    }

    SettingsSubPage(stringResource(R.string.baaja_clean_menu_player_title), navController) {
        Material3SettingsGroup(
            title = stringResource(R.string.baaja_clean_menu_player_title),
            items = buildList {
                add(switchItem(
                    icon = R.drawable.lyrics,
                    title = stringResource(R.string.baaja_player_show_lyric_preview),
                    description = stringResource(R.string.baaja_player_show_lyric_preview_desc),
                    checked = showLyricPreview,
                    onChange = onShowLyricPreviewChange,
                ))
                add(switchItem(
                    icon = R.drawable.skip_next,
                    title = stringResource(R.string.sponsorblock),
                    description = stringResource(R.string.baaja_clean_sponsorblock_desc),
                    checked = sponsorBlockEnabled,
                    onChange = onSponsorBlockEnabledChange,
                ))
                // Only show Cast setting in GMS builds (not in F-Droid/FOSS)
                if (BuildConfig.CAST_AVAILABLE) {
                    add(switchItem(
                        icon = R.drawable.cast,
                        title = stringResource(R.string.google_cast),
                        description = stringResource(R.string.google_cast_description),
                        checked = enableGoogleCast,
                        onChange = onEnableGoogleCastChange,
                    ))
                }
                add(Material3SettingsItem(
                    icon = painterResource(R.drawable.volume_up),
                    title = { Text(stringResource(R.string.baaja_pb_output_title)) },
                    description = {
                        Text(
                            when {
                                audioOutputKey.isBlank() -> stringResource(R.string.baaja_pb_output_automatic)
                                outputDevices.any { it.key == audioOutputKey } -> audioOutputLabel(audioOutputKey, outputDevices)
                                else -> stringResource(R.string.baaja_pb_output_not_connected, audioOutputLabel(audioOutputKey, outputDevices))
                            },
                        )
                    },
                    onClick = { showAudioOutputDialog = true },
                ))
                add(switchItem(
                    icon = R.drawable.graphic_eq,
                    title = stringResource(R.string.baaja_pb_float_title),
                    description = stringResource(R.string.baaja_clean_float_desc),
                    checked = floatOutput,
                    onChange = onFloatOutputChange,
                ))
            },
        )
    }
}

@Composable
fun QueueSettings(navController: NavController) {
    val (persistentQueue, onPersistentQueueChange) = rememberPreference(PersistentQueueKey, defaultValue = true)
    val keepPlayingSimilar by rememberKeepPlayingSimilar()
    val setKeepPlayingSimilar = rememberSetKeepPlayingSimilar()
    val (shufflePlaylistFirst, onShufflePlaylistFirstChange) = rememberPreference(ShufflePlaylistFirstKey, defaultValue = false)
    val (rememberShuffleAndRepeat, onRememberShuffleAndRepeatChange) = rememberPreference(RememberShuffleAndRepeatKey, defaultValue = true)
    val (preventDuplicateTracksInQueue, onPreventDuplicateTracksInQueueChange) = rememberPreference(PreventDuplicateTracksInQueueKey, defaultValue = false)
    val (autoSkipNextOnError, onAutoSkipNextOnErrorChange) = rememberPreference(AutoSkipNextOnErrorKey, defaultValue = false)
    val (stopMusicOnTaskClear, onStopMusicOnTaskClearChange) = rememberPreference(StopMusicOnTaskClearKey, defaultValue = false)
    val (pauseOnMute, onPauseOnMuteChange) = rememberPreference(PauseOnMute, defaultValue = false)
    val (resumeOnBluetoothConnect, onResumeOnBluetoothConnectChange) = rememberPreference(ResumeOnBluetoothConnectKey, defaultValue = false)
    val (keepScreenOn, onKeepScreenOnChange) = rememberPreference(KeepScreenOn, defaultValue = false)

    SettingsSubPage(stringResource(R.string.baaja_clean_menu_queue_title), navController) {
        Material3SettingsGroup(
            title = stringResource(R.string.queue),
            items = buildList {
                add(switchItem(
                    icon = R.drawable.queue_music,
                    title = stringResource(R.string.persistent_queue),
                    description = stringResource(R.string.persistent_queue_desc),
                    checked = persistentQueue,
                    onChange = onPersistentQueueChange,
                ))
                add(switchItem(
                    icon = R.drawable.radio,
                    title = stringResource(R.string.baaja_clean_keep_similar_title),
                    description = stringResource(R.string.baaja_clean_keep_similar_desc),
                    checked = keepPlayingSimilar,
                    onChange = setKeepPlayingSimilar,
                ))
                add(switchItem(
                    icon = R.drawable.shuffle,
                    title = stringResource(R.string.baaja_clean_remember_shuffle_title),
                    description = stringResource(R.string.baaja_clean_remember_shuffle_desc),
                    checked = rememberShuffleAndRepeat,
                    onChange = onRememberShuffleAndRepeatChange,
                ))
                add(switchItem(
                    icon = R.drawable.shuffle,
                    title = stringResource(R.string.shuffle_playlist_first),
                    description = stringResource(R.string.baaja_clean_shuffle_first_desc),
                    checked = shufflePlaylistFirst,
                    onChange = onShufflePlaylistFirstChange,
                ))
                add(switchItem(
                    icon = R.drawable.queue_music,
                    title = stringResource(R.string.prevent_duplicate_tracks_in_queue),
                    description = stringResource(R.string.baaja_clean_prevent_dup_desc),
                    checked = preventDuplicateTracksInQueue,
                    onChange = onPreventDuplicateTracksInQueueChange,
                ))
                add(switchItem(
                    icon = R.drawable.skip_next,
                    title = stringResource(R.string.auto_skip_next_on_error),
                    description = stringResource(R.string.baaja_clean_skip_error_desc),
                    checked = autoSkipNextOnError,
                    onChange = onAutoSkipNextOnErrorChange,
                ))
            },
        )

        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.misc),
            items = listOf(
                switchItem(
                    icon = R.drawable.clear_all,
                    title = stringResource(R.string.stop_music_on_task_clear),
                    description = null,
                    checked = stopMusicOnTaskClear,
                    onChange = onStopMusicOnTaskClearChange,
                ),
                switchItem(
                    icon = R.drawable.volume_off_pause,
                    title = stringResource(R.string.pause_music_when_media_is_muted),
                    description = null,
                    checked = pauseOnMute,
                    onChange = onPauseOnMuteChange,
                ),
                switchItem(
                    icon = R.drawable.bluetooth,
                    title = stringResource(R.string.resume_on_bluetooth_connect),
                    description = null,
                    checked = resumeOnBluetoothConnect,
                    onChange = onResumeOnBluetoothConnectChange,
                ),
                switchItem(
                    icon = R.drawable.screenshot,
                    title = stringResource(R.string.keep_screen_on_when_player_is_expanded),
                    description = null,
                    checked = keepScreenOn,
                    onChange = onKeepScreenOnChange,
                ),
            ),
        )
    }
}

@Composable
fun TimersSettings(navController: NavController) {
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    val (sleepTimerEnabled, onSleepTimerEnabledChange) = rememberPreference(SleepTimerEnabledKey, defaultValue = false)
    val (sleepTimerRepeat, onSleepTimerRepeatChange) = rememberPreference(SleepTimerRepeatKey, defaultValue = "daily")
    val (sleepTimerStartTime, onSleepTimerStartTimeChange) = rememberPreference(SleepTimerStartTimeKey, defaultValue = "22:00")
    val (sleepTimerEndTime, onSleepTimerEndTimeChange) = rememberPreference(SleepTimerEndTimeKey, defaultValue = "06:00")
    val (sleepTimerCustomDays, onSleepTimerCustomDaysChange) = rememberPreference(SleepTimerCustomDaysKey, defaultValue = "0,1,2,3,4")
    // Per-day time ranges used in custom mode
    val (sleepTimerDayTimes, onSleepTimerDayTimesChange) = rememberPreference(SleepTimerDayTimesKey, defaultValue = "")
    val (sleepTimerStopAfterCurrentSong, onSleepTimerStopAfterCurrentSongChange) = rememberPreference(SleepTimerStopAfterCurrentSongKey, defaultValue = false)
    val (sleepTimerFadeOut, onSleepTimerFadeOutChange) = rememberPreference(SleepTimerFadeOutKey, false)

    if (showSleepTimerDialog) {
        SleepTimerDialog(
            isVisible = true,
            onDismiss = { showSleepTimerDialog = false },
            onConfirm = { repeat, startTime, endTime, days, dayTimes ->
                onSleepTimerRepeatChange(repeat)
                onSleepTimerStartTimeChange(startTime)
                onSleepTimerEndTimeChange(endTime)
                onSleepTimerCustomDaysChange(days?.joinToString(",") ?: "0,1,2,3,4")
                onSleepTimerDayTimesChange(encodeDayTimes(dayTimes))
                showSleepTimerDialog = false
            },
            initialRepeat = sleepTimerRepeat,
            initialStartTime = sleepTimerStartTime,
            initialEndTime = sleepTimerEndTime,
            initialCustomDays = sleepTimerCustomDays.split(",").mapNotNull { it.toIntOrNull() },
            initialDayTimes = decodeDayTimes(sleepTimerDayTimes),
        )
    }

    SettingsSubPage(stringResource(R.string.baaja_clean_menu_timers_title), navController) {
        Material3SettingsGroup(
            title = stringResource(R.string.sleep_timer),
            items = listOf(
                switchItem(
                    icon = R.drawable.time_auto,
                    title = stringResource(R.string.enable_automatic_sleeptimer),
                    description = stringResource(R.string.baaja_clean_sleep_auto_desc),
                    checked = sleepTimerEnabled,
                    onChange = onSleepTimerEnabledChange,
                ),
                // The dialog already holds repeat mode, custom days and per-day times in one place.
                Material3SettingsItem(
                    icon = painterResource(R.drawable.baseline_event_repeat_24),
                    title = { Text(stringResource(R.string.baaja_clean_days_title)) },
                    description = { Text(stringResource(R.string.baaja_clean_days_desc)) },
                    onClick = { showSleepTimerDialog = true },
                ),
                switchItem(
                    icon = R.drawable.more_time,
                    title = stringResource(R.string.sleep_timer_stop_after_current_song_title),
                    description = stringResource(R.string.baaja_clean_sleep_finish_desc),
                    checked = sleepTimerStopAfterCurrentSong,
                    onChange = onSleepTimerStopAfterCurrentSongChange,
                ),
                switchItem(
                    icon = R.drawable.timer_arrow_down,
                    title = stringResource(R.string.sleep_timer_fade_out_title),
                    description = stringResource(R.string.baaja_clean_sleep_fade_desc),
                    checked = sleepTimerFadeOut,
                    onChange = onSleepTimerFadeOutChange,
                ),
            ),
        )

        Spacer(modifier = Modifier.height(16.dp))

        AlarmSettingsSection()
    }
}

/** Connected outputs, kept up to date while the screen is shown. */
@Composable
private fun rememberConnectedAudioOutputs(): List<AudioOutputDevice> {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService<AudioManager>()!! }
    var devices by remember { mutableStateOf(audioManager.connectedAudioOutputs()) }
    DisposableEffect(audioManager) {
        val callback =
            object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                    devices = audioManager.connectedAudioOutputs()
                }

                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                    devices = audioManager.connectedAudioOutputs()
                }
            }
        audioManager.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
        onDispose { audioManager.unregisterAudioDeviceCallback(callback) }
    }
    return devices
}

@Composable
private fun audioOutputKindLabel(kind: AudioOutputKind): String =
    when (kind) {
        AudioOutputKind.SPEAKER -> stringResource(R.string.baaja_pb_output_kind_speaker)
        AudioOutputKind.WIRED -> stringResource(R.string.baaja_pb_output_kind_wired)
        AudioOutputKind.USB -> stringResource(R.string.baaja_pb_output_kind_usb)
        AudioOutputKind.BLUETOOTH -> stringResource(R.string.baaja_pb_output_kind_bluetooth)
        AudioOutputKind.HDMI -> stringResource(R.string.baaja_pb_output_kind_hdmi)
    }

@Composable
private fun audioOutputLabel(key: String, connected: List<AudioOutputDevice>): String {
    if (key.isBlank()) return stringResource(R.string.baaja_pb_output_automatic)
    val device = connected.firstOrNull { it.key == key }
    // The built-in speaker reports the phone model as its name; "Phone speaker" reads better.
    if (device?.kind == AudioOutputKind.SPEAKER) return stringResource(R.string.baaja_pb_output_kind_speaker)
    val name = device?.productName ?: audioOutputKeyName(key)
    val kind = device?.kind?.let { audioOutputKindLabel(it) }
    return when {
        name.isBlank() -> kind ?: key
        kind == null -> name
        else -> "$name · $kind"
    }
}
