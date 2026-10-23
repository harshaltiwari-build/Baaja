/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.local

import androidx.compose.runtime.Immutable
import com.metrolist.music.models.MediaMetadata

/**
 * An audio file on the device, as reported by MediaStore.
 *
 * The [uri] (a `content://media/...` address) doubles as the song id everywhere in the player,
 * the queue and the database, so a restored queue still knows where the file lives.
 */
@Immutable
data class LocalAudio(
    val mediaStoreId: Long,
    val uri: String,
    val title: String,
    /** Empty when the file has no artist tag. */
    val artist: String,
    /** Empty when the file has no album tag. */
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    /** Seconds since the epoch, from MediaStore.DATE_ADDED. */
    val dateAddedSec: Long,
    val year: Int = 0,
    val trackNumber: Int = 0,
) {
    val artworkUri: String
        get() = albumArtUri(albumId)

    val durationSec: Int
        get() = (durationMs / 1000L).toInt()

    fun toMediaMetadata(unknownArtist: String): MediaMetadata =
        MediaMetadata(
            id = uri,
            title = title,
            artists = listOf(MediaMetadata.Artist(id = null, name = artist.ifBlank { unknownArtist })),
            duration = durationSec,
            thumbnailUrl = artworkUri,
            // No album link: Baaja's album screen only knows YouTube Music albums.
            album = null,
        )

    companion object {
        const val AUDIO_CONTENT_PREFIX = "content://media/external/audio/media/"
        private const val ALBUM_ART_PREFIX = "content://media/external/audio/albumart/"

        fun contentUri(mediaStoreId: Long) = AUDIO_CONTENT_PREFIX + mediaStoreId

        fun albumArtUri(albumId: Long) = ALBUM_ART_PREFIX + albumId
    }
}

/** True for ids/URIs that point at a file on the device rather than a YouTube video id. */
fun isLocalMediaId(id: String?): Boolean =
    id != null && (id.startsWith("content://") || id.startsWith("file://"))
