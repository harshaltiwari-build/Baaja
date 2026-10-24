/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YouTubePlaylistLinkParserTest {
    private val pl = "PLrAXtmErZgOeiKm4sgNOknGvNjby9efdf"
    private val album = "OLAK5uy_kXq3Zk2dN3yY1vF0mQ6hP9aBcDe"
    private val curated = "RDCLAK5uy_lJ1xAbCdEfGhIjKlMnOpQrStUvWx"

    private fun parse(input: String) = YouTubePlaylistLinkParser.parse(input)

    @Test
    fun `music youtube playlist link`() {
        assertEquals(YouTubePlaylistLinkParser.Result.Playlist(pl), parse("https://music.youtube.com/playlist?list=$pl"))
    }

    @Test
    fun `youtube playlist link with share query and whitespace`() {
        assertEquals(
            YouTubePlaylistLinkParser.Result.Playlist(pl),
            parse("  https://www.youtube.com/playlist?list=$pl&si=AbCdEf \n"),
        )
        assertEquals(YouTubePlaylistLinkParser.Result.Playlist(pl), parse("https://m.youtube.com/playlist?si=x&list=$pl"))
    }

    @Test
    fun `link without scheme`() {
        assertEquals(YouTubePlaylistLinkParser.Result.Playlist(pl), parse("music.youtube.com/playlist?list=$pl"))
    }

    @Test
    fun `watch and short links with a list`() {
        assertEquals(YouTubePlaylistLinkParser.Result.Playlist(pl), parse("https://www.youtube.com/watch?v=dQw4w9WgXcQ&list=$pl&index=3"))
        assertEquals(YouTubePlaylistLinkParser.Result.Playlist(pl), parse("https://music.youtube.com/watch?v=dQw4w9WgXcQ&list=$pl"))
        assertEquals(YouTubePlaylistLinkParser.Result.Playlist(pl), parse("https://youtu.be/dQw4w9WgXcQ?list=$pl&si=zz"))
    }

    @Test
    fun `bare ids of every kind`() {
        assertEquals(YouTubePlaylistLinkParser.Result.Playlist(pl), parse(pl))
        assertEquals(YouTubePlaylistLinkParser.Result.Playlist(album), parse(album))
        assertEquals(YouTubePlaylistLinkParser.Result.Playlist(curated), parse(curated))
        assertEquals(YouTubePlaylistLinkParser.Result.Playlist(pl), parse("  $pl\n"))
    }

    @Test
    fun `VL prefix is stripped`() {
        assertEquals(YouTubePlaylistLinkParser.Result.Playlist(pl), parse("VL$pl"))
        assertEquals(YouTubePlaylistLinkParser.Result.Playlist(album), parse("VL$album"))
        assertEquals(YouTubePlaylistLinkParser.Result.Playlist(pl), parse("https://music.youtube.com/playlist?list=VL$pl"))
        assertEquals(pl, YouTubePlaylistLinkParser.normalizeId("VL$pl"))
    }

    @Test
    fun `link inside share text`() {
        assertEquals(
            YouTubePlaylistLinkParser.Result.Playlist(pl),
            parse("Check out my playlist https://music.youtube.com/playlist?list=$pl&si=abc, it is great"),
        )
    }

    @Test
    fun `percent encoded list param`() {
        assertEquals(YouTubePlaylistLinkParser.Result.Playlist(pl), parse("https://www.youtube.com/playlist?list=%50%4C${pl.drop(2)}"))
    }

    @Test
    fun `youtube links without a playlist`() {
        assertEquals(YouTubePlaylistLinkParser.Result.NotAPlaylist, parse("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertEquals(YouTubePlaylistLinkParser.Result.NotAPlaylist, parse("https://youtu.be/dQw4w9WgXcQ"))
        assertEquals(YouTubePlaylistLinkParser.Result.NotAPlaylist, parse("https://www.youtube.com/playlist?list="))
    }

    @Test
    fun `auto generated mixes are not importable`() {
        assertEquals(
            YouTubePlaylistLinkParser.Result.NotAPlaylist,
            parse("https://www.youtube.com/watch?v=dQw4w9WgXcQ&list=RDdQw4w9WgXcQ"),
        )
        assertNull(YouTubePlaylistLinkParser.playlistId("RDdQw4w9WgXcQ"))
    }

    @Test
    fun `garbage is rejected`() {
        listOf(
            "",
            "   ",
            "hello world",
            "PL",
            "PL123",
            "https://example.com/playlist?list=$pl",
            "https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M",
            "https://notyoutube.com/playlist?list=$pl",
            "javascript:alert(1)",
            "PL$pl!!",
            "ftp://",
        ).forEach { input ->
            assertNull("should reject '$input'", YouTubePlaylistLinkParser.playlistId(input))
        }
        assertEquals(YouTubePlaylistLinkParser.Result.Invalid, parse("not a link"))
    }

    @Test
    fun `lookalike host is rejected`() {
        assertEquals(YouTubePlaylistLinkParser.Result.Invalid, parse("https://youtube.com.evil.io/playlist?list=$pl"))
        assertEquals(YouTubePlaylistLinkParser.Result.Invalid, parse("https://evil.io/?u=youtube.com&list=$pl"))
    }
}
