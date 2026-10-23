/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.BuildConfig
import com.metrolist.music.R
import com.metrolist.music.constants.DarkModeKey
import com.metrolist.music.constants.PureBlackKey
import com.metrolist.music.ui.screens.settings.DarkMode
import com.metrolist.music.ui.theme.BaajaCard
import com.metrolist.music.ui.theme.BaajaLogo
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference

/** Trailing chevron for settings rows that open another screen. */
@Composable
fun SettingsChevron() {
    Icon(
        painterResource(R.drawable.navigate_next),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.size(22.dp),
    )
}

/** Yellow account card at the top of Settings; opens the profile hub. */
@Composable
fun BaajaSettingsProfileCard(onClick: () -> Unit) {
    val account = rememberBaajaAccount()
    val profile = rememberBaajaProfile()
    val name = profile.name
    BaajaCard(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        onClick = onClick,
    ) {
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BaajaAvatar(
                name = name,
                imageUrl = profile.avatarModel,
                size = 58.dp,
                shadowOffset = 0.dp,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    name,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (profile.bio.isNotBlank()) {
                        profile.bio
                    } else if (account.isLoggedIn) {
                        account.email.ifBlank { stringResource(R.string.baaja_onb_settings_account_signed_in) }
                    } else {
                        stringResource(R.string.baaja_onb_settings_account_guest)
                    },
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                painterResource(R.drawable.navigate_next),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/**
 * The most-used appearance switches inline (theme, album-art colours, pure black), bound to the same
 * preferences as the full Appearance screen, which stays one tap away.
 */
@Composable
fun BaajaQuickAppearanceCard(onMoreClick: () -> Unit) {
    var darkMode by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    var pureBlack by rememberPreference(PureBlackKey, defaultValue = false)
    val modes = listOf(DarkMode.AUTO, DarkMode.OFF, DarkMode.ON)

    BaajaCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                stringResource(R.string.baaja_onb_settings_quick_appearance),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.theme),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                BaajaSegmentedToggle(
                    options =
                        listOf(
                            stringResource(R.string.baaja_onb_settings_theme_system),
                            stringResource(R.string.baaja_onb_settings_theme_light),
                            stringResource(R.string.baaja_onb_settings_theme_dark),
                        ),
                    selectedIndex = modes.indexOf(darkMode).coerceAtLeast(0),
                    onSelect = { darkMode = modes[it] },
                )
            }
            QuickSwitchRow(
                title = stringResource(R.string.pure_black),
                subtitle = stringResource(R.string.baaja_onb_settings_pure_black_sub),
                checked = pureBlack,
                onCheckedChange = { pureBlack = it },
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Button, onClick = onMoreClick),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.baaja_onb_settings_more_appearance),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    painterResource(R.drawable.navigate_next),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun QuickSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(role = Role.Switch) { onCheckedChange(!checked) },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** Logo, version and credits at the bottom of Settings. */
@Composable
fun BaajaSettingsFooter() {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val logoDescription = stringResource(R.string.baaja_onb_logo_description)
        BaajaLogo(Modifier.semantics { contentDescription = logoDescription }, size = 56.dp)
        Text(
            stringResource(R.string.baaja_onb_settings_footer_version, BuildConfig.VERSION_NAME),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.baaja_onb_settings_footer_credit),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
