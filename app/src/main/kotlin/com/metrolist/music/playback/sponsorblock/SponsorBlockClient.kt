/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback.sponsorblock

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.MessageDigest

/** A part of a video to skip, in milliseconds. */
data class SkipSegment(
    val category: String,
    val startMs: Long,
    val endMs: Long,
)

/** Reads crowd-sourced skip segments from the public SponsorBlock API. */
object SponsorBlockClient {
    private const val API_URL = "https://sponsor.ajay.app/api/skipSegments"

    /** Categories skipped automatically: paid promotion, self-promotion, "like and subscribe" and non-music parts of music videos. */
    val DEFAULT_CATEGORIES = listOf("sponsor", "selfpromo", "interaction", "music_offtopic")

    private val client by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(HttpTimeout) {
                requestTimeoutMillis = 10_000
                connectTimeoutMillis = 8_000
            }
            expectSuccess = false
        }
    }

    /**
     * Looks segments up by the first four characters of the video id's SHA-256 hash, so the server
     * never learns which exact video is playing; matches are filtered locally.
     */
    suspend fun segments(videoId: String, categories: List<String> = DEFAULT_CATEGORIES): List<SkipSegment> {
        val hashPrefix = sha256(videoId).take(4)
        val response =
            client.get("$API_URL/$hashPrefix") {
                parameter("categories", categories.joinToString(",", "[", "]") { "\"$it\"" })
                parameter("actionTypes", "[\"skip\"]")
            }
        if (response.status == HttpStatusCode.NotFound || response.status != HttpStatusCode.OK) return emptyList()

        return response.body<List<HashedVideo>>()
            .firstOrNull { it.videoID == videoId }
            ?.segments
            .orEmpty()
            .mapNotNull { it.toSkipSegment() }
            .sortedBy { it.startMs }
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }

    @Serializable
    private data class HashedVideo(
        val videoID: String,
        val segments: List<ApiSegment> = emptyList(),
    )

    @Serializable
    private data class ApiSegment(
        val category: String,
        val segment: List<Double>,
        val actionType: String = "skip",
    ) {
        fun toSkipSegment(): SkipSegment? {
            if (actionType != "skip" || segment.size != 2) return null
            val start = (segment[0] * 1000).toLong()
            val end = (segment[1] * 1000).toLong()
            return SkipSegment(category, start, end).takeIf { end - start >= 1_000 }
        }
    }
}
