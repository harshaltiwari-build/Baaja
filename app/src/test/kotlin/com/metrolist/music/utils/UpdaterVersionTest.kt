/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdaterVersionTest {
    @Test
    fun `a newer release tag counts as an update`() {
        assertTrue(Updater.isUpdateAvailable("1.1.1", "v1.1.2"))
        assertTrue(Updater.isUpdateAvailable("1.1.9", "v1.2.0"))
        assertTrue(Updater.isUpdateAvailable("1.9.0", "v2.0.0"))
    }

    @Test
    fun `the same or an older tag is not an update`() {
        assertFalse(Updater.isUpdateAvailable("1.1.2", "v1.1.2"))
        assertFalse(Updater.isUpdateAvailable("1.1.2", "v1.1.1"))
        assertFalse(Updater.isUpdateAvailable("1.1.2", "v1.0.9"))
    }

    @Test
    fun `versions with different lengths compare by value`() {
        assertEquals(0, Updater.compareVersions("1.1", "1.1.0"))
        assertEquals(1, Updater.compareVersions("v1.10.0", "1.9.9"))
        assertEquals(-1, Updater.compareVersions("1.1.1", "v1.1.10"))
    }
}
