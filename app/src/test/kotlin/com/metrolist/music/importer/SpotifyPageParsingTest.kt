/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpotifyPageParsingTest {
    @Test
    fun `parses tracks and skips episodes and removed tracks`() {
        val body =
            """
            {
              "items": [
                {"track": {"name": "Yellow", "type": "track", "duration_ms": 266773,
                           "artists": [{"name": "Coldplay"}], "album": {"name": "Parachutes"}, "popularity": 80}},
                {"track": {"name": "Some Podcast", "type": "episode", "duration_ms": 1000}},
                {"track": null},
                {"item": {"name": "Hello", "type": "track", "duration_ms": 295000, "artists": [{"name": "Adele"}, {"name": null}]}}
              ],
              "next": "https://api.spotify.com/v1/playlists/x/tracks?offset=100&limit=100",
              "total": 240
            }
            """.trimIndent()
        val page = SpotifyApi.parseTracksPage(body)
        assertEquals(listOf("Yellow", "Hello"), page.tracks.map { it.title })
        assertEquals(listOf("Coldplay"), page.tracks[0].artists)
        assertEquals("Parachutes", page.tracks[0].album)
        assertEquals(266_773L, page.tracks[0].durationMs)
        assertEquals(listOf("Adele"), page.tracks[1].artists)
        assertEquals(2, page.skipped)
        assertEquals(240, page.total)
        assertEquals("https://api.spotify.com/v1/playlists/x/tracks?offset=100&limit=100", page.next)
    }

    @Test
    fun `last page has no next`() {
        val page = SpotifyApi.parseTracksPage("""{"items": [], "next": null, "total": 0}""")
        assertNull(page.next)
        assertEquals(0, page.tracks.size)
    }
}
