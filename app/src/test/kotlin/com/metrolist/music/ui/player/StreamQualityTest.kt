package com.metrolist.music.ui.player

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** What the player's quality label says, from YouTube's stored format or the phone file's decoder format. */
class StreamQualityTest {
    // ---- YouTube streams (stored format row) ----

    @Test
    fun `youtube opus and aac streams show codec and kbps`() {
        assertEquals(StreamQuality.Bitrate("OPUS", 148), streamQuality(codecs = "opus", mimeType = "audio/webm", bitrate = 148_041))
        assertEquals(StreamQuality.Bitrate("AAC", 129), streamQuality(codecs = "mp4a.40.2", mimeType = "audio/mp4", bitrate = 129_500))
    }

    @Test
    fun `an unknown codec falls back to the container name`() {
        assertEquals(StreamQuality.Bitrate("WEBM", 64), streamQuality(codecs = "", mimeType = "audio/webm", bitrate = 64_000))
    }

    @Test
    fun `a stored format without a bitrate shows nothing`() {
        assertNull(streamQuality(codecs = "opus", mimeType = "audio/webm", bitrate = 0))
    }

    // ---- files on the phone (decoder format) ----

    @Test
    fun `lossless files show bit depth and sample rate`() {
        assertEquals(StreamQuality.Lossless("FLAC", 24, "96"), fileQuality(MimeTypes.AUDIO_FLAC, C.ENCODING_PCM_24BIT, 96_000))
        assertEquals(StreamQuality.Lossless("WAV", 16, "44.1"), fileQuality(MimeTypes.AUDIO_RAW, C.ENCODING_PCM_16BIT, 44_100))
        assertEquals(StreamQuality.Lossless("ALAC", 32, "192"), fileQuality(MimeTypes.AUDIO_ALAC, C.ENCODING_PCM_FLOAT, 192_000))
    }

    @Test
    fun `lossy files show kbps, preferring the average bitrate`() {
        assertEquals(StreamQuality.Bitrate("MP3", 192), fileQuality(MimeTypes.AUDIO_MPEG, Format.NO_VALUE, 44_100, averageBitrate = 192_000, bitrate = 320_000))
        assertEquals(StreamQuality.Bitrate("AAC", 256), fileQuality(MimeTypes.AUDIO_AAC, Format.NO_VALUE, 44_100, bitrate = 256_000))
    }

    @Test
    fun `without a bitrate the sample rate is shown, then just the codec`() {
        assertEquals(StreamQuality.Rate("VORBIS", "48"), fileQuality(MimeTypes.AUDIO_VORBIS, Format.NO_VALUE, 48_000))
        assertEquals(StreamQuality.Codec("OPUS"), fileQuality(MimeTypes.AUDIO_OPUS, Format.NO_VALUE, Format.NO_VALUE))
    }

    @Test
    fun `odd mime types from files are shortened and unknown ones show nothing`() {
        assertEquals(StreamQuality.Codec("X-APE"), fileQuality("audio/x-ape", Format.NO_VALUE, Format.NO_VALUE))
        assertNull(fileQuality(null, Format.NO_VALUE, Format.NO_VALUE))
    }

    @Test
    fun `a very long or strange mime type cannot flood the label`() {
        // Attack: a file's container can claim any MIME type.
        val label = fileQuality("audio/" + "A".repeat(500) + "\n<b>", Format.NO_VALUE, Format.NO_VALUE)
        assertEquals(StreamQuality.Codec("A".repeat(12)), label)
    }

    private fun fileQuality(
        mime: String?,
        pcmEncoding: Int,
        sampleRate: Int,
        averageBitrate: Int = Format.NO_VALUE,
        bitrate: Int = Format.NO_VALUE,
    ) = fileQuality(
        Format.Builder()
            .setSampleMimeType(mime)
            .setPcmEncoding(pcmEncoding)
            .setSampleRate(sampleRate)
            .setAverageBitrate(averageBitrate)
            .setPeakBitrate(bitrate)
            .build(),
    )
}
