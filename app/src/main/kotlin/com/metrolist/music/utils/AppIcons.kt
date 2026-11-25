/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.metrolist.music.MainActivity
import com.metrolist.music.R

/** Launcher icons, each an <activity-alias> named `Icon<alias>` in the manifest. Exactly one is enabled. */
enum class AppIcon(
    val alias: String,
    @StringRes val title: Int,
    val background: List<Color>,
    val logo: Color,
) {
    ORIGINAL("Original", R.string.baaja_app_icon_original, listOf(Color(0xFF885EFC), Color(0xFF6C3DFC)), Color.White),
    INK("Ink", R.string.baaja_app_icon_ink, listOf(Color(0xFF24212B), Color(0xFF121016)), Color.White),
    LIGHT("Light", R.string.baaja_app_icon_light, listOf(Color(0xFFFFFFFF), Color(0xFFF1ECFF)), Color(0xFF6C3DFC)),
    SUNSET("Sunset", R.string.baaja_app_icon_sunset, listOf(Color(0xFFFF9A4D), Color(0xFFFF4F7B)), Color.White),
    OCEAN("Ocean", R.string.baaja_app_icon_ocean, listOf(Color(0xFF2EC4B6), Color(0xFF3A86FF)), Color.White),
    ;

    @get:DrawableRes
    val logoRes: Int get() = R.mipmap.baaja_launcher_fg

    fun component(context: Context) = ComponentName(context.packageName, "${MainActivity::class.java.`package`!!.name}.Icon$alias")
}

fun Context.currentAppIcon(): AppIcon =
    AppIcon.entries.firstOrNull { icon ->
        when (packageManager.getComponentEnabledSetting(icon.component(this))) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> icon == AppIcon.ORIGINAL
            else -> false
        }
    } ?: AppIcon.ORIGINAL

/** Enables the chosen icon before disabling the others, so there is never a moment with no launcher entry. */
fun Context.setAppIcon(icon: AppIcon) {
    packageManager.setComponentEnabledSetting(
        icon.component(this),
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
        PackageManager.DONT_KILL_APP,
    )
    AppIcon.entries.filter { it != icon }.forEach { other ->
        packageManager.setComponentEnabledSetting(
            other.component(this),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )
    }
}
