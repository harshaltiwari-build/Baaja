package com.metrolist.music.recommend

import android.content.Context
import android.os.SystemClock
import androidx.datastore.preferences.core.stringPreferencesKey
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.safeDataStoreEdit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * What the listener's behaviour says about songs and artists, learned on the device:
 * a song left within 30 seconds by skipping is a skip, a song heard for most of its length is a finish.
 * Also remembers what was played and suggested lately so autoplay doesn't repeat itself.
 *
 * Stored as a small JSON blob in preferences, so no database change is needed.
 * Entry points are called on the main thread from the player service.
 */
class ListenSignals(
    context: Context,
    private val scope: CoroutineScope,
) {
    private val appContext = context.applicationContext

    private class Counts(
        var skips: Int = 0,
        var finishes: Int = 0,
    )

    private val songs = LinkedHashMap<String, Counts>(64, 0.75f, true)
    private val artists = LinkedHashMap<String, Counts>(64, 0.75f, true)
    private val recentPlays = LinkedHashMap<String, Long>()
    private val suggested = LinkedHashSet<String>()

    private var currentId: String? = null
    private var currentArtist: String? = null
    private var currentDurationMs = 0L
    private var playedMs = 0L
    private var playingSince = -1L
    private var saveJob: Job? = null

    init {
        scope.launch(Dispatchers.Main) {
            val raw = withContext(Dispatchers.IO) { appContext.dataStore.data.first()[KEY] } ?: return@launch
            runCatching {
                val json = JSONObject(raw)
                read(json.optJSONObject("songs"), songs)
                read(json.optJSONObject("artists"), artists)
            }
        }
    }

    /** A new item started; [userSkipped] is true when the listener moved on from the previous one themselves. */
    fun onTrackStarted(
        metadata: MediaMetadata?,
        isPlaying: Boolean,
        userSkipped: Boolean,
    ) {
        finishCurrent(userSkipped)
        currentId = metadata?.id
        currentArtist = metadata?.artists?.firstOrNull()?.name?.let(SongIdentity::mainArtist)
        currentDurationMs = (metadata?.duration ?: 0) * 1000L
        playedMs = 0L
        playingSince = -1L
        metadata?.id?.let { id ->
            recentPlays.remove(id)
            recentPlays[id] = System.currentTimeMillis()
            while (recentPlays.size > MAX_RECENT) recentPlays.remove(recentPlays.keys.first())
        }
        if (isPlaying) onPlayingChanged(true)
    }

    fun onPlayingChanged(isPlaying: Boolean) {
        if (isPlaying) {
            if (playingSince < 0) playingSince = SystemClock.elapsedRealtime()
        } else if (playingSince >= 0) {
            playedMs += SystemClock.elapsedRealtime() - playingSince
            playingSince = -1L
        }
    }

    private fun finishCurrent(userSkipped: Boolean) {
        val id = currentId ?: return
        onPlayingChanged(false)
        if (currentDurationMs < MIN_DURATION_MS) return
        val finished = playedMs >= minOf(currentDurationMs * 6 / 10, FINISH_MS)
        val skipped = userSkipped && playedMs < SKIP_MS
        if (!finished && !skipped) return
        val song = songs.getOrPut(id) { Counts() }
        val artist = currentArtist?.let { artists.getOrPut(it) { Counts() } }
        if (finished) {
            song.skips = 0
            song.finishes++
            artist?.let { it.finishes++ }
        } else {
            song.skips++
            artist?.let { it.skips++ }
        }
        trim(songs)
        trim(artists)
        scheduleSave()
    }

    fun wasRecent(id: String): Boolean {
        val playedAt = recentPlays[id]
        if (playedAt != null && System.currentTimeMillis() - playedAt < RECENT_WINDOW_MS) return true
        return id in suggested
    }

    fun isSkippedSong(id: String): Boolean = (songs[id]?.skips ?: 0) >= 2

    fun skippedArtists(): Set<String> =
        artists.filterValues { it.skips >= 3 && it.skips >= 2 * (it.finishes + 1) }.keys

    fun markSuggested(ids: List<String>) {
        suggested.addAll(ids)
        while (suggested.size > MAX_SUGGESTED) suggested.remove(suggested.first())
    }

    /** Recently played song ids, newest first, for picking extra radio seeds. */
    fun recentIds(): List<String> = recentPlays.keys.reversed()

    private fun scheduleSave() {
        saveJob?.cancel()
        val snapshot =
            JSONObject()
                .put("songs", write(songs))
                .put("artists", write(artists))
                .toString()
        saveJob =
            scope.launch(Dispatchers.IO) {
                delay(2_000)
                appContext.safeDataStoreEdit { it[KEY] = snapshot }
            }
    }

    private fun trim(map: LinkedHashMap<String, Counts>) {
        while (map.size > MAX_ENTRIES) map.remove(map.keys.first())
    }

    private fun read(
        json: JSONObject?,
        into: LinkedHashMap<String, Counts>,
    ) {
        json ?: return
        json.keys().forEach { key ->
            val a = json.optJSONArray(key) ?: return@forEach
            into.putIfAbsent(key, Counts(a.optInt(0), a.optInt(1)))
        }
    }

    private fun write(map: Map<String, Counts>): JSONObject {
        val json = JSONObject()
        map.forEach { (k, v) -> json.put(k, JSONArray().put(v.skips).put(v.finishes)) }
        return json
    }

    private companion object {
        val KEY = stringPreferencesKey("baajaListenSignals")
        const val MIN_DURATION_MS = 45_000L
        const val SKIP_MS = 30_000L
        const val FINISH_MS = 120_000L
        const val RECENT_WINDOW_MS = 3 * 60 * 60 * 1000L
        const val MAX_RECENT = 200
        const val MAX_SUGGESTED = 500
        const val MAX_ENTRIES = 400
    }
}
