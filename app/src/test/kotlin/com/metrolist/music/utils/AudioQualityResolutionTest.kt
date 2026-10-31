package com.metrolist.music.utils

import com.metrolist.innertubex.extraction.AudioQuality as Stream
import com.metrolist.music.constants.AudioQuality
import com.metrolist.music.constants.AudioQuality.AUTO
import com.metrolist.music.constants.AudioQuality.HIGH
import com.metrolist.music.constants.AudioQuality.LOW
import org.junit.Assert.assertEquals
import org.junit.Test

/** Which stream InnerTubeX is asked for, for every setting on every kind of network. */
class AudioQualityResolutionTest {
    private fun resolve(wifi: AudioQuality, mobile: AudioQuality, metered: Boolean) =
        InnerTubeXPlayer.resolveAudioQuality(wifi, mobile, metered)

    @Test
    fun `on wifi only the main setting counts`() {
        for (mobile in AudioQuality.entries) {
            assertEquals(Stream.HIGH, resolve(HIGH, mobile, metered = false))
            assertEquals(Stream.LOW, resolve(LOW, mobile, metered = false))
            assertEquals(Stream.AUTO, resolve(AUTO, mobile, metered = false))
        }
    }

    @Test
    fun `on mobile data an explicit mobile setting wins`() {
        for (wifi in AudioQuality.entries) {
            assertEquals(Stream.HIGH, resolve(wifi, HIGH, metered = true))
            assertEquals(Stream.LOW, resolve(wifi, LOW, metered = true))
        }
    }

    @Test
    fun `mobile auto follows an explicit main setting`() {
        // The 1.3.2 bug: High on mobile data quietly played the 48 kbps stream.
        assertEquals(Stream.HIGH, resolve(HIGH, AUTO, metered = true))
        assertEquals(Stream.LOW, resolve(LOW, AUTO, metered = true))
    }

    @Test
    fun `auto everywhere saves data on mobile`() {
        assertEquals(Stream.LOW, resolve(AUTO, AUTO, metered = true))
    }
}
