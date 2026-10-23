/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.importer

import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem

/** Finds an imported track on YouTube Music with the same InnerTube song search the search screen uses. */
object YouTubeTrackMatcher {
    sealed interface Outcome {
        data class Found(val song: SongItem, val score: Double) : Outcome

        data object NotFound : Outcome

        /** The search itself failed (network etc.); the user can retry. */
        data object Failed : Outcome
    }

    suspend fun match(track: ImportTrack): Outcome {
        val queries =
            buildList {
                add(track.searchQuery(maxArtists = 2))
                if (track.artists.size > 1) add(track.searchQuery(maxArtists = 1))
                if (track.artists.isEmpty() && !track.album.isNullOrBlank()) add("${track.title} ${track.album}")
            }.distinct()

        var anySucceeded = false
        var best: Outcome.Found? = null
        for (query in queries) {
            val result = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull() ?: continue
            anySucceeded = true
            val songs = result.items.filterIsInstance<SongItem>().distinctBy { it.id }.take(MAX_CANDIDATES)
            val pick =
                TrackMatchScoring.best(
                    track,
                    songs.map { song ->
                        MatchCandidate(
                            title = song.title,
                            artists = song.artists.map { it.name },
                            durationSec = song.duration,
                            isVideo = song.isVideoSong,
                        )
                    },
                )
            if (pick != null && (best == null || pick.second > best.score)) {
                best = Outcome.Found(songs[pick.first], pick.second)
            }
            if ((best?.score ?: 0.0) >= GOOD_ENOUGH) break
        }
        return best ?: if (anySucceeded) Outcome.NotFound else Outcome.Failed
    }

    private const val MAX_CANDIDATES = 8
    private const val GOOD_ENOUGH = 0.85
}
