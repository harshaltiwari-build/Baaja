/*
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import com.metrolist.music.eq.data.ParametricEQ
import com.metrolist.music.eq.data.ParametricEQBand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

class FloatPipelineTest {
    private val pcm16 = AudioProcessor.AudioFormat(48_000, 2, C.ENCODING_PCM_16BIT)
    private val pcmFloat = AudioProcessor.AudioFormat(48_000, 2, C.ENCODING_PCM_FLOAT)

    private fun shorts(vararg values: Short): ByteBuffer =
        ByteBuffer.allocateDirect(values.size * 2).order(ByteOrder.nativeOrder()).apply {
            values.forEach { putShort(it) }
            flip()
        }

    private fun floats(vararg values: Float): ByteBuffer =
        ByteBuffer.allocateDirect(values.size * 4).order(ByteOrder.nativeOrder()).apply {
            values.forEach { putFloat(it) }
            flip()
        }

    private fun AudioProcessor.run(input: ByteBuffer): FloatArray {
        queueInput(input)
        val out = output
        return FloatArray(out.remaining() / 4) { out.getFloat() }
    }

    @Suppress("DEPRECATION")
    private fun AudioProcessor.prepare(format: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat =
        configure(format).also { flush() }

    @Test
    fun `16-bit samples widen to float without changing their value`() {
        val processor = Pcm16ToFloatAudioProcessor()
        assertEquals(C.ENCODING_PCM_FLOAT, processor.prepare(pcm16).encoding)
        val out = processor.run(shorts(0, 16384, -32768, 32767))
        assertEquals(0f, out[0], 0f)
        assertEquals(0.5f, out[1], 0f)
        assertEquals(-1f, out[2], 0f)
        assertEquals(32767f / 32768f, out[3], 0f)
    }

    @Test
    fun `float equalizer is transparent without a profile`() {
        val processor = FloatEqualizerAudioProcessor()
        processor.prepare(pcmFloat)
        val out = processor.run(floats(0.25f, -0.5f, 1.5f, -1.5f))
        assertEquals(listOf(0.25f, -0.5f, 1.5f, -1.5f), out.toList())
    }

    @Test
    fun `float equalizer boosts are not clipped`() {
        val processor = FloatEqualizerAudioProcessor()
        processor.setProfile(ParametricEQ(preamp = 6.0, bands = listOf(ParametricEQBand(frequency = 1000.0, gain = 0.0))))
        processor.prepare(pcmFloat)
        val out = processor.run(floats(0.9f, 0.9f))
        // +6 dB preamp on 0.9 is ~1.8: a 16-bit path would have clamped this to 1.0.
        assertTrue(out.all { it > 1.7f })
    }

    @Test
    fun `sound shaper accepts float and is transparent when neutral`() {
        val processor = SoundShaperAudioProcessor()
        assertEquals(C.ENCODING_PCM_FLOAT, processor.prepare(pcmFloat).encoding)
        val out = processor.run(floats(0.3f, -0.7f, 1.2f, 0f))
        assertEquals(listOf(0.3f, -0.7f, 1.2f, 0f), out.toList())
    }

    @Test
    fun `sound shaper balance works in float`() {
        val processor = SoundShaperAudioProcessor()
        processor.update(SoundShaperSettings(balance = 1f))
        processor.prepare(pcmFloat)
        val out = processor.run(floats(0.5f, 0.5f))
        assertTrue(abs(out[0]) < 1e-6f)
        assertEquals(0.5f, out[1], 1e-6f)
    }

    @Test
    fun `float chain converts after the 16-bit stages`() {
        val chain = FloatOutputAudioProcessorChain(
            silenceDetector = SilenceDetectorAudioProcessor(onLongSilence = {}),
            floatProcessors = listOf(VolumeNormalizationAudioProcessor(), FloatEqualizerAudioProcessor(), SoundShaperAudioProcessor()),
        )
        val processors = chain.audioProcessors
        var format = pcm16
        processors.forEach { processor ->
            val out = processor.configure(format)
            if (out != AudioProcessor.AudioFormat.NOT_SET) format = out
        }
        assertEquals(C.ENCODING_PCM_FLOAT, format.encoding)
        assertEquals(7, processors.size)
        assertTrue(processors[3] is Pcm16ToFloatAudioProcessor)
    }
}
