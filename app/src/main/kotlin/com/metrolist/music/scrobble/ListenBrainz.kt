package com.metrolist.music.scrobble

import org.json.JSONArray
import org.json.JSONObject

/** Minimal ListenBrainz client using a user token from listenbrainz.org/settings. */
internal object ListenBrainz {
    private const val API_URL = "https://api.listenbrainz.org/1"
    const val MAX_BATCH = 50

    /** Returns the username the token belongs to, or throws if it is invalid. */
    fun validateToken(token: String): String {
        val text = ScrobbleHttp.request("$API_URL/validate-token", "GET", null, null, auth(token))
        val json = JSONObject(text)
        if (!json.optBoolean("valid")) throw ScrobbleHttpException(401, json.optString("message", "Invalid token"))
        return json.optString("user_name")
    }

    fun playingNow(
        token: String,
        track: LastFm.Track,
    ) = submit(token, "playing_now", listOf(track))

    fun listens(
        token: String,
        tracks: List<LastFm.Track>,
    ) = submit(token, if (tracks.size == 1) "single" else "import", tracks)

    private fun submit(
        token: String,
        type: String,
        tracks: List<LastFm.Track>,
    ) {
        val payload = JSONArray()
        tracks.forEach { track ->
            val metadata =
                JSONObject()
                    .put("artist_name", track.artist)
                    .put("track_name", track.title)
            track.album?.takeIf { it.isNotBlank() }?.let { metadata.put("release_name", it) }
            val info = JSONObject().put("media_player", "Baaja").put("submission_client", "Baaja")
            if (track.durationSec > 0) info.put("duration_ms", track.durationSec * 1000L)
            metadata.put("additional_info", info)
            val listen = JSONObject().put("track_metadata", metadata)
            if (type != "playing_now") listen.put("listened_at", track.timestampSec)
            payload.put(listen)
        }
        val body = JSONObject().put("listen_type", type).put("payload", payload).toString()
        ScrobbleHttp.request("$API_URL/submit-listens", "POST", body, "application/json", auth(token))
    }

    private fun auth(token: String) = mapOf("Authorization" to "Token $token")
}
