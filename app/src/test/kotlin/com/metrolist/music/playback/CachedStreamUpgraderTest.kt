package com.metrolist.music.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** When a cached song is dropped so it can be fetched again in better quality. */
class CachedStreamUpgraderTest {
    private val upgrader = CachedStreamUpgrader()

    @Test
    fun `a song cached from the 48 kbps data saving stream is upgraded`() {
        assertTrue(upgrader.shouldUpgrade("song", position = 0, storedItag = 139, prefersLowQuality = false))
    }

    @Test
    fun `every data saving format counts`() {
        for (itag in listOf(139, 249, 250, 599, 600)) {
            assertTrue("itag $itag", CachedStreamUpgrader().shouldUpgrade("song", 0, itag, prefersLowQuality = false))
        }
    }

    @Test
    fun `a best quality stream with a low bitrate is never refetched`() {
        // Attack finding: quiet tracks can have an Opus 251 stream under 100 kbps. Judging by bitrate
        // re-downloaded them on every play.
        for (itag in listOf(140, 141, 251, 774)) {
            assertFalse("itag $itag", upgrader.shouldUpgrade("song", 0, itag, prefersLowQuality = false))
        }
    }

    @Test
    fun `a song is upgraded at most once per session`() {
        // Attack finding: with repeat-one every loop starts at 0 and would throw the cache away again.
        assertTrue(upgrader.shouldUpgrade("song", 0, 139, prefersLowQuality = false))
        assertFalse(upgrader.shouldUpgrade("song", 0, 139, prefersLowQuality = false))
        assertTrue(upgrader.shouldUpgrade("other", 0, 139, prefersLowQuality = false))
    }

    @Test
    fun `nothing is dropped while saving data, mid-song, or without a stored format`() {
        assertFalse(upgrader.shouldUpgrade("a", 0, 139, prefersLowQuality = true))
        assertFalse(upgrader.shouldUpgrade("b", 512 * 1024L, 139, prefersLowQuality = false))
        assertFalse(upgrader.shouldUpgrade("c", 0, null, prefersLowQuality = false))
    }

    @Test
    fun `many songs checked at once each upgrade exactly once`() {
        val ids = (1..200).map { "song$it" }
        val results = java.util.concurrent.ConcurrentLinkedQueue<String>()
        val threads =
            (1..8).map {
                Thread { ids.forEach { id -> if (upgrader.shouldUpgrade(id, 0, 139, false)) results.add(id) } }
            }
        threads.forEach(Thread::start)
        threads.forEach(Thread::join)
        assertTrue(results.size == ids.size && results.toSet() == ids.toSet())
    }
}
