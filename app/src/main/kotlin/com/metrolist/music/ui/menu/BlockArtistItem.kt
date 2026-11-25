/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.menu

import android.widget.Toast
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.ui.component.Material3MenuItemData
import com.metrolist.music.utils.blockArtist
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Blocks the song's main artist: hidden from Home and skipped in the queue. Restored from
 * Privacy settings. Null when the song has no artist with a YouTube channel id.
 */
@Composable
fun rememberBlockArtistItem(
    videoId: String,
    artistId: String?,
    artistName: String?,
    onDismiss: () -> Unit,
): Material3MenuItemData? {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current
    if (artistId.isNullOrBlank() || artistName.isNullOrBlank()) return null
    return Material3MenuItemData(
        title = { Text(stringResource(R.string.baaja_block_artist, artistName)) },
        description = { Text(stringResource(R.string.baaja_block_artist_desc)) },
        icon = { Icon(painterResource(R.drawable.block), contentDescription = null) },
        onClick = {
            val appContext = context.applicationContext
            CoroutineScope(Dispatchers.IO).launch { appContext.blockArtist(artistId, artistName) }
            playerConnection?.player?.let { player ->
                if (player.currentMediaItem?.mediaId == videoId && player.hasNextMediaItem()) player.seekToNext()
            }
            Toast.makeText(context, context.getString(R.string.baaja_block_artist_done, artistName), Toast.LENGTH_SHORT).show()
            onDismiss()
        },
    )
}
