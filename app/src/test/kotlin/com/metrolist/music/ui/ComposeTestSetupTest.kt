package com.metrolist.music.ui

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Guards the screen-test setup itself, so a broken setup fails here instead of in every screen test. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = android.app.Application::class)
class ComposeTestSetupTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `compose content renders under robolectric`() {
        compose.setContent { Text("Baaja") }
        compose.onNodeWithText("Baaja").assertIsDisplayed()
    }
}
