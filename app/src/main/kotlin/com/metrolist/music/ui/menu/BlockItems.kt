package com.metrolist.music.ui.menu

import android.widget.Toast
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.blocklist.Blocklist
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.ui.component.Material3MenuItemData
import kotlinx.coroutines.launch

/** "Never play this artist" and "Never play this song" entries for a song's menu. */
@Composable
fun rememberBlockItems(
    song: MediaMetadata,
    onDismiss: () -> Unit,
): List<Material3MenuItemData> {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val player = LocalPlayerConnection.current?.player

    fun skipIfPlaying() {
        player?.let { if (it.currentMediaItem?.mediaId == song.id && it.hasNextMediaItem()) it.seekToNext() }
    }

    val artist = song.artists.firstOrNull()
    return buildList {
        if (artist != null) {
            add(
                Material3MenuItemData(
                    title = { Text(stringResource(R.string.baaja_block_artist)) },
                    description = { Text(artist.name) },
                    icon = { Icon(painterResource(R.drawable.close), contentDescription = null) },
                    onClick = {
                        scope.launch { Blocklist.blockArtist(context, artist.id, artist.name) }
                        skipIfPlaying()
                        Toast.makeText(context, context.getString(R.string.baaja_block_artist_done, artist.name), Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                ),
            )
        }
        add(
            Material3MenuItemData(
                title = { Text(stringResource(R.string.baaja_block_song)) },
                description = { Text(stringResource(R.string.baaja_block_song_desc)) },
                icon = { Icon(painterResource(R.drawable.remove), contentDescription = null) },
                onClick = {
                    scope.launch { Blocklist.blockSong(context, song.id, song.title, artist?.name) }
                    skipIfPlaying()
                    Toast.makeText(context, R.string.baaja_block_song_done, Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
            ),
        )
    }
}
