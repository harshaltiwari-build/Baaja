/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CsvPlaylistParserTest {
    @Test
    fun `exportify export with quoted fields`() {
        val csv =
            "﻿\"Track URI\",\"Track Name\",\"Album Name\",\"Artist Name(s)\",\"Release Date\",\"Duration (ms)\"\r\n" +
                "\"spotify:track:1\",\"Get Lucky (feat. Pharrell Williams and Nile Rodgers)\",\"Random Access Memories\",\"Daft Punk, Pharrell Williams, Nile Rodgers\",\"2013-05-17\",\"369626\"\r\n" +
                "\"spotify:track:2\",\"Say \"\"Hello\"\", Again\",\"Album, With Comma\",\"Someone\",\"2020\",\"200000\"\r\n"
        val result = CsvPlaylistParser.parse(csv)
        assertEquals(2, result.tracks.size)
        val first = result.tracks[0]
        assertEquals("Get Lucky (feat. Pharrell Williams and Nile Rodgers)", first.title)
        assertEquals(listOf("Daft Punk", "Pharrell Williams", "Nile Rodgers"), first.artists)
        assertEquals("Random Access Memories", first.album)
        assertEquals(369626L, first.durationMs)
        val second = result.tracks[1]
        assertEquals("Say \"Hello\", Again", second.title)
        assertEquals("Album, With Comma", second.album)
    }

    @Test
    fun `semicolon separated with clock durations`() {
        val csv = "Title;Artist;Duration\nYellow;Coldplay;4:26\nOne More Time;Daft Punk;5:20\n"
        val result = CsvPlaylistParser.parse(csv)
        assertEquals(2, result.tracks.size)
        assertEquals("Yellow", result.tracks[0].title)
        assertEquals(listOf("Coldplay"), result.tracks[0].artists)
        assertEquals(266_000L, result.tracks[0].durationMs)
    }

    @Test
    fun `line breaks inside quotes and blank rows`() {
        val csv = "Track Name,Artist Name(s)\n\"Two\nLines\",Band\n\n,Nobody\n"
        val result = CsvPlaylistParser.parse(csv)
        assertEquals(1, result.tracks.size)
        assertEquals("Two\nLines", result.tracks[0].title)
        assertEquals(1, result.skipped)
    }

    @Test
    fun `headerless two column sheet`() {
        val result = CsvPlaylistParser.parse("Yellow,Coldplay\nHello,Adele")
        assertEquals(listOf("Yellow", "Hello"), result.tracks.map { it.title })
        assertEquals(listOf("Adele"), result.tracks[1].artists)
        assertNull(result.tracks[1].durationMs)
    }

    @Test(expected = CsvPlaylistParser.MissingColumnException::class)
    fun `unrelated csv is rejected`() {
        CsvPlaylistParser.parse("id,price,stock\n1,2,3\n")
    }

    @Test
    fun `clock durations`() {
        assertEquals(65_000L, CsvPlaylistParser.parseClockDuration("1:05"))
        assertEquals(3_723_000L, CsvPlaylistParser.parseClockDuration("1:02:03"))
        assertNull(CsvPlaylistParser.parseClockDuration("abc"))
        assertNull(CsvPlaylistParser.parseClockDuration("125"))
    }
}
