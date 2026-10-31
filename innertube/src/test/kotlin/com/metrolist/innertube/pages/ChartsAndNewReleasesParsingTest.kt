package com.metrolist.innertube.pages

import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.response.BrowseResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * YouTube Music's Charts and New releases pages, read from real responses saved on 2026-11-01 (India).
 * They check the page format, not the day's songs: the app always loads these pages live.
 */
class ChartsAndNewReleasesParsingTest {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    private fun fixture(name: String): BrowseResponse =
        json.decodeFromString(javaClass.classLoader!!.getResource("fixtures/$name")!!.readText())

    // ---- Charts ----

    @Test
    fun `charts made of chart playlists and artists are read, not dropped`() {
        // 1.3.2 kept only songs, found none, and told Indian users charts weren't available.
        val page = YouTube.parseChartsPage(fixture("charts_in.json"))
        assertEquals(listOf("Video charts", "Languages", "Top artists"), page.sections.map { it.title })
    }

    @Test
    fun `chart playlists keep their names and ids`() {
        val page = YouTube.parseChartsPage(fixture("charts_in.json"))
        val video = page.sections.first { it.title == "Video charts" }
        assertEquals(ChartsPage.ChartType.PLAYLISTS, video.chartType)
        val playlists = video.items.map { it as PlaylistItem }
        assertEquals(listOf("Trending 20 India", "Daily Top Music Videos - India", "Top 100 Music Videos India"), playlists.map { it.title })
        assertTrue(playlists.all { it.id.isNotBlank() && !it.id.startsWith("VL") })
        assertEquals(7, page.sections.first { it.title == "Languages" }.items.size)
    }

    @Test
    fun `top artists come with their rank and subscribers`() {
        val artists = YouTube.parseChartsPage(fixture("charts_in.json")).sections.first { it.title == "Top artists" }
        assertEquals(ChartsPage.ChartType.ARTISTS, artists.chartType)
        val list = artists.items.map { it as ArtistItem }
        assertEquals(40, list.size)
        assertEquals(listOf("Alka Yagnik", "Udit Narayan", "Shreya Ghoshal", "Arijit Singh"), list.take(4).map { it.title })
        assertEquals((1..40).toList(), list.map { it.chartPosition })
        assertEquals("UCptBkLZ6XRxoyn8SkUMc_Iw", list.first().id)
        assertTrue(list.first().subscriberText!!.contains("subscribers"))
    }

    @Test
    fun `an empty response gives no sections instead of crashing`() {
        assertEquals(emptyList<ChartsPage.ChartSection>(), YouTube.parseChartsPage(json.decodeFromString<BrowseResponse>("""{"responseContext":{}}""")).sections)
    }

    // ---- New releases ----

    @Test
    fun `new releases has the featured playlists, albums and singles, and music videos in youtube's order`() {
        val page = YouTube.parseNewReleasesPage(fixture("new_releases_in.json"))
        assertEquals(listOf("RELEASED"), page.featured.map { it.title })
        assertEquals(24, page.albums.size)
        assertEquals(listOf("Alfaaz - Acoustic", "Anand Sahib Kirtan (Raag Ramkali)"), page.albums.take(2).map { it.title })
        assertTrue(page.albums.all { it.playlistId.isNotBlank() })
        assertEquals(listOf("Break Up in Reverse", "Ishq Tamasha"), page.videos.take(2).map { it.title })
    }

    @Test
    fun `new releases tolerates a missing section`() {
        val page = YouTube.parseNewReleasesPage(json.decodeFromString<BrowseResponse>("""{"responseContext":{}}"""))
        assertEquals(emptyList<PlaylistItem>(), page.featured)
        assertEquals(emptyList<AlbumItem>(), page.albums)
        assertEquals(emptyList<SongItem>(), page.videos)
    }
}
