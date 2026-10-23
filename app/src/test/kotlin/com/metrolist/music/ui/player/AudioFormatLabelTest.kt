package com.metrolist.music.ui.player

import com.metrolist.music.db.entities.FormatEntity
import com.metrolist.music.playback.audio.SoundShaperSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioFormatLabelTest {
    private fun format(mimeType: String, codecs: String, bitrate: Int) =
        FormatEntity(
            id = "id",
            itag = 251,
            mimeType = mimeType,
            codecs = codecs,
            bitrate = bitrate,
            sampleRate = 48_000,
            contentLength = 1,
            loudnessDb = null,
            playbackUrl = null,
        )

    @Test
    fun labelsCommonYouTubeMusicStreams() {
        assertEquals("OPUS · 160 kbps", format("audio/webm", "opus", 160_000).audioFormatLabel())
        assertEquals("AAC · 256 kbps", format("audio/mp4", "mp4a.40.2", 256_000).audioFormatLabel())
        assertEquals("WEBM", format("audio/webm", "", 0).audioFormatLabel())
        assertNull(format("", "", 0).audioFormatLabel())
    }

    @Test
    fun soundShaperIsNeutralOnlyAtDefaults() {
        assertTrue(SoundShaperSettings().isNeutral)
        assertFalse(SoundShaperSettings(width = 1.5f).isNeutral)
        assertFalse(SoundShaperSettings(balance = -0.3f).isNeutral)
        assertFalse(SoundShaperSettings(bassDb = 3f).isNeutral)
        assertFalse(SoundShaperSettings(trebleDb = -2f).isNeutral)
    }
}
