/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback.automix

import android.util.LruCache
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Keeps per-song analyses (so repeat plays and queue reorders are free) and turns them into
 * [AutomixPlan]s. Only successful analyses are cached: a failure usually means the audio isn't
 * cached yet and is worth retrying a few seconds later.
 */
@UnstableApi
class AutomixCoordinator(
    private val scanner: CachedAudioScanner,
    /** Extra gain (dB) the player applies to a song, e.g. loudness normalization. */
    private val playbackGainDb: suspend (String) -> Float,
) {
    private val tails = LruCache<String, TailAnalysis>(16)
    private val heads = LruCache<String, HeadAnalysis>(16)

    fun cachedPlan(
        outgoingId: String,
        incomingId: String,
        manualDurationMs: Long,
        outgoingGainDb: Float,
        incomingGainDb: Float,
    ): AutomixPlan? {
        val tail = synchronized(tails) { tails.get(outgoingId) } ?: return null
        val head = synchronized(heads) { heads.get(incomingId) } ?: return null
        return planTransition(outgoingId, incomingId, tail, head, manualDurationMs, outgoingGainDb, incomingGainDb)
    }

    fun hasAnalyses(outgoingId: String, incomingId: String): Boolean =
        synchronized(tails) { tails.get(outgoingId) } != null && synchronized(heads) { heads.get(incomingId) } != null

    /** Analyzes whatever is missing for this pair. True when both sides are available. */
    suspend fun prepare(
        outgoingId: String,
        outgoingDurationMs: Long,
        incomingId: String,
    ): Boolean =
        withContext(Dispatchers.IO) {
            val deadline = System.nanoTime() + SCAN_BUDGET_NANOS
            if (synchronized(tails) { tails.get(outgoingId) } == null) {
                val from = (outgoingDurationMs - AUTOMIX_TAIL_SCAN_MS).coerceAtLeast(0L)
                val envelope = scanner.scan(outgoingId, from, outgoingDurationMs, deadline)
                    ?.takeIf { it.endMs >= outgoingDurationMs - COVERAGE_TOLERANCE_MS }
                val tail = envelope?.let(::analyzeTail) ?: return@withContext false.also {
                    Timber.tag(TAG).d("Tail of %s not available yet", outgoingId)
                }
                synchronized(tails) { tails.put(outgoingId, tail) }
            }
            if (synchronized(heads) { heads.get(incomingId) } == null) {
                val envelope = scanner.scan(incomingId, 0L, AUTOMIX_HEAD_SCAN_MS, deadline)
                    ?.takeIf { it.endMs >= AUTOMIX_HEAD_SCAN_MS - COVERAGE_TOLERANCE_MS }
                val head = envelope?.let(::analyzeHead) ?: return@withContext false.also {
                    Timber.tag(TAG).d("Head of %s not available yet", incomingId)
                }
                synchronized(heads) { heads.put(incomingId, head) }
            }
            true
        }

    suspend fun gainsFor(outgoingId: String, incomingId: String): Pair<Float, Float> =
        withContext(Dispatchers.IO) {
            runCatching { playbackGainDb(outgoingId) to playbackGainDb(incomingId) }.getOrDefault(0f to 0f)
        }

    private companion object {
        const val TAG = "Automix"
        const val SCAN_BUDGET_NANOS = 8_000_000_000L
        const val COVERAGE_TOLERANCE_MS = 1_500L
    }
}
