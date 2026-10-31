package com.metrolist.music.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.music.R
import com.metrolist.music.ui.component.SpeedDialGridItem
import com.metrolist.music.ui.player.BaajaPlayerPill
import com.metrolist.music.ui.player.NextButton
import com.metrolist.music.ui.player.rememberBaajaPlayerColors
import com.metrolist.music.ui.theme.BaajaChip
import com.metrolist.music.ui.theme.BaajaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The 1.3.3 design changes: icon-only controls must still be usable with TalkBack. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = android.app.Application::class)
class DesignControlsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `player share and like are labelled icon buttons, and like reads liked once tapped`() {
        var liked by mutableStateOf(false)
        var shares = 0
        compose.setContent {
            BaajaTheme(darkTheme = false) {
                val colors = rememberBaajaPlayerColors(onImageBackground = false)
                androidx.compose.foundation.layout.Column {
                    BaajaPlayerPill(text = null, icon = R.drawable.share, contentDescription = "Share", onClick = { shares++ }, colors = colors, modifier = Modifier.size(44.dp))
                    BaajaPlayerPill(
                        text = null,
                        icon = if (liked) R.drawable.favorite else R.drawable.favorite_border,
                        contentDescription = if (liked) "Liked" else "Like",
                        onClick = { liked = !liked },
                        colors = colors,
                        active = liked,
                        modifier = Modifier.size(44.dp),
                    )
                }
            }
        }
        compose.onNodeWithContentDescription("Share").assertHasClickAction().performClick()
        assertEquals(1, shares)
        compose.onNodeWithContentDescription("Like").performClick()
        compose.onNodeWithContentDescription("Liked").assertIsDisplayed()
    }

    @Test
    fun `mini player next is labelled and disabled at the end of the queue`() {
        var canSkip by mutableStateOf(true)
        var skips = 0
        compose.setContent {
            BaajaTheme(darkTheme = false) {
                NextButton(enabled = canSkip, outlineColor = Color.Black, onSurfaceColor = Color.Black, onClick = { skips++ })
            }
        }
        compose.onNodeWithContentDescription("Next").assertIsEnabled().performClick()
        assertEquals(1, skips)
        canSkip = false
        compose.onNodeWithContentDescription("Next").assertIsNotEnabled().performClick()
        assertEquals("no skip past the last song", 1, skips)
    }

    @Test
    fun `speed dial tiles show the whole title for albums and the name for artists`() {
        val album = AlbumItem(browseId = "a", playlistId = "p", title = "Gehra Hua (From \"Dhurandhar\")", artists = null, thumbnail = "")
        val artist = ArtistItem(id = "ar", title = "Arijit Singh", thumbnail = null, shuffleEndpoint = null, radioEndpoint = null)
        compose.setContent {
            BaajaTheme(darkTheme = false) {
                androidx.compose.foundation.layout.Row {
                    SpeedDialGridItem(item = album, isPinned = false, modifier = Modifier.size(120.dp))
                    SpeedDialGridItem(item = artist, isPinned = true, modifier = Modifier.size(120.dp))
                }
            }
        }
        compose.onNodeWithText("Gehra Hua (From \"Dhurandhar\")").assertIsDisplayed()
        compose.onNodeWithText("Arijit Singh").assertIsDisplayed()
        compose.onNodeWithContentDescription("Pinned", substring = true, ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun `lighter chips still report which one is selected`() {
        var selected by mutableStateOf("Relax")
        compose.setContent {
            BaajaTheme(darkTheme = false) {
                androidx.compose.foundation.layout.Row {
                    listOf("Relax", "Party").forEach { label -> BaajaChip(label = label, selected = selected == label, onClick = { selected = label }) }
                }
            }
        }
        compose.onNodeWithText("Relax").assertIsSelected()
        compose.onNodeWithText("Party").assertIsNotSelected().performClick()
        compose.onNodeWithText("Party").assertIsSelected()
    }
}
