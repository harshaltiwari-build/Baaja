/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.importer

import java.net.URLDecoder

/** Pulls a playlist id out of whatever the user pasted from YouTube or YouTube Music. Pure JVM, no Android types. */
object YouTubePlaylistLinkParser {
    sealed interface Result {
        /** [id] never carries the "VL" browse prefix. */
        data class Playlist(val id: String) : Result

        /** A YouTube link that carries no importable playlist (a single video, a channel, an auto-generated mix). */
        data object NotAPlaylist : Result

        data object Invalid : Result
    }

    private val hosts = setOf("youtube.com", "www.youtube.com", "m.youtube.com", "music.youtube.com", "youtu.be", "www.youtu.be")
    private val idChars = Regex("^[A-Za-z0-9_-]{12,80}$")
    private val urlStart = Regex("^(?:https?://)?(?:[A-Za-z0-9-]+\\.)+[A-Za-z]{2,}(?:[/?#:]|$)", RegexOption.IGNORE_CASE)

    // Real playlists: user playlists (PL), album playlists (OLAK5uy_), curated mixes (RDCLAK), uploads (UU).
    private val importablePrefixes = listOf("PL", "OLAK5uy_", "RDCLAK", "UU")

    fun parse(input: String): Result {
        val trimmed = input.trim().trim('<', '>', '"', '\'')
        if (trimmed.isEmpty()) return Result.Invalid
        // Shared text ("Check this out https://...") works too: try each token, first hit wins.
        val tokens = trimmed.split(Regex("\\s+")).map { it.trim('<', '>', '"', '\'', ',', ')', '(') }.filter { it.isNotEmpty() }
        var sawYouTube = false
        for (token in tokens) {
            when (val result = parseToken(token)) {
                is Result.Playlist -> return result
                Result.NotAPlaylist -> sawYouTube = true
                Result.Invalid -> Unit
            }
        }
        return if (sawYouTube) Result.NotAPlaylist else Result.Invalid
    }

    fun playlistId(input: String): String? = (parse(input) as? Result.Playlist)?.id

    /** Strips the "VL" browse prefix and returns the id only when it looks like an importable playlist, else null. */
    fun normalizeId(raw: String): String? {
        var id = raw.trim()
        if (id.startsWith("VL") && importablePrefixes.any { id.removePrefix("VL").startsWith(it) }) id = id.removePrefix("VL")
        if (!idChars.matches(id)) return null
        return id.takeIf { importablePrefixes.any { prefix -> id.startsWith(prefix) } }
    }

    private fun parseToken(token: String): Result {
        if (urlStart.containsMatchIn(token) && token.contains('.')) return parseUrl(token)
        return normalizeId(token)?.let { Result.Playlist(it) } ?: Result.Invalid
    }

    private fun parseUrl(token: String): Result {
        val withoutScheme = token.substringAfter("://", token)
        val authority = withoutScheme.substringBefore('/').substringBefore('?').substringBefore('#')
        val host = authority.substringAfterLast('@').substringBefore(':').lowercase()
        if (host !in hosts) return Result.Invalid
        val query = withoutScheme.substringAfter('?', "").substringBefore('#')
        val list =
            query
                .split('&')
                .firstNotNullOfOrNull { pair ->
                    val key = pair.substringBefore('=')
                    if (key == "list") decode(pair.substringAfter('=', "")) else null
                }
        if (list.isNullOrBlank()) return Result.NotAPlaylist
        return normalizeId(list)?.let { Result.Playlist(it) } ?: Result.NotAPlaylist
    }

    private fun decode(value: String): String = runCatching { URLDecoder.decode(value, "UTF-8") }.getOrDefault(value)
}
