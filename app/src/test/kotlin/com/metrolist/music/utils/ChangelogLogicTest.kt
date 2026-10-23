package com.metrolist.music.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChangelogLogicTest {
    @Test
    fun showsOnFirstRunAndAfterUpdate() {
        assertTrue(shouldShowChangelog(null, "1.2.0"))
        assertTrue(shouldShowChangelog("1.1.0", "1.2.0"))
    }

    @Test
    fun hiddenWhenVersionAlreadySeen() {
        assertFalse(shouldShowChangelog("1.2.0", "1.2.0"))
    }
}
