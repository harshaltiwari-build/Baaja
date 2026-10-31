package com.metrolist.music.ui.player

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import com.metrolist.music.ui.theme.BaajaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The label as drawn, with the app's real English strings. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = android.app.Application::class)
class StreamQualityTextScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(quality: StreamQuality?) =
        compose.setContent { BaajaTheme(darkTheme = false) { StreamQualityText(quality, Color.Black) } }

    @Test
    fun `youtube stream`() {
        show(StreamQuality.Bitrate("OPUS", 148))
        compose.onNodeWithText("OPUS · 148 kbps").assertIsDisplayed()
    }

    @Test
    fun `lossless file`() {
        show(StreamQuality.Lossless("FLAC", 24, "96"))
        compose.onNodeWithText("FLAC · 24-bit · 96 kHz").assertIsDisplayed()
    }

    @Test
    fun `sample rate only and codec only`() {
        show(StreamQuality.Rate("VORBIS", "44.1"))
        compose.onNodeWithText("VORBIS · 44.1 kHz").assertIsDisplayed()
    }

    @Test
    fun `nothing known draws an empty label`() {
        show(null)
        compose.onNodeWithText("", useUnmergedTree = true).assertExists()
    }
}
