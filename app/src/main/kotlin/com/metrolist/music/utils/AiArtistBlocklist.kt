/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Community list of YouTube channels that publish AI-generated music (AiSList, CC BY-NC 4.0).
 * Downloaded at most once a day and kept in a file, so it also works offline.
 */
object AiArtistBlocklist {
    const val SOURCE_PAGE = "https://github.com/Override92/AiSList"
    private const val SOURCE_URL = "https://raw.githubusercontent.com/Override92/AiSList/main/AiSList/aislist_blocklist.txt"
    private const val FILE_NAME = "ai_artist_blocklist.txt"
    private const val REFRESH_INTERVAL_MS = 24L * 60 * 60 * 1000
    private const val MAX_CHARACTERS = 8 * 1024 * 1024
    private const val TAG = "AiArtistBlocklist"

    private val channelIdPattern = Regex("UC[A-Za-z0-9_-]{20,24}")
    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build()

    @Volatile
    private var entries: Set<String> = emptySet()

    private val _version = MutableStateFlow(0)

    /** Bumps whenever the list changes, so filters built from it can be recomputed. */
    val version: StateFlow<Int> = _version.asStateFlow()

    val size: Int get() = entries.size

    fun matches(
        channelId: String?,
        name: String?,
    ): Boolean {
        val current = entries
        if (current.isEmpty()) return false
        return channelId?.lowercase() in current || name?.trim()?.lowercase()?.let { "@$it" } in current
    }

    /** Loads the cached copy, then downloads a fresh one if the cache is missing or a day old. */
    suspend fun refresh(context: Context) =
        withContext(Dispatchers.IO) {
            val file = File(context.filesDir, FILE_NAME)
            if (entries.isEmpty() && file.exists()) publish(parse(file.readText()))
            if (file.exists() && System.currentTimeMillis() - file.lastModified() < REFRESH_INTERVAL_MS) return@withContext
            try {
                val body =
                    client.newCall(Request.Builder().url(SOURCE_URL).build()).execute().use { response ->
                        check(response.isSuccessful) { "HTTP ${response.code}" }
                        response.body.string()
                    }
                check(body.length <= MAX_CHARACTERS) { "List too large" }
                val parsed = parse(body)
                check(parsed.isNotEmpty()) { "List is empty" }
                file.writeText(body)
                publish(parsed)
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Refreshing the AI artist list failed; keeping %d cached entries", entries.size)
            }
        }

    private fun publish(parsed: Set<String>) {
        entries = parsed
        _version.value++
    }

    private fun parse(text: String): Set<String> =
        text
            .lineSequence()
            .map(String::trim)
            .filter { it.startsWith("@") || it.matches(channelIdPattern) }
            .mapTo(HashSet()) { it.lowercase() }
}
