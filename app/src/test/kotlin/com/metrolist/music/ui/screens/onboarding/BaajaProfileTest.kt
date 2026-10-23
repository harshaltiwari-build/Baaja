package com.metrolist.music.ui.screens.onboarding

import org.junit.Assert.assertEquals
import org.junit.Test

class BaajaProfileTest {
    @Test
    fun initialUsesFirstLetterOrDigit() {
        assertEquals("A", profileInitial("  asha"))
        assertEquals("7", profileInitial("7up"))
        assertEquals("R", profileInitial("@rahul"))
    }

    @Test
    fun initialFallsBackToNote() {
        assertEquals("♪", profileInitial("   "))
        assertEquals("♪", profileInitial(""))
    }

    @Test
    fun nameResolutionOrder() {
        assertEquals("Edited", resolveProfileName(" Edited ", "Account", "Guest"))
        assertEquals("Account", resolveProfileName("  ", "Account", "Guest"))
        assertEquals("Guest", resolveProfileName("", "", "Guest"))
    }

    @Test
    fun sampleSizeKeepsAtLeastMaxPx() {
        assertEquals(1, sampleSizeFor(400, 300, 512))
        assertEquals(1, sampleSizeFor(1000, 800, 512))
        assertEquals(2, sampleSizeFor(1100, 800, 512))
        assertEquals(4, sampleSizeFor(4000, 3000, 512))
    }
}
