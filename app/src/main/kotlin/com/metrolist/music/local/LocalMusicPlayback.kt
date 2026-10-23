/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.local

import androidx.media3.common.MediaItem
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.extensions.toMediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

/**
 * Glue between on-device files and Baaja's player.
 *
 * Local songs are stored in the existing `song` table with `isLocal = 1` and their content URI as
 * the id (no schema change). That lets play history, play counts and stats work for them, while
 * `inLibrary` stays null so they do not show up in the YouTube-backed library lists.
 */
object LocalMusicPlayback {
    private val registered: MutableSet<String> = ConcurrentHashMap.newKeySet()

    // Outlives the screen so leaving right after tapping play doesn't cancel the insert.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun registerInBackground(
        database: MusicDatabase,
        songs: List<LocalAudio>,
        unknownArtist: String,
    ) {
        scope.launch { register(database, songs, unknownArtist) }
    }

    fun mediaItems(songs: List<LocalAudio>, unknownArtist: String): List<MediaItem> =
        songs.map { it.toMediaMetadata(unknownArtist).toMediaItem() }

    /** Makes sure every song about to be queued has a row in the database (insert-or-ignore). */
    suspend fun register(
        database: MusicDatabase,
        songs: List<LocalAudio>,
        unknownArtist: String,
    ) = withContext(Dispatchers.IO) {
        val fresh = songs.filter { it.uri !in registered }
        if (fresh.isEmpty()) return@withContext
        try {
            database.withTransaction {
                fresh.forEach { song ->
                    insert(song.toMediaMetadata(unknownArtist)) { entity -> entity.copy(isLocal = true) }
                }
            }
            fresh.forEach { registered += it.uri }
        } catch (e: Exception) {
            Timber.tag("LocalMusicPlayback").w(e, "Could not register local songs")
        }
    }
}
