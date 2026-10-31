package com.metrolist.music.ui.menu

import android.widget.Toast
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.utils.parseCookieString
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.ui.component.Material3MenuItemData
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.utils.reportException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * YouTube Music's "Not interested": a thumbs down on the signed-in account, which its Home and mixes
 * learn from, and a skip if the song is playing. Null when signed out, as there is no account to teach.
 */
@Composable
fun rememberNotInterestedItem(
    videoId: String,
    onDismiss: () -> Unit,
): Material3MenuItemData? {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current
    val cookie by rememberPreference(InnerTubeCookieKey, "")
    if ("SAPISID" !in parseCookieString(cookie)) return null
    return Material3MenuItemData(
        title = { Text(stringResource(R.string.baaja_not_interested)) },
        description = { Text(stringResource(R.string.baaja_not_interested_desc)) },
        icon = { Icon(painterResource(R.drawable.thumb_down), contentDescription = null) },
        onClick = {
            CoroutineScope(Dispatchers.IO).launch {
                YouTube.dislikeVideo(videoId).onFailure(::reportException)
            }
            playerConnection?.player?.let { player ->
                if (player.currentMediaItem?.mediaId == videoId && player.hasNextMediaItem()) player.seekToNext()
            }
            Toast.makeText(context, R.string.baaja_not_interested_done, Toast.LENGTH_SHORT).show()
            onDismiss()
        },
    )
}
