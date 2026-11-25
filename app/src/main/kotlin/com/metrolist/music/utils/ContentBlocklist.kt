/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.Artist
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.EpisodeItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.PodcastItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_ATV
import com.metrolist.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_OMV
import com.metrolist.innertube.models.YTItem
import com.metrolist.music.models.MediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val BlockedArtistsKey = stringPreferencesKey("baajaBlockedArtists")
private val HiddenHomeItemsKey = stringPreferencesKey("baajaHiddenHomeItems")

/** Hide podcasts and their episodes from Home, search and radio (default off). */
val HidePodcastsKey = booleanPreferencesKey("baajaHidePodcasts")

/** Hide channels on the community list of AI-generated music (default off). */
val BlockAiArtistsKey = booleanPreferencesKey("baajaBlockAiArtists")

const val SHORT_SONG_SECONDS = 30

private val blocklistJson = Json { ignoreUnknownKeys = true }

/** An artist or item kept by id, with a name so the restore lists can show it. */
@Serializable
data class BlockedEntry(
    val id: String,
    val name: String,
)

/**
 * What the listener asked not to see: blocked artists (off Home, search and the queue), items hidden
 * from Home only, and the content filters. Stored in DataStore, so the database schema is untouched.
 */
data class ContentBlocklist(
    val blockedArtists: List<BlockedEntry> = emptyList(),
    val hiddenHomeItems: List<BlockedEntry> = emptyList(),
    val hidePodcasts: Boolean = false,
    /** Always on: vlogs, trailers and other videos that aren't music. */
    val hideNonMusicalVideos: Boolean = true,
    /** Always on: previews and clips shorter than [SHORT_SONG_SECONDS]. */
    val hideShortSongs: Boolean = true,
    val blockAiArtists: Boolean = false,
    /** Changes when the AI list is reloaded, so lists filtered with it are recomputed. */
    val aiListVersion: Int = 0,
) {
    private val artistIds = blockedArtists.mapTo(HashSet()) { it.id }
    private val artistNames = blockedArtists.mapTo(HashSet()) { it.name.normalized() }
    private val hiddenIds = hiddenHomeItems.mapTo(HashSet()) { it.id }

    val isEmpty: Boolean
        get() =
            blockedArtists.isEmpty() && hiddenHomeItems.isEmpty() &&
                !hidePodcasts && !hideNonMusicalVideos && !hideShortSongs && !blockAiArtists

    fun isArtistBlocked(
        id: String?,
        name: String?,
    ): Boolean =
        (blockAiArtists && AiArtistBlocklist.matches(id, name)) ||
            if (!id.isNullOrBlank()) {
                id in artistIds
            } else {
                !name.isNullOrBlank() && name.normalized() in artistNames
            }

    fun isArtistBlocked(artist: Artist?): Boolean =
        artist != null && isArtistBlocked(artist.id, artist.name)

    /** Whether a song by these artists should be kept out of the queue and recommendations. */
    fun blocksSongBy(artists: List<Pair<String?, String?>>): Boolean =
        (artistIds.isNotEmpty() || blockAiArtists) && artists.any { (id, name) -> isArtistBlocked(id, name) }

    private fun filteredOut(
        isEpisode: Boolean,
        musicVideoType: String?,
        durationSeconds: Int?,
    ): Boolean =
        (hidePodcasts && isEpisode) ||
            (hideNonMusicalVideos && musicVideoType != null && musicVideoType != MUSIC_VIDEO_TYPE_ATV && musicVideoType != MUSIC_VIDEO_TYPE_OMV) ||
            (hideShortSongs && durationSeconds != null && durationSeconds in 1 until SHORT_SONG_SECONDS)

    /** Whether a queued song should be skipped: a blocked artist or a content filter. */
    fun blocks(metadata: MediaMetadata): Boolean =
        filteredOut(metadata.isEpisode, metadata.musicVideoType, metadata.duration) ||
            blocksSongBy(metadata.artists.map { it.id to it.name })

    fun blocks(item: YTItem): Boolean =
        when (item) {
            is SongItem -> filteredOut(item.isEpisode, item.musicVideoType, item.duration) || item.artists.any(::isArtistBlocked)
            is AlbumItem -> item.artists.orEmpty().any(::isArtistBlocked)
            is ArtistItem -> isArtistBlocked(item.id, item.title)
            is PlaylistItem -> false
            is PodcastItem -> hidePodcasts || isArtistBlocked(item.author)
            is EpisodeItem -> hidePodcasts || isArtistBlocked(item.author)
        }

    /** Search results and other lists outside Home: everything blocked, but Home-only hides stay. */
    fun <T : YTItem> filter(items: List<T>): List<T> = if (isEmpty) items else items.filterNot(::blocks)

    /** Whether an item should be left off Home: hidden there, or blocked. */
    fun hidesOnHome(item: YTItem): Boolean = item.id in hiddenIds || blocks(item)

    fun hidesOnHome(
        id: String,
        artists: List<Pair<String?, String?>>,
    ): Boolean = id in hiddenIds || blocksSongBy(artists)

    fun <T : YTItem> filterHome(items: List<T>): List<T> =
        if (isEmpty) items else items.filterNot(::hidesOnHome)
}

private fun String.normalized() = trim().lowercase()

private fun decode(raw: String?): List<BlockedEntry> =
    if (raw.isNullOrBlank()) {
        emptyList()
    } else {
        runCatching { blocklistJson.decodeFromString<List<BlockedEntry>>(raw) }.getOrDefault(emptyList())
    }

private fun Preferences.toBlocklist() =
    ContentBlocklist(
        blockedArtists = decode(this[BlockedArtistsKey]),
        hiddenHomeItems = decode(this[HiddenHomeItemsKey]),
        hidePodcasts = this[HidePodcastsKey] ?: false,
        blockAiArtists = this[BlockAiArtistsKey] ?: false,
    )

/** Recomputed when the settings change, and when the AI list finishes downloading. */
val Context.contentBlocklist: Flow<ContentBlocklist>
    get() =
        combine(dataStore.data.map { it.toBlocklist() }.distinctUntilChanged(), AiArtistBlocklist.version) { list, version ->
            if (list.blockAiArtists) list.copy(aiListVersion = version) else list
        }

/** Current blocklist, read blocking; for one-off checks off the main thread or in callbacks. */
fun Context.currentContentBlocklist(): ContentBlocklist = runBlocking(Dispatchers.IO) { dataStore.data.first().toBlocklist() }

private suspend fun Context.editEntries(
    key: Preferences.Key<String>,
    transform: (List<BlockedEntry>) -> List<BlockedEntry>,
) {
    safeDataStoreEdit { prefs ->
        val updated = transform(decode(prefs[key])).distinctBy { it.id }
        prefs[key] = blocklistJson.encodeToString(updated)
    }
}

suspend fun Context.blockArtist(
    id: String,
    name: String,
) {
    if (id.isBlank()) return
    editEntries(BlockedArtistsKey) { listOf(BlockedEntry(id, name.ifBlank { id })) + it.filterNot { e -> e.id == id } }
}

suspend fun Context.unblockArtist(id: String) = editEntries(BlockedArtistsKey) { it.filterNot { e -> e.id == id } }

suspend fun Context.hideFromHome(
    id: String,
    name: String,
) {
    if (id.isBlank()) return
    editEntries(HiddenHomeItemsKey) { listOf(BlockedEntry(id, name.ifBlank { id })) + it.filterNot { e -> e.id == id } }
}

suspend fun Context.restoreToHome(id: String) = editEntries(HiddenHomeItemsKey) { it.filterNot { e -> e.id == id } }
