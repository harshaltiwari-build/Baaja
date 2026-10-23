/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback.automix

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/*
 * Automix: plain signal processing, no model. We look at the loudness (RMS, dBFS) of the last
 * ~30 s of the outgoing track and the first ~12 s of the incoming one, then decide
 *  - where the outgoing track really ends (trailing silence is skipped),
 *  - whether it fades out, and if so start the crossfade where the fade starts,
 *  - where the incoming track really starts (leading silence is skipped),
 *  - how much to turn the louder side down so both meet at the same level mid-fade.
 */

const val AUTOMIX_WINDOW_MS = 50
const val AUTOMIX_TAIL_SCAN_MS = 30_000L
const val AUTOMIX_HEAD_SCAN_MS = 12_000L

internal const val SILENCE_DB = -50f
internal const val MIN_CROSSFADE_MS = 1_000L
internal const val MAX_CROSSFADE_MS = 12_000L
internal const val HARD_ENDING_MAX_MS = 4_000L
internal const val MAX_LEADING_SKIP_MS = 8_000L
internal const val FADE_DROP_DB = 3f
internal const val MAX_LEVEL_MATCH_DB = 9f
private const val SMOOTHING_WINDOWS = 5
private const val FLOOR_DB = -100f

/** Loudness of consecutive [windowMs] windows, the first one starting at [startMs] of the track. */
class LoudnessEnvelope(
    val startMs: Long,
    val windowMs: Int,
    val levelsDb: FloatArray,
) {
    val endMs: Long get() = startMs + levelsDb.size.toLong() * windowMs

    fun windowStartMs(index: Int): Long = startMs + index.toLong() * windowMs

    fun windowEndMs(index: Int): Long = windowStartMs(index + 1)
}

/**
 * Accumulates interleaved PCM (already scaled to -1..1) into per-window RMS levels.
 * Samples before [startMs] are ignored, so a decoder that lands on an earlier sync point is fine.
 */
class LoudnessEnvelopeBuilder(
    private val sampleRate: Int,
    private val channelCount: Int,
    private val startMs: Long,
    private val windowMs: Int = AUTOMIX_WINDOW_MS,
) {
    private val framesPerWindow = (sampleRate.toLong() * windowMs / 1000).toInt().coerceAtLeast(1)
    private val levels = ArrayList<Float>()
    private var sumSquares = 0.0
    private var framesInWindow = 0
    private var channelIndex = 0
    private var frameSum = 0.0

    /** Adds one interleaved sample that belongs to the frame at [frameTimeUs]. */
    fun add(sample: Float, frameTimeUs: Long) {
        if (frameTimeUs < startMs * 1000) {
            channelIndex = (channelIndex + 1) % channelCount
            return
        }
        frameSum += sample.toDouble() * sample
        channelIndex++
        if (channelIndex == channelCount) {
            sumSquares += frameSum / channelCount
            frameSum = 0.0
            channelIndex = 0
            framesInWindow++
            if (framesInWindow == framesPerWindow) closeWindow()
        }
    }

    /** Convenience for tests and simple callers: interleaved samples starting at [firstFrameUs]. */
    fun addAll(samples: FloatArray, firstFrameUs: Long) {
        val frameUs = 1_000_000.0 / sampleRate
        for (i in samples.indices) add(samples[i], firstFrameUs + ((i / channelCount) * frameUs).toLong())
    }

    private fun closeWindow() {
        levels += rmsToDb(sqrt(sumSquares / framesInWindow))
        sumSquares = 0.0
        framesInWindow = 0
    }

    val windowCount: Int get() = levels.size

    fun build(): LoudnessEnvelope = LoudnessEnvelope(startMs, windowMs, levels.toFloatArray())
}

internal fun rmsToDb(rms: Double): Float = if (rms <= 1e-5) FLOOR_DB else (20 * log10(rms)).toFloat().coerceAtLeast(FLOOR_DB)

/** What the end of the outgoing track looks like. All times are positions in that track. */
data class TailAnalysis(
    /** End of the last audible window; anything after is (near) silence. */
    val audibleEndMs: Long,
    /** Where a fade-out / quiet outro starts, or null for a hard ending. */
    val fadeStartMs: Long?,
    /** Typical level of the music before the ending. */
    val bodyLevelDb: Float,
)

/** What the start of the incoming track looks like. */
data class HeadAnalysis(
    /** Start of the first audible window (leading silence length). */
    val audibleStartMs: Long,
    val bodyLevelDb: Float,
)

/** One concrete transition. */
data class AutomixPlan(
    val outgoingId: String,
    val incomingId: String,
    /** Position in the outgoing track where the crossfade starts. */
    val startMs: Long,
    val durationMs: Long,
    /** Where the incoming track starts playing (skips leading silence). */
    val incomingStartMs: Long,
    /** How far the outgoing track is turned down by the end of the fade (it was louder). */
    val outgoingAttenuationDb: Float,
    /** How far the incoming track starts turned down (it was louder); recovers during the fade. */
    val incomingAttenuationDb: Float,
)

