package com.metrolist.music.recommend

import androidx.media3.common.MediaItem
import com.metrolist.music.extensions.metadata

/**
 * Cleans up songs that radio/autoplay is about to add to the queue:
 * - drops songs already in the queue, heard recently, or suggested recently
 * - drops other uploads of a song that's already queued (video, lyric video, re-upload)
 * - drops songs the listener keeps skipping and moves artists they skip to the back
 * - spreads artists out so the same artist doesn't play twice in a row
 */
object AutoplayRefiner {
    fun refine(
        candidates: List<MediaItem>,
        queue: List<MediaItem>,
        signals: ListenSignals?,
    ): List<MediaItem> {
        val strict = refine(candidates, queue, signals, avoidRecent = true)
        // A radio that only has recent songs left is better than an empty queue.
        val result =
            if (strict.size >= MIN_FRESH || candidates.isEmpty()) {
                strict
            } else {
                refine(candidates, queue, signals, avoidRecent = false)
            }
        signals?.markSuggested(result.map { it.mediaId })
        return result
    }

    private fun refine(
        candidates: List<MediaItem>,
        queue: List<MediaItem>,
        signals: ListenSignals?,
        avoidRecent: Boolean,
    ): List<MediaItem> {
        val queuedIds = queue.mapTo(HashSet()) { it.mediaId }
        val seenKeys = queue.mapNotNullTo(HashSet()) { identity(it) }
        val skippedArtists = signals?.skippedArtists().orEmpty()

        val kept = ArrayList<MediaItem>(candidates.size)
        for (item in candidates) {
            val id = item.mediaId
            if (id in queuedIds) continue
            if (signals != null && ((avoidRecent && signals.wasRecent(id)) || signals.isSkippedSong(id))) continue
            val key = identity(item)
            if (key != null && !seenKeys.add(key)) continue
            queuedIds += id
            kept += item
        }

        // Artists the listener keeps skipping go last rather than disappearing outright.
        val (liked, skipped) = kept.partition { artistOf(it) !in skippedArtists }
        return spreadArtists(liked, lastArtist = queue.lastOrNull()?.let(::artistOf)) + skipped
    }

    /** Greedy reorder so no artist plays twice in a row where the list allows it; otherwise keeps order. */
    fun spreadArtists(
        items: List<MediaItem>,
        lastArtist: String?,
    ): List<MediaItem> {
        val pending = items.toMutableList()
        val result = ArrayList<MediaItem>(items.size)
        var previous = lastArtist
        while (pending.isNotEmpty()) {
            val index = pending.indexOfFirst { artistOf(it) != previous }.takeIf { it >= 0 } ?: 0
            val next = pending.removeAt(index)
            result += next
            previous = artistOf(next)
        }
        return result
    }

    private fun identity(item: MediaItem): String? {
        val m = item.metadata ?: return null
        return SongIdentity.key(m.title, m.artists.firstOrNull()?.name)
    }

    internal fun artistOf(item: MediaItem): String? =
        item.metadata
            ?.artists
            ?.firstOrNull()
            ?.name
            ?.let(SongIdentity::mainArtist)

    private const val MIN_FRESH = 3
}
