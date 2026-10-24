package com.metrolist.music.playback.audio

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundShaperSettingsTest {
    @Test
    fun soundShaperIsNeutralOnlyAtDefaults() {
        assertTrue(SoundShaperSettings().isNeutral)
        assertFalse(SoundShaperSettings(width = 1.5f).isNeutral)
        assertFalse(SoundShaperSettings(balance = -0.3f).isNeutral)
        assertFalse(SoundShaperSettings(bassDb = 3f).isNeutral)
        assertFalse(SoundShaperSettings(trebleDb = -2f).isNeutral)
    }
}
