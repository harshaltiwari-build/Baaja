package com.metrolist.music.playback

import androidx.media3.common.MediaItem
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/**
 * MusicService.onCreate returns early when Android denies the foreground-service start, yet the
 * service can still be bound. The UI then called playNext/addToQueue on a service whose player was
 * never created and crashed with a NullPointerException (Baaja 1.3.3 crash report, Android 15).
 */
@RunWith(RobolectricTestRunner::class)
class MusicServicePlayerGuardTest {
    /** A built-but-never-created service has no player, which is the state that crashed. */
    private fun serviceWithoutPlayer(): MusicService =
        Robolectric.buildService(MusicService::class.java).get()

    @Test
    fun `playNext does nothing when the player was never created`() {
        val service = serviceWithoutPlayer()
        assertFalse(service.isPlayerReady.value)

        service.playNext(listOf(MediaItem.fromUri("https://example.com/a.mp3")))
    }

    @Test
    fun `addToQueue does nothing when the player was never created`() {
        val service = serviceWithoutPlayer()
        assertFalse(service.isPlayerReady.value)

        service.addToQueue(listOf(MediaItem.fromUri("https://example.com/a.mp3")))
    }

    @Test
    fun `repeated calls with a large batch stay safe without a player`() {
        val service = serviceWithoutPlayer()
        val items = List(5_000) { MediaItem.fromUri("https://example.com/$it.mp3") }

        repeat(200) {
            service.playNext(items)
            service.addToQueue(items)
        }
    }
}
