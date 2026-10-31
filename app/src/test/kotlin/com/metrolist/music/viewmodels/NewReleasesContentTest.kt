package com.metrolist.music.viewmodels

import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.pages.NewReleasesPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** What Baaja's New releases shows from YouTube Music's New releases page. */
class NewReleasesContentTest {
    private fun album(id: String, explicit: Boolean = false) =
        AlbumItem(browseId = id, playlistId = "OLAK_$id", title = id, artists = emptyList(), thumbnail = "", explicit = explicit)

    private fun video(id: String, explicit: Boolean = false) = SongItem(id = id, title = id, artists = emptyList(), thumbnail = "", explicit = explicit)

    private val featured = PlaylistItem(id = "RDCLAK", title = "RELEASED", author = null, songCountText = null, thumbnail = "", playEndpoint = null, shuffleEndpoint = null, radioEndpoint = null)

    @Test
    fun `youtube's order is kept`() {
        val page = NewReleasesPage(listOf(featured), listOf(album("z"), album("a"), album("m")), listOf(video("v1")))
        val content = newReleasesContent(page, hideExplicit = false, hideVideos = false)!!
        assertEquals(listOf("z", "a", "m"), content.albums.map { it.id })
        assertEquals(listOf("RELEASED"), content.featured.map { it.title })
        assertEquals(listOf("v1"), content.videos.map { it.id })
    }

    @Test
    fun `explicit albums and videos follow the hide explicit setting`() {
        val page = NewReleasesPage(emptyList(), listOf(album("clean"), album("rude", explicit = true)), listOf(video("ok"), video("rude", explicit = true)))
        val content = newReleasesContent(page, hideExplicit = true, hideVideos = false)!!
        assertEquals(listOf("clean"), content.albums.map { it.id })
        assertEquals(listOf("ok"), content.videos.map { it.id })
    }

    @Test
    fun `the music videos row follows the hide video songs setting`() {
        val page = NewReleasesPage(emptyList(), listOf(album("a")), listOf(video("v1")))
        assertEquals(emptyList<SongItem>(), newReleasesContent(page, hideExplicit = false, hideVideos = true)!!.videos)
    }

    @Test
    fun `without albums the old sources are used instead`() {
        assertNull(newReleasesContent(NewReleasesPage(listOf(featured), emptyList(), listOf(video("v"))), hideExplicit = false, hideVideos = false))
        assertNull(newReleasesContent(null, hideExplicit = false, hideVideos = false))
    }

    @Test
    fun `an album listed twice is shown once`() {
        val page = NewReleasesPage(emptyList(), listOf(album("a"), album("a"), album("b")), emptyList())
        assertEquals(listOf("a", "b"), newReleasesContent(page, hideExplicit = false, hideVideos = false)!!.albums.map { it.id })
    }
}