private fun FloatArray.smoothed(): FloatArray {
    if (isEmpty()) return this
    val half = SMOOTHING_WINDOWS / 2
    return FloatArray(size) { i ->
        var sum = 0f
        var n = 0
        for (j in (i - half).coerceAtLeast(0)..(i + half).coerceAtMost(lastIndex)) {
            sum += this[j]
            n++
        }
        sum / n
    }
}

private fun List<Float>.percentile(fraction: Float): Float {
    if (isEmpty()) return FLOOR_DB
    val sorted = sorted()
    return sorted[((sorted.size - 1) * fraction).roundToInt().coerceIn(0, sorted.lastIndex)]
}

fun analyzeTail(envelope: LoudnessEnvelope): TailAnalysis? {
    val levels = envelope.levelsDb
    val lastAudible = levels.indexOfLast { it > SILENCE_DB }
    if (lastAudible < 0) return null
    val audibleEndMs = envelope.windowEndMs(lastAudible)

    val audible = (0..lastAudible).map { levels[it] }.filter { it > SILENCE_DB }
    val bodyLevel = audible.percentile(0.9f)
    val threshold = bodyLevel - FADE_DROP_DB

    // Walk back from the end until the (smoothed) level is back at body level: that's where the
    // outro / fade-out begins.
    val smooth = levels.smoothed()
    val lastLoud = (0..lastAudible).lastOrNull { smooth[it] >= threshold }
    val fadeStartMs = lastLoud?.let { envelope.windowEndMs(it) }
        ?.takeIf { audibleEndMs - it >= MIN_CROSSFADE_MS }

    return TailAnalysis(audibleEndMs = audibleEndMs, fadeStartMs = fadeStartMs, bodyLevelDb = bodyLevel)
}

fun analyzeHead(envelope: LoudnessEnvelope): HeadAnalysis? {
    val levels = envelope.levelsDb
    val firstAudible = levels.indexOfFirst { it > SILENCE_DB }
    if (firstAudible < 0) return null
    val body = (firstAudible..levels.lastIndex).map { levels[it] }.filter { it > SILENCE_DB }
    return HeadAnalysis(
        audibleStartMs = envelope.windowStartMs(firstAudible),
        bodyLevelDb = body.percentile(0.9f),
    )
}

/**
 * Builds the transition. [manualDurationMs] is the user's crossfade setting, used as the length
 * for hard endings (capped, since a long blend over a full-volume ending sounds muddy).
 * Gains are extra dB already applied to each track (e.g. loudness normalization), so levels are
 * compared as heard.
 */
fun planTransition(
    outgoingId: String,
    incomingId: String,
    tail: TailAnalysis,
    head: HeadAnalysis?,
    manualDurationMs: Long,
    outgoingGainDb: Float = 0f,
    incomingGainDb: Float = 0f,
): AutomixPlan {
    val end = tail.audibleEndMs
    val duration =
        if (tail.fadeStartMs != null) {
            (end - tail.fadeStartMs).coerceIn(MIN_CROSSFADE_MS, MAX_CROSSFADE_MS)
        } else {
            manualDurationMs.coerceIn(MIN_CROSSFADE_MS, HARD_ENDING_MAX_MS)
        }.coerceAtMost(end)
    val incomingStart =
        head?.audibleStartMs
            ?.takeIf { it <= MAX_LEADING_SKIP_MS }
            ?.minus(LEADING_MARGIN_MS)
            ?.coerceAtLeast(0L)
            ?: 0L

    val outLevel = tail.bodyLevelDb + outgoingGainDb
    val inLevel = (head?.bodyLevelDb ?: tail.bodyLevelDb) + incomingGainDb
    val difference = if (head == null) 0f else outLevel - inLevel

    return AutomixPlan(
        outgoingId = outgoingId,
        incomingId = incomingId,
        startMs = end - duration,
        durationMs = duration,
        incomingStartMs = incomingStart,
        outgoingAttenuationDb = difference.coerceIn(0f, MAX_LEVEL_MATCH_DB),
        incomingAttenuationDb = (-difference).coerceIn(0f, MAX_LEVEL_MATCH_DB),
    )
}

private const val LEADING_MARGIN_MS = 30L

/**
 * Volumes at [progress] (0..1) through the fade: equal-power curves (constant perceived loudness
 * when the tracks are uncorrelated) plus the level match.
 * Returns incoming to outgoing.
 */
fun automixGains(progress: Float, plan: AutomixPlan): Pair<Float, Float> {
    val p = progress.coerceIn(0f, 1f)
    val incoming = sin(p * PI / 2).toFloat() * dbToGain(-plan.incomingAttenuationDb * (1f - p))
    val outgoing = cos(p * PI / 2).toFloat() * dbToGain(-plan.outgoingAttenuationDb * p)
    return incoming.coerceIn(0f, 1f) to outgoing.coerceIn(0f, 1f)
}

internal fun dbToGain(db: Float): Float = 10.0.pow(db / 20.0).toFloat()
