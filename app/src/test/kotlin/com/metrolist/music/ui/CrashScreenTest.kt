package com.metrolist.music.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.metrolist.music.ui.screens.CrashScreen
import com.metrolist.music.ui.theme.BaajaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = android.app.Application::class)
class CrashScreenTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `the report button is shown and only reports when tapped`() {
        var reports = 0
        var shares = 0
        compose.setContent {
            BaajaTheme(darkTheme = false) {
                CrashScreen(crashLog = "java.lang.RuntimeException: boom", onClose = {}, onShare = { shares++ }, onReport = { reports++ })
            }
        }
        compose.onNodeWithText("java.lang.RuntimeException: boom").assertIsDisplayed()
        assertEquals("nothing is sent by just opening the crash screen", 0, reports)

        compose.onNodeWithText("Report this crash on GitHub").performClick()
        assertEquals(1, reports)
        assertEquals(0, shares)
    }
}
