/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.importer

import androidx.compose.runtime.Immutable

/** A track read from a Spotify playlist or a CSV export, before it is matched on YouTube Music. */
@Immutable
data class ImportTrack(
    val title: String,
    val artists: List<String>,
    val album: String? = null,
    val durationMs: Long? = null,
) {
    val artistLine: String get() = artists.joinToString(", ")

    /** Search text sent to YouTube Music: the title plus (at most) the first two artists. */
    fun searchQuery(maxArtists: Int = 2): String =
        buildString {
            append(title.trim())
            artists.take(maxArtists).forEach { artist ->
                if (artist.isNotBlank()) append(' ').append(artist.trim())
            }
        }
}

/** A playlist read from Spotify or a CSV file. */
data class ImportedPlaylist(
    val name: String?,
    val tracks: List<ImportTrack>,
    /** Entries that were skipped because they were podcast episodes or had no title. */
    val skipped: Int = 0,
)
