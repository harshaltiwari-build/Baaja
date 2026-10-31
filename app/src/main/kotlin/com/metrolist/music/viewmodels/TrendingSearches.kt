package com.metrolist.music.viewmodels

import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.pages.ChartsPage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Trending songs first, then top songs: eight unique, trimmed titles to tap on an empty search box. */
internal fun trendingTitles(sections: List<ChartsPage.ChartSection>): List<String> =
    (sections.filter { it.chartType == ChartsPage.ChartType.TRENDING } + sections.filter { it.chartType == ChartsPage.ChartType.TOP })
        .flatMap { it.items }
        .filterIsInstance<SongItem>()
        .map { it.title.trim().take(MAX_TITLE_CHARS) }
        .filter { it.isNotEmpty() }
        .distinct()
        .take(8)

private const val MAX_TITLE_CHARS = 100

/**
 * YouTube's charts often come back empty or unlabelled (the Charts screen falls back the same way), so
 * without trending or top songs there the song lists from the Home feed are used instead.
 */
internal suspend fun trendingSections(
    charts: suspend () -> Result<List<ChartsPage.ChartSection>>,
    homeFeed: suspend () -> Result<List<ChartsPage.ChartSection>>,
): Result<List<ChartsPage.ChartSection>> {
    val fromCharts = charts()
    if (fromCharts.getOrNull()?.let(::trendingTitles)?.isNotEmpty() == true) return fromCharts
    return homeFeed()
}

/**
 * Trending titles shared by every search screen and kept for [FRESH_MS], so opening search again and
 * again doesn't download the charts each time. A failed fetch isn't kept and is retried next time.
 */
internal class TrendingCache(
    private val now: () -> Long = System::currentTimeMillis,
    private val fetch: suspend () -> Result<List<ChartsPage.ChartSection>>,
) {
    private val mutex = Mutex()
    private var titles: List<String> = emptyList()
    private var fetchedAt: Long? = null

    suspend fun get(): List<String> =
        mutex.withLock {
            fetchedAt?.let { if (now() - it <= FRESH_MS) return titles }
            fetch()
                .onSuccess { sections ->
                    titles = trendingTitles(sections)
                    fetchedAt = now()
                }.map { titles }
                .getOrDefault(emptyList())
        }

    companion object {
        const val FRESH_MS = 30 * 60 * 1000L

        val shared =
            TrendingCache {
                trendingSections(
                    charts = { YouTube.getChartsPage().map { it.sections } },
                    homeFeed = { YouTube.home().map { it.toTrendingChartsPage()?.sections.orEmpty() } },
                )
            }
    }
}
