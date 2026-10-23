/*
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback.automix

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class AutomixAnalysisTest {
    /** Envelope with one level per 50 ms window, given as (seconds, dB) segments. */
    private fun envelope(startMs: Long, vararg segments: Pair<Double, Float>): LoudnessEnvelope {
        val levels = ArrayList<Float>()
        for ((seconds, db) in segments) repeat((seconds * 1000 / AUTOMIX_WINDOW_MS).toInt()) { levels += db }
        return LoudnessEnvelope(startMs, AUTOMIX_WINDOW_MS, levels.toFloatArray())
    }

    /** Envelope that ramps linearly from [fromDb] to [toDb] over [seconds]. */
    private fun ramp(seconds: Double, fromDb: Float, toDb: Float): List<Float> {
        val n = (seconds * 1000 / AUTOMIX_WINDOW_MS).toInt()
        return List(n) { fromDb + (toDb - fromDb) * it / (n - 1) }
    }

    @Test
    fun `envelope builder measures rms per window`() {
        val sampleRate = 1000
        val builder = LoudnessEnvelopeBuilder(sampleRate, channelCount = 2, startMs = 0, windowMs = 50)
        // 100 ms of a full-scale square wave (RMS 1.0 = 0 dBFS), then 100 ms at half amplitude (-6 dBFS).
        val loud = FloatArray(200) { if ((it / 2) % 2 == 0) 1f else -1f }
        val quiet = FloatArray(200) { if ((it / 2) % 2 == 0) 0.5f else -0.5f }
        builder.addAll(loud + quiet, firstFrameUs = 0)
        val env = builder.build()

        assertEquals(4, env.levelsDb.size)
        assertEquals(0f, env.levelsDb[0], 0.01f)
        assertEquals(-6.02f, env.levelsDb[3], 0.05f)
    }

    @Test
    fun `envelope builder skips audio before the requested start`() {
        val builder = LoudnessEnvelopeBuilder(sampleRate = 1000, channelCount = 1, startMs = 100, windowMs = 50)
        builder.addAll(FloatArray(200) { 1f }, firstFrameUs = 0)
        val env = builder.build()
        assertEquals(100L, env.startMs)
        assertEquals(2, env.levelsDb.size)
    }

    @Test
    fun `silence is reported at the floor`() {
        assertTrue(rmsToDb(0.0) <= -99f)
    }

    @Test
    fun `tail with trailing silence and no fade is a hard ending`() {
        // 25 s of music at -14 dB, then 5 s of silence. Envelope covers 170..200 s.
        val env = envelope(170_000, 25.0 to -14f, 5.0 to -90f)
        val tail = analyzeTail(env)!!
        assertEquals(195_000L, tail.audibleEndMs)
        assertNull(tail.fadeStartMs)
        assertEquals(-14f, tail.bodyLevelDb, 0.01f)
    }

    @Test
    fun `tail fade out is found where the level starts dropping`() {
        val levels = ArrayList<Float>()
        repeat(20_000 / AUTOMIX_WINDOW_MS) { levels += -12f } // 20 s body
        levels += ramp(8.0, -12f, -60f) // 8 s fade-out
        repeat(2_000 / AUTOMIX_WINDOW_MS) { levels += -90f } // 2 s silence
        val env = LoudnessEnvelope(0, AUTOMIX_WINDOW_MS, levels.toFloatArray())

        val tail = analyzeTail(env)!!
        assertNotNull(tail.fadeStartMs)
        val fadeStart = tail.fadeStartMs!!
        // The level crosses body-3dB about half a second into the ramp.
        assertTrue("fade start $fadeStart", fadeStart in 20_000L..21_200L)
        // Audible end: last window above -50 dB, roughly 6 s into the 8 s ramp.
        assertTrue("audible end ${tail.audibleEndMs}", tail.audibleEndMs in 25_500L..26_500L)
    }

    @Test
    fun `fully silent tail cannot be analysed`() {
        assertNull(analyzeTail(envelope(0, 30.0 to -90f)))
    }

    @Test
    fun `head finds leading silence`() {
        val head = analyzeHead(envelope(0, 1.5 to -80f, 10.0 to -10f))!!
        assertEquals(1_500L, head.audibleStartMs)
        assertEquals(-10f, head.bodyLevelDb, 0.01f)
    }

    @Test
    fun `plan for a fade-out starts at the fade and skips the next song's silence`() {
        val tail = TailAnalysis(audibleEndMs = 200_000, fadeStartMs = 192_000, bodyLevelDb = -12f)
        val head = HeadAnalysis(audibleStartMs = 1_500, bodyLevelDb = -12f)
        val plan = planTransition("a", "b", tail, head, manualDurationMs = 5_000)

        assertEquals(192_000L, plan.startMs)
        assertEquals(8_000L, plan.durationMs)
        assertEquals(1_470L, plan.incomingStartMs)
        assertEquals(0f, plan.outgoingAttenuationDb, 0.001f)
        assertEquals(0f, plan.incomingAttenuationDb, 0.001f)
    }

    @Test
    fun `very long fades are capped`() {
        val tail = TailAnalysis(audibleEndMs = 200_000, fadeStartMs = 170_000, bodyLevelDb = -12f)
        val plan = planTransition("a", "b", tail, null, manualDurationMs = 5_000)
        assertEquals(MAX_CROSSFADE_MS, plan.durationMs)
        assertEquals(200_000L - MAX_CROSSFADE_MS, plan.startMs)
        assertEquals(0L, plan.incomingStartMs)
    }

    @Test
    fun `hard endings use the manual length capped to a short blend before the audible end`() {
        val tail = TailAnalysis(audibleEndMs = 195_000, fadeStartMs = null, bodyLevelDb = -14f)
        assertEquals(195_000L - HARD_ENDING_MAX_MS, planTransition("a", "b", tail, null, 10_000).startMs)
        assertEquals(2_000L, planTransition("a", "b", tail, null, 2_000).durationMs)
    }

    @Test
    fun `long leading silence is not skipped`() {
        val tail = TailAnalysis(200_000, null, -14f)
        val head = HeadAnalysis(audibleStartMs = MAX_LEADING_SKIP_MS + 1_000, bodyLevelDb = -14f)
        assertEquals(0L, planTransition("a", "b", tail, head, 5_000).incomingStartMs)
    }

    @Test
    fun `louder side is turned down, including applied gains, and capped`() {
        val tail = TailAnalysis(200_000, null, bodyLevelDb = -8f)
        val quietHead = HeadAnalysis(0, bodyLevelDb = -14f)
        val plan = planTransition("a", "b", tail, quietHead, 5_000)
        assertEquals(6f, plan.outgoingAttenuationDb, 0.001f)
        assertEquals(0f, plan.incomingAttenuationDb, 0.001f)

        // Normalization already pulls the outgoing song down by 4 dB: only 2 dB left to match.
        val normalized = planTransition("a", "b", tail, quietHead, 5_000, outgoingGainDb = -4f)
        assertEquals(2f, normalized.outgoingAttenuationDb, 0.001f)

        val loudHead = HeadAnalysis(0, bodyLevelDb = 10f)
        val capped = planTransition("a", "b", tail, loudHead, 5_000)
        assertEquals(MAX_LEVEL_MATCH_DB, capped.incomingAttenuationDb, 0.001f)
        assertEquals(0f, capped.outgoingAttenuationDb, 0.001f)
    }

    @Test
    fun `gains are equal power and apply the level match progressively`() {
        val plan = AutomixPlan("a", "b", 0, 8_000, 0, outgoingAttenuationDb = 6f, incomingAttenuationDb = 0f)
        val (inStart, outStart) = automixGains(0f, plan)
        assertEquals(0f, inStart, 0.0001f)
        assertEquals(1f, outStart, 0.0001f)

        val (inEnd, outEnd) = automixGains(1f, plan)
        assertEquals(1f, inEnd, 0.0001f)
        assertEquals(0f, outEnd, 0.0001f)

        val (inMid, outMid) = automixGains(0.5f, plan)
        assertEquals(sin(PI / 4).toFloat(), inMid, 0.0001f)
        // Outgoing is 3 dB (half of 6 dB) below its equal-power value at the midpoint.
        assertEquals(sin(PI / 4).toFloat() * dbToGain(-3f), outMid, 0.0001f)

        val flat = plan.copy(outgoingAttenuationDb = 0f)
        val (a, b) = automixGains(0.5f, flat)
        assertTrue(abs(a * a + b * b - 1f) < 0.001f)
    }
}
