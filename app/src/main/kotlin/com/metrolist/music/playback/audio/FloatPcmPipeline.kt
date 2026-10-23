/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback.audio

import androidx.media3.common.C
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessorChain
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.DefaultAudioSink
import java.nio.ByteBuffer

/**
 * Widens 16-bit PCM to 32-bit float. Everything after it in the chain works in float, so gain
 * and EQ boosts keep their headroom instead of being clipped and re-quantised at every stage.
 */
@UnstableApi
class Pcm16ToFloatAudioProcessor : BaseAudioProcessor() {
    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        return AudioProcessor.AudioFormat(inputAudioFormat.sampleRate, inputAudioFormat.channelCount, C.ENCODING_PCM_FLOAT)
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return
        val samples = inputBuffer.remaining() / 2
        val output = replaceOutputBuffer(samples * 4)
        repeat(samples) { output.putFloat(inputBuffer.getShort() / 32768f) }
        output.flip()
    }
}

/**
 * Audio processor chain used when 32-bit float output is on.
 *
 * Media3's own float path (`DefaultAudioSink.setEnableFloatOutput`) skips every custom processor
 * when the decoder hands it float/high-res PCM, which would silently switch off normalization, the
 * equalizer, sound shaping, silence skipping and speed changes. Instead the sink stays in its
 * normal mode (so the decoder delivers 16-bit) and this chain converts to float itself:
 *
 * 16-bit: silence detector -> silence skipping -> speed (Sonic) -> to float ->
 * float: normalization -> equalizer -> sound shaper -> AudioTrack (ENCODING_PCM_FLOAT)
 *
 * Silence skipping is 16-bit only in Media3, so the steps that change timing stay before the
 * conversion; the steps that change level run in float afterwards.
 */
@UnstableApi
class FloatOutputAudioProcessorChain(
    silenceDetector: AudioProcessor,
    floatProcessors: List<AudioProcessor>,
) : AudioProcessorChain {
    private val base =
        DefaultAudioSink.DefaultAudioProcessorChain(
            arrayOf(silenceDetector),
            androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor(2_000_000, 20_000, 256),
            androidx.media3.common.audio.SonicAudioProcessor(),
        )
    private val processors: Array<AudioProcessor> =
        base.audioProcessors + Pcm16ToFloatAudioProcessor() + floatProcessors

    override fun getAudioProcessors(): Array<AudioProcessor> = processors

    override fun applyPlaybackParameters(playbackParameters: PlaybackParameters): PlaybackParameters =
        base.applyPlaybackParameters(playbackParameters)

    override fun applySkipSilenceEnabled(skipSilenceEnabled: Boolean): Boolean =
        base.applySkipSilenceEnabled(skipSilenceEnabled)

    override fun getMediaDuration(playoutDuration: Long): Long = base.getMediaDuration(playoutDuration)

    override fun getSkippedOutputFrameCount(): Long = base.skippedOutputFrameCount
}
