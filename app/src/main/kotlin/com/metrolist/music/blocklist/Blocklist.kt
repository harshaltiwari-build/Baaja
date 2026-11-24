package com.metrolist.music.blocklist

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.safeDataStoreEdit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * Songs and artists the listener never wants in radio, autoplay and mixes.
 * Kept as one JSON blob in preferences (no database change) and mirrored in memory so queue
 * code can check it without suspending.
 */
object Blocklist {
    data class Entry(
        val id: String,
        val name: String,
        val subtitle: String? = null,
    )

    data class State(
        val artists: List<Entry> = emptyList(),
        val songs: List<Entry> = emptyList(),
    ) {
        val isEmpty: Boolean get() = artists.isEmpty() && songs.isEmpty()
    }

    private val KEY = stringPreferencesKey("baajaBlocklist")
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    /** Keeps the in-memory copy in step with preferences. Never returns, so launch it in a scope. */
    suspend fun sync(context: Context) {
        context.applicationContext.dataStore.data
            .map { parse(it[KEY]) }
            .collect { _state.value = it }
    }

    suspend fun blockArtist(
        context: Context,
        id: String?,
        name: String,
    ) = update(context) { s ->
        val entry = Entry(id ?: name.key(), name)
        if (s.artists.any { it.id == entry.id }) s else s.copy(artists = s.artists + entry)
    }

    suspend fun blockSong(
        context: Context,
        id: String,
        title: String,
        artist: String?,
    ) = update(context) { s ->
        if (s.songs.any { it.id == id }) s else s.copy(songs = s.songs + Entry(id, title, artist))
    }

    suspend fun unblockArtist(
        context: Context,
        id: String,
    ) = update(context) { s -> s.copy(artists = s.artists.filterNot { it.id == id }) }

    suspend fun unblockSong(
        context: Context,
        id: String,
    ) = update(context) { s -> s.copy(songs = s.songs.filterNot { it.id == id }) }

    fun blocks(metadata: MediaMetadata?): Boolean {
        metadata ?: return false
        val s = _state.value
        if (s.isEmpty) return false
        if (s.songs.any { it.id == metadata.id }) return true
        return metadata.artists.any { artist -> isArtistBlocked(s, artist.id, artist.name) }
    }

    fun isArtistBlocked(
        id: String?,
        name: String,
    ) = isArtistBlocked(_state.value, id, name)

    private fun isArtistBlocked(
        s: State,
        id: String?,
        name: String,
    ) = s.artists.any { it.id == id || it.id == name.key() }

    private suspend fun update(
        context: Context,
        change: (State) -> State,
    ) {
        context.applicationContext.safeDataStoreEdit { prefs ->
            val next = change(parse(prefs[KEY]))
            prefs[KEY] = serialize(next)
        }
    }

    private fun String.key() = "name:" + trim().lowercase(Locale.ROOT)

    private fun parse(raw: String?): State {
        if (raw.isNullOrBlank()) return State()
        return runCatching {
            val json = JSONObject(raw)
            State(readList(json.optJSONArray("artists")), readList(json.optJSONArray("songs")))
        }.getOrDefault(State())
    }

    private fun readList(array: JSONArray?): List<Entry> =
        array?.let { a ->
            (0 until a.length()).mapNotNull { i ->
                val o = a.optJSONObject(i) ?: return@mapNotNull null
                val id = o.optString("id").takeIf { it.isNotBlank() } ?: return@mapNotNull null
                Entry(id, o.optString("name"), o.optString("sub").takeIf { it.isNotBlank() })
            }
        }.orEmpty()

    private fun serialize(s: State): String =
        JSONObject()
            .put("artists", writeList(s.artists))
            .put("songs", writeList(s.songs))
            .toString()

    private fun writeList(list: List<Entry>) =
        JSONArray().also { a ->
            list.forEach { e ->
                a.put(JSONObject().put("id", e.id).put("name", e.name).put("sub", e.subtitle.orEmpty()))
            }
        }
}
