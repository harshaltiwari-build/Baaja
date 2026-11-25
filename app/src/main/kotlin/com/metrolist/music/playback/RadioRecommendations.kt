/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback

import androidx.media3.common.MediaItem
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.random.Random

/**
 * Picks the next batch of radio songs the way Metrolist 14 does: it remembers what it recommended
 * lately and pages further into YouTube's radio to find songs the listener hasn't been offered yet.
 */
class RadioRecommendationHistory(
    private val capacity: Int = 500,
) {
    private val recentIds = LinkedHashSet<String>()

    @Synchronized
    fun remember(items: List<MediaItem>) {
        items.forEach { item ->
            recentIds.remove(item.mediaId)
            recentIds.add(item.mediaId)
        }
        while (recentIds.size > capacity) recentIds.remove(recentIds.first())
    }

    @Synchronized
    private fun snapshot(): Map<String, Int> = recentIds.withIndex().associate { it.value to it.index }

    /**
     * @param isEligible drops songs that must never be added (already queued, blocked, filtered out).
     * @param loadPage fetches the next page of the radio, or null once it has no more pages.
     */
    suspend fun select(
        isEligible: (MediaItem) -> Boolean,
        loadPage: suspend () -> List<MediaItem>?,
        random: Random = Random.Default,
    ): List<MediaItem> {
        val previous = snapshot()
        var fallback = emptyList<MediaItem>()
        var repeatedPages = 0
        for (pageIndex in 0 until MAX_PAGES) {
            currentCoroutineContext().ensureActive()
            val page = loadPage() ?: break
            val eligible = page.filter(isEligible).distinctBy { it.mediaId }
            val fresh = eligible.filterNot { it.mediaId in previous }
            if (fresh.isNotEmpty()) return fresh.shuffled(random).also(::remember)
            if (fallback.isEmpty()) fallback = eligible
            if (fallback.isNotEmpty() && ++repeatedPages == MAX_REPEATED_PAGES) break
        }
        // Nothing new turned up: replay the least recently offered songs first.
        return fallback.sortedBy { previous[it.mediaId] ?: -1 }.also(::remember)
    }

    private companion object {
        const val MAX_PAGES = 10
        const val MAX_REPEATED_PAGES = 3
    }
}
