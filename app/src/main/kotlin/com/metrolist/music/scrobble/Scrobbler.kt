package com.metrolist.music.scrobble

import android.content.Context
import android.os.SystemClock
import com.metrolist.music.constants.LastFmScrobbleEnabledKey
import com.metrolist.music.constants.LastFmSessionKey
import com.metrolist.music.constants.LastFmUsernameKey
import com.metrolist.music.constants.ListenBrainzEnabledKey
import com.metrolist.music.constants.ListenBrainzTokenKey
import com.metrolist.music.constants.ScrobbleNowPlayingKey
import com.metrolist.music.constants.ScrobblePendingKey
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.safeDataStoreEdit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

/**
 * Sends what the player plays to Last.fm and ListenBrainz.
 *
 * A song counts once it has been heard for half its length or 4 minutes, whichever comes first,
 * and songs of 30 seconds or less never count (the Last.fm rules). Listens that cannot be sent are
 * kept in a small queue in preferences and retried with back-off, so no database change is needed.
 * All entry points must be called on the main thread.
 */
class Scrobbler(
    context: Context,
    private val scope: CoroutineScope,
) {
    private val appContext = context.applicationContext
    private val queueLock = Mutex()

    private class Listen(
        val id: String,
        val track: LastFm.Track,
        val thresholdMs: Long,
    ) {
        var playedMs = 0L
        var playingSince = -1L
        var scrobbled = false
    }

    private var current: Listen? = null
    private var timer: Job? = null
    private var retryJob: Job? = null
    private var retryDelayMs = INITIAL_RETRY_MS

    init {
        scope.launch(Dispatchers.IO) { flush() }
    }

    /** A new item started, or the same one started again (repeat). */
    fun onTrackStarted(
        metadata: MediaMetadata?,
        isPlaying: Boolean,
    ) {
        pause()
        timer?.cancel()
        current = metadata?.toListen()
        current?.let { listen ->
            if (isPlaying) resume()
            scope.launch(Dispatchers.IO) { sendNowPlaying(listen.track) }
        }
    }

    fun onPlayingChanged(isPlaying: Boolean) {
        if (isPlaying) resume() else pause()
    }

    private fun resume() {
        val listen = current ?: return
        if (listen.scrobbled || listen.playingSince >= 0) return
        listen.playingSince = SystemClock.elapsedRealtime()
        timer?.cancel()
        timer =
            scope.launch(Dispatchers.Main) {
                delay((listen.thresholdMs - listen.playedMs).coerceAtLeast(0L))
                if (listen !== current || listen.playingSince < 0) return@launch
                val now = SystemClock.elapsedRealtime()
                listen.playedMs += now - listen.playingSince
                listen.playingSince = now
                if (!listen.scrobbled && listen.playedMs >= listen.thresholdMs) {
                    listen.scrobbled = true
                    scope.launch(Dispatchers.IO) { enqueue(listen.track) }
                }
            }
    }

    private fun pause() {
        val listen = current ?: return
        if (listen.playingSince >= 0) {
            listen.playedMs += SystemClock.elapsedRealtime() - listen.playingSince
            listen.playingSince = -1L
        }
        timer?.cancel()
    }

    private fun MediaMetadata.toListen(): Listen? {
        if (isEpisode || duration <= MIN_DURATION_SEC) return null
        val artist = artists.firstOrNull()?.name?.takeIf { it.isNotBlank() } ?: return null
        val track =
            LastFm.Track(
                artist = artist,
                title = title,
                album = album?.title,
                durationSec = duration,
                timestampSec = System.currentTimeMillis() / 1000,
            )
        val thresholdMs = minOf(duration * 1000L / 2, MAX_THRESHOLD_MS)
        return Listen(id, track, thresholdMs)
    }

    private suspend fun prefs() = appContext.dataStore.data.first()

    private suspend fun sendNowPlaying(track: LastFm.Track) {
        val prefs = prefs()
        if (!(prefs[ScrobbleNowPlayingKey] ?: true)) return
        lastFmSession(prefs)?.let { session ->
            runCatching { LastFm.updateNowPlaying(session, track) }
                .onFailure { handleLastFmFailure(it) }
        }
        listenBrainzToken(prefs)?.let { token ->
            runCatching { ListenBrainz.playingNow(token, track) }
                .onFailure { Timber.d(it, "ListenBrainz now playing failed") }
        }
    }

    private fun lastFmSession(prefs: androidx.datastore.preferences.core.Preferences): String? =
        prefs[LastFmSessionKey]
            ?.takeIf { it.isNotBlank() && (prefs[LastFmScrobbleEnabledKey] ?: true) && LastFm.isConfigured }

    private fun listenBrainzToken(prefs: androidx.datastore.preferences.core.Preferences): String? =
        prefs[ListenBrainzTokenKey]?.takeIf { it.isNotBlank() && (prefs[ListenBrainzEnabledKey] ?: true) }

    private suspend fun enqueue(track: LastFm.Track) {
        val prefs = prefs()
        val toLastFm = lastFmSession(prefs) != null
        val toListenBrainz = listenBrainzToken(prefs) != null
        if (!toLastFm && !toListenBrainz) return
        queueLock.withLock {
            val queue = readQueue(prefs).toMutableList()
            queue += Pending(track, toLastFm, toListenBrainz)
            writeQueue(queue.takeLast(MAX_QUEUE))
        }
        flush()
    }

    private suspend fun flush() {
        queueLock.withLock {
            val prefs = prefs()
            var queue = readQueue(prefs)
            if (queue.isEmpty()) return
            var failedRetryable = false

            val lastFm = lastFmSession(prefs)
            if (lastFm == null) {
                queue = queue.map { it.copy(lastFm = false) }
            } else {
                queue
                    .filter { it.lastFm }
                    .chunked(LastFm.MAX_BATCH)
                    .forEach { batch ->
                        val result = runCatching { LastFm.scrobble(lastFm, batch.map { it.track }) }
                        if (result.isSuccess || !isRetryable(result.exceptionOrNull())) {
                            result.exceptionOrNull()?.let { handleLastFmFailure(it) }
                            val sent = batch.toSet()
                            queue = queue.map { if (it in sent) it.copy(lastFm = false) else it }
                        } else {
                            failedRetryable = true
                        }
                    }
            }

            val token = listenBrainzToken(prefs)
            if (token == null) {
                queue = queue.map { it.copy(listenBrainz = false) }
            } else {
                queue
                    .filter { it.listenBrainz }
                    .chunked(ListenBrainz.MAX_BATCH)
                    .forEach { batch ->
                        val result = runCatching { ListenBrainz.listens(token, batch.map { it.track }) }
                        if (result.isSuccess || !isRetryable(result.exceptionOrNull())) {
                            result.exceptionOrNull()?.let { Timber.w(it, "ListenBrainz dropped listens") }
                            val sent = batch.toSet()
                            queue = queue.map { if (it in sent) it.copy(listenBrainz = false) else it }
                        } else {
                            failedRetryable = true
                        }
                    }
            }

            writeQueue(queue.filter { it.lastFm || it.listenBrainz })
            if (failedRetryable) scheduleRetry() else retryDelayMs = INITIAL_RETRY_MS
        }
    }

    private fun scheduleRetry() {
        if (retryJob?.isActive == true) return
        val wait = retryDelayMs
        retryDelayMs = (retryDelayMs * 2).coerceAtMost(MAX_RETRY_MS)
        retryJob =
            scope.launch(Dispatchers.IO) {
                delay(wait)
                flush()
            }
    }

    private fun isRetryable(error: Throwable?): Boolean =
        when (error) {
            is ScrobbleHttpException -> error.retryable
            is LastFm.SessionExpiredException -> false
            null -> false
            else -> true
        }

    private suspend fun handleLastFmFailure(error: Throwable) {
        Timber.w(error, "Last.fm request failed")
        if (error is LastFm.SessionExpiredException) {
            appContext.safeDataStoreEdit {
                it.remove(LastFmSessionKey)
                it.remove(LastFmUsernameKey)
            }
        }
    }

    private data class Pending(
        val track: LastFm.Track,
        val lastFm: Boolean,
        val listenBrainz: Boolean,
    )

    private fun readQueue(prefs: androidx.datastore.preferences.core.Preferences): List<Pending> {
        val raw = prefs[ScrobblePendingKey]?.takeIf { it.isNotBlank() } ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                Pending(
                    track =
                        LastFm.Track(
                            artist = o.getString("artist"),
                            title = o.getString("title"),
                            album = o.optString("album").takeIf { it.isNotBlank() },
                            durationSec = o.optInt("duration"),
                            timestampSec = o.getLong("ts"),
                        ),
                    lastFm = o.optBoolean("lf"),
                    listenBrainz = o.optBoolean("lb"),
                )
            }
        }.getOrElse { emptyList() }
    }

    private suspend fun writeQueue(queue: List<Pending>) {
        val array = JSONArray()
        queue.forEach { p ->
            array.put(
                JSONObject()
                    .put("artist", p.track.artist)
                    .put("title", p.track.title)
                    .put("album", p.track.album.orEmpty())
                    .put("duration", p.track.durationSec)
                    .put("ts", p.track.timestampSec)
                    .put("lf", p.lastFm)
                    .put("lb", p.listenBrainz),
            )
        }
        appContext.safeDataStoreEdit { it[ScrobblePendingKey] = array.toString() }
    }

    private companion object {
        const val MIN_DURATION_SEC = 30
        const val MAX_THRESHOLD_MS = 4 * 60 * 1000L
        const val MAX_QUEUE = 500
        const val INITIAL_RETRY_MS = 30_000L
        const val MAX_RETRY_MS = 6 * 60 * 60 * 1000L
    }
}
