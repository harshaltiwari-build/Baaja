package com.metrolist.music.viewmodels

import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.pages.HomePage
import org.junit.Assert.assertEquals
import org.junit.Test

/** YouTube Music's own Speed dial row is folded into Baaja's instead of showing a second Speed dial. */
class HomeSpeedDialSplitTest {
    private fun song(id: String) = SongItem(id = id, title = id, artists = emptyList(), thumbnail = "")

    private fun section(title: String, label: String?, vararg ids: String) =
        HomePage.Section(title = title, label = label, thumbnail = null, endpoint = null, items = ids.map(::song))

    private val speedDial = section("Speed dial", "HARSSHAL TIWRI", "a", "b")
    private val quickPicks = section("Quick picks", null, "c")
    private val editorial = section("Brb, Being Nostalgic!", "THROWBACK TO THE OG ERAS", "d")

    @Test
    fun `the row labelled with the account name becomes speed dial items`() {
        val (dial, rest) = splitYouTubeSpeedDial(listOf(speedDial, quickPicks, editorial), accountName = "Harsshal Tiwri", signedIn = true)
        assertEquals(listOf("a", "b"), dial.map { it.id })
        assertEquals(listOf(quickPicks, editorial), rest)
    }

    @Test
    fun `signed out or without an account name nothing is taken`() {
        val all = listOf(speedDial, quickPicks)
        assertEquals(emptyList<Any>() to all, splitYouTubeSpeedDial(all, accountName = "Harsshal Tiwri", signedIn = false))
        assertEquals(emptyList<Any>() to all, splitYouTubeSpeedDial(all, accountName = "  ", signedIn = true))
    }

    @Test
    fun `an account name that is only part of a label does not match`() {
        // Attack: a user called "Music" must not swallow "MUSIC THAT'S HOT AND HAPPENING!".
        val hot = section("India's biggest hits", "MUSIC THAT'S HOT AND HAPPENING!", "e")
        val (dial, rest) = splitYouTubeSpeedDial(listOf(hot), accountName = "Music", signedIn = true)
        assertEquals(emptyList<Any>(), dial)
        assertEquals(listOf(hot), rest)
    }

    @Test
    fun `extra spaces around the account name still match`() {
        val (dial, _) = splitYouTubeSpeedDial(listOf(section("Speed dial", " Harsshal  Tiwri ", "a")), accountName = "Harsshal Tiwri", signedIn = true)
        assertEquals(listOf("a"), dial.map { it.id })
    }
}
