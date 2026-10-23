/*
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback

import android.media.AudioDeviceInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AudioOutputRoutingTest {
    private val speaker = AudioOutputDevice(2, AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, "", "Pixel 9")
    private val earpiece = AudioOutputDevice(1, AudioDeviceInfo.TYPE_BUILTIN_EARPIECE, "", "Pixel 9")
    private val budsA2dp = AudioOutputDevice(30, AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, "AA:BB:CC:DD:EE:FF", "Buds")
    private val budsSco = AudioOutputDevice(31, AudioDeviceInfo.TYPE_BLUETOOTH_SCO, "AA:BB:CC:DD:EE:FF", "Buds")
    private val dac = AudioOutputDevice(40, AudioDeviceInfo.TYPE_USB_HEADSET, "card=1;device=0", "DAC")
    private val dacAgain = dac.copy(id = 41)

    @Test
    fun `only music outputs are offered, once each`() {
        val result = selectableAudioOutputs(listOf(earpiece, budsSco, dac, budsA2dp, speaker, dacAgain))
        assertEquals(listOf(speaker, dac, budsA2dp), result)
    }

    @Test
    fun `saved key survives reconnects with a new device id`() {
        val saved = budsA2dp.key
        val reconnected = budsA2dp.copy(id = 77)
        assertEquals(reconnected, resolveAudioOutput(saved, listOf(speaker, reconnected)))
    }

    @Test
    fun `missing or blank choice means automatic`() {
        assertNull(resolveAudioOutput(budsA2dp.key, listOf(speaker)))
        assertNull(resolveAudioOutput("", listOf(speaker)))
        assertNull(resolveAudioOutput(null, listOf(speaker)))
    }

    @Test
    fun `a different device of the same model does not match`() {
        val otherBuds = budsA2dp.copy(address = "11:22:33:44:55:66")
        assertNull(resolveAudioOutput(budsA2dp.key, listOf(otherBuds)))
    }

    @Test
    fun `key keeps the product name for display`() {
        assertEquals("Buds", audioOutputKeyName(budsA2dp.key))
        assertEquals("", audioOutputKeyName("garbage"))
    }
}
