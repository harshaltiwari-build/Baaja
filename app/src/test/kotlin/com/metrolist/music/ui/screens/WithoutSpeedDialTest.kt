package com.metrolist.music.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

/** Quick picks skip songs already on Speed dial's first page, unless that leaves too few to be useful. */
class WithoutSpeedDialTest {
    private val ids = (1..20).map { "s$it" }

    @Test
    fun `songs already on speed dial are skipped`() {
        assertEquals(ids.drop(9), ids.withoutSpeedDial(ids.take(9).toSet()) { it })
    }

    @Test
    fun `a row that would drop below eight keeps everything`() {
        val row = ids.take(10)
        assertEquals(row, row.withoutSpeedDial(row.take(3).toSet()) { it })
    }

    @Test
    fun `items without an id, like albums in a song filter, always stay`() {
        val row = listOf("album1", "s1", "album2") + ids.drop(10)
        val result = row.withoutSpeedDial(setOf("s1")) { if (it.startsWith("s")) it else null }
        assertEquals(row - "s1", result)
    }

    @Test
    fun `nothing on speed dial changes nothing`() {
        assertEquals(ids, ids.withoutSpeedDial(emptySet()) { it })
    }
}
