package com.metrolist.music.ui.screens

import com.metrolist.innertube.models.AlbumItem
import org.junit.Assert.assertEquals
import org.junit.Test

/** "New from artists you play": one card per album, newest artist order, at most six. */
class HomeReleaseAlbumsTest {
    @Test
    fun `an album shared by two artists appears once`() {
        // The 1.3.2 crash: both artists return the same album and the row got two equal keys.
        val shared = album("MPREb_aGmlqkANGBv")
        val result =
            newReleaseAlbums(
                artistIds = listOf("artistA", "artistB"),
                albumsByArtist = mapOf("artistA" to listOf(shared), "artistB" to listOf(shared)),
            )

        assertEquals(listOf(shared), result)
    }

    @Test
    fun `takes each artist's newest album in artist order`() {
        val a1 = album("a1")
        val a2 = album("a2")
        val b1 = album("b1")
        val result =
            newReleaseAlbums(
                artistIds = listOf("artistA", "artistB"),
                albumsByArtist = mapOf("artistA" to listOf(a1, a2), "artistB" to listOf(b1)),
            )

        assertEquals(listOf(a1, b1), result)
    }

    @Test
    fun `skips artists with no albums and keeps at most six`() {
        val albumsByArtist = (1..8).associate { "artist$it" to listOf(album("album$it")) }
        val result =
            newReleaseAlbums(
                artistIds = listOf("missing") + albumsByArtist.keys,
                albumsByArtist = albumsByArtist,
            )

        assertEquals((1..6).map { "album$it" }, result.map { it.id })
    }

    @Test
    fun `a duplicate does not use up one of the six places`() {
        val shared = album("shared")
        val albumsByArtist =
            mapOf("artist0" to listOf(shared), "artistDup" to listOf(shared)) +
                (1..6).associate { "artist$it" to listOf(album("album$it")) }
        val result =
            newReleaseAlbums(
                artistIds = listOf("artist0", "artistDup") + (1..6).map { "artist$it" },
                albumsByArtist = albumsByArtist,
            )

        assertEquals(listOf("shared", "album1", "album2", "album3", "album4", "album5"), result.map { it.id })
    }

    @Test
    fun `no artists gives an empty row`() {
        assertEquals(emptyList<AlbumItem>(), newReleaseAlbums(emptyList(), emptyMap()))
    }

    private fun album(id: String) =
        AlbumItem(browseId = id, playlistId = "OLAK_$id", title = id, artists = emptyList(), thumbnail = "")
}
