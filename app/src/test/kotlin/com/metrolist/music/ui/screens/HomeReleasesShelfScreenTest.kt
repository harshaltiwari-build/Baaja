package com.metrolist.music.ui.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.music.ui.component.home.TasteCard
import com.metrolist.music.ui.component.home.TasteShelf
import com.metrolist.music.ui.component.home.TasteStage
import com.metrolist.music.ui.component.home.tasteHome
import com.metrolist.music.ui.theme.BaajaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The Home taste block drawn for real, with the album data that crashed 1.3.2. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = android.app.Application::class)
class HomeReleasesShelfScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val shared = AlbumItem(browseId = "MPREb_aGmlqkANGBv", playlistId = "OLAK_x", title = "Shared album", artists = emptyList(), thumbnail = "")
    private val albumsByArtist = mapOf("artistA" to listOf(shared), "artistB" to listOf(shared))

    @Test
    fun `releases row with a shared album renders it once`() {
        render(newReleaseAlbums(listOf("artistA", "artistB"), albumsByArtist))

        compose.waitForIdle()
        assertEquals(1, compose.onAllNodesWithText("Shared album").fetchSemanticsNodes().size)
    }

    @Test
    fun `without the fix the same data crashes the row`() {
        // Proves this test can see the original bug: two cards with one key.
        val undeduplicated = listOf("artistA", "artistB").flatMap { albumsByArtist[it].orEmpty().take(1) }

        assertThrows(IllegalArgumentException::class.java) {
            render(undeduplicated)
            compose.waitForIdle()
        }
    }

    private fun render(albums: List<AlbumItem>) {
        val shelf =
            TasteShelf(
                title = "New from artists you play",
                label = null,
                cards = albums.map { TasteCard("a_${it.id}", it.title, "", it.thumbnail) },
                onClick = {},
            )
        compose.setContent {
            BaajaTheme(darkTheme = false) {
                LazyColumn {
                    tasteHome(
                        stage = TasteStage.HISTORY,
                        heroArtists = emptyList(),
                        onPlayMix = {},
                        mixes = emptyList(),
                        mixesLabel = null,
                        historyShelf = null,
                        releasesShelf = shelf,
                        becauseHistory = null,
                        becausePicks = emptyList(),
                        discoverShelf = null,
                        tints = emptyList(),
                    )
                }
            }
        }
    }
}
