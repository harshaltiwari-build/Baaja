/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.equalizer

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.metrolist.music.R
import com.metrolist.music.constants.SoundBalanceKey
import com.metrolist.music.constants.SoundBassKey
import com.metrolist.music.constants.SoundTrebleKey
import com.metrolist.music.constants.SoundWidthKey
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.utils.rememberPreference
import kotlin.math.roundToInt

/**
 * Stereo width, balance, bass and treble sliders shown above the equalizer profiles.
 * Reset also turns off [eqActive] through [onDisableEq], so one tap gives the untouched sound.
 */
@Composable
fun QuickToneSection(
    modifier: Modifier = Modifier,
    eqActive: Boolean = false,
    onDisableEq: () -> Unit = {},
) {
    val (soundWidth, onSoundWidthChange) = rememberPreference(SoundWidthKey, defaultValue = 1f)
    val (soundBalance, onSoundBalanceChange) = rememberPreference(SoundBalanceKey, defaultValue = 0f)
    val (soundBass, onSoundBassChange) = rememberPreference(SoundBassKey, defaultValue = 0f)
    val (soundTreble, onSoundTrebleChange) = rememberPreference(SoundTrebleKey, defaultValue = 0f)

    Column(modifier) {
        Material3SettingsGroup(
            title = stringResource(R.string.baaja_clean_quick_tone),
            items = buildList {
                add(quickToneSlider(
                    icon = R.drawable.graphic_eq,
                    title = R.string.sound_width,
                    valueText = "${(soundWidth * 100).roundToInt()}%",
                    value = soundWidth,
                    range = 0f..2f,
                    steps = 19,
                    onValueChange = onSoundWidthChange,
                ))
                add(quickToneSlider(
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
                add(quickToneSlider(
                    icon = R.drawable.equalizer,
                    title = R.string.sound_bass,
                    valueText = formatDb(soundBass),
                    value = soundBass,
                    range = -12f..12f,
                    steps = 23,
                    onValueChange = onSoundBassChange,
                ))
                add(quickToneSlider(
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
                    enabled = eqActive || soundWidth != 1f || soundBalance != 0f || soundBass != 0f || soundTreble != 0f,
                    onClick = {
                        if (eqActive) onDisableEq()
                        onSoundWidthChange(1f)
                        onSoundBalanceChange(0f)
                        onSoundBassChange(0f)
                        onSoundTrebleChange(0f)
                    },
                ))
            },
        )
    }
}

private fun quickToneSlider(
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
