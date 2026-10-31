package com.metrolist.music.viewmodels

import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.pages.ChartsPage
import com.metrolist.innertube.pages.ChartsPage.ChartType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class TrendingSearchesTest {
    private fun song(title: String) = SongItem(id = title, title = title, artists = emptyList(), thumbnail = "")

    private fun chart(type: ChartType, vararg titles: String) =
        ChartsPage.ChartSection(title = type.name, items = titles.map(::song), chartType = type)

    // ---- unit: picking titles ----

    @Test
    fun `trending first, then top songs, eight unique titles`() {
        val charts =
            listOf(
                chart(ChartType.TOP, "t1", "t2", "shared", "t3", "t4", "t5", "t6"),
                chart(ChartType.TRENDING, "n1", "shared", "n2"),
                chart(ChartType.GENRE, "g1"),
            )
        assertEquals(listOf("n1", "shared", "n2", "t1", "t2", "t3", "t4", "t5"), trendingTitles(charts))
    }

    @Test
    fun `blank titles and non-songs are skipped`() {
        val album = AlbumItem(browseId = "a", playlistId = "p", title = "Album", artists = null, thumbnail = "")
        val charts =
            listOf(
                ChartsPage.ChartSection("Trending", listOf(song(" "), album, song("ok")), ChartType.TRENDING),
            )
        assertEquals(listOf("ok"), trendingTitles(charts))
    }

    @Test
    fun `titles are trimmed and capped so a hostile chart cannot flood the list`() {
        val charts = listOf(chart(ChartType.TRENDING, "  Saiyaara  ", "x".repeat(500)))
        assertEquals(listOf("Saiyaara", "x".repeat(100)), trendingTitles(charts))
    }

    // ---- integration: loading with a shared cache ----

    @Test
    fun `charts are fetched once and reused while fresh`() =
        runBlocking {
            var now = 0L
            val calls = AtomicInteger()
            val cache = TrendingCache(now = { now }) {
                calls.incrementAndGet()
                Result.success(listOf(chart(ChartType.TRENDING, "n1")))
            }
            repeat(20) { assertEquals(listOf("n1"), cache.get()) } // search opened and closed 20 times
            assertEquals(1, calls.get())

            now += TrendingCache.FRESH_MS + 1
            cache.get()
            assertEquals(2, calls.get())
        }

    @Test
    fun `a failed fetch shows nothing and is retried next time`() =
        runBlocking {
            val calls = AtomicInteger()
            val cache = TrendingCache(now = { 0L }) {
                if (calls.incrementAndGet() == 1) Result.failure(RuntimeException("offline")) else Result.success(listOf(chart(ChartType.TOP, "t1")))
            }
            assertEquals(emptyList<String>(), cache.get())
            assertEquals(listOf("t1"), cache.get())
        }

    // ---- end-to-end finding: YouTube's charts often come back empty ----

    @Test
    fun `empty charts fall back to the home feed's songs`() =
        runBlocking {
            var homeCalls = 0
            val sections =
                trendingSections(
                    charts = { Result.success(listOf(chart(ChartType.GENRE, "g1"))) },
                    homeFeed = { homeCalls++; Result.success(listOf(chart(ChartType.TRENDING, "h1", "h2"))) },
                ).getOrThrow()
            assertEquals(1, homeCalls)
            assertEquals(listOf("h1", "h2"), trendingTitles(sections))
        }

    @Test
    fun `failing charts fall back too, and both failing is a failure`() =
        runBlocking {
            val fromHome = trendingSections(charts = { Result.failure(RuntimeException("x")) }, homeFeed = { Result.success(listOf(chart(ChartType.TRENDING, "h1"))) })
            assertEquals(listOf("h1"), trendingTitles(fromHome.getOrThrow()))
            val none = trendingSections(charts = { Result.failure(RuntimeException("x")) }, homeFeed = { Result.failure(RuntimeException("y")) })
            assertEquals(true, none.isFailure)
        }

    @Test
    fun `good charts never touch the home feed`() =
        runBlocking {
            var homeCalls = 0
            trendingSections(charts = { Result.success(listOf(chart(ChartType.TOP, "t1"))) }, homeFeed = { homeCalls++; Result.success(emptyList()) })
            assertEquals(0, homeCalls)
        }
}
