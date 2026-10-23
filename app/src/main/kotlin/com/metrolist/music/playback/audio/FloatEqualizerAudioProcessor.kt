/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.metrolist.music.eq.audio.BiquadFilter
import com.metrolist.music.eq.data.ParametricEQ
import java.nio.ByteBuffer
import kotlin.math.pow

/**
 * The parametric EQ for the 32-bit float chain. Same biquads and preamp as the 16-bit
 * `CustomEqualizerAudioProcessor`, but samples stay in float so boosts are not clipped here.
 * The service feeds it the active profile; `null` means flat.
 */
@UnstableApi
class FloatEqualizerAudioProcessor : BaseAudioProcessor() {
    private var sampleRate = 0
    private var channelCount = 0
    private var profile: ParametricEQ? = null
    private var filters: List<BiquadFilter> = emptyList()
    private var preampGain = 1.0

    @Synchronized
    fun setProfile(newProfile: ParametricEQ?) {
        profile = newProfile
        rebuild()
    }

    private fun rebuild() {
        val current = profile
        if (current == null || sampleRate == 0) {
            filters = emptyList()
            preampGain = 1.0
            return
        }
        preampGain = 10.0.pow(current.preamp / 20.0)
        filters =
            current.bands
                .filter { it.enabled && it.frequency < sampleRate / 2.0 }
                .map { BiquadFilter(sampleRate, it.frequency, it.gain, it.q, it.filterType) }
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT || inputAudioFormat.channelCount > 2) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        synchronized(this) {
            sampleRate = inputAudioFormat.sampleRate
            channelCount = inputAudioFormat.channelCount
            rebuild()
        }
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return
        val output = replaceOutputBuffer(inputBuffer.remaining())
        synchronized(this) {
            val active = profile != null && filters.isNotEmpty()
            when {
                !active -> output.put(inputBuffer)
                channelCount == 2 -> {
                    repeat(inputBuffer.remaining() / 8) {
                        var left = inputBuffer.getFloat().toDouble()
                        var right = inputBuffer.getFloat().toDouble()
                        for (filter in filters) {
                            val (l, r) = filter.processStereo(left, right)
                            left = l
                            right = r
                        }
                        output.putFloat((left * preampGain).toFloat())
                        output.putFloat((right * preampGain).toFloat())
                    }
                }
                else -> {
                    repeat(inputBuffer.remaining() / 4) {
                        var sample = inputBuffer.getFloat().toDouble()
                        for (filter in filters) sample = filter.processSample(sample)
                        output.putFloat((sample * preampGain).toFloat())
                    }
                }
            }
        }
        output.flip()
    }

    override fun onFlush() {
        synchronized(this) { filters.forEach { it.reset() } }
    }

    override fun onReset() {
        synchronized(this) {
            sampleRate = 0
            channelCount = 0
            filters = emptyList()
        }
    }
}
