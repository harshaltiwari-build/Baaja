/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.metrolist.music.BuildConfig
import com.metrolist.music.R
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.ui.screens.onboarding.SettingsChevron

/** Whether Android Auto is installed, so its settings are worth showing. */
@Composable
internal fun rememberHasAndroidAuto(): Boolean {
    val context = LocalContext.current
    return remember {
        runCatching { context.packageManager.getPackageInfo("com.google.android.projection.gearhead", 0) }.isSuccess
    }
}

@Composable
private fun linkRow(
    icon: Int,
    title: String,
    description: String? = null,
    onClick: () -> Unit,
) = Material3SettingsItem(
    icon = painterResource(icon),
    title = { Text(title) },
    description = description?.let { { Text(it) } },
    trailingContent = { SettingsChevron() },
    onClick = onClick,
)

/** "Devices": Device Connect, Listen Together and, when installed, Android Auto. */
@Composable
fun DevicesSettings(navController: NavController) {
    val hasAndroidAuto = rememberHasAndroidAuto()
    SettingsSubPage(stringResource(R.string.baaja_settings_devices_title), navController) {
        Material3SettingsGroup(
            items =
                buildList {
                    add(
                        linkRow(R.drawable.devices, stringResource(R.string.baaja_dc_title), stringResource(R.string.baaja_dc_desc)) {
                            navController.navigate("device_connect")
                        },
                    )
                    add(
                        linkRow(R.drawable.group, stringResource(R.string.listen_together)) {
                            navController.navigate("settings/listen_together")
                        },
                    )
                    if (hasAndroidAuto) {
                        add(linkRow(R.drawable.ic_android_auto, stringResource(R.string.android_auto)) { navController.navigate("settings/android_auto") })
                    }
                },
        )
    }
}

/** "About Baaja": updates, the changelog, link handling and the about page. */
@Composable
fun AboutBaajaSettings(navController: NavController) {
    val context = LocalContext.current
    var showChangelog by rememberSaveable { mutableStateOf(false) }
    if (showChangelog) ChangelogScreen(onDismiss = { showChangelog = false })

    SettingsSubPage(stringResource(R.string.baaja_settings_about), navController) {
        Material3SettingsGroup(
            items =
                buildList {
                    if (BuildConfig.UPDATER_AVAILABLE) {
                        add(linkRow(R.drawable.update, stringResource(R.string.updater)) { navController.navigate("settings/updater") })
                    }
                    add(linkRow(R.drawable.history, stringResource(R.string.changelog)) { showChangelog = true })
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        add(linkRow(R.drawable.link, stringResource(R.string.default_links)) { openLinkSettings(context) })
                    }
                    add(linkRow(R.drawable.info, stringResource(R.string.about)) { navController.navigate("settings/about") })
                },
        )
    }
}

private fun openLinkSettings(context: Context) {
    try {
        context.startActivity(
            Intent(Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS, "package:${context.packageName}".toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    } catch (e: Exception) {
        Toast.makeText(context, R.string.open_app_settings_error, Toast.LENGTH_LONG).show()
    }
}
