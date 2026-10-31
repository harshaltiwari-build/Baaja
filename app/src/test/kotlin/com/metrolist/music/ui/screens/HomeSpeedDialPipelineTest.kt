package com.metrolist.music.ui.screens

import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.YTItem
import com.metrolist.innertube.pages.HomePage
import com.metrolist.music.viewmodels.buildSpeedDialItems
import com.metrolist.music.viewmodels.splitYouTubeSpeedDial
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** A signed-in YouTube Music feed through the same steps Home takes, checked for repeats. */
class HomeSpeedDialPipelineTest {
    private fun song(id: String) = SongItem(id = id, title = id, artists = emptyList(), thumbnail = "")

    private fun section(title: String, label: String?, ids: List<String>) =
        HomePage.Section(title = title, label = label, thumbnail = null, endpoint = null, items = ids.map(::song))

    @Test
    fun `one speed dial, and quick picks start with songs not on it`() {
        val feed =
            listOf(
                section("Speed dial", "Harsshal Tiwri", (1..6).map { "dial$it" }),
                section("Quick picks", null, (1..5).map { "dial$it" } + (1..12).map { "qp$it" }),
                section("Albums for you", null, listOf("al1")),
            )

        val (youTubeDial, rest) = splitYouTubeSpeedDial(feed, "Harsshal Tiwri", signedIn = true)
        val speedDial: List<YTItem> =
            buildSpeedDialItems(
                pinned = listOf(song("pinned")),
                keepListening = emptyList(),
                quickPicks = emptyList(),
                home = rest.flatMap { it.items },
                youTubeSpeedDial = youTubeDial,
            )
        val firstPage = speedDial.take(9).map { it.id }.toSet()
        val quickPicksShown = rest.first { it.title == "Quick picks" }.items.withoutSpeedDial(firstPage) { it.id }

        assertEquals("YouTube's Speed dial is not also a Home row", listOf("Quick picks", "Albums for you"), rest.map { it.title })
        assertEquals("pinned first, then YouTube's own dial", listOf("pinned") + (1..6).map { "dial$it" }, speedDial.take(7).map { it.id })
        assertEquals("no repeats on Speed dial", speedDial.size, speedDial.distinctBy { it.id }.size)
        assertTrue("no Quick picks song repeats the first Speed dial page", quickPicksShown.none { it.id in firstPage })
        assertTrue("Quick picks still has plenty", quickPicksShown.size >= 8)
    }
}
