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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.metrolist.music.BuildConfig
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.constants.SponsorBlockEnabledKey
import com.metrolist.music.constants.DownloadAudioQualityKey
import com.metrolist.music.constants.MobileAudioQualityKey
import com.metrolist.music.constants.SoundBalanceKey
import com.metrolist.music.constants.SoundBassKey
import com.metrolist.music.constants.SoundTrebleKey
import com.metrolist.music.constants.SoundWidthKey
import com.metrolist.music.constants.ImmersivePlayerKey
import com.metrolist.music.constants.ShowAudioFormatOnPlayerKey
import com.metrolist.music.constants.ShowLyricPreviewOnPlayerKey
import com.metrolist.music.constants.AudioNormalizationKey
import com.metrolist.music.constants.AudioOffload
import com.metrolist.music.constants.AudioTrackPlaybackParamsKey
import com.metrolist.music.constants.AudioQuality
import com.metrolist.music.constants.AudioQualityKey
import com.metrolist.music.constants.AutoDownloadOnLikeKey
import com.metrolist.music.constants.CrossfadeDurationKey
import com.metrolist.music.constants.CrossfadeEnabledKey
import com.metrolist.music.constants.CrossfadeGaplessKey
import com.metrolist.music.constants.AutoLoadMoreKey
import com.metrolist.music.constants.AutoRadioQueueKey
import com.metrolist.music.constants.AutoSkipNextOnErrorKey
import com.metrolist.music.constants.AutoplayKey
import com.metrolist.music.constants.DisableLoadMoreWhenRepeatAllKey
import com.metrolist.music.constants.EnableGoogleCastKey
import com.metrolist.music.constants.HistoryDuration
import com.metrolist.music.constants.KeepScreenOn
import com.metrolist.music.constants.LoudnessLevel
import com.metrolist.music.constants.LoudnessLevelKey
import com.metrolist.music.constants.PauseOnMute
import com.metrolist.music.constants.PersistentQueueKey
import com.metrolist.music.constants.PersistentShuffleAcrossQueuesKey
import com.metrolist.music.constants.PreventDuplicateTracksInQueueKey
import com.metrolist.music.constants.RememberShuffleAndRepeatKey
import com.metrolist.music.constants.ResumeOnBluetoothConnectKey
import com.metrolist.music.constants.SeekExtraSeconds
import com.metrolist.music.constants.ShufflePlaylistFirstKey
import com.metrolist.music.constants.SimilarContent
import com.metrolist.music.constants.SkipSilenceInstantKey
import com.metrolist.music.constants.SkipSilenceKey
import com.metrolist.music.constants.StopMusicOnTaskClearKey
import com.metrolist.music.constants.VarispeedKey
import com.metrolist.music.ui.component.DefaultDialog
import com.metrolist.music.ui.component.EnumDialog
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import kotlin.math.roundToInt
import com.metrolist.music.ui.component.SleepTimerDialog
import com.metrolist.music.constants.SleepTimerEnabledKey
import com.metrolist.music.constants.SleepTimerRepeatKey
import com.metrolist.music.constants.SleepTimerCustomDaysKey
import com.metrolist.music.constants.SleepTimerEndTimeKey
import com.metrolist.music.constants.SleepTimerStartTimeKey
import com.metrolist.music.constants.SleepTimerDayTimesKey
import com.metrolist.music.ui.component.decodeDayTimes
import com.metrolist.music.ui.component.encodeDayTimes
import com.metrolist.music.constants.SleepTimerFadeOutKey
import com.metrolist.music.constants.SleepTimerStopAfterCurrentSongKey
import com.metrolist.music.ui.utils.getLoudnessLevelLabel
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.getSystemService
import com.metrolist.music.constants.AudioOutputDeviceKey
import com.metrolist.music.constants.AutomixTransitionsKey
import com.metrolist.music.constants.FloatOutputKey
import com.metrolist.music.constants.SmartDownloadsCountKey
import com.metrolist.music.constants.SmartDownloadsWifiOnlyKey
import com.metrolist.music.playback.AudioOutputDevice
import com.metrolist.music.playback.AudioOutputKind
import com.metrolist.music.playback.SmartDownloads
import com.metrolist.music.playback.SmartDownloadsCounts
import com.metrolist.music.playback.audioOutputKeyName
import com.metrolist.music.playback.connectedAudioOutputs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSettings(
    navController: NavController
) {
    val (mobileAudioQuality, onMobileAudioQualityChange) = rememberEnumPreference(
        MobileAudioQualityKey,
        defaultValue = AudioQuality.AUTO
    )
    val (downloadAudioQuality, onDownloadAudioQualityChange) = rememberEnumPreference(
        DownloadAudioQualityKey,
        defaultValue = AudioQuality.AUTO
    )
    val (audioQuality, onAudioQualityChange) = rememberEnumPreference(
        AudioQualityKey,
        defaultValue = AudioQuality.AUTO
    )
    val (crossfadeEnabled, onCrossfadeEnabledChange) = rememberPreference(
        CrossfadeEnabledKey,
        defaultValue = false
    )
    val (crossfadeDuration, onCrossfadeDurationChange) = rememberPreference(
        CrossfadeDurationKey,
        defaultValue = 5f
    )
    val (crossfadeGapless, onCrossfadeGaplessChange) = rememberPreference(
        CrossfadeGaplessKey,
        defaultValue = true
    )
    val (persistentQueue, onPersistentQueueChange) = rememberPreference(
        PersistentQueueKey,
        defaultValue = true
    )
    val (soundWidth, onSoundWidthChange) = rememberPreference(SoundWidthKey, defaultValue = 1f)
    val (soundBalance, onSoundBalanceChange) = rememberPreference(SoundBalanceKey, defaultValue = 0f)
    val (soundBass, onSoundBassChange) = rememberPreference(SoundBassKey, defaultValue = 0f)
    val (soundTreble, onSoundTrebleChange) = rememberPreference(SoundTrebleKey, defaultValue = 0f)
    val (sponsorBlockEnabled, onSponsorBlockEnabledChange) = rememberPreference(SponsorBlockEnabledKey, defaultValue = false)
    val (showAudioFormat, onShowAudioFormatChange) = rememberPreference(
        ShowAudioFormatOnPlayerKey,
        defaultValue = true
    )
    val (showLyricPreview, onShowLyricPreviewChange) = rememberPreference(ShowLyricPreviewOnPlayerKey, defaultValue = true)
    val (immersivePlayer, onImmersivePlayerChange) = rememberPreference(ImmersivePlayerKey, defaultValue = false)
    val (skipSilence, onSkipSilenceChange) = rememberPreference(
        SkipSilenceKey,
        defaultValue = false
    )
    val (skipSilenceInstant, onSkipSilenceInstantChange) = rememberPreference(
        SkipSilenceInstantKey,
        defaultValue = false
    )
    val (audioNormalization, onAudioNormalizationChange) = rememberPreference(
        AudioNormalizationKey,
        defaultValue = true
    )

    val (loudnessLevel, onLoudnessLevelChange) = rememberEnumPreference(
        LoudnessLevelKey,
        defaultValue = LoudnessLevel.BALANCED,
    )

    val (audioOffload, onAudioOffloadChange) = rememberPreference(
        key = AudioOffload,
        defaultValue = false
    )

    val (audioTrackPlaybackParams, onAudioTrackPlaybackParamsChange) = rememberPreference(
        key = AudioTrackPlaybackParamsKey,
        defaultValue = true
    )

    val (varispeed, onVarispeedChange) = rememberPreference(
        key = VarispeedKey,
        defaultValue = false
    )

    val (enableGoogleCast, onEnableGoogleCastChange) = rememberPreference(
        key = EnableGoogleCastKey,
        defaultValue = true
    )

    val (seekExtraSeconds, onSeekExtraSeconds) = rememberPreference(
        SeekExtraSeconds,
        defaultValue = false
    )

    val (autoLoadMore, onAutoLoadMoreChange) = rememberPreference(
        AutoLoadMoreKey,
        defaultValue = true
    )
    val (autoRadioQueue, onAutoRadioQueueChange) = rememberPreference(
        AutoRadioQueueKey,
        defaultValue = true
    )
    val (disableLoadMoreWhenRepeatAll, onDisableLoadMoreWhenRepeatAllChange) = rememberPreference(
        DisableLoadMoreWhenRepeatAllKey,
        defaultValue = false
    )
    val (autoDownloadOnLike, onAutoDownloadOnLikeChange) = rememberPreference(
        AutoDownloadOnLikeKey,
        defaultValue = false
    )
    val (similarContentEnabled, similarContentEnabledChange) = rememberPreference(
        key = SimilarContent,
        defaultValue = true
    )
    val (autoSkipNextOnError, onAutoSkipNextOnErrorChange) = rememberPreference(
        AutoSkipNextOnErrorKey,
        defaultValue = false
    )
    val (autoplay, onAutoplayChange) = rememberPreference(
        AutoplayKey,
        defaultValue = true
    )
    val (persistentShuffleAcrossQueues, onPersistentShuffleAcrossQueuesChange) = rememberPreference(
        PersistentShuffleAcrossQueuesKey,
        defaultValue = false
    )
    val (rememberShuffleAndRepeat, onRememberShuffleAndRepeatChange) = rememberPreference(
        RememberShuffleAndRepeatKey,
        defaultValue = true
    )
    val (shufflePlaylistFirst, onShufflePlaylistFirstChange) = rememberPreference(
        ShufflePlaylistFirstKey,
        defaultValue = false
    )
    val (preventDuplicateTracksInQueue, onPreventDuplicateTracksInQueueChange) = rememberPreference(
        PreventDuplicateTracksInQueueKey,
        defaultValue = false
    )
    val (stopMusicOnTaskClear, onStopMusicOnTaskClearChange) = rememberPreference(
        StopMusicOnTaskClearKey,
        defaultValue = false
    )
    val (pauseOnMute, onPauseOnMuteChange) = rememberPreference(
        PauseOnMute,
        defaultValue = false
    )
    val (resumeOnBluetoothConnect, onResumeOnBluetoothConnectChange) = rememberPreference(
        ResumeOnBluetoothConnectKey,
        defaultValue = false
    )
    val (keepScreenOn, onKeepScreenOnChange) = rememberPreference(
        KeepScreenOn,
        defaultValue = false
    )
    val (historyDuration, onHistoryDurationChange) = rememberPreference(
        HistoryDuration,
        defaultValue = 30f
    )

    val (automixEnabled, onAutomixEnabledChange) = rememberPreference(AutomixTransitionsKey, defaultValue = false)
    val (floatOutput, onFloatOutputChange) = rememberPreference(FloatOutputKey, defaultValue = false)
    val (audioOutputKey, onAudioOutputKeyChange) = rememberPreference(AudioOutputDeviceKey, defaultValue = "")
    val (smartDownloadsCount, onSmartDownloadsCountChange) = rememberPreference(SmartDownloadsCountKey, defaultValue = 0)
    val (smartDownloadsWifiOnly, onSmartDownloadsWifiOnlyChange) = rememberPreference(SmartDownloadsWifiOnlyKey, defaultValue = true)
    val context = LocalContext.current
    val outputDevices = rememberConnectedAudioOutputs()
    var showAudioOutputDialog by remember { mutableStateOf(false) }
    var showSmartDownloadsDialog by remember { mutableStateOf(false) }

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

    if (showSmartDownloadsDialog) {
        EnumDialog(
            onDismiss = { showSmartDownloadsDialog = false },
            onSelect = { count ->
                onSmartDownloadsCountChange(count)
                SmartDownloads.schedule(context, count, smartDownloadsWifiOnly)
                showSmartDownloadsDialog = false
            },
            title = stringResource(R.string.baaja_pb_smart_dialog_title),
            current = smartDownloadsCount,
            values = SmartDownloadsCounts,
            valueText = { count ->
                if (count == 0) stringResource(R.string.baaja_pb_smart_off) else stringResource(R.string.baaja_pb_smart_option, count)
            },
        )
    }

    var showAudioQualityDialog by remember {
        mutableStateOf(false)
    }
    var showMobileQualityDialog by remember { mutableStateOf(false) }
    var showDownloadQualityDialog by remember { mutableStateOf(false) }

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

    var showLoudnessLevelDialog by remember {
        mutableStateOf(false)
    }

    if (showAudioQualityDialog) {
        EnumDialog(
            onDismiss = { showAudioQualityDialog = false },
            onSelect = {
                onAudioQualityChange(it)
                showAudioQualityDialog = false
            },
            title = stringResource(R.string.audio_quality),
            current = audioQuality,
            values = AudioQuality.values().toList(),
            valueText = {
                when (it) {
                    AudioQuality.AUTO -> stringResource(R.string.audio_quality_auto)
                    AudioQuality.HIGH -> stringResource(R.string.audio_quality_high)
                    AudioQuality.LOW -> stringResource(R.string.audio_quality_low)
                }
            }
        )
    }

    if (showLoudnessLevelDialog) {
        EnumDialog(
            onDismiss = { showLoudnessLevelDialog = false },
            onSelect = {
                onLoudnessLevelChange(it)
                showLoudnessLevelDialog = false
            },
            title = stringResource(R.string.loudness_level),
            current = loudnessLevel,
            values = LoudnessLevel.values().toList(),
            valueText = { getLoudnessLevelLabel(it) }
        )
    }

    Column(
        Modifier
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                )
            )
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

        Material3SettingsGroup(
            title = stringResource(R.string.player),
            items = buildList {
                add(Material3SettingsItem(
                    icon = painterResource(R.drawable.graphic_eq),
                    title = { Text(stringResource(R.string.audio_quality)) },
                    description = {
                        Text(
                            when (audioQuality) {
                                AudioQuality.AUTO -> stringResource(R.string.audio_quality_auto)
                                AudioQuality.HIGH -> stringResource(R.string.audio_quality_high)
                                AudioQuality.LOW -> stringResource(R.string.audio_quality_low)
                            }
                        )
                    },
                    onClick = { showAudioQualityDialog = true }
                ))
                add(Material3SettingsItem(
                    icon = painterResource(R.drawable.graphic_eq),
                    title = { Text(stringResource(R.string.audio_quality_mobile)) },
                    description = {
                        Text(
                            if (mobileAudioQuality == AudioQuality.AUTO) stringResource(R.string.audio_quality_auto_save_data)
                            else audioQualityLabel(mobileAudioQuality)
                        )
                    },
                    onClick = { showMobileQualityDialog = true }
                ))
                add(Material3SettingsItem(
                    icon = painterResource(R.drawable.download),
                    title = { Text(stringResource(R.string.audio_quality_download)) },
                    description = { Text(audioQualityLabel(downloadAudioQuality)) },
                    onClick = { showDownloadQualityDialog = true }
                ))
                add(Material3SettingsItem(
                    icon = painterResource(R.drawable.linear_scale),
                    title = { Text(stringResource(R.string.crossfade)) },
                    description = { Text(stringResource(R.string.crossfade_desc)) },
                    trailingContent = {
                        Switch(
                            checked = crossfadeEnabled,
                            onCheckedChange = onCrossfadeEnabledChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (crossfadeEnabled) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onCrossfadeEnabledChange(!crossfadeEnabled) }
                ))
                if (crossfadeEnabled) {
                    add(Material3SettingsItem(
                        icon = painterResource(R.drawable.timer),
                        title = { Text(stringResource(R.string.crossfade_duration)) },
                        description = {
                            Column {
                                Text(pluralStringResource(R.plurals.seconds, crossfadeDuration.toInt(), crossfadeDuration.toInt()))
                                if (automixEnabled) {
                                    Text(stringResource(R.string.baaja_pb_crossfade_duration_automix))
                                }
                                Slider(
                                    value = crossfadeDuration,
                                    onValueChange = onCrossfadeDurationChange,
                                    valueRange = 1f..15f,
                                    steps = 14
                                )
                            }
                        }
                    ))
                    add(Material3SettingsItem(
                        icon = painterResource(R.drawable.album),
                        title = { Text(stringResource(R.string.crossfade_gapless)) },
                        description = { Text(stringResource(R.string.crossfade_gapless_desc)) },
                        trailingContent = {
                            Switch(
                                checked = crossfadeGapless,
                                onCheckedChange = onCrossfadeGaplessChange,
                                thumbContent = {
                                    Icon(
                                        painter = painterResource(
                                            id = if (crossfadeGapless) R.drawable.check else R.drawable.close
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        },
                        onClick = { onCrossfadeGaplessChange(!crossfadeGapless) }
                    ))
                }
                add(Material3SettingsItem(
                    icon = painterResource(R.drawable.swipe),
                    title = { Text(stringResource(R.string.baaja_pb_automix_title)) },
                    description = {
                        Text(
                            stringResource(
                                if (crossfadeEnabled) R.string.baaja_pb_automix_desc else R.string.baaja_fix2_automix_needs_crossfade,
                            ),
                        )
                    },
                    enabled = crossfadeEnabled,
                    trailingContent = {
                        Switch(
                            checked = automixEnabled,
                            enabled = crossfadeEnabled,
                            onCheckedChange = onAutomixEnabledChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (automixEnabled) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onAutomixEnabledChange(!automixEnabled) }
                ))
                add(Material3SettingsItem(
                    icon = painterResource(R.drawable.history),
                    title = { Text(stringResource(R.string.history_duration)) },
                    description = {
                        Column {
                            Text(historyDuration.roundToInt().toString())
                            Slider(
                                value = historyDuration,
                                onValueChange = onHistoryDurationChange,
                                valueRange = 1f..100f
                            )
                        }
                    }
                ))
                add(Material3SettingsItem(
                    icon = painterResource(R.drawable.info),
                    title = { Text(stringResource(R.string.show_audio_format_on_player)) },
                    description = { Text(stringResource(R.string.show_audio_format_on_player_desc)) },
                    trailingContent = {
                        Switch(
                            checked = showAudioFormat,
                            onCheckedChange = onShowAudioFormatChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (showAudioFormat) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onShowAudioFormatChange(!showAudioFormat) }
                ))
                add(Material3SettingsItem(
                    icon = painterResource(R.drawable.lyrics),
                    title = { Text(stringResource(R.string.baaja_player_show_lyric_preview)) },
                    description = { Text(stringResource(R.string.baaja_player_show_lyric_preview_desc)) },
                    trailingContent = {
                        Switch(
                            checked = showLyricPreview,
                            onCheckedChange = onShowLyricPreviewChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (showLyricPreview) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onShowLyricPreviewChange(!showLyricPreview) }
                ))
                add(Material3SettingsItem(
                    icon = painterResource(R.drawable.fullscreen),
                    title = { Text(stringResource(R.string.baaja_player_immersive_player)) },
                    description = { Text(stringResource(R.string.baaja_player_immersive_player_desc)) },
                    trailingContent = {
                        Switch(
                            checked = immersivePlayer,
                            onCheckedChange = onImmersivePlayerChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (immersivePlayer) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onImmersivePlayerChange(!immersivePlayer) }
                ))
                add(Material3SettingsItem(
                    icon = painterResource(R.drawable.skip_next),
                    title = { Text(stringResource(R.string.sponsorblock)) },
                    description = { Text(stringResource(R.string.sponsorblock_desc)) },
                    trailingContent = {
                        Switch(
                            checked = sponsorBlockEnabled,
                            onCheckedChange = onSponsorBlockEnabledChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (sponsorBlockEnabled) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onSponsorBlockEnabledChange(!sponsorBlockEnabled) }
                ))
                add(Material3SettingsItem(
                    icon = painterResource(R.drawable.fast_forward),
                    title = { Text(stringResource(R.string.skip_silence)) },
                    description = { Text(stringResource(R.string.skip_silence_desc)) },
                    trailingContent = {
                        Switch(
                            checked = skipSilence,
                            onCheckedChange = onSkipSilenceChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (skipSilence) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onSkipSilenceChange(!skipSilence) }
                ))
                add(Material3SettingsItem(
                    icon = painterResource(R.drawable.skip_next),
                    title = { Text(stringResource(R.string.skip_silence_instant)) },
                    description = { Text(stringResource(R.string.skip_silence_instant_desc)) },
                    trailingContent = {
                        Switch(
                            checked = skipSilenceInstant,
                            onCheckedChange = { onSkipSilenceInstantChange(it) },
                            enabled = skipSilence,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (skipSilenceInstant) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { if (skipSilence) onSkipSilenceInstantChange(!skipSilenceInstant) }
                ))
                add(Material3SettingsItem(
                    icon = painterResource(R.drawable.volume_up),
                    title = { Text(stringResource(R.string.audio_normalization)) },
                    trailingContent = {
                        Switch(
                            checked = audioNormalization,
                            onCheckedChange = onAudioNormalizationChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (audioNormalization) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onAudioNormalizationChange(!audioNormalization) }
                ))
                if (audioNormalization) {
                    add(Material3SettingsItem(
                        icon = painterResource(R.drawable.volume_up),
                        title = { Text(stringResource(R.string.loudness_level)) },
                        description = {
                            Text(getLoudnessLevelLabel(loudnessLevel))
                        },
                        onClick = { showLoudnessLevelDialog = true }
                    ))
                }
                add(Material3SettingsItem(
                    icon = painterResource(R.drawable.graphic_eq),
                    title = { Text(stringResource(R.string.audio_offload)) },
                    description = {
                        Text(
                            if (crossfadeEnabled) stringResource(R.string.audio_offload_disabled_by_crossfade)
                            else stringResource(R.string.audio_offload_description)
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = if (crossfadeEnabled) false else audioOffload,
                            onCheckedChange = onAudioOffloadChange,
                            enabled = !crossfadeEnabled,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (!crossfadeEnabled && audioOffload) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { if (!crossfadeEnabled) onAudioOffloadChange(!audioOffload) }
                ))
                add(Material3SettingsItem(
                    icon = painterResource(R.drawable.graphic_eq),
                    title = { Text(stringResource(R.string.varispeed)) },
                    description = {
                        Text(
                            stringResource(R.string.varispeed_description)
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = varispeed,
                            onCheckedChange = onVarispeedChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (varispeed) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onVarispeedChange(!varispeed) }
                ))
                add(Material3SettingsItem(
                    icon = painterResource(R.drawable.speed),
                    title = { Text(stringResource(R.string.audio_track_playback_params)) },
                    description = {
                        Text(stringResource(R.string.audio_track_playback_params_description))
                    },
                    trailingContent = {
                        Switch(
                            checked = audioTrackPlaybackParams,
                            onCheckedChange = onAudioTrackPlaybackParamsChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (audioTrackPlaybackParams) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onAudioTrackPlaybackParamsChange(!audioTrackPlaybackParams) }
                ))
                // Only show Cast setting in GMS builds (not in F-Droid/FOSS)
                if (BuildConfig.CAST_AVAILABLE) {
                    add(Material3SettingsItem(
                        icon = painterResource(R.drawable.cast),
                        title = { Text(stringResource(R.string.google_cast)) },
                        description = { Text(stringResource(R.string.google_cast_description)) },
                        trailingContent = {
                            Switch(
                                checked = enableGoogleCast,
                                onCheckedChange = onEnableGoogleCastChange,
                                thumbContent = {
                                    Icon(
                                        painter = painterResource(
                                            id = if (enableGoogleCast) R.drawable.check else R.drawable.close
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        },
                        onClick = { onEnableGoogleCastChange(!enableGoogleCast) }
                    ))
                }
                add(Material3SettingsItem(
                    icon = painterResource(R.drawable.arrow_forward),
                    title = { Text(stringResource(R.string.seek_seconds_addup)) },
                    description = { Text(stringResource(R.string.seek_seconds_addup_description)) },
                    trailingContent = {
                        Switch(
                            checked = seekExtraSeconds,
                            onCheckedChange = onSeekExtraSeconds,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (seekExtraSeconds) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onSeekExtraSeconds(!seekExtraSeconds) }
                ))
            }
        )

        Spacer(modifier = Modifier.height(27.dp))

        var showSleepTimerDialog by remember { mutableStateOf(false) }

        val (sleepTimerEnabled, onSleepTimerEnabledChange) = rememberPreference(
            SleepTimerEnabledKey,
            defaultValue = false
        )
        val (sleepTimerRepeat, onSleepTimerRepeatChange) = rememberPreference(
            SleepTimerRepeatKey,
            defaultValue = "daily"
        )
        val (sleepTimerStartTime, onSleepTimerStartTimeChange) = rememberPreference(
            SleepTimerStartTimeKey,
            defaultValue = "22:00"
        )
        val (sleepTimerEndTime, onSleepTimerEndTimeChange) = rememberPreference(
            SleepTimerEndTimeKey,
            defaultValue = "06:00"
        )
        val (sleepTimerCustomDays, onSleepTimerCustomDaysChange) = rememberPreference(
            SleepTimerCustomDaysKey,
            defaultValue = "0,1,2,3,4"
        )
        // Per-day time ranges used in custom mode
        val (sleepTimerDayTimes, onSleepTimerDayTimesChange) = rememberPreference(
            SleepTimerDayTimesKey,
            defaultValue = ""
        )

        val (sleepTimerStopAfterCurrentSong, onSleepTimerStopAfterCurrentSongChange) = rememberPreference (
        SleepTimerStopAfterCurrentSongKey,
        defaultValue = false)
        val (sleepTimerFadeOut, onSleepTimerFadeOutChange) = rememberPreference(
            SleepTimerFadeOutKey,
            false
        )

        if (showSleepTimerDialog) {
            val customDays = sleepTimerCustomDays.split(",").mapNotNull { it.toIntOrNull() }
            val dayTimesMap = decodeDayTimes(sleepTimerDayTimes)

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
                initialCustomDays = customDays,
                initialDayTimes = dayTimesMap
            )
        }

        Material3SettingsGroup(
            title = stringResource(R.string.baaja_pb_group_output),
            items = buildList {
                add(Material3SettingsItem(
                    icon = painterResource(R.drawable.volume_up),
                    title = { Text(stringResource(R.string.baaja_pb_output_title)) },
                    description = {
                        Text(
                            when {
                                audioOutputKey.isBlank() -> stringResource(R.string.baaja_pb_output_automatic)
                                outputDevices.any { it.key == audioOutputKey } -> audioOutputLabel(audioOutputKey, outputDevices)
                                else -> stringResource(R.string.baaja_pb_output_not_connected, audioOutputLabel(audioOutputKey, outputDevices))
                            }
                        )
                    },
                    onClick = { showAudioOutputDialog = true }
                ))
                add(Material3SettingsItem(
                    icon = painterResource(R.drawable.graphic_eq),
                    title = { Text(stringResource(R.string.baaja_pb_float_title)) },
                    description = { Text(stringResource(R.string.baaja_pb_float_desc)) },
                    trailingContent = {
                        Switch(
                            checked = floatOutput,
                            onCheckedChange = onFloatOutputChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (floatOutput) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onFloatOutputChange(!floatOutput) }
                ))
                add(Material3SettingsItem(
                    icon = painterResource(R.drawable.offline),
                    title = { Text(stringResource(R.string.baaja_pb_smart_title)) },
                    description = {
                        Text(
                            if (smartDownloadsCount <= 0) stringResource(R.string.baaja_pb_smart_off)
                            else stringResource(R.string.baaja_pb_smart_on, smartDownloadsCount)
                        )
                    },
                    onClick = { showSmartDownloadsDialog = true }
                ))
                if (smartDownloadsCount > 0) {
                    add(Material3SettingsItem(
                        icon = painterResource(R.drawable.wifi_proxy),
                        title = { Text(stringResource(R.string.baaja_pb_smart_wifi_title)) },
                        description = { Text(stringResource(R.string.baaja_pb_smart_wifi_desc)) },
                        trailingContent = {
                            Switch(
                                checked = smartDownloadsWifiOnly,
                                onCheckedChange = {
                                    onSmartDownloadsWifiOnlyChange(it)
                                    SmartDownloads.schedule(context, smartDownloadsCount, it, runNow = false)
                                },
                                thumbContent = {
                                    Icon(
                                        painter = painterResource(
                                            id = if (smartDownloadsWifiOnly) R.drawable.check else R.drawable.close
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        },
                        onClick = {
                            onSmartDownloadsWifiOnlyChange(!smartDownloadsWifiOnly)
                            SmartDownloads.schedule(context, smartDownloadsCount, !smartDownloadsWifiOnly, runNow = false)
                        }
                    ))
                }
            }
        )

        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.sound_shaping),
            items = buildList {
                add(soundShaperSliderItem(
                    icon = R.drawable.graphic_eq,
                    title = R.string.sound_width,
                    valueText = "${(soundWidth * 100).roundToInt()}%",
                    value = soundWidth,
                    range = 0f..2f,
                    steps = 19,
                    onValueChange = onSoundWidthChange,
                ))
                add(soundShaperSliderItem(
                    icon = R.drawable.tune,
                    title = R.string.sound_balance,
                    valueText = when {
                        soundBalance < -0.005f -> stringResource(R.string.sound_balance_left, (-soundBalance * 100).roundToInt())
                        soundBalance > 0.005f -> stringResource(R.string.sound_balance_right, (soundBalance * 100).roundToInt())
                        else -> stringResource(R.string.sound_balance_center)
                    },
                    value = soundBalance,
                    range = -1f..1f,
                    steps = 19,
                    onValueChange = onSoundBalanceChange,
                ))
                add(soundShaperSliderItem(
                    icon = R.drawable.equalizer,
                    title = R.string.sound_bass,
                    valueText = formatDb(soundBass),
                    value = soundBass,
                    range = -12f..12f,
                    steps = 23,
                    onValueChange = onSoundBassChange,
                ))
                add(soundShaperSliderItem(
                    icon = R.drawable.equalizer,
                    title = R.string.sound_treble,
                    valueText = formatDb(soundTreble),
                    value = soundTreble,
                    range = -12f..12f,
                    steps = 23,
                    onValueChange = onSoundTrebleChange,
                ))
                add(Material3SettingsItem(
                    icon = painterResource(R.drawable.close),
                    title = { Text(stringResource(R.string.sound_reset)) },
                    description = { Text(stringResource(R.string.sound_reset_desc)) },
                    enabled = soundWidth != 1f || soundBalance != 0f || soundBass != 0f || soundTreble != 0f,
                    onClick = {
                        onSoundWidthChange(1f)
                        onSoundBalanceChange(0f)
                        onSoundBassChange(0f)
                        onSoundTrebleChange(0f)
                    }
                ))
            }
        )

        Material3SettingsGroup(
            title = stringResource(R.string.sleep_timer),
            items = buildList {
                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.time_auto),
                        title = { Text(stringResource(R.string.enable_automatic_sleeptimer)) },
                        description = { Text(stringResource(R.string.sleeptimer_description)) },
                        trailingContent = {
                            Switch(
                                checked = sleepTimerEnabled,
                                onCheckedChange = onSleepTimerEnabledChange,
                                thumbContent = {
                                    Icon(
                                        painter = painterResource(
                                            id = if (sleepTimerEnabled) R.drawable.check else R.drawable.close
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        },
                        onClick = { onSleepTimerEnabledChange(!sleepTimerEnabled) }
                    )
                )

                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.baseline_event_repeat_24),
                            title = { Text(stringResource(R.string.sleep_timer_repeat)) },
                            description = {
                                Text(
                                    stringResource(R.string.sleep_timer_repeat_description)
                                )
                            },
                            trailingContent = {
                                Switch(
                                    checked = sleepTimerEnabled,
                                    onCheckedChange = {showSleepTimerDialog = true},
                                    thumbContent = {
                                        Icon(
                                            painter = painterResource(
                                                id = if (sleepTimerEnabled) R.drawable.check else R.drawable.close
                                            ),
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize)
                                        )
                                    }
                                )
                            },
                            onClick = { showSleepTimerDialog = true }
                        )
                    )


                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.more_time),
                        title = { Text(stringResource(R.string.sleep_timer_stop_after_current_song_title)) },
                        description = { Text(stringResource(R.string.sleep_timer_stop_after_current_song_description)) },
                        trailingContent = {
                            Switch(
                                checked = sleepTimerStopAfterCurrentSong,
                                onCheckedChange = onSleepTimerStopAfterCurrentSongChange,
                                thumbContent = {
                                    Icon(
                                        painter = painterResource(
                                            id = if (sleepTimerStopAfterCurrentSong) R.drawable.check else R.drawable.close
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        },
                        onClick = { onSleepTimerStopAfterCurrentSongChange(!sleepTimerStopAfterCurrentSong) }
                    )
                )

                add(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.timer_arrow_down),
                        title = { Text(stringResource(R.string.sleep_timer_fade_out_title)) },
                        description = { Text(stringResource(R.string.sleep_timer_fade_out_description)) },
                        trailingContent = {
                            Switch(
                                checked = sleepTimerFadeOut,
                                onCheckedChange = onSleepTimerFadeOutChange,
                                thumbContent = {
                                    Icon(
                                        painter = painterResource(
                                            id = if (sleepTimerFadeOut) R.drawable.check else R.drawable.close
                                        ),
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            )
                        },
                        onClick = { onSleepTimerFadeOutChange(!sleepTimerFadeOut) }
                    )
                )

            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        AlarmSettingsSection()

        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.queue),
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.queue_music),
                    title = { Text(stringResource(R.string.persistent_queue)) },
                    description = { Text(stringResource(R.string.persistent_queue_desc)) },
                    trailingContent = {
                        Switch(
                            checked = persistentQueue,
                            onCheckedChange = onPersistentQueueChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (persistentQueue) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onPersistentQueueChange(!persistentQueue) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.playlist_add),
                    title = { Text(stringResource(R.string.auto_load_more)) },
                    description = { Text(stringResource(R.string.auto_load_more_desc)) },
                    trailingContent = {
                        Switch(
                            checked = autoLoadMore,
                            onCheckedChange = onAutoLoadMoreChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (autoLoadMore) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onAutoLoadMoreChange(!autoLoadMore) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.radio),
                    title = { Text(stringResource(R.string.auto_radio_queue)) },
                    description = { Text(stringResource(R.string.auto_radio_queue_desc)) },
                    trailingContent = {
                        Switch(
                            checked = autoRadioQueue,
                            onCheckedChange = onAutoRadioQueueChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (autoRadioQueue) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onAutoRadioQueueChange(!autoRadioQueue) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.skip_next),
                    title = { Text(stringResource(R.string.autoplay)) },
                    description = { Text(stringResource(R.string.autoplay_desc)) },
                    trailingContent = {
                        Switch(
                            checked = autoplay,
                            onCheckedChange = onAutoplayChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (autoplay) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onAutoplayChange(!autoplay) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.repeat),
                    title = { Text(stringResource(R.string.disable_load_more_when_repeat_all)) },
                    description = { Text(stringResource(R.string.disable_load_more_when_repeat_all_desc)) },
                    trailingContent = {
                        Switch(
                            checked = disableLoadMoreWhenRepeatAll,
                            onCheckedChange = onDisableLoadMoreWhenRepeatAllChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (disableLoadMoreWhenRepeatAll) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onDisableLoadMoreWhenRepeatAllChange(!disableLoadMoreWhenRepeatAll) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.download),
                    title = { Text(stringResource(R.string.auto_download_on_like)) },
                    description = { Text(stringResource(R.string.auto_download_on_like_desc)) },
                    trailingContent = {
                        Switch(
                            checked = autoDownloadOnLike,
                            onCheckedChange = onAutoDownloadOnLikeChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (autoDownloadOnLike) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onAutoDownloadOnLikeChange(!autoDownloadOnLike) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.similar),
                    title = { Text(stringResource(R.string.enable_similar_content)) },
                    description = { Text(stringResource(R.string.similar_content_desc)) },
                    trailingContent = {
                        Switch(
                            checked = similarContentEnabled,
                            onCheckedChange = similarContentEnabledChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (similarContentEnabled) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { similarContentEnabledChange(!similarContentEnabled) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.shuffle),
                    title = { Text(stringResource(R.string.persistent_shuffle_title)) },
                    description = { Text(stringResource(R.string.persistent_shuffle_desc)) },
                    trailingContent = {
                        Switch(
                            checked = persistentShuffleAcrossQueues,
                            onCheckedChange = onPersistentShuffleAcrossQueuesChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (persistentShuffleAcrossQueues) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onPersistentShuffleAcrossQueuesChange(!persistentShuffleAcrossQueues) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.shuffle),
                    title = { Text(stringResource(R.string.remember_shuffle_and_repeat)) },
                    description = { Text(stringResource(R.string.remember_shuffle_and_repeat_desc)) },
                    trailingContent = {
                        Switch(
                            checked = rememberShuffleAndRepeat,
                            onCheckedChange = onRememberShuffleAndRepeatChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (rememberShuffleAndRepeat) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onRememberShuffleAndRepeatChange(!rememberShuffleAndRepeat) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.shuffle),
                    title = { Text(stringResource(R.string.shuffle_playlist_first)) },
                    description = { Text(stringResource(R.string.shuffle_playlist_first_desc)) },
                    trailingContent = {
                        Switch(
                            checked = shufflePlaylistFirst,
                            onCheckedChange = onShufflePlaylistFirstChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (shufflePlaylistFirst) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onShufflePlaylistFirstChange(!shufflePlaylistFirst) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.queue_music),
                    title = { Text(stringResource(R.string.prevent_duplicate_tracks_in_queue)) },
                    description = { Text(stringResource(R.string.prevent_duplicate_tracks_in_queue_desc)) },
                    trailingContent = {
                        Switch(
                            checked = preventDuplicateTracksInQueue,
                            onCheckedChange = onPreventDuplicateTracksInQueueChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (preventDuplicateTracksInQueue) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onPreventDuplicateTracksInQueueChange(!preventDuplicateTracksInQueue) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.skip_next),
                    title = { Text(stringResource(R.string.auto_skip_next_on_error)) },
                    description = { Text(stringResource(R.string.auto_skip_next_on_error_desc)) },
                    trailingContent = {
                        Switch(
                            checked = autoSkipNextOnError,
                            onCheckedChange = onAutoSkipNextOnErrorChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (autoSkipNextOnError) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onAutoSkipNextOnErrorChange(!autoSkipNextOnError) }
                )
            )
        )

        Spacer(modifier = Modifier.height(27.dp))

        Material3SettingsGroup(
            title = stringResource(R.string.misc),
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(R.drawable.clear_all),
                    title = { Text(stringResource(R.string.stop_music_on_task_clear)) },
                    trailingContent = {
                        Switch(
                            checked = stopMusicOnTaskClear,
                            onCheckedChange = onStopMusicOnTaskClearChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (stopMusicOnTaskClear) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onStopMusicOnTaskClearChange(!stopMusicOnTaskClear) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.volume_off_pause),
                    title = { Text(stringResource(R.string.pause_music_when_media_is_muted)) },
                    trailingContent = {
                        Switch(
                            checked = pauseOnMute,
                            onCheckedChange = onPauseOnMuteChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (pauseOnMute) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onPauseOnMuteChange(!pauseOnMute) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.bluetooth),
                    title = { Text(stringResource(R.string.resume_on_bluetooth_connect)) },
                    trailingContent = {
                        Switch(
                            checked = resumeOnBluetoothConnect,
                            onCheckedChange = onResumeOnBluetoothConnectChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (resumeOnBluetoothConnect) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onResumeOnBluetoothConnectChange(!resumeOnBluetoothConnect) }
                ),
                Material3SettingsItem(
                    icon = painterResource(R.drawable.screenshot),
                    title = { Text(stringResource(R.string.keep_screen_on_when_player_is_expanded)) },
                    trailingContent = {
                        Switch(
                            checked = keepScreenOn,
                            onCheckedChange = onKeepScreenOnChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(
                                        id = if (keepScreenOn) R.drawable.check else R.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onKeepScreenOnChange(!keepScreenOn) }
                )
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.player_and_audio)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null
                )
            }
        }
    )
}

private fun soundShaperSliderItem(
    @DrawableRes icon: Int,
    @StringRes title: Int,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
) = Material3SettingsItem(
    leadingContent = { Icon(painterResource(icon), contentDescription = null) },
    title = { Text(stringResource(title)) },
    description = {
        Column {
            Text(valueText)
            Slider(
                value = value,
                onValueChange = { onValueChange((it * 10f).roundToInt() / 10f) },
                valueRange = range,
                steps = steps,
            )
        }
    },
)

private fun formatDb(value: Float): String =
    when {
        value > 0.05f -> "+${value.roundToInt()} dB"
        value < -0.05f -> "${value.roundToInt()} dB"
        else -> "0 dB"
    }

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
