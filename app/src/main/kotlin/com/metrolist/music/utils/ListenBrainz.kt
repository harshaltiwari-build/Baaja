/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import com.metrolist.music.BuildConfig
import com.metrolist.music.models.MediaMetadata
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import timber.log.Timber

/** Submits listens to ListenBrainz (https://listenbrainz.org) with the user's own token. */
object ListenBrainz {
    private const val SUBMIT_URL = "https://api.listenbrainz.org/1/submit-listens"

    private val client by lazy {
        HttpClient(OkHttp) {
            install(HttpTimeout) {
                requestTimeoutMillis = 15_000
                connectTimeoutMillis = 10_000
            }
            expectSuccess = false
        }
    }

    suspend fun playingNow(token: String, metadata: MediaMetadata) =
        submit(token, "playing_now", listen(metadata, listenedAt = null))

    suspend fun listened(token: String, metadata: MediaMetadata, listenedAtSeconds: Long) =
        submit(token, "single", listen(metadata, listenedAtSeconds))

    private suspend fun submit(token: String, type: String, listen: JsonObject) {
        val body = JsonObject(mapOf("listen_type" to kotlinx.serialization.json.JsonPrimitive(type), "payload" to JsonArray(listOf(listen))))
        runCatching {
            val response =
                client.post(SUBMIT_URL) {
                    header("Authorization", "Token $token")
                    contentType(ContentType.Application.Json)
                    setBody(body.toString())
                }
            if (!response.status.isSuccess()) Timber.tag(TAG).w("Submit $type failed: ${response.status}")
        }.onFailure { Timber.tag(TAG).w(it, "Submit $type failed") }
    }

    private fun listen(metadata: MediaMetadata, listenedAt: Long?): JsonObject =
        buildJsonObject {
            listenedAt?.let { put("listened_at", it) }
            putJsonObject("track_metadata") {
                put("artist_name", metadata.artists.joinToString { it.name })
                put("track_name", metadata.title)
                metadata.album?.title?.let { put("release_name", it) }
                putJsonObject("additional_info") {
                    if (metadata.duration > 0) put("duration_ms", metadata.duration * 1000L)
                    put("origin_url", "https://music.youtube.com/watch?v=${metadata.id}")
                    put("submission_client", "Baaja")
                    put("submission_client_version", BuildConfig.VERSION_NAME)
                    put("media_player", "Baaja")
                    put("music_service", "music.youtube.com")
                }
            }
        }

    private const val TAG = "ListenBrainz"
}
