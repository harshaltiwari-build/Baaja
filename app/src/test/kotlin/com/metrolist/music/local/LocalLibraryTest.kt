/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalLibraryTest {
    private fun song(
        id: Long,
        title: String,
        artist: String,
        album: String,
        albumId: Long,
        added: Long,
        track: Int = 0,
    ) = LocalAudio(
        mediaStoreId = id,
        uri = LocalAudio.contentUri(id),
        title = title,
        artist = artist,
        album = album,
        albumId = albumId,
        durationMs = 200_000,
        dateAddedSec = added,
        trackNumber = track,
    )

    private val songs =
        listOf(
            song(1, "Yellow", "Coldplay", "Parachutes", 10, added = 300, track = 5),
            song(2, "Shiver", "Coldplay", "Parachutes", 10, added = 100, track = 2),
            song(3, "Clocks", "Coldplay", "A Rush of Blood", 11, added = 200, track = 4),
            song(4, "Halo", "Beyoncé", "I Am... Sasha Fierce", 12, added = 400, track = 1),
            song(5, "untitled", "", "", 13, added = 50),
        )

    @Test
    fun `sort by title is case insensitive and reversible`() {
        assertEquals(
            listOf("Clocks", "Halo", "Shiver", "untitled", "Yellow"),
            LocalLibrary.sort(songs, LocalSort.TITLE, descending = false).map { it.title },
        )
        assertEquals(
            listOf("Yellow", "untitled", "Shiver", "Halo", "Clocks"),
            LocalLibrary.sort(songs, LocalSort.TITLE, descending = true).map { it.title },
        )
    }

    @Test
    fun `sort by artist groups albums in track order and puts untagged last`() {
        assertEquals(
            listOf("Halo", "Clocks", "Shiver", "Yellow", "untitled"),
            LocalLibrary.sort(songs, LocalSort.ARTIST, descending = false).map { it.title },
        )
    }

    @Test
    fun `date added lists newest first`() {
        assertEquals(
            listOf(4L, 1L, 3L, 2L, 5L),
            LocalLibrary.sort(songs, LocalSort.DATE_ADDED, descending = false).map { it.mediaStoreId },
        )
    }

    @Test
    fun `search ignores case and accents and needs every word`() {
        assertEquals(listOf("Halo"), LocalLibrary.filter(songs, "beyonce").map { it.title })
        assertEquals(listOf("Yellow", "Shiver"), LocalLibrary.filter(songs, "COLDPLAY parachutes").map { it.title })
        assertEquals(songs, LocalLibrary.filter(songs, "   "))
        assertTrue(LocalLibrary.filter(songs, "coldplay halo").isEmpty())
    }

    @Test
    fun `albums and artists are grouped`() {
        val albums = LocalLibrary.albums(songs)
        assertEquals(4, albums.size)
        val parachutes = albums.first { it.albumId == 10L }
        assertEquals(listOf("Shiver", "Yellow"), parachutes.songs.map { it.title })
        assertEquals("Coldplay", parachutes.artist)

        val artists = LocalLibrary.artists(songs)
        assertEquals(listOf("Beyoncé", "Coldplay", ""), artists.map { it.name })
        assertEquals(2, artists.first { it.name == "Coldplay" }.albumCount)
    }

    @Test
    fun `media store placeholders are cleaned`() {
        assertEquals("", LocalLibrary.cleanTag("<unknown>"))
        assertEquals("", LocalLibrary.cleanTag(null))
        assertEquals("Coldplay", LocalLibrary.cleanTag(" Coldplay "))
    }

    @Test
    fun `local ids are recognised`() {
        assertTrue(isLocalMediaId(LocalAudio.contentUri(42)))
        assertTrue(isLocalMediaId("file:///sdcard/Music/a.mp3"))
        assertFalse(isLocalMediaId("dQw4w9WgXcQ"))
        assertFalse(isLocalMediaId(null))
        assertEquals("content://media/external/audio/albumart/7", LocalAudio.albumArtUri(7))
    }
}
