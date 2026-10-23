/*
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartDownloadsPlanTest {
    private val ranked = listOf("a", "b", "c", "d", "e", "f")

    @Test
    fun `first run downloads the top N`() {
        val plan = planSmartDownloads(ranked, limit = 3, tracked = emptySet(), present = emptySet(), failed = emptySet(), dismissed = emptySet())
        assertEquals(listOf("a", "b", "c"), plan.toDownload)
        assertEquals(emptyList<String>(), plan.toRemove)
        assertEquals(setOf("a", "b", "c"), plan.tracked)
    }

    @Test
    fun `manual downloads are never removed or taken over`() {
        // "b" was downloaded by hand, "z" too and it isn't in the ranking at all.
        val plan = planSmartDownloads(ranked, limit = 3, tracked = emptySet(), present = setOf("b", "z"), failed = emptySet(), dismissed = emptySet())
        assertEquals(listOf("a", "c"), plan.toDownload)
        assertTrue(plan.toRemove.isEmpty())
        assertEquals(setOf("a", "c"), plan.tracked)
    }

    @Test
    fun `songs that fell out of the top N are removed only if we added them`() {
        val newRanking = listOf("d", "a", "e", "b", "c")
        val plan =
            planSmartDownloads(
                newRanking,
                limit = 2,
                tracked = setOf("a", "b", "c"),
                present = setOf("a", "b", "c", "x"),
                failed = emptySet(),
                dismissed = emptySet(),
            )
        assertEquals(listOf("d"), plan.toDownload)
        assertEquals(setOf("b", "c"), plan.toRemove.toSet())
        assertEquals(setOf("a", "d"), plan.tracked)
    }

    @Test
    fun `lowering N shrinks the set and turning it to zero removes everything we added`() {
        val plan = planSmartDownloads(ranked, limit = 0, tracked = setOf("a", "b"), present = setOf("a", "b", "c"), failed = emptySet(), dismissed = emptySet())
        assertTrue(plan.toDownload.isEmpty())
        assertEquals(setOf("a", "b"), plan.toRemove.toSet())
        assertTrue(plan.tracked.isEmpty())
    }

    @Test
    fun `a song the user deleted is not downloaded again and the next one fills its place`() {
        // "a" was ours but has no download any more: the user removed it.
        val plan = planSmartDownloads(ranked, limit = 3, tracked = setOf("a", "b", "c"), present = setOf("b", "c"), failed = emptySet(), dismissed = emptySet())
        assertEquals(listOf("d"), plan.toDownload)
        assertTrue(plan.toRemove.isEmpty())
        assertEquals(setOf("b", "c", "d"), plan.tracked)
        assertEquals(setOf("a"), plan.dismissed)
    }

    @Test
    fun `failed downloads of ours are retried, failed manual ones are left alone`() {
        val plan =
            planSmartDownloads(
                ranked,
                limit = 3,
                tracked = setOf("a"),
                present = setOf("a", "b", "c"),
                failed = setOf("a", "b"),
                dismissed = emptySet(),
            )
        assertEquals(listOf("a"), plan.toDownload)
        assertEquals(setOf("a"), plan.tracked)
    }

    @Test
    fun `dismissals are forgotten once a song leaves the ranking`() {
        val plan = planSmartDownloads(listOf("b", "c"), limit = 2, tracked = emptySet(), present = emptySet(), failed = emptySet(), dismissed = setOf("a", "b"))
        assertEquals(listOf("c"), plan.toDownload)
        assertEquals(setOf("b"), plan.dismissed)
    }

    @Test
    fun `duplicate ids in the ranking count once`() {
        val plan = planSmartDownloads(listOf("a", "a", "b"), limit = 2, tracked = emptySet(), present = emptySet(), failed = emptySet(), dismissed = emptySet())
        assertEquals(listOf("a", "b"), plan.toDownload)
    }
}
