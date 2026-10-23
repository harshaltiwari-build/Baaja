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
import com.metrolist.music.eq.data.FilterType
import java.nio.ByteBuffer
import kotlin.math.abs

/**
 * User sound shaping applied after the equalizer.
 *
 * @property width stereo width: 0 = mono, 1 = unchanged, 2 = widest.
 * @property balance -1 = left only, 0 = centred, 1 = right only.
 * @property bassDb low-shelf gain around 110 Hz.
 * @property trebleDb high-shelf gain around 8 kHz.
 */
data class SoundShaperSettings(
    val width: Float = 1f,
    val balance: Float = 0f,
    val bassDb: Float = 0f,
    val trebleDb: Float = 0f,
) {
    val isNeutral: Boolean
        get() = abs(width - 1f) < 0.01f && abs(balance) < 0.01f && abs(bassDb) < 0.05f && abs(trebleDb) < 0.05f
}

@UnstableApi
class SoundShaperAudioProcessor : BaseAudioProcessor() {
    private var sampleRate = 0
    private var channelCount = 0
    private var isFloat = false
    private var settings = SoundShaperSettings()
    private var bass: BiquadFilter? = null
    private var treble: BiquadFilter? = null

    @Synchronized
    fun update(newSettings: SoundShaperSettings) {
        val shelvesChanged = newSettings.bassDb != settings.bassDb || newSettings.trebleDb != settings.trebleDb
        settings = newSettings
        if (shelvesChanged) rebuildShelves()
    }

    private fun rebuildShelves() {
        if (sampleRate == 0) return
        bass = settings.bassDb.takeIf { abs(it) >= 0.05f }?.let {
            BiquadFilter(sampleRate, BASS_FREQUENCY_HZ, it.toDouble(), SHELF_Q, FilterType.LSC)
        }
        treble = settings.trebleDb.takeIf { abs(it) >= 0.05f && TREBLE_FREQUENCY_HZ < sampleRate / 2.0 }?.let {
            BiquadFilter(sampleRate, TREBLE_FREQUENCY_HZ, it.toDouble(), SHELF_Q, FilterType.HSC)
        }
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        val encoding = inputAudioFormat.encoding
        if ((encoding != C.ENCODING_PCM_16BIT && encoding != C.ENCODING_PCM_FLOAT) || inputAudioFormat.channelCount > 2) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        synchronized(this) {
            isFloat = encoding == C.ENCODING_PCM_FLOAT
            sampleRate = inputAudioFormat.sampleRate
            channelCount = inputAudioFormat.channelCount
            rebuildShelves()
        }
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return
        val output = replaceOutputBuffer(inputBuffer.remaining())
        synchronized(this) {
            if (settings.isNeutral) {
                output.put(inputBuffer)
            } else if (channelCount == 2) {
                processStereo(inputBuffer, output)
            } else {
                processMono(inputBuffer, output)
            }
        }
        output.flip()
    }

    private fun processStereo(input: ByteBuffer, output: ByteBuffer) {
        val width = settings.width.toDouble()
        val leftGain = if (settings.balance > 0f) 1.0 - settings.balance else 1.0
        val rightGain = if (settings.balance < 0f) 1.0 + settings.balance else 1.0
        val bassFilter = bass
        val trebleFilter = treble

        repeat(input.remaining() / (bytesPerSample * 2)) {
            var left = readSample(input)
            var right = readSample(input)
            bassFilter?.processStereo(left, right)?.let { (l, r) -> left = l; right = r }
            trebleFilter?.processStereo(left, right)?.let { (l, r) -> left = l; right = r }

            val mid = (left + right) * 0.5
            val side = (left - right) * 0.5 * width
            writeSample(output, (mid + side) * leftGain)
            writeSample(output, (mid - side) * rightGain)
        }
    }

    private fun processMono(input: ByteBuffer, output: ByteBuffer) {
        val bassFilter = bass
        val trebleFilter = treble
        repeat(input.remaining() / bytesPerSample) {
            var sample = readSample(input)
            bassFilter?.let { sample = it.processSample(sample) }
            trebleFilter?.let { sample = it.processSample(sample) }
            writeSample(output, sample)
        }
    }

    override fun onFlush() {
        synchronized(this) {
            bass?.reset()
            treble?.reset()
        }
    }

    override fun onReset() {
        synchronized(this) {
            sampleRate = 0
            channelCount = 0
            isFloat = false
            bass = null
            treble = null
        }
    }

    private val bytesPerSample: Int get() = if (isFloat) 4 else 2

    private fun readSample(buffer: ByteBuffer): Double =
        if (isFloat) buffer.getFloat().toDouble() else buffer.getShort() / 32768.0

    // In float mode peaks above full scale pass through; Android clamps them only at the mixer output.
    private fun writeSample(buffer: ByteBuffer, value: Double) {
        if (isFloat) buffer.putFloat(value.toFloat()) else buffer.putShort(toPcm(value))
    }

    private fun toPcm(value: Double): Short = (value * 32768.0).coerceIn(-32768.0, 32767.0).toInt().toShort()

    private companion object {
        const val BASS_FREQUENCY_HZ = 110.0
        const val TREBLE_FREQUENCY_HZ = 8000.0
        const val SHELF_Q = 0.71
    }
}
