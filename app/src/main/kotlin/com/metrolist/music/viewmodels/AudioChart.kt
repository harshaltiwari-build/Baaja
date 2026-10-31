package com.metrolist.music.viewmodels

import com.metrolist.innertube.models.SongItem
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * YouTube's India charts are lists of music videos. Baaja is music only, so like YouTube Music's own
 * "Top 100 Songs India" each entry is swapped for its official audio song, found with a song search.
 * An entry is kept only when the match is clear; it is never shown as a video and never guessed.
 */
internal suspend fun audioChart(
    entries: List<SongItem>,
    parallelism: Int = 6,
    searchSongs: suspend (query: String) -> List<SongItem>,
): List<SongItem> =
    coroutineScope {
        val permits = Semaphore(parallelism)
        entries
            .map { entry ->
                async {
                    if (!entry.isVideoSong) return@async entry
                    permits.withPermit {
                        runCatching { searchSongs("${entry.title} ${entry.artists.firstOrNull()?.name.orEmpty()}".trim()) }
                            .getOrNull()
                            ?.let { audioSongFor(entry, it) }
                    }
                }
            }.awaitAll()
            .filterNotNull()
            .distinctBy { it.id }
    }

/**
 * The official audio song for a chart [entry] among search [candidates], or null. Titles must agree (the song's
 * title may add "(From "Film")"), and an artist must be shared, except for fan or TV channel uploads whose
 * channel isn't the artist: then the top song with the same title is taken, as YouTube Music does.
 */
internal fun audioSongFor(
    entry: SongItem,
    candidates: List<SongItem>,
): SongItem? {
    if (!entry.isVideoSong) return entry
    val title = normalizedSongTitle(entry.title)
    val artists = entry.artists.map { normalizedArtist(it.name) }.toSet()
    val songs = candidates.filter { !it.isVideoSong && titlesAgree(title, normalizedSongTitle(it.title)) }
    return songs.firstOrNull { song -> song.artists.any { normalizedArtist(it.name) in artists } }
        ?: songs.firstOrNull().takeIf { entry.musicVideoType == UGC_VIDEO }
}

private fun titlesAgree(
    a: String,
    b: String,
): Boolean =
    a.isNotEmpty() &&
        (a == b || (a.length >= MIN_PREFIX_CHARS && b.startsWith("$a ")) || (b.length >= MIN_PREFIX_CHARS && a.startsWith("$b ")))

/** "KALYANI (Remix) (Official Video) | Label" → "kalyani remix": video-only words and everything after them go. */
internal fun normalizedSongTitle(title: String): String {
    var t = title.substringBefore('|')
    t = VIDEO_DASH_SUFFIX.replace(t, "")
    VIDEO_BRACKET.find(t)?.let { t = t.substring(0, it.range.first) }
    return t
        .lowercase()
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()
}

private fun normalizedArtist(name: String) = name.lowercase().filter { it.isLetterOrDigit() }

private val VIDEO_WORDS = "video|lyric|lyrical|official|song|audio|visualizer|4k|hd|full"
private val VIDEO_BRACKET = Regex("""\s*[(\[][^)\]]*\b($VIDEO_WORDS)\b[^)\]]*[)\]]""", RegexOption.IGNORE_CASE)
private val VIDEO_DASH_SUFFIX = Regex("""\s+-\s+.*\b($VIDEO_WORDS)\b.*$""", RegexOption.IGNORE_CASE)
private const val UGC_VIDEO = "MUSIC_VIDEO_TYPE_UGC"
private const val MIN_PREFIX_CHARS = 4
