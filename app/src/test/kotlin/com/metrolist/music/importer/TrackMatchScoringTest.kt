/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackMatchScoringTest {
    private val getLucky =
        ImportTrack(
            title = "Get Lucky (feat. Pharrell Williams and Nile Rodgers) - Radio Edit",
            artists = listOf("Daft Punk", "Pharrell Williams", "Nile Rodgers"),
            durationMs = 248_000,
        )

    @Test
    fun `core title drops qualifiers and featuring credits`() {
        assertEquals("get lucky", TrackMatchScoring.coreTitle(getLucky.title))
        assertEquals("hey jude", TrackMatchScoring.coreTitle("Hey Jude - Remastered 2015"))
        assertEquals("despacito", TrackMatchScoring.coreTitle("Despacito ft. Daddy Yankee"))
        assertEquals("beyonce", TrackMatchScoring.coreTitle("Beyoncé!"))
    }

    @Test
    fun `exact recording scores high`() {
        val score =
            TrackMatchScoring.score(
                getLucky,
                MatchCandidate("Get Lucky (Radio Edit)", listOf("Daft Punk", "Pharrell Williams", "Nile Rodgers"), 248),
            )
        assertTrue("score was $score", score > 0.9)
    }

    @Test
    fun `cover by another artist is rejected`() {
        val track = ImportTrack("Hallelujah", listOf("Leonard Cohen"), durationMs = 279_000)
        val cover = MatchCandidate("Hallelujah", listOf("Pentatonix"), 279)
        assertTrue(TrackMatchScoring.score(track, cover) < TrackMatchScoring.MATCH_THRESHOLD)
    }

    @Test
    fun `different song by same artist is rejected`() {
        val track = ImportTrack("Yellow", listOf("Coldplay"), durationMs = 266_000)
        val other = MatchCandidate("Viva La Vida", listOf("Coldplay"), 242)
        assertTrue(TrackMatchScoring.score(track, other) < TrackMatchScoring.MATCH_THRESHOLD)
    }

    @Test
    fun `live version loses to studio version`() {
        val track = ImportTrack("Yellow", listOf("Coldplay"), durationMs = 266_000)
        val live = MatchCandidate("Yellow (Live)", listOf("Coldplay"), 290)
        val studio = MatchCandidate("Yellow", listOf("Coldplay"), 267)
        val best = TrackMatchScoring.best(track, listOf(live, studio))
        assertEquals(1, best?.first)
    }

    @Test
    fun `duration breaks ties between similar results`() {
        val track = ImportTrack("Intro", listOf("The xx"), durationMs = 127_000)
        val wrong = MatchCandidate("Intro", listOf("The xx"), 300)
        val right = MatchCandidate("Intro", listOf("The xx"), 128)
        assertTrue(TrackMatchScoring.score(track, right) > TrackMatchScoring.score(track, wrong))
    }

    @Test
    fun `video results are slightly penalised`() {
        val track = ImportTrack("Hello", listOf("Adele"), durationMs = 295_000)
        val song = MatchCandidate("Hello", listOf("Adele"), 295)
        val video = song.copy(isVideo = true)
        assertTrue(TrackMatchScoring.score(track, song) > TrackMatchScoring.score(track, video))
    }

    @Test
    fun `nothing above threshold gives null`() {
        val track = ImportTrack("Some Obscure Song", listOf("Tiny Band"), durationMs = 200_000)
        assertNull(
            TrackMatchScoring.best(
                track,
                listOf(MatchCandidate("Completely Different", listOf("Someone Else"), 180)),
            ),
        )
        assertNull(TrackMatchScoring.best(track, emptyList()))
    }

    @Test
    fun `missing artist info falls back on title and duration`() {
        val track = ImportTrack("Bohemian Rhapsody", emptyList(), durationMs = 354_000)
        val candidate = MatchCandidate("Bohemian Rhapsody", listOf("Queen"), 355)
        assertTrue(TrackMatchScoring.score(track, candidate) >= TrackMatchScoring.MATCH_THRESHOLD)
    }

    @Test
    fun `artist matching tolerates accents and joined names`() {
        assertEquals(1.0, TrackMatchScoring.artistSimilarity(listOf("Beyoncé"), listOf("Beyonce")) ?: 0.0, 0.0001)
        assertEquals(
            1.0,
            TrackMatchScoring.artistSimilarity(listOf("Simon", "Garfunkel"), listOf("Simon & Garfunkel")) ?: 0.0,
            0.0001,
        )
        assertNull(TrackMatchScoring.artistSimilarity(emptyList(), listOf("Anyone")))
    }

    @Test
    fun `non latin titles still compare`() {
        val track = ImportTrack("तुम ही हो", listOf("Arijit Singh"), durationMs = 262_000)
        val candidate = MatchCandidate("Tum Hi Ho", listOf("Arijit Singh"), 262)
        val exact = MatchCandidate("तुम ही हो", listOf("Arijit Singh"), 262)
        assertTrue(TrackMatchScoring.score(track, exact) > TrackMatchScoring.score(track, candidate))
        assertTrue(TrackMatchScoring.score(track, exact) > 0.9)
    }

    @Test
    fun `search query uses title and first two artists`() {
        assertEquals("Get Lucky Daft Punk Pharrell Williams", getLucky.copy(title = "Get Lucky").searchQuery())
        assertEquals("Get Lucky Daft Punk", getLucky.copy(title = "Get Lucky").searchQuery(maxArtists = 1))
    }
}
