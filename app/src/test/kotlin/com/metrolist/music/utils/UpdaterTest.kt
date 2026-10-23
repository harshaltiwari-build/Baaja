package com.metrolist.music.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UpdaterTest {
    @Test
    fun comparesReleaseTagsAgainstAppVersion() {
        assertTrue(Updater.isUpdateAvailable(currentVersion = "1.0.0", latestVersion = "v1.0.1"))
        assertTrue(Updater.isUpdateAvailable(currentVersion = "1.9.0", latestVersion = "1.10.0"))
        assertFalse(Updater.isUpdateAvailable(currentVersion = "1.0.0", latestVersion = "v1.0.0"))
        assertFalse(Updater.isUpdateAvailable(currentVersion = "1.2.0", latestVersion = "1.1.9"))
        assertEquals(0, Updater.compareVersions("1.0", "1.0.0"))
    }
}
