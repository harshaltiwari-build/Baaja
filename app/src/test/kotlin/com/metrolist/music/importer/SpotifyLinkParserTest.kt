/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.importer

import org.junit.Assert.assertEquals
import org.junit.Test

class SpotifyLinkParserTest {
    private val id = "37i9dQZF1DXcBWIGoYBM5M"

    private fun playlist(input: String) = SpotifyLinkParser.parse(input)

    @Test
    fun `plain open spotify link`() {
        assertEquals(SpotifyLinkParser.Result.Playlist(id), playlist("https://open.spotify.com/playlist/$id"))
    }

    @Test
    fun `link with share query and whitespace`() {
        assertEquals(
            SpotifyLinkParser.Result.Playlist(id),
            playlist("  https://open.spotify.com/playlist/$id?si=a1b2c3d4e5f6&pi=xyz \n"),
        )
    }

    @Test
    fun `localized intl path and embed path`() {
        assertEquals(SpotifyLinkParser.Result.Playlist(id), playlist("https://open.spotify.com/intl-de/playlist/$id"))
        assertEquals(SpotifyLinkParser.Result.Playlist(id), playlist("https://open.spotify.com/embed/playlist/$id?utm_source=x"))
    }

    @Test
    fun `old user playlist link`() {
        assertEquals(SpotifyLinkParser.Result.Playlist(id), playlist("https://open.spotify.com/user/someone/playlist/$id"))
    }

    @Test
    fun `spotify uris`() {
        assertEquals(SpotifyLinkParser.Result.Playlist(id), playlist("spotify:playlist:$id"))
        assertEquals(SpotifyLinkParser.Result.Playlist(id), playlist("spotify:user:someone:playlist:$id"))
    }

    @Test
    fun `bare id and link inside share text`() {
        assertEquals(SpotifyLinkParser.Result.Playlist(id), playlist(id))
        assertEquals(
            SpotifyLinkParser.Result.Playlist(id),
            playlist("Listen to Today's Hits on Spotify: https://open.spotify.com/playlist/$id?si=abc"),
        )
    }

    @Test
    fun `short links are reported separately`() {
        assertEquals(SpotifyLinkParser.Result.ShortLink, playlist("https://spotify.link/AbCdEf123"))
        assertEquals(SpotifyLinkParser.Result.ShortLink, playlist("https://spoti.fi/3xyz"))
    }

    @Test
    fun `album and track links are not playlists`() {
        assertEquals(SpotifyLinkParser.Result.NotAPlaylist, playlist("https://open.spotify.com/album/$id"))
        assertEquals(SpotifyLinkParser.Result.NotAPlaylist, playlist("https://open.spotify.com/track/$id?si=1"))
        assertEquals(SpotifyLinkParser.Result.NotAPlaylist, playlist("spotify:track:$id"))
    }

    @Test
    fun `garbage and wrong length ids are invalid`() {
        assertEquals(SpotifyLinkParser.Result.Invalid, playlist(""))
        assertEquals(SpotifyLinkParser.Result.Invalid, playlist("hello world"))
        assertEquals(SpotifyLinkParser.Result.Invalid, playlist("https://open.spotify.com/playlist/tooShort"))
        assertEquals(SpotifyLinkParser.Result.Invalid, playlist("https://example.com/playlist/$id"))
        assertEquals(null, SpotifyLinkParser.playlistId("https://open.spotify.com/playlist/${id}X"))
    }
}
