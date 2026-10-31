package com.metrolist.music.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.music.ui.component.home.BaajaChartArtistList
import com.metrolist.music.ui.component.home.BaajaChartTileRow
import com.metrolist.music.ui.theme.BaajaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The Charts page pieces YouTube Music shows for India: chart playlists and a ranked Top artists list. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = android.app.Application::class)
class ChartPartsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private fun playlist(id: String, title: String) =
        PlaylistItem(id = id, title = title, author = null, songCountText = null, thumbnail = "", playEndpoint = null, shuffleEndpoint = null, radioEndpoint = null)

    private fun artist(id: String, name: String, rank: Int?) =
        ArtistItem(id = id, title = name, thumbnail = null, shuffleEndpoint = null, radioEndpoint = null, chartPosition = rank, subscriberText = "2.31M subscribers")

    @Test
    fun `chart tiles use song chart names and open when tapped`() {
        val opened = mutableListOf<String>()
        compose.setContent {
            BaajaTheme(darkTheme = false) {
                BaajaChartTileRow(listOf(playlist("p1", "Trending 20 India"), playlist("p2", "Top 100 Music Videos India")), onOpen = { opened += it.id })
            }
        }
        compose.onNodeWithText("Trending 20 India").assertIsDisplayed()
        compose.onNodeWithText("Top 100 Songs India").assertIsDisplayed().performClick()
        assertEquals(0, compose.onAllNodesWithText("Videos", substring = true).fetchSemanticsNodes().size)
        assertEquals(listOf("p2"), opened)
    }

    @Test
    fun `top artists show rank, name and subscribers and open the artist`() {
        val opened = mutableListOf<String>()
        compose.setContent {
            BaajaTheme(darkTheme = false) {
                BaajaChartArtistList(listOf(artist("a1", "Alka Yagnik", 1), artist("a2", "Udit Narayan", 2)), onOpen = { opened += it.id })
            }
        }
        compose.onNodeWithText("Alka Yagnik").assertIsDisplayed()
        compose.onNodeWithText("2").assertIsDisplayed()
        assertEquals(2, compose.onAllNodesWithText("2.31M subscribers", substring = true).fetchSemanticsNodes().size)
        compose.onNodeWithText("Udit Narayan").performClick()
        assertEquals(listOf("a2"), opened)
    }

    @Test
    fun `artists without a rank are numbered by position`() {
        compose.setContent {
            BaajaTheme(darkTheme = false) {
                BaajaChartArtistList(listOf(artist("a1", "First", null), artist("a2", "Second", null)), onOpen = {})
            }
        }
        compose.onNodeWithText("1").assertIsDisplayed()
        compose.onNodeWithText("2").assertIsDisplayed()
    }
}
