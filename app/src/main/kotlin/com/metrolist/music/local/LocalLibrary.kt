/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.local

import androidx.compose.runtime.Immutable
import java.text.Collator
import java.text.Normalizer
import java.util.Locale

enum class LocalSort { TITLE, ARTIST, ALBUM, DATE_ADDED }

@Immutable
data class LocalAlbum(
    val albumId: Long,
    val title: String,
    val artist: String,
    val songs: List<LocalAudio>,
) {
    val key: String get() = "album:$albumId"
    val artworkUri: String get() = LocalAudio.albumArtUri(albumId)
}

@Immutable
data class LocalArtist(
    val name: String,
    val songs: List<LocalAudio>,
    val albumCount: Int,
) {
    val key: String get() = "artist:${name.lowercase(Locale.ROOT)}"

    /** Artwork of the artist's first album, used as a stand-in avatar. */
    val artworkUri: String? get() = songs.firstOrNull()?.artworkUri
}

/** Pure sorting / searching / grouping for the on-device library. */
object LocalLibrary {
    /** MediaStore clips shorter than this are treated as ringtones, notification sounds or voice snippets. */
    const val MIN_DURATION_MS = 30_000L

    private val collator: Collator =
        Collator.getInstance(Locale.getDefault()).apply { strength = Collator.PRIMARY }

    fun sort(songs: List<LocalAudio>, sort: LocalSort, descending: Boolean): List<LocalAudio> {
        val comparator: Comparator<LocalAudio> =
            when (sort) {
                LocalSort.TITLE -> compareBy(collator) { it.title }
                LocalSort.ARTIST ->
                    compareBy<LocalAudio> { it.artist.isBlank() }
                        .thenBy(collator) { it.artist }
                        .thenBy { it.album.isBlank() }
                        .thenBy(collator) { it.album }
                        .thenBy { it.trackNumber }
                        .thenBy(collator) { it.title }
                LocalSort.ALBUM ->
                    compareBy<LocalAudio> { it.album.isBlank() }
                        .thenBy(collator) { it.album }
                        .thenBy { it.trackNumber }
                        .thenBy(collator) { it.title }
                // Newest first is the natural reading of "date added", so ascending means newest first here.
                LocalSort.DATE_ADDED -> compareByDescending<LocalAudio> { it.dateAddedSec }.thenBy(collator) { it.title }
            }
        val sorted = songs.sortedWith(comparator)
        return if (descending) sorted.asReversed() else sorted
    }

    /** Case- and accent-insensitive search over title, artist and album; every word must match. */
    fun filter(songs: List<LocalAudio>, query: String): List<LocalAudio> {
        val words = fold(query).split(' ').filter { it.isNotBlank() }
        if (words.isEmpty()) return songs
        return songs.filter { song ->
            val haystack = fold("${song.title} ${song.artist} ${song.album}")
            words.all { haystack.contains(it) }
        }
    }

    fun albums(songs: List<LocalAudio>): List<LocalAlbum> =
        songs
            .groupBy { it.albumId }
            .map { (albumId, tracks) ->
                val ordered = tracks.sortedWith(compareBy<LocalAudio> { it.trackNumber }.thenBy(collator) { it.title })
                val artists = ordered.map { it.artist }.filter { it.isNotBlank() }.distinct()
                LocalAlbum(
                    albumId = albumId,
                    title = ordered.first().album,
                    artist = if (artists.size == 1) artists.first() else artists.joinToString(", ").take(80),
                    songs = ordered,
                )
            }.sortedWith(compareBy<LocalAlbum> { it.title.isBlank() }.thenBy(collator) { it.title })

    fun artists(songs: List<LocalAudio>): List<LocalArtist> =
        songs
            .groupBy { it.artist.trim().lowercase(Locale.ROOT) }
            .map { (_, tracks) ->
                val ordered =
                    tracks.sortedWith(
                        compareBy<LocalAudio, String>(collator) { it.album }
                            .thenBy { it.trackNumber }
                            .thenBy(collator) { it.title },
                    )
                LocalArtist(
                    name = ordered.first().artist.trim(),
                    songs = ordered,
                    albumCount = ordered.map { it.albumId }.distinct().size,
                )
            }.sortedWith(compareBy<LocalArtist> { it.name.isBlank() }.thenBy(collator) { it.name })

    private val COMBINING_ACCENTS = Regex("\\p{InCombiningDiacriticalMarks}+")

    /** Lowercases and strips accents so "Beyoncé" matches "beyonce". */
    fun fold(text: String): String =
        Normalizer
            .normalize(text, Normalizer.Form.NFD)
            .replace(COMBINING_ACCENTS, "")
            .lowercase(Locale.ROOT)
            .trim()

    /** MediaStore writes "<unknown>" for missing tags; Baaja shows its own label instead. */
    fun cleanTag(value: String?): String =
        value?.trim()?.takeUnless { it.isEmpty() || it.equals("<unknown>", ignoreCase = true) } ?: ""
}
