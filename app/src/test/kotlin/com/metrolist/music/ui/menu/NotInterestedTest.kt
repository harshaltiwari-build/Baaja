package com.metrolist.music.ui.menu

import com.metrolist.music.db.entities.SongEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class NotInterestedTest {
    @Test
    fun `only a signed in account can say not interested`() {
        assertFalse(isSignedIn(null))
        assertFalse(isSignedIn(""))
        assertFalse(isSignedIn("VISITOR_INFO1_LIVE=abc"))
        assertFalse("name only, no value", isSignedIn("SAPISID"))
        assertTrue(isSignedIn("SAPISID=abc; HSID=def"))
    }

    @Test
    fun `a liked song stops being liked here too`() {
        // Attack finding: YouTube drops the like on a thumbs down, but Baaja kept showing it as liked.
        val liked = SongEntity(id = "s", title = "t", liked = true, likedDate = LocalDateTime.now())
        val after = songAfterNotInterested(liked)
        assertFalse(after.liked)
        assertNull(after.likedDate)
        assertEquals(liked.inLibrary, after.inLibrary)
    }

    @Test
    fun `a song that was not liked is left alone`() {
        val song = SongEntity(id = "s", title = "t")
        assertSame(song, songAfterNotInterested(song))
    }
}
