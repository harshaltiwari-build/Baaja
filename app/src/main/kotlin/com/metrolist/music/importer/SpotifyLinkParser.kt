/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.importer

/** Pulls a playlist id out of whatever the user pasted from Spotify. */
object SpotifyLinkParser {
    sealed interface Result {
        data class Playlist(val id: String) : Result

        /** spotify.link / spoti.fi short links hide the id behind a redirect we don't follow. */
        data object ShortLink : Result

        /** A Spotify link to something other than a playlist (album, track, artist…). */
        data object NotAPlaylist : Result

        data object Invalid : Result
    }

    private val bareId = Regex("^[A-Za-z0-9]{22}$")
    private val uri = Regex("spotify:(?:user:[^:\\s]+:)?playlist:([A-Za-z0-9]{22})(?![A-Za-z0-9])")
    private val url =
        Regex(
            "(?:open|play)\\.spotify\\.com/(?:[^\\s?#]*/)?playlist/([A-Za-z0-9]{22})(?![A-Za-z0-9])",
            RegexOption.IGNORE_CASE,
        )
    private val otherSpotify =
        Regex("(?:open\\.spotify\\.com/|spotify:)(?:[^\\s?#]*/)?(album|track|artist|show|episode|user)\\b", RegexOption.IGNORE_CASE)
    private val shortLink = Regex("(?:spotify\\.link|spoti\\.fi)/", RegexOption.IGNORE_CASE)

    fun parse(input: String): Result {
        val text = input.trim().trim('<', '>', '"', '\'')
        if (text.isEmpty()) return Result.Invalid
        if (bareId.matches(text)) return Result.Playlist(text)
        uri.find(text)?.let { return Result.Playlist(it.groupValues[1]) }
        url.find(text)?.let { return Result.Playlist(it.groupValues[1]) }
        if (shortLink.containsMatchIn(text)) return Result.ShortLink
        if (otherSpotify.containsMatchIn(text)) return Result.NotAPlaylist
        return Result.Invalid
    }

    fun playlistId(input: String): String? = (parse(input) as? Result.Playlist)?.id
}
