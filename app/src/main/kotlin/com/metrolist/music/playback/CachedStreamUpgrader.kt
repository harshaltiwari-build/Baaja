package com.metrolist.music.playback

import java.util.concurrent.ConcurrentHashMap

/**
 * Decides when a song cached from a data-saving stream is dropped so it can be fetched again in
 * better quality. Judged by format (itag), not bitrate: some songs' best stream is under 100 kbps and
 * would otherwise be re-downloaded on every play. Each song is tried once per session, so repeat-one
 * loops and seeks back to the start never throw the cache away again.
 */
internal class CachedStreamUpgrader {
    private val tried = ConcurrentHashMap.newKeySet<String>()

    fun shouldUpgrade(
        mediaId: String,
        position: Long,
        storedItag: Int?,
        prefersLowQuality: Boolean,
    ): Boolean {
        if (position != 0L || prefersLowQuality || storedItag !in DATA_SAVING_ITAGS) return false
        return tried.add(mediaId)
    }

    private companion object {
        /** YouTube's smallest audio formats: AAC 48 kbps, Opus 50/70 kbps and the ultra-low 30 kbps pair. */
        val DATA_SAVING_ITAGS = setOf(139, 249, 250, 599, 600)
    }
}
