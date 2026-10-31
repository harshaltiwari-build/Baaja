package com.metrolist.music.ui

import com.metrolist.music.ui.component.home.musicChartTitle
import org.junit.Assert.assertEquals
import org.junit.Test

/** Baaja plays music only, so YouTube's video chart names are shown as song charts. */
class MusicChartTitleTest {
    @Test
    fun `video chart names become song chart names`() {
        assertEquals("Top 100 Songs India", musicChartTitle("Top 100 Music Videos India"))
        assertEquals("Daily Top Songs - India", musicChartTitle("Daily Top Music Videos - India"))
        assertEquals("Top Weekly Songs Tamil", musicChartTitle("Top Weekly Videos Tamil"))
        assertEquals("Song charts", musicChartTitle("Video charts"))
    }

    @Test
    fun `names without video stay as they are`() {
        assertEquals("Trending 20 India", musicChartTitle("Trending 20 India"))
        assertEquals("Languages", musicChartTitle("Languages"))
        assertEquals("Top artists", musicChartTitle("Top artists"))
    }

    @Test
    fun `case and spacing are handled`() {
        assertEquals("Top Songs", musicChartTitle("Top  VIDEOS "))
        assertEquals("Top Songs", musicChartTitle("Top video"))
    }
}

class SongChartTilesTest {
    private fun playlist(id: String, title: String) =
        com.metrolist.innertube.models.PlaylistItem(id = id, title = title, author = null, songCountText = null, thumbnail = "", playEndpoint = null, shuffleEndpoint = null, radioEndpoint = null)

    private val youTubeRow =
        listOf(playlist("t20", "Trending 20 India"), playlist("daily", "Daily Top Music Videos - India"), playlist("v100", "Top 100 Music Videos India"))

    @Test
    fun `in india the real top 100 songs chart replaces the converted video chart`() {
        val tiles = com.metrolist.music.ui.component.home.songChartTiles(youTubeRow, country = "IN")
        org.junit.Assert.assertEquals(listOf("PL4fGSI1pDJn4pTWyM3t61lOyZ6_4jcNOw", "t20", "daily"), tiles.map { it.id })
        org.junit.Assert.assertEquals("Top 100 Songs India", tiles.first().title)
    }

    @Test
    fun `elsewhere the row is unchanged`() {
        org.junit.Assert.assertEquals(youTubeRow, com.metrolist.music.ui.component.home.songChartTiles(youTubeRow, country = "US"))
    }
}
