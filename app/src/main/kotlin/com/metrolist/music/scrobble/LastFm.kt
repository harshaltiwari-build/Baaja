package com.metrolist.music.scrobble

import com.metrolist.music.BuildConfig
import org.json.JSONObject
import java.security.MessageDigest

/** Minimal Last.fm client: mobile sign-in, now playing and batched scrobbles. */
internal object LastFm {
    private const val API_URL = "https://ws.audioscrobbler.com/2.0/"
    const val MAX_BATCH = 50

    val isConfigured: Boolean
        get() = BuildConfig.LASTFM_API_KEY.isNotBlank() && BuildConfig.LASTFM_SECRET.isNotBlank()

    data class Session(
        val username: String,
        val key: String,
    )

    data class Track(
        val artist: String,
        val title: String,
        val album: String?,
        val durationSec: Int,
        val timestampSec: Long,
    )

    /** Last.fm error codes that mean the stored session is no longer valid. */
    class SessionExpiredException : Exception("Last.fm session expired")

    fun getMobileSession(
        username: String,
        password: String,
    ): Session {
        val json =
            call(
                mapOf(
                    "method" to "auth.getMobileSession",
                    "username" to username,
                    "password" to password,
                ),
            )
        val session = json.getJSONObject("session")
        return Session(session.getString("name"), session.getString("key"))
    }

    fun updateNowPlaying(
        sessionKey: String,
        track: Track,
    ) {
        val params =
            mutableMapOf(
                "method" to "track.updateNowPlaying",
                "sk" to sessionKey,
                "artist" to track.artist,
                "track" to track.title,
            )
        track.album?.takeIf { it.isNotBlank() }?.let { params["album"] = it }
        if (track.durationSec > 0) params["duration"] = track.durationSec.toString()
        call(params)
    }

    fun scrobble(
        sessionKey: String,
        tracks: List<Track>,
    ) {
        require(tracks.size in 1..MAX_BATCH)
        val params = mutableMapOf("method" to "track.scrobble", "sk" to sessionKey)
        tracks.forEachIndexed { i, track ->
            params["artist[$i]"] = track.artist
            params["track[$i]"] = track.title
            params["timestamp[$i]"] = track.timestampSec.toString()
            params["chosenByUser[$i]"] = "1"
            track.album?.takeIf { it.isNotBlank() }?.let { params["album[$i]"] = it }
            if (track.durationSec > 0) params["duration[$i]"] = track.durationSec.toString()
        }
        call(params)
    }

    private fun call(params: Map<String, String>): JSONObject {
        val signed = params + ("api_key" to BuildConfig.LASTFM_API_KEY)
        val body = signed + mapOf("api_sig" to sign(signed), "format" to "json")
        val text =
            try {
                ScrobbleHttp.postForm(API_URL, body)
            } catch (e: ScrobbleHttpException) {
                throw errorFrom(e.message.orEmpty()) ?: e
            }
        val json = JSONObject(text)
        errorFrom(text)?.let { throw it }
        return json
    }

    private fun errorFrom(text: String): Exception? {
        val json = runCatching { JSONObject(text) }.getOrNull() ?: return null
        if (!json.has("error")) return null
        val code = json.optInt("error")
        // 4: auth failed, 9: invalid session key, 14/15: token problems
        if (code == 9) return SessionExpiredException()
        val retryable = code == 11 || code == 16 || code == 29
        return ScrobbleHttpException(if (retryable) 503 else 400, json.optString("message", "Last.fm error $code"))
    }

    private fun sign(params: Map<String, String>): String {
        val raw =
            params.toSortedMap().entries.joinToString("") { (k, v) -> k + v } +
                BuildConfig.LASTFM_SECRET
        return MessageDigest
            .getInstance("MD5")
            .digest(raw.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
