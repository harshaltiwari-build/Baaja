/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.utils.parseCookieString
import com.metrolist.music.constants.AccountChannelHandleKey
import com.metrolist.music.constants.AccountEmailKey
import com.metrolist.music.constants.AccountNameKey
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.utils.rememberPreference

/** What Baaja's profile/settings headers show about the signed-in YouTube account. */
internal data class BaajaAccountState(
    val isLoggedIn: Boolean,
    val name: String,
    val email: String,
    val channelHandle: String,
    val imageUrl: String?,
)

/** The avatar URL is fetched once per process; signing in or out restarts the app, which resets it. */
private object BaajaAccountImageCache {
    var url: String? = null
}

/**
 * Account details from the same sources the rest of the app uses: the saved account preferences written at sign-in,
 * plus [YouTube.accountInfo] for the avatar.
 */
@Composable
internal fun rememberBaajaAccount(): BaajaAccountState {
    val cookie by rememberPreference(InnerTubeCookieKey, "")
    val name by rememberPreference(AccountNameKey, "")
    val email by rememberPreference(AccountEmailKey, "")
    val handle by rememberPreference(AccountChannelHandleKey, "")
    val isLoggedIn =
        remember(cookie) {
            "SAPISID" in runCatching { parseCookieString(cookie) }.getOrDefault(emptyMap())
        }
    var imageUrl by remember { mutableStateOf(BaajaAccountImageCache.url) }

    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) {
            imageUrl = null
            return@LaunchedEffect
        }
        if (BaajaAccountImageCache.url == null) {
            YouTube.accountInfo().onSuccess { info ->
                BaajaAccountImageCache.url = info.thumbnailUrl
                imageUrl = info.thumbnailUrl
            }
        } else {
            imageUrl = BaajaAccountImageCache.url
        }
    }

    return BaajaAccountState(
        isLoggedIn = isLoggedIn,
        name = name,
        email = email,
        channelHandle = handle,
        imageUrl = if (isLoggedIn) imageUrl else null,
    )
}
