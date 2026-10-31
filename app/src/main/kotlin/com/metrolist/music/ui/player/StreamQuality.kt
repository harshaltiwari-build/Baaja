package com.metrolist.music.ui.player

import androidx.annotation.OptIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import com.metrolist.music.R
import java.util.Locale

/** What the player's quality label says about the audio that is playing. */
internal sealed interface StreamQuality {
    data class Bitrate(val codec: String, val kbps: Int) : StreamQuality

    data class Lossless(val codec: String, val bits: Int, val khz: String) : StreamQuality

    data class Rate(val codec: String, val khz: String) : StreamQuality

    data class Codec(val codec: String) : StreamQuality
}

/** A YouTube stream from its stored format row, e.g. OPUS · 148 kbps. Null without a bitrate. */
internal fun streamQuality(
    codecs: String,
    mimeType: String,
    bitrate: Int,
): StreamQuality? {
    if (bitrate <= 0) return null
    val codec =
        when {
            codecs.contains("opus", ignoreCase = true) -> "OPUS"
            codecs.startsWith("mp4a", ignoreCase = true) -> "AAC"
            codecs.contains("flac", ignoreCase = true) -> "FLAC"
            else -> codecName(mimeType) ?: return null
        }
    return StreamQuality.Bitrate(codec, bitrate / 1000)
}

/** A file on the phone from what its decoder reports: bit depth and kHz when lossless, else kbps. */
@OptIn(UnstableApi::class)
internal fun fileQuality(format: Format): StreamQuality? {
    val codec =
        when (format.sampleMimeType) {
            MimeTypes.AUDIO_FLAC -> "FLAC"
            MimeTypes.AUDIO_ALAC -> "ALAC"
            MimeTypes.AUDIO_RAW, MimeTypes.AUDIO_WAV -> "WAV"
            MimeTypes.AUDIO_MPEG -> "MP3"
            MimeTypes.AUDIO_AAC -> "AAC"
            MimeTypes.AUDIO_OPUS -> "OPUS"
            MimeTypes.AUDIO_VORBIS -> "VORBIS"
            else -> codecName(format.sampleMimeType) ?: return null
        }
    val bits =
        when (format.pcmEncoding) {
            C.ENCODING_PCM_16BIT -> 16
            C.ENCODING_PCM_24BIT -> 24
            C.ENCODING_PCM_32BIT, C.ENCODING_PCM_FLOAT -> 32
            else -> null
        }
    val khz =
        format.sampleRate.takeIf { it > 0 }?.let {
            if (it % 1000 == 0) (it / 1000).toString() else String.format(Locale.US, "%.1f", it / 1000f)
        }
    val bitrate = (if (format.averageBitrate > 0) format.averageBitrate else format.peakBitrate).takeIf { it > 0 }
    return when {
        bits != null && khz != null -> StreamQuality.Lossless(codec, bits, khz)
        bitrate != null -> StreamQuality.Bitrate(codec, bitrate / 1000)
        khz != null -> StreamQuality.Rate(codec, khz)
        else -> StreamQuality.Codec(codec)
    }
}

/** "audio/x-ape" → "X-APE". A file can claim any MIME type, so only a short, plain name is kept. */
private fun codecName(mimeType: String?): String? =
    mimeType
        ?.substringAfter('/')
        ?.filter { it.isLetterOrDigit() || it == '-' }
        ?.uppercase(Locale.US)
        ?.take(MAX_CODEC_CHARS)
        ?.ifEmpty { null }

private const val MAX_CODEC_CHARS = 12

@Composable
internal fun StreamQualityText(
    quality: StreamQuality?,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val text =
        when (quality) {
            is StreamQuality.Bitrate -> stringResource(R.string.baaja_player_stream_quality, quality.codec, quality.kbps)
            is StreamQuality.Lossless -> stringResource(R.string.baaja_player_stream_lossless, quality.codec, quality.bits, quality.khz)
            is StreamQuality.Rate -> stringResource(R.string.baaja_player_stream_rate, quality.codec, quality.khz)
            is StreamQuality.Codec -> quality.codec
            null -> ""
        }
    Text(
        text = text,
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        style = TextStyle(fontFeatureSettings = "tnum"),
        maxLines = 1,
        modifier = modifier,
    )
}
