/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.importer

import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Reads public playlists from other services without an account (Spotify's embed page, Deezer's
 * public API, Apple Music's web page) and matches each track on YouTube Music.
 */
object ExternalPlaylistImporter {
    enum class Service(val label: String) { SPOTIFY("Spotify"), DEEZER("Deezer"), APPLE_MUSIC("Apple Music") }

    data class Link(val service: Service, val kind: String, val id: String, val url: String)

    data class Track(val title: String, val artists: String)

    data class Playlist(
        val service: Service,
        val title: String,
        val author: String?,
        val thumbnail: String?,
        val tracks: List<Track>,
    )

    class NotFoundException : Exception()

    private val json = Json { ignoreUnknownKeys = true }
    private val client =
        OkHttpClient
            .Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

    private val spotifyRegex = Regex("""open\.spotify\.com/(?:intl-[a-z-]+/)?(playlist|album)/([A-Za-z0-9]+)""")
    private val deezerRegex = Regex("""deezer\.com/(?:[a-z]{2}/)?(playlist|album)/(\d+)""")
    private val appleRegex = Regex("""music\.apple\.com/[a-z]{2}/(playlist|album)/[^/]+/((?:pl\.)?[A-Za-z0-9.-]+)""")

    fun parse(text: String): Link? {
        val input = text.trim()
        spotifyRegex.find(input)?.let { m ->
            val (kind, id) = m.destructured
            return Link(Service.SPOTIFY, kind, id, "https://open.spotify.com/embed/$kind/$id")
        }
        deezerRegex.find(input)?.let { m ->
            val (kind, id) = m.destructured
            return Link(Service.DEEZER, kind, id, "https://api.deezer.com/$kind/$id")
        }
        appleRegex.find(input)?.let { m ->
            val (kind, id) = m.destructured
            val url = input.substringBefore('?').let { if (it.startsWith("http")) it else "https://$it" }
            return Link(Service.APPLE_MUSIC, kind, id, url)
        }
        return null
    }

    /** Fetches the playlist's name and track list. Throws [NotFoundException] or [IOException]. */
    suspend fun fetch(link: Link): Playlist =
        when (link.service) {
            Service.SPOTIFY -> fetchSpotify(link)
            Service.DEEZER -> fetchDeezer(link)
            Service.APPLE_MUSIC -> fetchApple(link)
        }.also { if (it.tracks.isEmpty()) throw NotFoundException() }

    /** Best YouTube Music song for each track, in order; tracks with no match are left out. */
    suspend fun match(
        tracks: List<Track>,
        onProgress: (done: Int) -> Unit,
    ): List<SongItem> {
        val songs = LinkedHashMap<String, SongItem>()
        tracks.forEachIndexed { index, track ->
            currentCoroutineContext().ensureActive()
            val query = "${track.title} ${track.artists}".trim()
            YouTube
                .search(query, YouTube.SearchFilter.FILTER_SONG)
                .getOrNull()
                ?.items
                ?.filterIsInstance<SongItem>()
                ?.firstOrNull()
                ?.let { songs.putIfAbsent(it.id, it) }
            onProgress(index + 1)
        }
        return songs.values.toList()
    }

    private fun get(url: String): String {
        val request =
            Request
                .Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/131 Mobile Safari/537.36")
                .build()
        client.newCall(request).execute().use { response ->
            if (response.code == 404) throw NotFoundException()
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            return response.body.string()
        }
    }

    private fun scriptJson(html: String, id: String): JsonElement? =
        Regex("""<script[^>]*id="?${Regex.escape(id)}"?[^>]*>(.*?)</script>""", RegexOption.DOT_MATCHES_ALL)
            .find(html)
            ?.groupValues
            ?.get(1)
            ?.let { runCatching { json.parseToJsonElement(it) }.getOrNull() }

    private fun JsonElement?.obj(key: String): JsonElement? = (this as? JsonObject)?.get(key)

    private fun JsonElement?.str(key: String): String? = (obj(key) as? JsonPrimitive)?.contentOrNull

    private fun fetchSpotify(link: Link): Playlist {
        val html = get(link.url)
        val entity =
            scriptJson(html, "__NEXT_DATA__")
                .obj("props")
                .obj("pageProps")
                .obj("state")
                .obj("data")
                .obj("entity") ?: throw NotFoundException()
        val tracks =
            (entity.obj("trackList") as? JsonArray).orEmpty().mapNotNull { track ->
                val title = track.str("title") ?: return@mapNotNull null
                // Spotify joins artists with a comma and a non-breaking space.
                Track(title, track.str("subtitle").orEmpty().replace(' ', ' '))
            }
        val cover = (entity.obj("coverArt").obj("sources") as? JsonArray)?.firstOrNull().str("url")
        return Playlist(Service.SPOTIFY, entity.str("name") ?: entity.str("title").orEmpty(), entity.str("subtitle"), cover, tracks)
    }

    private fun fetchDeezer(link: Link): Playlist {
        val root = json.parseToJsonElement(get(link.url)).jsonObject
        if (root["error"] != null) throw NotFoundException()
        val tracks = mutableListOf<Track>()
        var page: JsonElement? = root["tracks"]
        var pages = 0
        while (page != null && pages++ < 50) {
            page.obj("data")?.jsonArray?.forEach { track ->
                val title = track.str("title") ?: return@forEach
                tracks += Track(title, track.obj("artist").str("name").orEmpty())
            }
            page = page.str("next")?.let { json.parseToJsonElement(get(it)) }
        }
        val author = root.obj("creator").str("name") ?: root.obj("artist").str("name")
        val cover = root.str("picture_xl") ?: root.str("cover_xl")
        return Playlist(Service.DEEZER, root.str("title").orEmpty(), author, cover, tracks)
    }

    private fun fetchApple(link: Link): Playlist {
        val html = get(link.url)
        val schema = scriptJson(html, "schema:music-${if (link.kind == "album") "album" else "playlist"}")
        val tracks = mutableListOf<Track>()
        scriptJson(html, "serialized-server-data")?.let { collectAppleTracks(it, tracks) }
        val author = schema.obj("author").str("name") ?: schema.obj("byArtist").let { (it as? JsonArray)?.firstOrNull() ?: it }.str("name")
        val cover = schema.str("image") ?: schema.obj("image").str("url")
        return Playlist(Service.APPLE_MUSIC, schema.str("name").orEmpty(), author, cover, tracks.distinct())
    }

    /** Apple's page data nests each track as an object with a `title` and an `artistName`. */
    private fun collectAppleTracks(element: JsonElement, into: MutableList<Track>) {
        when (element) {
            is JsonObject -> {
                val title = element.str("title")
                val artist = element.str("artistName")
                if (title != null && artist != null) {
                    into += Track(title, artist)
                } else {
                    element.values.forEach { collectAppleTracks(it, into) }
                }
            }
            is JsonArray -> element.forEach { collectAppleTracks(it, into) }
            else -> Unit
        }
    }
}
