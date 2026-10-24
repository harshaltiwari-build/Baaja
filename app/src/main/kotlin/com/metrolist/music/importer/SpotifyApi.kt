/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.importer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.Base64
import java.util.concurrent.TimeUnit

/**
 * Minimal client for the official Spotify Web API using the Client Credentials flow with the
 * user's own app keys. Only public playlist data is read; nothing is written to Spotify.
 * Keys and tokens are never logged or put into exception messages.
 */
class SpotifyApi(
    private val client: OkHttpClient = defaultClient,
) {
    enum class Failure { BAD_KEYS, NOT_FOUND, RATE_LIMITED, NETWORK, SERVER }

    class SpotifyException(
        val failure: Failure,
        val httpCode: Int? = null,
        /** Spotify's own explanation (its `message` / `error_description`), never any credentials. */
        val detail: String? = null,
    ) : IOException("Spotify request failed: $failure")

    private var cachedToken: String? = null
    private var cachedTokenOwner: Int = 0
    private var cachedTokenExpiry: Long = 0L

    suspend fun fetchPlaylist(
        clientId: String,
        clientSecret: String,
        playlistId: String,
        onProgress: (loaded: Int, total: Int) -> Unit = { _, _ -> },
    ): ImportedPlaylist =
        withContext(Dispatchers.IO) {
            val token = token(clientId.trim(), clientSecret.trim())
            val name =
                runCatching {
                    val url =
                        "$API/playlists/$playlistId".toHttpUrl().newBuilder()
                            .addQueryParameter("fields", "name")
                            .build()
                            .toString()
                    json.decodeFromString<SpotifyPlaylistName>(get(url, token)).name
                }.getOrElse { if (it is SpotifyException && it.failure != Failure.NOT_FOUND) throw it else null }

            val tracks = ArrayList<ImportTrack>()
            var skipped = 0
            var body: String =
                try {
                    get(firstTracksPage(playlistId, "tracks"), token)
                } catch (e: SpotifyException) {
                    // Newer API versions expose playlist contents at /items instead of /tracks.
                    if (e.failure == Failure.NOT_FOUND) get(firstTracksPage(playlistId, "items"), token) else throw e
                }
            var pages = 0
            while (true) {
                val page = parseTracksPage(body)
                tracks += page.tracks
                skipped += page.skipped
                onProgress(tracks.size + skipped, page.total)
                val next = page.next ?: break
                if (++pages >= MAX_PAGES) break
                body = get(next, token)
            }
            ImportedPlaylist(name = name, tracks = tracks, skipped = skipped)
        }

    private fun firstTracksPage(playlistId: String, path: String): String =
        "$API/playlists/$playlistId/$path".toHttpUrl().newBuilder()
            .addQueryParameter("limit", "100")
            .addQueryParameter("offset", "0")
            .addQueryParameter("additional_types", "track")
            .build()
            .toString()

    private suspend fun token(clientId: String, clientSecret: String): String {
        val owner = (clientId + "\u0000" + clientSecret).hashCode()
        val cached = cachedToken
        if (cached != null && owner == cachedTokenOwner && System.currentTimeMillis() < cachedTokenExpiry) return cached

        val basic = Base64.getEncoder().encodeToString("$clientId:$clientSecret".toByteArray(Charsets.UTF_8))
        val request =
            Request.Builder()
                .url(TOKEN_URL)
                .header("Authorization", "Basic $basic")
                .post(FormBody.Builder().add("grant_type", "client_credentials").build())
                .build()
        val body = execute(request)
        val token = json.decodeFromString<SpotifyToken>(body)
        cachedToken = token.accessToken
        cachedTokenOwner = owner
        cachedTokenExpiry = System.currentTimeMillis() + (token.expiresIn - 60).coerceAtLeast(60) * 1000L
        return token.accessToken
    }

    private suspend fun get(url: String, token: String): String =
        execute(Request.Builder().url(url).header("Authorization", "Bearer $token").get().build())

    private suspend fun execute(request: Request): String {
        var attempt = 0
        while (true) {
            val response =
                try {
                    client.newCall(request).execute()
                } catch (e: IOException) {
                    throw SpotifyException(Failure.NETWORK)
                }
            response.use { res ->
                when {
                    res.isSuccessful -> return res.body?.string().orEmpty()
                    res.code == 429 && attempt < 3 -> {
                        val waitSec = res.header("Retry-After")?.toLongOrNull()?.coerceIn(1, 10) ?: 2
                        attempt++
                        delay(waitSec * 1000)
                    }
                    res.code == 429 -> throw SpotifyException(Failure.RATE_LIMITED, 429)
                    res.code == 400 || res.code == 401 -> throw SpotifyException(Failure.BAD_KEYS, res.code, errorDetail(res.body?.string()))
                    res.code == 403 || res.code == 404 || res.code == 410 -> throw SpotifyException(Failure.NOT_FOUND, res.code, errorDetail(res.body?.string()))
                    else -> throw SpotifyException(Failure.SERVER, res.code, errorDetail(res.body?.string()))
                }
            }
        }
    }

    companion object {
        private const val TOKEN_URL = "https://accounts.spotify.com/api/token"
        private const val API = "https://api.spotify.com/v1"
        private const val MAX_PAGES = 100 // 10,000 tracks

        private val defaultClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
        }

        internal val json =
            Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
                explicitNulls = false
            }

        /** Reads the human-readable reason out of a Spotify error body, or null if there is none. */
        internal fun errorDetail(body: String?): String? {
            if (body.isNullOrBlank()) return null
            return runCatching {
                val root = json.parseToJsonElement(body).jsonObject
                val error = root["error"]
                val text =
                    when {
                        error is JsonObject -> error["message"]?.jsonPrimitive?.contentOrNull
                        else -> root["error_description"]?.jsonPrimitive?.contentOrNull ?: error?.jsonPrimitive?.contentOrNull
                    }
                text?.trim()?.takeIf { it.isNotEmpty() }?.take(200)
            }.getOrNull()
        }

        data class TracksPage(
            val tracks: List<ImportTrack>,
            val skipped: Int,
            val next: String?,
            val total: Int,
        )

        /** Parses one page of playlist items (either the `track` or the newer `item` field). */
        fun parseTracksPage(body: String): TracksPage {
            val page = json.decodeFromString<SpotifyPage>(body)
            var skipped = 0
            val tracks =
                page.items.mapNotNull { entry ->
                    val track = entry.track ?: entry.item
                    val title = track?.name?.trim().orEmpty()
                    if (track == null || title.isEmpty() || (track.type != null && track.type != "track")) {
                        skipped++
                        return@mapNotNull null
                    }
                    ImportTrack(
                        title = title,
                        artists = track.artists.mapNotNull { it.name?.trim()?.takeIf(String::isNotEmpty) },
                        album = track.album?.name?.takeIf { it.isNotBlank() },
                        durationMs = track.durationMs?.takeIf { it > 0 },
                    )
                }
            return TracksPage(tracks, skipped, page.next, page.total)
        }
    }
}

@Serializable
private data class SpotifyToken(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Long = 3600,
)

@Serializable
private data class SpotifyPlaylistName(val name: String? = null)

@Serializable
internal data class SpotifyPage(
    val items: List<SpotifyPlaylistEntry> = emptyList(),
    val next: String? = null,
    val total: Int = 0,
)

@Serializable
internal data class SpotifyPlaylistEntry(
    val track: SpotifyTrack? = null,
    val item: SpotifyTrack? = null,
)

@Serializable
internal data class SpotifyTrack(
    val name: String? = null,
    val type: String? = null,
    @SerialName("duration_ms") val durationMs: Long? = null,
    val artists: List<SpotifyNamed> = emptyList(),
    val album: SpotifyNamed? = null,
)

@Serializable
internal data class SpotifyNamed(val name: String? = null)
